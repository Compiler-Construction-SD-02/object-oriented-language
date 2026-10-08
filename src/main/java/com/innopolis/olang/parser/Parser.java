package com.innopolis.olang.parser;

import com.innopolis.olang.lexer.Span;
import com.innopolis.olang.lexer.Token;
import com.innopolis.olang.lexer.TokenType;
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
import com.innopolis.olang.parser.ast.Statement;
import com.innopolis.olang.parser.ast.ThisExpression;
import com.innopolis.olang.parser.ast.VariableDeclaration;
import com.innopolis.olang.parser.ast.WhileLoop;

import java.util.ArrayList;
import java.util.List;

/**
 * Рекурсивный спуск: на каждую конструкцию языка - один метод разбора.
 * Лексер отдал готовый список токенов, парсер по нему идёт слева направо
 * и собирает дерево. Последний токен всегда EOF, поэтому список не пустой.
 *
 * <p>Какую ветку разбора выбрать, всегда видно по текущему токену, а в двух
 * местах нужен ещё один токен вперёд: присваивание отличается от выражения
 * по := после имени, а создание объекта от обычного имени - по скобке.
 */
public final class Parser {

    /** Готовый поток токенов из лексера */
    private final List<Token> tokens;

    /** Индекс токена, который сейчас рассматриваем */
    private int pos = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    // ---------- объявления ----------

    /** Program : { ClassDeclaration } - точка входа */
    public Program parseProgram() {
        List<ClassDeclaration> classes = new ArrayList<>();
        while (!check(TokenType.EOF)) {
            classes.add(parseClassDeclaration());
        }
        return new Program(classes);
    }

    /** ClassDeclaration : class ClassName [ extends ClassName ] is { MemberDeclaration } end */
    private ClassDeclaration parseClassDeclaration() {
        expect(TokenType.CLASS, "'class'");
        Token name = expect(TokenType.IDENTIFIER, "class name");

        String baseName = null;
        if (match(TokenType.EXTENDS)) {
            baseName = expect(TokenType.IDENTIFIER, "base class name").getText();
        }

        expect(TokenType.IS, "'is'");

        List<MemberDeclaration> members = new ArrayList<>();
        while (!check(TokenType.END)) {
            members.add(parseMemberDeclaration());
        }

        expect(TokenType.END, "'end'");

        return new ClassDeclaration(name.getText(), baseName, members, name.getSpan());
    }

    /** MemberDeclaration : VariableDeclaration | MethodDeclaration | ConstructorDeclaration */
    private MemberDeclaration parseMemberDeclaration() {
        return switch (peek().getType()) {
            case VAR -> parseVariableDeclaration();
            case METHOD -> parseMethodDeclaration();
            case THIS -> parseConstructorDeclaration();
            default -> throw error("expected 'var', 'method' or 'this'");
        };
    }

    /** VariableDeclaration : var Identifier : Expression */
    private VariableDeclaration parseVariableDeclaration() {
        expect(TokenType.VAR, "'var'");
        Token name = expect(TokenType.IDENTIFIER, "variable name");
        expect(TokenType.COLON, "':'");
        Expression initializer = parseExpression();
        return new VariableDeclaration(name.getText(), initializer, name.getSpan());
    }

    /**
     * MethodDeclaration : MethodHeader [ MethodBody ]
     * MethodHeader      : method Identifier [ Parameters ] [ : ClassName ]
     * MethodBody        : is Body end | => Expression
     */
    private MethodDeclaration parseMethodDeclaration() {
        expect(TokenType.METHOD, "'method'");
        Token name = expect(TokenType.IDENTIFIER, "method name");

        List<ParameterDeclaration> parameters = check(TokenType.LPARENTHESIS)
                ? parseParameters()
                : List.of();

        String returnTypeName = null;
        if (match(TokenType.COLON)) {
            returnTypeName = expect(TokenType.IDENTIFIER, "return type name").getText();
        }

        List<BodyElement> body;
        if (match(TokenType.IS)) {
            body = parseBody(TokenType.END);
            expect(TokenType.END, "'end'");
        } else if (check(TokenType.ARROW)) {
            Token arrow = advance();
            // Короткая форма => Expression равна телу из одного return,
            // поэтому сразу приводим её к общему виду.
            body = List.of(new ReturnStatement(parseExpression(), arrow.getSpan()));
        } else {
            // Тела нет совсем - метод абстрактный, его реализуют наследники.
            body = null;
        }

        return new MethodDeclaration(name.getText(), parameters, returnTypeName, body, name.getSpan());
    }

    /** ConstructorDeclaration : this [ Parameters ] is Body end */
    private ConstructorDeclaration parseConstructorDeclaration() {
        Token keyword = expect(TokenType.THIS, "'this'");

        List<ParameterDeclaration> parameters = check(TokenType.LPARENTHESIS)
                ? parseParameters()
                : List.of();

        expect(TokenType.IS, "'is'");
        List<BodyElement> body = parseBody(TokenType.END);
        expect(TokenType.END, "'end'");

        return new ConstructorDeclaration(parameters, body, keyword.getSpan());
    }

