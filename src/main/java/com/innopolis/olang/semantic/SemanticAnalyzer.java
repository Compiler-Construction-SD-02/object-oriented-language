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
import com.innopolis.olang.parser.ast.ParameterDeclaration;
import com.innopolis.olang.parser.ast.Program;
import com.innopolis.olang.parser.ast.RealLiteral;
import com.innopolis.olang.parser.ast.ReturnStatement;
import com.innopolis.olang.parser.ast.ThisExpression;
import com.innopolis.olang.parser.ast.VariableDeclaration;
import com.innopolis.olang.parser.ast.WhileLoop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Семантический анализатор: проверки, которые дерево не меняют.
 *
 * Работа идёт в четыре прохода:
 * 1) записываем только имена классов. Больше пока ничего и нельзя: чтобы записать родителя, надо,
 * чтобы родитель уже существовал, а он может быть объявлен ниже по файлу.
 * 2) теперь все имена есть, можно проставить родителей. Заодно ловим циклы (A extends B, B extends A).
 * 3) теперь известна иерархия, можно записывать методы и поля.
 * 4) таблица готова, идём по телам и проверяем.
 *
 * Ошибки не бросаются, а собираются в список.
 */
public final class SemanticAnalyzer {

    private final SymbolTable symbols = new SymbolTable();

    private final List<SemanticError> errors = new ArrayList<>();

    /** Известные на этапе компиляции длины массивов - для проверки выхода за границу. */
    private final Map<VariableSymbol, Integer> arrayLengths = new HashMap<>();

    private ClassSymbol currentClass;

    /** Метод или конструктор, тело которого сейчас проверяем. */
    private MethodSymbol currentMethod;

    private boolean inConstructor;

    private Scope scope;

    public List<SemanticError> analyze(Program program) {
        declareClasses(program);
        resolveBases(program);
        declareMembers(program);
        checkBodies(program);
        return errors;
    }

    public SymbolTable getSymbols() {
        return symbols;
    }

    // проход 1: классы

    private void declareClasses(Program program) {
        for (ClassDeclaration declaration : program.getClasses()) {
            if (symbols.contains(declaration.getName())) {
                ClassSymbol existing = symbols.find(declaration.getName());
                error(declaration.getSpan(), existing.isBuiltin()
                        ? "class '" + declaration.getName() + "' redefines a standard library class"
                        : "class '" + declaration.getName() + "' is already declared");
                continue;
            }
            symbols.declare(declaration.getName(), declaration.getBaseName(), false, declaration.getSpan());
        }
    }

    // проход 2: наследование

    private void resolveBases(Program program) {
        ClassSymbol root = symbols.find("Class");

        for (ClassDeclaration declaration : program.getClasses()) {
            ClassSymbol symbol = symbols.find(declaration.getName());
            if (!owns(symbol, declaration)) {
                continue;
            }
            if (declaration.getBaseName() == null) {
                symbol.setBase(root);
                continue;
            }
            ClassSymbol base = symbols.find(declaration.getBaseName());
            if (base == null) {
                error(declaration.getSpan(),
                        "unknown base class '" + declaration.getBaseName() + "'");
                symbol.setBase(root);
            } else {
                symbol.setBase(base);
            }
        }

        for (ClassDeclaration declaration : program.getClasses()) {
            ClassSymbol symbol = symbols.find(declaration.getName());
            if (symbol != null && hasInheritanceCycle(symbol)) {
                error(declaration.getSpan(),
                        "class '" + symbol.getName() + "' is part of an inheritance cycle");
                // Рвём цепочку, иначе любой проход вверх по родителям зациклится.
                symbol.setBase(root);
            }
        }
    }

    private boolean hasInheritanceCycle(ClassSymbol symbol) {
        Set<ClassSymbol> visited = new HashSet<>();
        for (ClassSymbol current = symbol; current != null; current = current.getBase()) {
            if (!visited.add(current)) {
                return true;
            }
        }
        return false;
    }

