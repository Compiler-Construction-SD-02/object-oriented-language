package com.innopolis.olang.lexer;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Координаты токена в исходном файле
 */
@Getter
@EqualsAndHashCode
public final class Span {

    /** Номер строки, с единицы */
    private final int line;

    /** Индекс начала токена в строке, начиная с единицы */
    private final int startColumn;

    /** Индекс конца токена в строке, начиная с единицы */
    private final int endColumn;

    public Span(int line, int startColumn, int endColumn) {

        if (line < 1) {
            throw new IllegalArgumentException("line must be >= 1, got " + line);
        }

        if (startColumn < 1) {
            throw new IllegalArgumentException("startColumn must be >= 1, got " + startColumn);
        }

        if (endColumn < startColumn) {
            throw new IllegalArgumentException(
                    "endColumn (" + endColumn + ") is before startColumn (" + startColumn + ")");
        }

        this.line = line;
        this.startColumn = startColumn;
        this.endColumn = endColumn;
    }

    /** Координаты одного символа. Для токенов вроде . , ( ) */
    public Span(int line, int column) {
        this(line, column, column);
    }

    /** Сколько символов занимает токен */
    public int length() {
        return endColumn - startColumn + 1;
    }

    @Override
    public String toString() {
        return length() <= 1
                ? line + ":" + startColumn
                : line + ":" + startColumn + "-" + endColumn;
    }
}
