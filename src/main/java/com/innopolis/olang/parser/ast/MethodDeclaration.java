package com.innopolis.olang.parser.ast;

import com.innopolis.olang.lexer.Span;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * MethodHeader : method Identifier [ Parameters ] [ : ClassName ]
 * MethodBody   : is Body end | => Expression
 *
 * <p>Короткую форму => Expression парсер сразу превращает в тело из одного
 * return, потому что смысл у них одинаковый. Дальше по компилятору ходит
 * только одна форма, и обрабатывать два случая больше нигде не надо.
 */
@Getter
@RequiredArgsConstructor
public final class MethodDeclaration extends Node implements MemberDeclaration {

    private final String name;

    private final List<ParameterDeclaration> parameters;

    /** Имя класса-результата. null, если метод ничего не возвращает. */
    private final String returnTypeName;

    /** Тело метода. null, если тела нет вообще - такой метод абстрактный. */
    private final List<BodyElement> body;

    /** Координаты имени метода. */
    private final Span span;

    /** Тела нет - значит метод только объявлен, а реализован в наследниках. */
    public boolean isAbstract() {
        return body == null;
    }
}
