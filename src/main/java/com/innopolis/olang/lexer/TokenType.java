package com.innopolis.olang.lexer;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Все виды токенов языка O
 */
public enum TokenType {

    // ключевые слова
    CLASS("class"),
    EXTENDS("extends"),
    IS("is"),
    END("end"),
    VAR("var"),
    METHOD("method"),
    THIS("this"),
    WHILE("while"),
    LOOP("loop"),
    IF("if"),
    THEN("then"),
    ELSE("else"),
    RETURN("return"),
    TRUE("true"),
    FALSE("false"),

    // знаки
    COLON(":"),
    ASSIGN(":="),
    ARROW("=>"),
    DOT("."),
    COMMA(","),
    LPARENTHESIS("("),
    RPARENTHESIS(")"),

    // всё остальное
    IDENTIFIER(null),
    INT_LITERAL(null),
    REAL_LITERAL(null),
    EOF(null);

    private final String text;

    TokenType(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    /**
     * Словарь "слово -> вид токена" для ключевых слов
     * "class" -> CLASS
     * "extends" - > EXTENDS
     */
    private static final Map<String, TokenType> KEYWORDS = Arrays.stream(values())
            .filter(TokenType::isKeyword)
            .collect(Collectors.toMap(TokenType::getText, Function.identity()));

    /**
     * Проверка на то, является ли токен ключевым словом
     */
    private boolean isKeyword() {
        return text != null && Character.isLetter(text.charAt(0));
    }

    /**
     * Если слово является ключевым, то вернуть его вид, иначе обертку с null
     */
    public static Optional<TokenType> keyword(String word) {
        return Optional.ofNullable(KEYWORDS.get(word));
    }
}
