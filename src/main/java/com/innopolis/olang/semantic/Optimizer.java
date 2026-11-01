package com.innopolis.olang.semantic;

import com.innopolis.olang.lexer.Span;
import com.innopolis.olang.parser.ast.Assignment;
import com.innopolis.olang.parser.ast.BodyElement;
import com.innopolis.olang.parser.ast.BooleanLiteral;
import com.innopolis.olang.parser.ast.ClassDeclaration;
import com.innopolis.olang.parser.ast.ConstructorDeclaration;
import com.innopolis.olang.parser.ast.ConstructorInvocation;
import com.innopolis.olang.parser.ast.Expression;
import com.innopolis.olang.parser.ast.ExpressionStatement;
import com.innopolis.olang.parser.ast.FieldAccess;
import com.innopolis.olang.parser.ast.IfStatement;
import com.innopolis.olang.parser.ast.IntegerLiteral;
import com.innopolis.olang.parser.ast.MemberDeclaration;
import com.innopolis.olang.parser.ast.MethodCall;
import com.innopolis.olang.parser.ast.MethodDeclaration;
import com.innopolis.olang.parser.ast.NameReference;
import com.innopolis.olang.parser.ast.Program;
import com.innopolis.olang.parser.ast.RealLiteral;
import com.innopolis.olang.parser.ast.ReturnStatement;
import com.innopolis.olang.parser.ast.ThisExpression;
import com.innopolis.olang.parser.ast.VariableDeclaration;
import com.innopolis.olang.parser.ast.WhileLoop;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Оптимизатор: четыре преобразования, которые дерево меняют.
 *
 * 1) свёртка констант - Integer(2).Plus(Integer(3)) превращается в 5;
 * 2) упрощение if и while с постоянным условием;
 * 3) удаление недостижимого кода после return;
 * 4) удаление объявлений переменных, которые нигде не используются.
 *
 * Узлы дерева неизменяемы - у них все поля final. Поэтому оптимизатор не
 * правит дерево на месте, а строит новое: каждый метод принимает узел и
 * возвращает либо его же, либо новый. Так проще не ошибиться, и старое дерево
 * остаётся целым, если его захочется напечатать для сравнения.
 *
 * Проходы повторяются, пока дерево меняется: свёртка констант может сделать
 * условие постоянным, упрощение if - выкинуть использование переменной, после
 * чего переменная станет лишней. За один проход такую цепочку не поймать.
 */
public final class Optimizer {

    /** Что именно было применено - для отчёта и для демонстрации. */
    private final List<String> applied = new ArrayList<>();

    private boolean changed;

    public List<String> getApplied() {
        return applied;
    }

    public Program optimize(Program program) {
        Program result = program;
        for (int pass = 0; pass < 10; pass++) {
            changed = false;
            result = rewriteProgram(result);
            if (!changed) {
                break;
            }
        }
        return result;
    }

    private Program rewriteProgram(Program program) {
        List<ClassDeclaration> classes = new ArrayList<>();
        for (ClassDeclaration declaration : program.getClasses()) {
            classes.add(rewriteClass(declaration));
        }
        return new Program(classes);
    }

    private ClassDeclaration rewriteClass(ClassDeclaration declaration) {
        List<MemberDeclaration> members = new ArrayList<>();
        for (MemberDeclaration member : declaration.getMembers()) {
            members.add(rewriteMember(member));
        }
        return new ClassDeclaration(declaration.getName(), declaration.getBaseName(),
                members, declaration.getSpan());
    }

    private MemberDeclaration rewriteMember(MemberDeclaration member) {
        if (member instanceof VariableDeclaration field) {
            return new VariableDeclaration(field.getName(),
                    foldExpression(field.getInitializer()), field.getSpan());
        }
        if (member instanceof MethodDeclaration method) {
            if (method.isAbstract()) {
                return method;
            }
            return new MethodDeclaration(method.getName(), method.getParameters(),
                    method.getReturnTypeName(), rewriteMethodBody(method.getBody()),
                    method.getSpan());
        }
        if (member instanceof ConstructorDeclaration constructor) {
            return new ConstructorDeclaration(constructor.getParameters(),
                    rewriteMethodBody(constructor.getBody()), constructor.getSpan());
        }
        return member;
    }

