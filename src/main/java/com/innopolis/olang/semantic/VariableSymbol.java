package com.innopolis.olang.semantic;

import com.innopolis.olang.lexer.Span;
import lombok.Getter;

/**
 * Переменная: локальная, параметр метода или поле класса.
 *
 * В дереве у объявления лежит только имя и выражение. Здесь появляется
 * главное, чего в дереве нет - вычисленный тип.
 */
@Getter
public final class VariableSymbol {

    private final String name;

    private final ClassSymbol type;

    private final Span span;

    public VariableSymbol(String name, ClassSymbol type, Span span) {
        this.name = name;
        this.type = type;
        this.span = span;
    }

}
