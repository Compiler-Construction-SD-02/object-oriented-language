package com.innopolis.olang.lexer;

import lombok.Getter;

/**
 * Встретилось то, из чего токен не собрать.
 * Несёт координаты, чтобы сообщение показывало место
 */
@Getter
public class LexicalException extends RuntimeException {

    private final Span span;

    public LexicalException(String message, Span span) {
        super(span + ": " + message);
        this.span = span;
    }
}