    // проход 3: методы, конструкторы, поля

    private void declareMembers(Program program) {
        // Сначала сигнатуры: их типы - это имена классов, а все классы уже известны.
        for (ClassDeclaration declaration : program.getClasses()) {
            currentClass = symbols.find(declaration.getName());
            if (!owns(currentClass, declaration)) {
                continue;
            }
            for (MemberDeclaration member : declaration.getMembers()) {
                if (member instanceof MethodDeclaration method) {
                    declareMethod(method);
                } else if (member instanceof ConstructorDeclaration constructor) {
                    declareConstructor(constructor);
                }
            }
            addDefaultConstructorIfMissing(declaration);
        }

        // Потом поля: их типы берутся из выражений, а выражения могут звать методы.
        for (ClassDeclaration declaration : program.getClasses()) {
            currentClass = symbols.find(declaration.getName());
            if (!owns(currentClass, declaration)) {
                continue;
            }
            currentMethod = null;
            inConstructor = false;
            scope = new Scope(null);
            for (MemberDeclaration member : declaration.getMembers()) {
                if (member instanceof VariableDeclaration field) {
                    declareField(field);
                }
            }
        }
        currentClass = null;
    }

    private void declareField(VariableDeclaration field) {
        ClassSymbol type = typeOf(field.getInitializer());
        if (type == SymbolTable.VOID) {
            error(field.getSpan(), "cannot declare field '" + field.getName()
                    + "': the expression returns nothing");
            type = SymbolTable.UNKNOWN;
        }
        if (currentClass.getFields().containsKey(field.getName())) {
            error(field.getSpan(), "field '" + field.getName() + "' is already declared in class '"
                    + currentClass.getName() + "'");
            return;
        }
        currentClass.getFields().put(field.getName(),
                new VariableSymbol(field.getName(), type, field.getSpan()));
    }

    private void declareMethod(MethodDeclaration method) {
        List<VariableSymbol> parameters = buildParameters(method.getParameters());
        ClassSymbol returnType = null;
        if (method.getReturnTypeName() != null) {
            returnType = resolveType(method.getReturnTypeName(), method.getSpan());
        }

        MethodSymbol symbol = new MethodSymbol(method.getName(), parameters, returnType,
                currentClass, !method.isAbstract(), method.getSpan());

        MethodSymbol existing = findSameSignature(currentClass.getMethods(), symbol);
        if (existing != null) {
            // Предварительное объявление без тела и реализация - это один метод.
            if (existing.hasBody() && symbol.hasBody()) {
                error(method.getSpan(), "method '" + symbol.signature()
                        + "' is already declared in class '" + currentClass.getName() + "'");
            } else {
                existing.setHasBody(existing.hasBody() || symbol.hasBody());
            }
            return;
        }
        currentClass.getMethods().add(symbol);
    }

    private void declareConstructor(ConstructorDeclaration constructor) {
        List<VariableSymbol> parameters = buildParameters(constructor.getParameters());
        MethodSymbol symbol = new MethodSymbol(currentClass.getName(), parameters, null,
                currentClass, true, constructor.getSpan());

        if (findSameSignature(currentClass.getConstructors(), symbol) != null) {
            error(constructor.getSpan(), "constructor '" + symbol.signature()
                    + "' is already declared in class '" + currentClass.getName() + "'");
            return;
        }
        currentClass.getConstructors().add(symbol);
    }

    /** Решение 7.6: у каждого класса всегда есть конструктор без параметров. */
    private void addDefaultConstructorIfMissing(ClassDeclaration declaration) {
        boolean hasParameterless = currentClass.getConstructors().stream()
                .anyMatch(constructor -> constructor.getParameters().isEmpty());
        if (!hasParameterless) {
            currentClass.getConstructors().add(new MethodSymbol(currentClass.getName(),
                    List.of(), null, currentClass, true, declaration.getSpan()));
        }
    }