    /**
     * Тело метода целиком. Удаление неиспользуемых переменных делается здесь,
     * а не внутри каждого блока: переменная, объявленная снаружи, может
     * использоваться во вложенном цикле, и смотреть надо на всё тело сразу.
     */
    private List<BodyElement> rewriteMethodBody(List<BodyElement> body) {
        List<BodyElement> rewritten = rewriteBody(body);
        Set<String> used = new HashSet<>();
        collectUsedNames(rewritten, used);
        return removeUnusedVariables(rewritten, used);
    }

    private List<BodyElement> rewriteBody(List<BodyElement> body) {
        List<BodyElement> result = new ArrayList<>();
        for (BodyElement element : body) {
            result.addAll(rewriteElement(element));
        }
        return removeUnreachable(result);
    }

    private List<BodyElement> rewriteElement(BodyElement element) {
        if (element instanceof VariableDeclaration declaration) {
            return List.of(new VariableDeclaration(declaration.getName(),
                    foldExpression(declaration.getInitializer()), declaration.getSpan()));
        }
        if (element instanceof Assignment assignment) {
            return List.of(new Assignment(assignment.getTarget(),
                    foldExpression(assignment.getValue()), assignment.getSpan()));
        }
        if (element instanceof ExpressionStatement statement) {
            return List.of(new ExpressionStatement(
                    foldExpression(statement.getExpression()), statement.getSpan()));
        }
        if (element instanceof ReturnStatement statement) {
            Expression value = statement.getValue() == null
                    ? null
                    : foldExpression(statement.getValue());
            return List.of(new ReturnStatement(value, statement.getSpan()));
        }
        if (element instanceof WhileLoop loop) {
            return rewriteWhile(loop);
        }
        if (element instanceof IfStatement statement) {
            return rewriteIf(statement);
        }
        return List.of(element);
    }

    /** Оптимизация 2a: while с условием false не выполнится ни разу. */
    private List<BodyElement> rewriteWhile(WhileLoop loop) {
        Expression condition = foldExpression(loop.getCondition());
        Boolean constant = constantBoolean(condition);
        if (Boolean.FALSE.equals(constant)) {
            record(loop.getSpan(), "removed a 'while' loop whose condition is always false");
            return List.of();
        }
        return List.of(new WhileLoop(condition, rewriteBody(loop.getBody()), loop.getSpan()));
    }

    /** Оптимизация 2b: у if с постоянным условием одна ветка не нужна вовсе. */
    private List<BodyElement> rewriteIf(IfStatement statement) {
        Expression condition = foldExpression(statement.getCondition());
        Boolean constant = constantBoolean(condition);

        if (Boolean.TRUE.equals(constant)) {
            record(statement.getSpan(),
                    "'if' condition is always true: kept the 'then' branch only");
            return rewriteBody(statement.getThenBody());
        }
        if (Boolean.FALSE.equals(constant)) {
            record(statement.getSpan(), statement.getElseBody() == null
                    ? "'if' condition is always false: removed the statement"
                    : "'if' condition is always false: kept the 'else' branch only");
            return statement.getElseBody() == null
                    ? List.of()
                    : rewriteBody(statement.getElseBody());
        }

        return List.of(new IfStatement(condition,
                rewriteBody(statement.getThenBody()),
                statement.getElseBody() == null ? null : rewriteBody(statement.getElseBody()),
                statement.getSpan()));
    }

