package com.innopolis.olang.parser.ast;

import com.innopolis.olang.lexer.Span;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Ключевое слово this как выражение - ссылка на текущий объект.
 *
 * <p>Хранить нечего, кроме координат: что именно означает this,
 * определяется тем, внутри какого класса он написан.
 */
@Getter
@RequiredArgsConstructor
public final class ThisExpression extends Node implements Expression {

    private final Span span;
}
