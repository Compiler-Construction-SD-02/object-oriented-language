package com.innopolis.olang.parser.ast;

import com.innopolis.olang.lexer.Span;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Создание объекта: ClassName Arguments. Например Integer(0) или Speaker().
 *
 * <p>От обычного имени отличается только скобками, поэтому парсер различает
 * их по следующему токену.
 */
@Getter
@RequiredArgsConstructor
public final class ConstructorInvocation extends Node implements Expression {

    private final String className;

    private final List<Expression> arguments;

    /** Координаты имени класса. */
    private final Span span;
}
