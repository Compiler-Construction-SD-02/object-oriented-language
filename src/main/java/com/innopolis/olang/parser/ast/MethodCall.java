package com.innopolis.olang.parser.ast;

import com.innopolis.olang.lexer.Span;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Вызов метода: Receiver . Identifier Arguments. Например a.Plus(b).
 *
 * <p>receiver - это всё, что стоит слева от точки, само выражение. Поэтому
 * цепочка a.Plus(b).Mult(c) - это MethodCall(Mult), внутри которого лежит
 * MethodCall(Plus), внутри которого NameReference(a). Дерево растёт влево,
 * как и положено: вычисляется цепочка слева направо.
 */
@Getter
@RequiredArgsConstructor
public final class MethodCall extends Node implements Expression {

    /** Объект, у которого зовём метод. */
    private final Expression receiver;

    private final String methodName;

    private final List<Expression> arguments;

    /** Координаты имени метода. */
    private final Span span;
}
