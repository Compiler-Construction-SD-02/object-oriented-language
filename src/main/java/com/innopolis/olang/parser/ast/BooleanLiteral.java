package com.innopolis.olang.parser.ast;

import com.innopolis.olang.lexer.Span;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Логический литерал: true или false. */
@Getter
@RequiredArgsConstructor
public final class BooleanLiteral extends Node implements Expression {

    private final boolean value;

    private final Span span;
}
