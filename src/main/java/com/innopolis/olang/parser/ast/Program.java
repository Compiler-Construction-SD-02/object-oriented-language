package com.innopolis.olang.parser.ast;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/** Program : { ClassDeclaration } - вся программа, список классов. */
@Getter
@RequiredArgsConstructor
public final class Program extends Node {
    private final List<ClassDeclaration> classes;
}