    /** Оптимизация 3: после return в том же теле ничего выполниться не может. */
    private List<BodyElement> removeUnreachable(List<BodyElement> body) {
        for (int i = 0; i < body.size(); i++) {
            if (!(body.get(i) instanceof ReturnStatement statement)) {
                continue;
            }
            if (i == body.size() - 1) {
                return body;
            }
            record(statement.getSpan(),
                    "removed " + (body.size() - i - 1) + " unreachable statement(s) after 'return'");
            return new ArrayList<>(body.subList(0, i + 1));
        }
        return body;
    }

    /** Оптимизация 4: объявление, которым никто не пользуется, можно убрать. */
    private List<BodyElement> removeUnusedVariables(List<BodyElement> body, Set<String> used) {
        List<BodyElement> result = new ArrayList<>();
        for (BodyElement element : body) {
            if (element instanceof VariableDeclaration declaration
                    && !used.contains(declaration.getName())
                    && isPure(declaration.getInitializer())) {
                record(declaration.getSpan(),
                        "removed unused variable '" + declaration.getName() + "'");
                continue;
            }
            if (element instanceof WhileLoop loop) {
                result.add(new WhileLoop(loop.getCondition(),
                        removeUnusedVariables(loop.getBody(), used), loop.getSpan()));
                continue;
            }
            if (element instanceof IfStatement statement) {
                result.add(new IfStatement(statement.getCondition(),
                        removeUnusedVariables(statement.getThenBody(), used),
                        statement.getElseBody() == null
                                ? null
                                : removeUnusedVariables(statement.getElseBody(), used),
                        statement.getSpan()));
                continue;
            }
            result.add(element);
        }
        return result;
    }

    /**
     * Имена, которые где-либо читаются или которым что-либо присваивается.
     * Присваивание тоже считается использованием: если убрать объявление,
     * присваивание останется без переменной.
     */
    private void collectUsedNames(List<BodyElement> body, Set<String> used) {
        for (BodyElement element : body) {
            if (element instanceof VariableDeclaration declaration) {
                collectUsedNames(declaration.getInitializer(), used);
            } else if (element instanceof Assignment assignment) {
                used.add(assignment.getTarget());
                collectUsedNames(assignment.getValue(), used);
            } else if (element instanceof ExpressionStatement statement) {
                collectUsedNames(statement.getExpression(), used);
            } else if (element instanceof ReturnStatement statement) {
                if (statement.getValue() != null) {
                    collectUsedNames(statement.getValue(), used);
                }
            } else if (element instanceof WhileLoop loop) {
                collectUsedNames(loop.getCondition(), used);
                collectUsedNames(loop.getBody(), used);
            } else if (element instanceof IfStatement statement) {
                collectUsedNames(statement.getCondition(), used);
                collectUsedNames(statement.getThenBody(), used);
                if (statement.getElseBody() != null) {
                    collectUsedNames(statement.getElseBody(), used);
                }
            }
        }
    }

    private void collectUsedNames(Expression expression, Set<String> used) {
        if (expression instanceof NameReference reference) {
            used.add(reference.getName());
        } else if (expression instanceof MethodCall call) {
            collectUsedNames(call.getReceiver(), used);
            call.getArguments().forEach(argument -> collectUsedNames(argument, used));
        } else if (expression instanceof ConstructorInvocation invocation) {
            invocation.getArguments().forEach(argument -> collectUsedNames(argument, used));
        } else if (expression instanceof FieldAccess access) {
            collectUsedNames(access.getReceiver(), used);
        }
    }

    /**
     * Можно ли выбросить выражение, не изменив поведение программы.
     * Осторожно: вызов метода может что-нибудь напечатать или изменить поле,
     * поэтому вызовы считаем нечистыми всегда, даже Plus.
     */
    private boolean isPure(Expression expression) {
        if (expression instanceof IntegerLiteral
                || expression instanceof RealLiteral
                || expression instanceof BooleanLiteral
                || expression instanceof ThisExpression
                || expression instanceof NameReference) {
            return true;
        }
        if (expression instanceof ConstructorInvocation invocation) {
            boolean valueClass = invocation.getClassName().equals("Integer")
                    || invocation.getClassName().equals("Real")
                    || invocation.getClassName().equals("Boolean");
            return valueClass && invocation.getArguments().stream().allMatch(this::isPure);
        }
        return false;
    }

