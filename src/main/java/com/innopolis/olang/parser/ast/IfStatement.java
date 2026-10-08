package com.innopolis.olang.parser.ast;

import com.innopolis.olang.lexer.Span;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/** IfStatement : if Expression then Body [ else Body ] end */
@Getter
@RequiredArgsConstructor
public final class IfStatement extends Node implements Statement {

    private final Expression condition;

    private final List<BodyElement> thenBody;

    /** Ветка else. null, если её не написали. */
    private final List<BodyElement> elseBody;

    /** Координаты ключевого слова if. */
    private final Span span;
}
