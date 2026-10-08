package com.innopolis.olang.parser.ast;

import com.innopolis.olang.lexer.Span;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Целый литерал: 0, 42. Лексема уже разобрана в число. */
@Getter
@RequiredArgsConstructor
public final class IntegerLiteral extends Node implements Expression {

    private final int value;

    private final Span span;
}
