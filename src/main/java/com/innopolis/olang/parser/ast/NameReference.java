package com.innopolis.olang.parser.ast;

import com.innopolis.olang.lexer.Span;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Имя, использованное как выражение: переменная, параметр или поле класса.
 *
 * <p>Парсер не знает, что из этого - он видит только идентификатор.
 * Разбираться, на что имя ссылается, будет следующий этап.
 */
@Getter
@RequiredArgsConstructor
public final class NameReference extends Node implements Expression {

    private final String name;

    private final Span span;
}
