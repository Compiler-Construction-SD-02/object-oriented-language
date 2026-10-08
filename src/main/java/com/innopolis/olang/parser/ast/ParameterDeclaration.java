package com.innopolis.olang.parser.ast;

import com.innopolis.olang.lexer.Span;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * ParameterDeclaration : Identifier : ClassName
 *
 * <p>Здесь после двоеточия стоит именно имя класса, а не выражение -
 * в отличие от объявления переменной.
 */
@Getter
@RequiredArgsConstructor
public final class ParameterDeclaration extends Node {

    private final String name;

    /** Имя класса-типа параметра. */
    private final String typeName;

    /** Координаты имени параметра. */
    private final Span span;
}