    // ---------- оптимизация 1: свёртка констант ----------

    private Expression foldExpression(Expression expression) {
        if (expression instanceof MethodCall call) {
            Expression receiver = foldExpression(call.getReceiver());
            List<Expression> arguments = new ArrayList<>();
            for (Expression argument : call.getArguments()) {
                arguments.add(foldExpression(argument));
            }
            Expression folded = foldCall(receiver, call.getMethodName(), arguments, call.getSpan());
            if (folded != null) {
                return folded;
            }
            return new MethodCall(receiver, call.getMethodName(), arguments, call.getSpan());
        }
        if (expression instanceof ConstructorInvocation invocation) {
            List<Expression> arguments = new ArrayList<>();
            for (Expression argument : invocation.getArguments()) {
                arguments.add(foldExpression(argument));
            }
            return new ConstructorInvocation(invocation.getClassName(), arguments,
                    invocation.getSpan());
        }
        if (expression instanceof FieldAccess access) {
            return new FieldAccess(foldExpression(access.getReceiver()), access.getName(),
                    access.getSpan());
        }
        return expression;
    }

    /** Возвращает посчитанный литерал или null, если вычислить нельзя. */
    private Expression foldCall(Expression receiver, String name, List<Expression> arguments,
                                Span span) {
        Boolean leftBool = constantBoolean(receiver);
        if (leftBool != null) {
            Expression result = foldBoolean(leftBool, name, arguments, span);
            return report(result, span);
        }

        Integer leftInt = constantInt(receiver);
        Double leftReal = constantReal(receiver);
        if (leftInt == null && leftReal == null) {
            return null;
        }

        if (arguments.isEmpty()) {
            return report(foldUnary(leftInt, leftReal, name, span), span);
        }
        if (arguments.size() != 1) {
            return null;
        }

        Expression argument = arguments.get(0);
        Integer rightInt = constantInt(argument);
        Double rightReal = constantReal(argument);
        if (rightInt == null && rightReal == null) {
            return null;
        }

        // Целыми считаем только если оба целые, иначе переходим к вещественным.
        if (leftInt != null && rightInt != null) {
            return report(foldIntegers(leftInt, rightInt, name, span), span);
        }
        double left = leftInt != null ? leftInt : leftReal;
        double right = rightInt != null ? rightInt : rightReal;
        return report(foldReals(left, right, name, span), span);
    }

    private Expression foldBoolean(boolean left, String name, List<Expression> arguments,
                                   Span span) {
        if (name.equals("Not") && arguments.isEmpty()) {
            return new BooleanLiteral(!left, span);
        }
        if (name.equals("toInteger") && arguments.isEmpty()) {
            return new IntegerLiteral(left ? 1 : 0, span);
        }
        if (arguments.size() != 1) {
            return null;
        }
        Boolean right = constantBoolean(arguments.get(0));
        if (right == null) {
            return null;
        }
        return switch (name) {
            case "And" -> new BooleanLiteral(left && right, span);
            case "Or" -> new BooleanLiteral(left || right, span);
            case "Xor" -> new BooleanLiteral(left ^ right, span);
            case "Equal" -> new BooleanLiteral(left == right.booleanValue(), span);
            default -> null;
        };
    }

    private Expression foldUnary(Integer leftInt, Double leftReal, String name, Span span) {
        if (name.equals("UnaryMinus")) {
            return leftInt != null
                    ? new IntegerLiteral(-leftInt, span)
                    : new RealLiteral(-leftReal, span);
        }
        if (name.equals("toReal") && leftInt != null) {
            return new RealLiteral(leftInt, span);
        }
        if (name.equals("toInteger") && leftReal != null) {
            return new IntegerLiteral((int) (double) leftReal, span);
        }
        return null;
    }

