package com.innopolis.olang.parser.ast;

import com.innopolis.olang.lexer.Span;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * VariableDeclaration : var Identifier : Expression
 *
 * <p>Тип не пишется - он берётся из выражения справа. В var x : Integer(0)
 * после двоеточия стоит не имя типа, а создание объекта.
 *
 * <p>Объявить переменную можно и в классе, и в теле метода, поэтому узел
 * реализует обе роли сразу.
 */
@Getter
@RequiredArgsConstructor
public final class VariableDeclaration extends Node implements MemberDeclaration, BodyElement {

    private final String name;

    /** Выражение справа от двоеточия. Оно же задаёт тип переменной. */
    private final Expression initializer;

    /** Координаты имени переменной. */
    private final Span span;
}
