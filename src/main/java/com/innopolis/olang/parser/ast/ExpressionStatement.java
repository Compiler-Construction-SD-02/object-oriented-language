package com.innopolis.olang.parser.ast;

import com.innopolis.olang.lexer.Span;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Выражение в роли оператора
 */
@Getter
@RequiredArgsConstructor
public final class ExpressionStatement extends Node implements Statement {

    private final Expression expression;

    /** Координаты начала выражения. */
    private final Span span;
}
