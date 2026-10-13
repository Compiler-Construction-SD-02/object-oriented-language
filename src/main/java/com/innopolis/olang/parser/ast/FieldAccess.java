package com.innopolis.olang.parser.ast;

import com.innopolis.olang.lexer.Span;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Обращение к полю: Receiver . Identifier без скобок.
 */
@Getter
@RequiredArgsConstructor
public final class FieldAccess extends Node implements Expression {

    private final Expression receiver;

    private final String name;

    /** Координаты имени после точки. */
    private final Span span;
}