    private List<VariableSymbol> buildParameters(List<ParameterDeclaration> declarations) {
        List<VariableSymbol> parameters = new ArrayList<>();
        Set<String> names = new HashSet<>();
        for (ParameterDeclaration parameter : declarations) {
            if (!names.add(parameter.getName())) {
                error(parameter.getSpan(), "parameter '" + parameter.getName() + "' is duplicated");
            }
            parameters.add(new VariableSymbol(parameter.getName(),
                    resolveType(parameter.getTypeName(), parameter.getSpan()), parameter.getSpan()));
        }
        return parameters;
    }

    private ClassSymbol resolveType(String name, Span span) {
        ClassSymbol type = symbols.find(name);
        if (type == null) {
            error(span, "unknown type '" + name + "'");
            return SymbolTable.UNKNOWN;
        }
        return type;
    }

    private MethodSymbol findSameSignature(List<MethodSymbol> methods, MethodSymbol symbol) {
        return methods.stream()
                .filter(method -> method.getName().equals(symbol.getName()))
                .filter(method -> method.sameParameterTypes(symbol))
                .findFirst()
                .orElse(null);
    }

    // проход 4: тела

    private void checkBodies(Program program) {
        for (ClassDeclaration declaration : program.getClasses()) {
            currentClass = symbols.find(declaration.getName());
            if (!owns(currentClass, declaration)) {
                continue;
            }
            for (MemberDeclaration member : declaration.getMembers()) {
                if (member instanceof MethodDeclaration method && !method.isAbstract()) {
                    checkMethod(method);
                } else if (member instanceof ConstructorDeclaration constructor) {
                    checkConstructor(constructor);
                }
            }
        }
        currentClass = null;
    }

    private void checkMethod(MethodDeclaration method) {
        currentMethod = new MethodSymbol(method.getName(), buildParameters(method.getParameters()),
                method.getReturnTypeName() == null ? null
                        : resolveType(method.getReturnTypeName(), method.getSpan()),
                currentClass, true, method.getSpan());
        inConstructor = false;

        openScopeWithParameters();
        checkBody(method.getBody());
        scope = null;

        if (currentMethod.getReturnType() != null && !alwaysReturns(method.getBody())) {
            error(method.getSpan(), "method '" + method.getName()
                    + "' must return a value on every path");
        }
        currentMethod = null;
    }

    private void checkConstructor(ConstructorDeclaration constructor) {
        currentMethod = new MethodSymbol(currentClass.getName(),
                buildParameters(constructor.getParameters()), null, currentClass, true,
                constructor.getSpan());
        inConstructor = true;

        openScopeWithParameters();
        checkBody(constructor.getBody());
        scope = null;

        currentMethod = null;
        inConstructor = false;
    }

    private void openScopeWithParameters() {
        scope = new Scope(null);
        for (VariableSymbol parameter : currentMethod.getParameters()) {
            scope.declare(parameter);
        }
    }

    private void checkBody(List<BodyElement> body) {
        for (BodyElement element : body) {
            checkElement(element);
        }
    }

    private void checkNestedBody(List<BodyElement> body) {
        scope = new Scope(scope);
        checkBody(body);
        scope = scope.getParent();
    }

    private void checkElement(BodyElement element) {
        if (element instanceof VariableDeclaration declaration) {
            checkLocalVariable(declaration);
        } else if (element instanceof Assignment assignment) {
            checkAssignment(assignment);
        } else if (element instanceof WhileLoop loop) {
            checkCondition(loop.getCondition(), loop.getSpan(), "while");
            checkNestedBody(loop.getBody());
        } else if (element instanceof IfStatement statement) {
            checkCondition(statement.getCondition(), statement.getSpan(), "if");
            checkNestedBody(statement.getThenBody());
            if (statement.getElseBody() != null) {
                checkNestedBody(statement.getElseBody());
            }
        } else if (element instanceof ReturnStatement statement) {
            checkReturn(statement);
        } else if (element instanceof ExpressionStatement statement) {
            typeOf(statement.getExpression());
        }
    }

