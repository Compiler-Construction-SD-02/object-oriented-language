package com.innopolis.olang.parser.ast;

import com.innopolis.olang.lexer.Span;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/** ClassDeclaration : class ClassName [ extends ClassName ] is { MemberDeclaration } end */
@Getter
@RequiredArgsConstructor
public final class ClassDeclaration extends Node {

    private final String name;

    /** Имя родителя. null, если extends не написан */
    private final String baseName;

    private final List<MemberDeclaration> members;

    /** Координаты имени класса */
    private final Span span;
}