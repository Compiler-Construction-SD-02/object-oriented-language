package com.innopolis.olang.parser.ast;

import com.innopolis.olang.lexer.Span;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * ReturnStatement : return [ Expression ]
 *
 * <p>Без выражения - выход из метода, который ничего не возвращает.
 */
@Getter
@RequiredArgsConstructor
public final class ReturnStatement extends Node implements Statement {

    /** Что возвращаем. null, если просто return. */
    private final Expression value;

    /** Координаты ключевого слова return. */
    private final Span span;
}
