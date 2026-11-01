package com.innopolis.olang.semantic;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * Область видимости: что за имена доступны в текущем месте программы.
 *
 * Только для переменных - локальных и параметров
 *
 * Области вложены одна в другую и образуют цепочку. Поиск имени идёт
 * от самой внутренней наружу, и побеждает ближайшее объявление.
 *
 * Поля класса в эту цепочку не входят - они ищутся отдельно, через
 * ClassSymbol, потому что видны и в методах, и в наследниках.
 */
public final class Scope {

    @Getter
    private final Scope parent;

    private final Map<String, VariableSymbol> variables = new HashMap<>();

    public Scope(Scope parent) {
        this.parent = parent;
    }

    /** Искать только здесь. Нужно для проверки на повторное объявление. */
    public VariableSymbol findHere(String name) {
        return variables.get(name);
    }

    /** Искать здесь и во всех внешних областях. */
    public VariableSymbol find(String name) {
        for (Scope current = this; current != null; current = current.parent) {
            VariableSymbol variable = current.variables.get(name);
            if (variable != null) {
                return variable;
            }
        }
        return null;
    }

    public void declare(VariableSymbol variable) {
        variables.put(variable.getName(), variable);
    }
}