    private void checkLocalVariable(VariableDeclaration declaration) {
        ClassSymbol type = typeOf(declaration.getInitializer());
        if (type == SymbolTable.VOID) {
            error(declaration.getSpan(), "cannot declare variable '" + declaration.getName()
                    + "': the expression returns nothing");
            type = SymbolTable.UNKNOWN;
        }
        if (scope.findHere(declaration.getName()) != null) {
            error(declaration.getSpan(),
                    "variable '" + declaration.getName() + "' is already declared in this scope");
            return;
        }
        VariableSymbol variable =
                new VariableSymbol(declaration.getName(), type, declaration.getSpan());
        scope.declare(variable);
        rememberArrayLength(variable, declaration.getInitializer());
    }

    private void checkAssignment(Assignment assignment) {
        VariableSymbol target = resolveName(assignment.getTarget(), assignment.getSpan());
        ClassSymbol valueType = typeOf(assignment.getValue());

        if (target == null) {
            return;
        }
        // Длина массива после присваивания нам больше не известна.
        arrayLengths.remove(target);

        if (valueType == SymbolTable.VOID) {
            error(assignment.getSpan(), "cannot assign to '" + assignment.getTarget()
                    + "': the expression returns nothing");
            return;
        }
        if (SymbolTable.isUsable(valueType) && SymbolTable.isUsable(target.getType())
                && !valueType.isSubclassOf(target.getType())) {
            error(assignment.getSpan(), "cannot assign " + valueType.getName() + " to '"
                    + assignment.getTarget() + "' of type " + target.getType().getName());
        }
    }

    private void checkCondition(Expression condition, Span span, String keyword) {
        ClassSymbol type = typeOf(condition);
        ClassSymbol bool = symbols.find("Boolean");
        if (SymbolTable.isUsable(type) && type != bool) {
            error(span, "condition of '" + keyword + "' must be Boolean, found " + type.getName());
        }
    }

    private void checkReturn(ReturnStatement statement) {
        ClassSymbol valueType =
                statement.getValue() == null ? null : typeOf(statement.getValue());

        if (inConstructor) {
            if (statement.getValue() != null) {
                error(statement.getSpan(), "a constructor cannot return a value");
            }
            return;
        }

        ClassSymbol expected = currentMethod.getReturnType();
        if (expected == null) {
            if (statement.getValue() != null) {
                error(statement.getSpan(), "method '" + currentMethod.getName()
                        + "' does not declare a return type, so it cannot return a value");
            }
            return;
        }
        if (statement.getValue() == null) {
            error(statement.getSpan(), "method '" + currentMethod.getName()
                    + "' must return a value of type " + expected.getName());
            return;
        }
        if (SymbolTable.isUsable(valueType) && SymbolTable.isUsable(expected)
                && !valueType.isSubclassOf(expected)) {
            error(statement.getSpan(), "method '" + currentMethod.getName() + "' must return "
                    + expected.getName() + ", found " + valueType.getName());
        }
    }

    private boolean alwaysReturns(List<BodyElement> body) {
        for (BodyElement element : body) {
            if (element instanceof ReturnStatement) {
                return true;
            }
            if (element instanceof IfStatement statement
                    && statement.getElseBody() != null
                    && alwaysReturns(statement.getThenBody())
                    && alwaysReturns(statement.getElseBody())) {
                return true;
            }
        }
        return false;
    }

    // типы выражений

