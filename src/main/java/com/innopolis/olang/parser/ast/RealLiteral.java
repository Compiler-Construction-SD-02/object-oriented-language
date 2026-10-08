package com.innopolis.olang.parser.ast;

import com.innopolis.olang.lexer.Span;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Вещественный литерал: 1.5, 0.0. */
@Getter
@RequiredArgsConstructor
public final class RealLiteral extends Node implements Expression {

    private final double value;

    private final Span span;
}
