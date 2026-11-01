package com.innopolis.olang.semantic;

import com.innopolis.olang.lexer.Span;

/**
 * Одна семантическая ошибка.
 */
public final class SemanticError {

    private final String message;

    private final Span span;

    public SemanticError(String message, Span span) {
        this.message = message;
        this.span = span;
    }

    public String getMessage() {
        return message;
    }

    public Span getSpan() {
        return span;
    }

    @Override
    public String toString() {
        return span + ": " + message;
    }
}