    private ClassSymbol typeOf(Expression expression) {
        if (expression instanceof IntegerLiteral) {
            return symbols.find("Integer");
        }
        if (expression instanceof RealLiteral) {
            return symbols.find("Real");
        }
        if (expression instanceof BooleanLiteral) {
            return symbols.find("Boolean");
        }
        if (expression instanceof ThisExpression) {
            return currentClass == null ? SymbolTable.UNKNOWN : currentClass;
        }
        if (expression instanceof NameReference reference) {
            VariableSymbol variable = resolveName(reference.getName(), reference.getSpan());
            return variable == null ? SymbolTable.UNKNOWN : variable.getType();
        }
        if (expression instanceof ConstructorInvocation invocation) {
            return typeOfConstructorInvocation(invocation);
        }
        if (expression instanceof MethodCall call) {
            return typeOfMethodCall(call);
        }
        if (expression instanceof FieldAccess access) {
            return typeOfFieldAccess(access);
        }
        return SymbolTable.UNKNOWN;
    }

    /**
     * Integer(0) - создание объекта, а IsOdd(n) внутри своего класса - вызов
     * собственного метода. Парсер их не различает: и то и другое записано как
     * "имя со скобками". Разбираемся здесь, по таблице символов.
     */
    private ClassSymbol typeOfConstructorInvocation(ConstructorInvocation invocation) {
        List<ClassSymbol> argumentTypes = typesOf(invocation.getArguments());
        ClassSymbol target = symbols.find(invocation.getClassName());

        if (target != null) {
            MethodSymbol constructor = resolve(target.getConstructors(), argumentTypes);
            if (constructor == null) {
                error(invocation.getSpan(), "class '" + target.getName()
                        + "' has no constructor applicable to " + describe(argumentTypes));
            }
            return target;
        }

        List<MethodSymbol> candidates = currentClass == null
                ? List.of()
                : currentClass.findMethods(invocation.getClassName());
        if (!candidates.isEmpty()) {
            return returnTypeOf(candidates, argumentTypes, invocation.getClassName(),
                    currentClass, invocation.getSpan());
        }

        error(invocation.getSpan(),
                "unknown class or method '" + invocation.getClassName() + "'");
        return SymbolTable.UNKNOWN;
    }

    private ClassSymbol typeOfMethodCall(MethodCall call) {
        ClassSymbol receiverType = typeOf(call.getReceiver());
        List<ClassSymbol> argumentTypes = typesOf(call.getArguments());

        if (receiverType == SymbolTable.VOID) {
            error(call.getSpan(), "cannot call '" + call.getMethodName()
                    + "': the expression on the left returns nothing");
            return SymbolTable.UNKNOWN;
        }
        if (!SymbolTable.isUsable(receiverType)) {
            return SymbolTable.UNKNOWN;
        }

        checkArrayBounds(call, argumentTypes);

        List<MethodSymbol> candidates = receiverType.findMethods(call.getMethodName());
        if (candidates.isEmpty()) {
            error(call.getSpan(), "class '" + receiverType.getName() + "' has no method '"
                    + call.getMethodName() + "'");
            return SymbolTable.UNKNOWN;
        }
        return returnTypeOf(candidates, argumentTypes, call.getMethodName(),
                receiverType, call.getSpan());
    }

    private ClassSymbol typeOfFieldAccess(FieldAccess access) {
        ClassSymbol receiverType = typeOf(access.getReceiver());
        if (!SymbolTable.isUsable(receiverType)) {
            return SymbolTable.UNKNOWN;
        }
        VariableSymbol field = receiverType.findField(access.getName());
        if (field == null) {
            error(access.getSpan(), "class '" + receiverType.getName() + "' has no field '"
                    + access.getName() + "'");
            return SymbolTable.UNKNOWN;
        }
        return field.getType();
    }

    private ClassSymbol returnTypeOf(List<MethodSymbol> candidates, List<ClassSymbol> argumentTypes,
                                     String name, ClassSymbol owner, Span span) {
        MethodSymbol method = resolve(candidates, argumentTypes);
        if (method == null) {
            error(span, "no method '" + name + "' of class '" + owner.getName()
                    + "' is applicable to " + describe(argumentTypes));
            return SymbolTable.UNKNOWN;
        }
        return method.getReturnType() == null ? SymbolTable.VOID : method.getReturnType();
    }