    /**
     * Parameters : ( ParameterDeclaration { , ParameterDeclaration } )
     *
     * <p>Пустые скобки тоже разрешаем: this() и method Run() пишутся
     * в наших тестах, и у каждого класса по умолчанию есть конструктор
     * без параметров.
     */
    private List<ParameterDeclaration> parseParameters() {
        expect(TokenType.LPARENTHESIS, "'('");

        List<ParameterDeclaration> parameters = new ArrayList<>();
        if (!check(TokenType.RPARENTHESIS)) {
            parameters.add(parseParameterDeclaration());
            while (match(TokenType.COMMA)) {
                parameters.add(parseParameterDeclaration());
            }
        }

        expect(TokenType.RPARENTHESIS, "')'");
        return parameters;
    }

    /** ParameterDeclaration : Identifier : ClassName */
    private ParameterDeclaration parseParameterDeclaration() {
        Token name = expect(TokenType.IDENTIFIER, "parameter name");
        expect(TokenType.COLON, "':'");
        Token typeName = expect(TokenType.IDENTIFIER, "parameter type name");
        return new ParameterDeclaration(name.getText(), typeName.getText(), name.getSpan());
    }

    // ---------- тело и операторы ----------

    /**
     * Body : { VariableDeclaration | Statement }
     *
     * <p>Где тело заканчивается, грамматика не говорит - это зависит от того,
     * кто его открыл. Поэтому зовущий передаёт сюда токены, на которых
     * надо остановиться: end, а для ветки then ещё и else.
     */
    private List<BodyElement> parseBody(TokenType... terminators) {
        List<BodyElement> body = new ArrayList<>();

        while (!isAnyOf(terminators)) {
            if (check(TokenType.EOF)) {
                throw error("unexpected end of file, 'end' is missing");
            }
            body.add(check(TokenType.VAR) ? parseVariableDeclaration() : parseStatement());
        }

        return body;
    }

    /** Statement : Assignment | WhileLoop | IfStatement | ReturnStatement | Expression */
    private Statement parseStatement() {
        if (check(TokenType.WHILE)) {
            return parseWhileLoop();
        }
        if (check(TokenType.IF)) {
            return parseIfStatement();
        }
        if (check(TokenType.RETURN)) {
            return parseReturnStatement();
        }
        // И присваивание, и выражение начинаются с идентификатора.
        // Разводит их второй токен: := значит присваивание.
        if (check(TokenType.IDENTIFIER) && peek(1).getType() == TokenType.ASSIGN) {
            return parseAssignment();
        }
        return parseExpressionStatement();
    }

    /** Assignment : Identifier := Expression */
    private Assignment parseAssignment() {
        Token target = expect(TokenType.IDENTIFIER, "variable name");
        expect(TokenType.ASSIGN, "':='");
        Expression value = parseExpression();
        return new Assignment(target.getText(), value, target.getSpan());
    }

    /** WhileLoop : while Expression loop Body end */
    private WhileLoop parseWhileLoop() {
        Token keyword = expect(TokenType.WHILE, "'while'");
        Expression condition = parseExpression();
        expect(TokenType.LOOP, "'loop'");
        List<BodyElement> body = parseBody(TokenType.END);
        expect(TokenType.END, "'end'");
        return new WhileLoop(condition, body, keyword.getSpan());
    }

    /** IfStatement : if Expression then Body [ else Body ] end */
    private IfStatement parseIfStatement() {
        Token keyword = expect(TokenType.IF, "'if'");
        Expression condition = parseExpression();
        expect(TokenType.THEN, "'then'");

        List<BodyElement> thenBody = parseBody(TokenType.ELSE, TokenType.END);

        List<BodyElement> elseBody = null;
        if (match(TokenType.ELSE)) {
            elseBody = parseBody(TokenType.END);
        }

        expect(TokenType.END, "'end'");
        return new IfStatement(condition, thenBody, elseBody, keyword.getSpan());
    }

    /** ReturnStatement : return [ Expression ] */
    private ReturnStatement parseReturnStatement() {
        Token keyword = expect(TokenType.RETURN, "'return'");
        // Есть ли значение, видно по следующему токену: если с него может
        // начаться выражение - значит есть, иначе это просто выход.
        Expression value = startsExpression() ? parseExpression() : null;
        return new ReturnStatement(value, keyword.getSpan());
    }

    /** Выражение в роли оператора: вызов ради действия, результат выбрасываем. */
    private ExpressionStatement parseExpressionStatement() {
        Span span = peek().getSpan();
        return new ExpressionStatement(parseExpression(), span);
    }

    // ---------- выражения ----------

