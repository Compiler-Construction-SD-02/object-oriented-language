package com.innopolis.olang.semantic;

import com.innopolis.olang.lexer.Span;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Все классы программы по именам: и объявленные в файле, и встроенные.
 *
 * VOID - "ничего". Его возвращает метод без типа результата, например Print.
 * Нужен, чтобы поймать попытку использовать такой вызов как значение.
 *
 * UNKNOWN - "тип выяснить не удалось".
 */
public final class SymbolTable {

    public static final ClassSymbol VOID = new ClassSymbol("<nothing>", null, true, null);

    public static final ClassSymbol UNKNOWN = new ClassSymbol("<unknown>", null, true, null);

    private final Map<String, ClassSymbol> classes = new LinkedHashMap<>();

    public SymbolTable() {
        Builtins.fill(this);
    }

    public ClassSymbol find(String name) {
        return classes.get(name);
    }

    public boolean contains(String name) {
        return classes.containsKey(name);
    }

    public ClassSymbol declare(String name, String baseName, boolean builtin, Span span) {
        ClassSymbol symbol = new ClassSymbol(name, baseName, builtin, span);
        classes.put(name, symbol);
        return symbol;
    }

    public Map<String, ClassSymbol> getClasses() {
        return classes;
    }

    /** Есть ли смысл сравнивать этот тип с чем-либо: не null, не UNKNOWN, не VOID. */
    public static boolean isUsable(ClassSymbol type) {
        return type != null && type != UNKNOWN && type != VOID;
    }
}