    private Expression foldIntegers(int left, int right, String name, Span span) {
        return switch (name) {
            case "Plus" -> new IntegerLiteral(left + right, span);
            case "Minus" -> new IntegerLiteral(left - right, span);
            case "Mult" -> new IntegerLiteral(left * right, span);
            // Деление на ноль оставляем программе: это ошибка времени исполнения,
            // а не повод упасть компилятору.
            case "Div" -> right == 0 ? null : new IntegerLiteral(left / right, span);
            case "Rem" -> right == 0 ? null : new IntegerLiteral(left % right, span);
            case "Min" -> new IntegerLiteral(Math.min(left, right), span);
            case "Max" -> new IntegerLiteral(Math.max(left, right), span);
            case "Less" -> new BooleanLiteral(left < right, span);
            case "LessEqual" -> new BooleanLiteral(left <= right, span);
            case "Greater" -> new BooleanLiteral(left > right, span);
            case "GreaterEqual" -> new BooleanLiteral(left >= right, span);
            case "Equal" -> new BooleanLiteral(left == right, span);
            default -> null;
        };
    }

    private Expression foldReals(double left, double right, String name, Span span) {
        return switch (name) {
            case "Plus" -> new RealLiteral(left + right, span);
            case "Minus" -> new RealLiteral(left - right, span);
            case "Mult" -> new RealLiteral(left * right, span);
            case "Div" -> right == 0.0 ? null : new RealLiteral(left / right, span);
            case "Min" -> new RealLiteral(Math.min(left, right), span);
            case "Max" -> new RealLiteral(Math.max(left, right), span);
            case "Less" -> new BooleanLiteral(left < right, span);
            case "LessEqual" -> new BooleanLiteral(left <= right, span);
            case "Greater" -> new BooleanLiteral(left > right, span);
            case "GreaterEqual" -> new BooleanLiteral(left >= right, span);
            case "Equal" -> new BooleanLiteral(left == right, span);
            default -> null;
        };
    }

    private Expression report(Expression folded, Span span) {
        if (folded != null) {
            record(span, "folded a constant expression into " + describe(folded));
        }
        return folded;
    }

    private String describe(Expression expression) {
        if (expression instanceof IntegerLiteral literal) {
            return String.valueOf(literal.getValue());
        }
        if (expression instanceof RealLiteral literal) {
            return String.valueOf(literal.getValue());
        }
        if (expression instanceof BooleanLiteral literal) {
            return String.valueOf(literal.isValue());
        }
        return "a literal";
    }

    // ---------- распознавание констант ----------

    private Integer constantInt(Expression expression) {
        if (expression instanceof IntegerLiteral literal) {
            return literal.getValue();
        }
        if (expression instanceof ConstructorInvocation invocation
                && invocation.getClassName().equals("Integer")
                && invocation.getArguments().size() == 1) {
            return constantInt(invocation.getArguments().get(0));
        }
        return null;
    }

    private Double constantReal(Expression expression) {
        if (expression instanceof RealLiteral literal) {
            return literal.getValue();
        }
        if (expression instanceof ConstructorInvocation invocation
                && invocation.getClassName().equals("Real")
                && invocation.getArguments().size() == 1) {
            return constantReal(invocation.getArguments().get(0));
        }
        return null;
    }

    private Boolean constantBoolean(Expression expression) {
        if (expression instanceof BooleanLiteral literal) {
            return literal.isValue();
        }
        if (expression instanceof ConstructorInvocation invocation
                && invocation.getClassName().equals("Boolean")
                && invocation.getArguments().size() == 1) {
            return constantBoolean(invocation.getArguments().get(0));
        }
        return null;
    }

    private void record(Span span, String message) {
        applied.add(span + ": " + message);
        changed = true;
    }
}
