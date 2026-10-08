package com.innopolis.olang.parser.ast;

import com.innopolis.olang.lexer.Span;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/** WhileLoop : while Expression loop Body end */
@Getter
@RequiredArgsConstructor
public final class WhileLoop extends Node implements Statement {

    /** Условие. Должно вычисляться в Boolean - это проверит следующий этап. */
    private final Expression condition;

    private final List<BodyElement> body;

    /** Координаты ключевого слова while. */
    private final Span span;
}
