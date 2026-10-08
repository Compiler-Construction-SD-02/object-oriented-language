package com.innopolis.olang.parser.ast;

import com.innopolis.olang.lexer.Span;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Обращение к полю: Receiver . Identifier без скобок.
 *
 * <p>Грамматика такое допускает, поэтому парсер это разбирает. Разрешено ли
 * оно на самом деле, решает следующий этап: он знает, поле это или метод.
 */
@Getter
@RequiredArgsConstructor
public final class FieldAccess extends Node implements Expression {

    private final Expression receiver;

    private final String name;

    /** Координаты имени после точки. */
    private final Span span;
}
