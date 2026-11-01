package com.innopolis.olang.semantic;

import com.innopolis.olang.lexer.Span;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Метод или конструктор: имя, типы параметров, тип результата.
 *
 * returnType == null означает, что метод ничего не возвращает. Для
 * конструкторов это всегда так.
 *
 * hasBody == false - метод только объявлен. В тесте 12 так сделано
 * предварительное объявление IsOdd, чтобы IsEven мог его позвать выше по тексту.
 */
public final class MethodSymbol {

    @Getter
    private final String name;

    @Getter
    private final List<VariableSymbol> parameters;

    @Getter
    private final ClassSymbol returnType;

    @Getter
    private final ClassSymbol owner;

    @Getter
    private final Span span;

    @Setter
    private boolean hasBody;

    public MethodSymbol(String name, List<VariableSymbol> parameters, ClassSymbol returnType,
                        ClassSymbol owner, boolean hasBody, Span span) {
        this.name = name;
        this.parameters = parameters;
        this.returnType = returnType;
        this.owner = owner;
        this.hasBody = hasBody;
        this.span = span;
    }

    public boolean hasBody() {
        return hasBody;
    }

    /** Совпадают ли типы параметров - по этому мы отличаем перегрузку от дубликата. */
    public boolean sameParameterTypes(MethodSymbol other) {
        if (parameters.size() != other.parameters.size()) {
            return false;
        }
        for (int i = 0; i < parameters.size(); i++) {
            if (parameters.get(i).getType() != other.parameters.get(i).getType()) {
                return false;
            }
        }
        return true;
    }

    /** Для сообщений об ошибках. */
    public String signature() {
        return name + parameters.stream()
                .map(parameter -> parameter.getType().getName())
                .collect(Collectors.joining(", ", "(", ")"));
    }
}
