package com.innopolis.olang.lexer;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Один токен: вид, кусок исходного текста и координаты
 */
@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
public final class Token {

    /** Вид токена. */
    private final TokenType type;

    /** Лексема - фрагмент исходника как есть. У EOF пустая строка. */
    private final String text;

    /** Где токен находится в файле. */
    private final Span span;

    @Override
    public String toString() {
        return type + " " + text + " " + span;
    }
}
