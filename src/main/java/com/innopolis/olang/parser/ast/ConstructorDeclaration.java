package com.innopolis.olang.parser.ast;

import com.innopolis.olang.lexer.Span;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * ConstructorDeclaration : this [ Parameters ] is Body end
 *
 * <p>Имени у конструктора нет - он всегда this.
 */
@Getter
@RequiredArgsConstructor
public final class ConstructorDeclaration extends Node implements MemberDeclaration {

    private final List<ParameterDeclaration> parameters;

    private final List<BodyElement> body;

    /** Координаты ключевого слова this. */
    private final Span span;
}