    /**
     * Expression : Primary { . Identifier [ Arguments ] }
     *
     * <p>Левой рекурсии здесь нет, поэтому и метод простой: сначала разобрали
     * начало цепочки, потом в цикле навешиваем на него точку за точкой.
     * Каждый шаг кладёт прежний результат внутрь нового узла, и a.Plus(b).Print()
     * собирается слева направо - именно в том порядке, в котором считается.
     */
    private Expression parseExpression() {
        Expression expression = parsePrimary();

        while (match(TokenType.DOT)) {
            Token name = expect(TokenType.IDENTIFIER, "member name after '.'");
            expression = check(TokenType.LPARENTHESIS)
                    ? new MethodCall(expression, name.getText(), parseArguments(), name.getSpan())
                    : new FieldAccess(expression, name.getText(), name.getSpan());
        }

        return expression;
    }

    /**
     * Primary : IntegerLiteral | RealLiteral | BooleanLiteral
     *         | this | Identifier | ClassName Arguments
     *         | ( Expression )
     */
    private Expression parsePrimary() {
        Token token = peek();

        switch (token.getType()) {
            case INT_LITERAL:
                advance();
                return new IntegerLiteral(parseInt(token), token.getSpan());

            case REAL_LITERAL:
                advance();
                return new RealLiteral(Double.parseDouble(token.getText()), token.getSpan());

            case TRUE:
            case FALSE:
                advance();
                return new BooleanLiteral(token.getType() == TokenType.TRUE, token.getSpan());

            case THIS:
                advance();
                return new ThisExpression(token.getSpan());

            case IDENTIFIER:
                advance();
                // Integer(0) - создание объекта, x - просто имя.
                // Начало одинаковое, решает скобка сразу после имени.
                return check(TokenType.LPARENTHESIS)
                        ? new ConstructorInvocation(token.getText(), parseArguments(), token.getSpan())
                        : new NameReference(token.getText(), token.getSpan());

            case LPARENTHESIS: {
                // Выражение в скобках. Своего узла не получает: скобки нужны
                // только чтобы прочитать запись, а в дереве группировка
                // и так видна по его форме.
                advance();
                Expression inner = parseExpression();
                expect(TokenType.RPARENTHESIS, "')'");
                return inner;
            }

            default:
                throw error("expected expression");
        }
    }

    /** Arguments : ( [ Expression { , Expression } ] ) */
    private List<Expression> parseArguments() {
        expect(TokenType.LPARENTHESIS, "'('");

        List<Expression> arguments = new ArrayList<>();
        if (!check(TokenType.RPARENTHESIS)) {
            arguments.add(parseExpression());
            while (match(TokenType.COMMA)) {
                arguments.add(parseExpression());
            }
        }

        expect(TokenType.RPARENTHESIS, "')'");
        return arguments;
    }

    /**
     * Может ли выражение начаться с текущего токена.
     * Это список первых токенов всех вариантов Primary.
     */
    private boolean startsExpression() {
        return isAnyOf(TokenType.INT_LITERAL, TokenType.REAL_LITERAL,
                TokenType.TRUE, TokenType.FALSE, TokenType.THIS, TokenType.IDENTIFIER,
                TokenType.LPARENTHESIS);
    }

    /** Лексер проверил, что это цифры, но в int они могут и не влезть. */
    private int parseInt(Token token) {
        try {
            return Integer.parseInt(token.getText());
        } catch (NumberFormatException e) {
            throw new SyntaxException("integer literal is too large: " + token.getText(), token.getSpan());
        }
    }

    // ---------- вспомогательные методы ----------

    /** Текущий токен, не сдвигая позицию */
    private Token peek() {
        return tokens.get(pos);
    }

    /** Токен на offset вперёд. За концом списка отдаём EOF */
    private Token peek(int offset) {
        int i = pos + offset;
        return i < tokens.size() ? tokens.get(i) : tokens.get(tokens.size() - 1);
    }

    /** Текущий токен такого вида? Нужно, чтобы выбрать ветку разбора */
    private boolean check(TokenType type) {
        return peek().getType() == type;
    }

    /** Текущий токен - любой из перечисленных? */
    private boolean isAnyOf(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                return true;
            }
        }
        return false;
    }

    /** Съесть текущий токен и вернуть его */
    private Token advance() {
        return tokens.get(pos++);
    }

    /** Съесть токен, если он такого вида */
    private boolean match(TokenType type) {
        if (check(type)) {
            advance();
            return true;
        }
        return false;
    }

    /** Съесть обязательный токен. Не тот вид - это ошибка в программе. */
    private Token expect(TokenType type, String what) {
        if (check(type)) {
            return advance();
        }
        throw error("expected " + what);
    }

    /** Ошибка с координатами того токена, на котором споткнулись. */
    private SyntaxException error(String message) {
        return new SyntaxException(message + ", found " + peek().getType(), peek().getSpan());
    }
}
