package com.innopolis.olang.parser;

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

import java.util.List;
import java.util.stream.Collectors;

/**
 * Печать дерева отступами.
 *
 * <p>Нужна ровно для одного: посмотреть глазами, что парсер разобрал файл
 * так, как мы думаем. В компиляции не участвует.
 */
public final class AstPrinter {

    private final StringBuilder out = new StringBuilder();

    public String print(Program program) {
        for (ClassDeclaration declaration : program.getClasses()) {
            printNode(declaration, 0);
        }
        return out.toString();
    }

    /**
     * Один метод на все виды узлов: switch по типу объекта.
     * Так короче, чем писать обход отдельно для каждого класса.
     */
    private void printNode(Object node, int depth) {
        switch (node) {
            case ClassDeclaration declaration -> {
                write(depth, "class " + declaration.getName()
                        + (declaration.getBaseName() == null ? "" : " extends " + declaration.getBaseName()));
                for (MemberDeclaration member : declaration.getMembers()) {
                    printNode(member, depth + 1);
                }
            }
            case VariableDeclaration declaration -> {
                write(depth, "var " + declaration.getName());
                printNode(declaration.getInitializer(), depth + 1);
            }
            case MethodDeclaration declaration -> {
                write(depth, "method " + declaration.getName()
                        + parameters(declaration.getParameters())
                        + (declaration.getReturnTypeName() == null ? "" : " : " + declaration.getReturnTypeName())
                        + (declaration.isAbstract() ? "   (no body)" : ""));
                if (!declaration.isAbstract()) {
                    printBody(declaration.getBody(), depth + 1);
                }
            }
            case ConstructorDeclaration declaration -> {
                write(depth, "this" + parameters(declaration.getParameters()));
                printBody(declaration.getBody(), depth + 1);
            }
            case Assignment assignment -> {
                write(depth, assignment.getTarget() + " :=");
                printNode(assignment.getValue(), depth + 1);
            }
            case WhileLoop loop -> {
                write(depth, "while");
                printNode(loop.getCondition(), depth + 1);
                write(depth, "loop");
                printBody(loop.getBody(), depth + 1);
            }
            case IfStatement statement -> {
                write(depth, "if");
                printNode(statement.getCondition(), depth + 1);
                write(depth, "then");
                printBody(statement.getThenBody(), depth + 1);
                if (statement.getElseBody() != null) {
                    write(depth, "else");
                    printBody(statement.getElseBody(), depth + 1);
                }
            }
            case ReturnStatement statement -> {
                write(depth, "return");
                if (statement.getValue() != null) {
                    printNode(statement.getValue(), depth + 1);
                }
            }
            case ExpressionStatement statement -> printNode(statement.getExpression(), depth);
            case IntegerLiteral literal -> write(depth, "int " + literal.getValue());
            case RealLiteral literal -> write(depth, "real " + literal.getValue());
            case BooleanLiteral literal -> write(depth, "bool " + literal.isValue());
            case ThisExpression ignored -> write(depth, "this");
            case NameReference reference -> write(depth, "name " + reference.getName());
            case ConstructorInvocation invocation -> {
                write(depth, "new " + invocation.getClassName());
                for (Expression argument : invocation.getArguments()) {
                    printNode(argument, depth + 1);
                }
            }
            case MethodCall call -> {
                write(depth, "call " + call.getMethodName());
                printNode(call.getReceiver(), depth + 1);
                for (Expression argument : call.getArguments()) {
                    printNode(argument, depth + 1);
                }
            }
            case FieldAccess access -> {
                write(depth, "field " + access.getName());
                printNode(access.getReceiver(), depth + 1);
            }
            default -> write(depth, "? " + node.getClass().getSimpleName());
        }
    }

    private void printBody(List<BodyElement> body, int depth) {
        for (BodyElement element : body) {
            printNode(element, depth);
        }
    }

    private String parameters(List<ParameterDeclaration> parameters) {
        return parameters.stream()
                .map(parameter -> parameter.getName() + ": " + parameter.getTypeName())
                .collect(Collectors.joining(", ", "(", ")"));
    }

    private void write(int depth, String text) {
        out.append("  ".repeat(depth)).append(text).append('\n');
    }
}
