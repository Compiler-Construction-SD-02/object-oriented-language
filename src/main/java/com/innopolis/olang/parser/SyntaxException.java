package com.innopolis.olang.parser;

import com.innopolis.olang.lexer.Span;
import lombok.Getter;

/**
 * Токены пришли в порядке, который грамматика не допускает.
 * Несёт координаты токена, на котором парсер споткнулся.
 */
@Getter
public class SyntaxException extends RuntimeException {

    private final Span span;

    public SyntaxException(String message, Span span) {
        super(span + ": " + message);
        this.span = span;
    }
}