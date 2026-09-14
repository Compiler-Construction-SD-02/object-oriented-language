package com.innopolis.olang.lexer;

import java.util.ArrayList;
import java.util.List;

/**
 * Лексический анализатор языка O
 *
 * Устройство - один switch по первому символу токена
 *
 * <p>Токены складываются в список целиком, а не выдаются парсеру по одному
 * по запросу. Так парсер может заглядывать вперёд на сколько угодно токенов,
 * и это нужно из-за того, что в O класс можно использовать до его объявления.
 */
public final class Lexer {

    /** Символ, который возвращается вместо чтения за концом файла. */
    private static final char EOF_CHAR = '\0';

    private final String source;

    /** Индекс следующего непрочитанного символа в source. */
    private int pos = 0;

    /** Текущая строка, с единицы. */
    private int line = 1;

    /** Текущая колонка, с единицы. */
    private int column = 1;

    public Lexer(String source) {
        this.source = source;
    }

    /**
     * Прогоняет весь файл и возвращает список токенов.
     * Последним элементом всегда идёт EOF.
     */
    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        Token token;
        do {
            token = nextToken();
            tokens.add(token);
        } while (token.getType() != TokenType.EOF);
        return tokens;
    }

    /** Читает очередной токен. */
    private Token nextToken() {
        // Пробелы и комментарии убираем ДО
        skipWhitespaceAndComments();

        if (atEnd()) {
            return new Token(TokenType.EOF, "", new Span(line, column));
        }

        int startLine = line;
        int startColumn = column;
        char c = peek();

        if (Character.isLetter(c) || c == '_') {
            return readWordOrKeyword(startLine, startColumn);
        }
        if (Character.isDigit(c)) {
            return readNumber(startLine, startColumn);
        }

        switch (c) {
            case ':':
                advance();
                if (peek() == '=') {
                    advance();
                    return token(TokenType.ASSIGN, startLine, startColumn);
                }
                return token(TokenType.COLON, startLine, startColumn);

            case '=':
                advance();
                if (peek() == '>') {
                    advance();
                    return token(TokenType.ARROW, startLine, startColumn);
                }
                throw new LexicalException(
                        "expected '>' after '='", new Span(startLine, startColumn));

            case '.':
                advance();
                return token(TokenType.DOT, startLine, startColumn);
            case ',':
                advance();
                return token(TokenType.COMMA, startLine, startColumn);
            case '(':
                advance();
                return token(TokenType.LPARENTHESIS, startLine, startColumn);
            case ')':
                advance();
                return token(TokenType.RPARENTHESIS, startLine, startColumn);

            default:
                advance();
                throw new LexicalException(
                        "unexpected character '" + c + "'", new Span(startLine, startColumn));
        }
    }

    /**
     * Читает слово из букв, цифр и подчёркиваний.
     * Дальше смотрит по таблице, ключевое оно или обычный идентификатор.
     */
    private Token readWordOrKeyword(int startLine, int startColumn) {
        StringBuilder word = new StringBuilder();
        while (!atEnd() && (Character.isLetterOrDigit(peek()) || peek() == '_')) {
            word.append(advance());
        }
        String text = word.toString();
        TokenType type = TokenType.keyword(text).orElse(TokenType.IDENTIFIER);
        return new Token(type, text, spanFrom(startLine, startColumn));
    }

    /**
     * Читает число.
     */
    private Token readNumber(int startLine, int startColumn) {
        StringBuilder number = new StringBuilder();
        while (!atEnd() && Character.isDigit(peek())) {
            number.append(advance());
        }

        if (peek() == '.' && Character.isDigit(peek(1))) {
            number.append(advance());
            while (!atEnd() && Character.isDigit(peek())) {
                number.append(advance());
            }
            return new Token(TokenType.REAL_LITERAL, number.toString(),
                    spanFrom(startLine, startColumn));
        }

        return new Token(TokenType.INT_LITERAL, number.toString(),
                spanFrom(startLine, startColumn));
    }

    /** Пропускает пробелы, переводы строк и однострочные комментарии. */
    private void skipWhitespaceAndComments() {
        while (!atEnd()) {
            char c = peek();
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
                advance();
            } else if (c == '/' && peek(1) == '/') {
                // Комментарий идёт до конца строки
                while (!atEnd() && peek() != '\n') {
                    advance();
                }
            } else {
                return;
            }
        }
    }

    private boolean atEnd() {
        return pos >= source.length();
    }

    /** Текущий символ, не сдвигая позицию. */
    private char peek() {
        return peek(0);
    }

    /** Символ через offset позиций вперёд, не сдвигая позицию. */
    private char peek(int offset) {
        int index = pos + offset;
        return index < source.length() ? source.charAt(index) : EOF_CHAR;
    }

    /** Съедает текущий символ и возвращает его, сдвигая счётчики. */
    private char advance() {
        char c = source.charAt(pos++);
        if (c == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return c;
    }

    /** Токен с фиксированным написанием - текст берётся из TokenType. */
    private Token token(TokenType type, int startLine, int startColumn) {
        return new Token(type, type.getText(), spanFrom(startLine, startColumn));
    }

    /**
     * Координаты от запомненного начала до текущего места.
     * column уже указывает на символ ПОСЛЕ токена, поэтому минус один.
     */
    private Span spanFrom(int startLine, int startColumn) {
        return new Span(startLine, startColumn, column - 1);
    }
}