    /**
     * Выбор перегрузки. Сначала ищем точное совпадение типов, потом совместимое
     * по наследованию - чтобы Combine(Integer) не перехватывал вызов, для которого
     * есть Combine(Real).
     */
    private MethodSymbol resolve(List<MethodSymbol> candidates, List<ClassSymbol> argumentTypes) {
        List<MethodSymbol> byCount = candidates.stream()
                .filter(method -> method.getParameters().size() == argumentTypes.size())
                .collect(Collectors.toList());

        for (MethodSymbol method : byCount) {
            if (matches(method, argumentTypes, true)) {
                return method;
            }
        }
        for (MethodSymbol method : byCount) {
            if (matches(method, argumentTypes, false)) {
                return method;
            }
        }
        return null;
    }

    private boolean matches(MethodSymbol method, List<ClassSymbol> argumentTypes, boolean exact) {
        for (int i = 0; i < argumentTypes.size(); i++) {
            ClassSymbol argument = argumentTypes.get(i);
            ClassSymbol parameter = method.getParameters().get(i).getType();
            if (!SymbolTable.isUsable(argument) || !SymbolTable.isUsable(parameter)) {
                continue;
            }
            boolean ok = exact ? argument == parameter : argument.isSubclassOf(parameter);
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    private List<ClassSymbol> typesOf(List<Expression> expressions) {
        return expressions.stream().map(this::typeOf).collect(Collectors.toList());
    }

    private String describe(List<ClassSymbol> types) {
        return types.stream().map(ClassSymbol::getName).collect(Collectors.joining(", ", "(", ")"));
    }

    // проверка границ массива

    /**
     * Если массив создан с постоянной длиной, а индекс в get/set тоже постоянный,
     * выход за границу виден уже при компиляции.
     */
    private void checkArrayBounds(MethodCall call, List<ClassSymbol> argumentTypes) {
        if (!(call.getReceiver() instanceof NameReference reference)) {
            return;
        }
        if (!call.getMethodName().equals("get") && !call.getMethodName().equals("set")) {
            return;
        }
        if (call.getArguments().isEmpty() || argumentTypes.isEmpty()) {
            return;
        }
        VariableSymbol variable = scope == null ? null : scope.find(reference.getName());
        Integer length = variable == null ? null : arrayLengths.get(variable);
        Integer index = constantInt(call.getArguments().get(0));
        if (length == null || index == null) {
            return;
        }
        if (index < 0 || index >= length) {
            error(call.getSpan(), "index " + index + " is out of bounds for array '"
                    + reference.getName() + "' of length " + length);
        }
    }

    private void rememberArrayLength(VariableSymbol variable, Expression initializer) {
        if (!(initializer instanceof ConstructorInvocation invocation)) {
            return;
        }
        if (!invocation.getClassName().startsWith("Array") || invocation.getArguments().size() != 1) {
            return;
        }
        Integer length = constantInt(invocation.getArguments().get(0));
        if (length != null) {
            arrayLengths.put(variable, length);
        }
    }

    /** Значение целой константы, если выражение вычислимо при компиляции. */
    static Integer constantInt(Expression expression) {
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

    // мелочи

    private VariableSymbol resolveName(String name, Span span) {
        VariableSymbol variable = scope == null ? null : scope.find(name);
        if (variable != null) {
            return variable;
        }
        if (currentClass != null) {
            VariableSymbol field = currentClass.findField(name);
            if (field != null) {
                return field;
            }
        }
        error(span, "unknown name '" + name + "'");
        return null;
    }

    /**
     * Этот ли узел породил символ. Если имя класса объявлено дважды, символ
     * принадлежит первому объявлению, и второе надо целиком пропустить -
     * иначе оно затрёт родителя и члены первого.
     */
    private boolean owns(ClassSymbol symbol, ClassDeclaration declaration) {
        return symbol != null && !symbol.isBuiltin()
                && declaration.getSpan().equals(symbol.getSpan());
    }

    private void error(Span span, String message) {
        errors.add(new SemanticError(message, span));
    }
}
