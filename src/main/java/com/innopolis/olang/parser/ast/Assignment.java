package com.innopolis.olang.parser.ast;

import com.innopolis.olang.lexer.Span;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Assignment : Identifier := Expression
 */
@Getter
@RequiredArgsConstructor
public final class Assignment extends Node implements Statement {

    /** Имя переменной слева от :=. */
    private final String target;

    private final Expression value;

    /** Координаты имени слева. */
    private final Span span;
}
