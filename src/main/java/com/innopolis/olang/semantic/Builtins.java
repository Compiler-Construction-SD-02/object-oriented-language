package com.innopolis.olang.semantic;

import java.util.ArrayList;
import java.util.List;

/**
 * Стандартная библиотека языка O, описанная вручную.
 */
final class Builtins {

    private Builtins() {
    }

    static void fill(SymbolTable table) {
        // Class
        // ├─ AnyValue - Integer, Real, Boolean
        // └─ AnyRef   - ArrayInteger, ListInteger, ...
        ClassSymbol root = table.declare("Class", null, true, null);
        ClassSymbol anyValue = table.declare("AnyValue", "Class", true, null);
        ClassSymbol anyRef = table.declare("AnyRef", "Class", true, null);
        anyValue.setBase(root);
        anyRef.setBase(root);

        ClassSymbol integer = table.declare("Integer", "AnyValue", true, null);
        ClassSymbol real = table.declare("Real", "AnyValue", true, null);
        ClassSymbol bool = table.declare("Boolean", "AnyValue", true, null);
        integer.setBase(anyValue);
        real.setBase(anyValue);
        bool.setBase(anyValue);

        // Integer
        constructor(integer);
        constructor(integer, integer);
        constructor(integer, real);
        arithmetic(integer, integer, real);
        comparisons(integer, bool, integer, real);
        method(integer, "Min", integer, integer);
        method(integer, "Max", integer, integer);
        method(integer, "UnaryMinus", integer);
        method(integer, "toReal", real);
        method(integer, "toBoolean", bool);
        method(integer, "Print", null);

        // Real
        constructor(real);
        constructor(real, real);
        constructor(real, integer);
        arithmetic(real, real, integer);
        comparisons(real, bool, real, integer);
        method(real, "Min", real, real);
        method(real, "Max", real, real);
        method(real, "UnaryMinus", real);
        method(real, "toInteger", integer);
        method(real, "Epsilon", real);
        method(real, "Print", null);

        // Boolean
        constructor(bool);
        constructor(bool, bool);
        method(bool, "Or", bool, bool);
        method(bool, "And", bool, bool);
        method(bool, "Xor", bool, bool);
        method(bool, "Not", bool);
        method(bool, "toInteger", integer);
        method(bool, "Print", null);

        // Контейнеры. Дженериков нет, поэтому шесть отдельных классов,
        // и каждый описывается одинаково, только с другим типом элемента.
        container(table, anyRef, "ArrayInteger", "ListInteger", integer);
        container(table, anyRef, "ArrayReal", "ListReal", real);
        container(table, anyRef, "ArrayBoolean", "ListBoolean", bool);
    }

    private static void container(SymbolTable table, ClassSymbol anyRef,
                                  String arrayName, String listName, ClassSymbol element) {
        ClassSymbol array = table.find(arrayName) != null
                ? table.find(arrayName)
                : table.declare(arrayName, "AnyRef", true, null);
        ClassSymbol list = table.find(listName) != null
                ? table.find(listName)
                : table.declare(listName, "AnyRef", true, null);
        array.setBase(anyRef);
        list.setBase(anyRef);

        ClassSymbol integer = table.find("Integer");

        // Array: длина задаётся в конструкторе
        constructor(array, integer);
        method(array, "Length", integer);
        method(array, "get", element, integer);
        method(array, "set", null, integer, element);
        method(array, "toList", list);

        // List: пустой, из одного элемента, из элемента с количеством
        constructor(list);
        constructor(list, element);
        constructor(list, element, integer);
        method(list, "append", list, element);
        method(list, "head", element);
        method(list, "tail", list);
    }

    /** Plus, Minus, Mult, Div, Rem - перегружены для своего типа и для второго. */
    private static void arithmetic(ClassSymbol owner, ClassSymbol same, ClassSymbol other) {
        for (String name : List.of("Plus", "Minus", "Mult", "Div", "Rem")) {
            method(owner, name, same, same);
            method(owner, name, other, other);
        }
    }

    /** Less, LessEqual, Greater, GreaterEqual, Equal - всегда дают Boolean. */
    private static void comparisons(ClassSymbol owner, ClassSymbol bool,
                                    ClassSymbol same, ClassSymbol other) {
        for (String name : List.of("Less", "LessEqual", "Greater", "GreaterEqual", "Equal")) {
            method(owner, name, bool, same);
            method(owner, name, bool, other);
        }
    }

    /** returnType == null означает "метод ничего не возвращает". */
    private static void method(ClassSymbol owner, String name,
                               ClassSymbol returnType, ClassSymbol... parameterTypes) {
        owner.getMethods().add(
                new MethodSymbol(name, parameters(parameterTypes), returnType, owner, true, null));
    }

    private static void constructor(ClassSymbol owner, ClassSymbol... parameterTypes) {
        owner.getConstructors().add(
                new MethodSymbol(owner.getName(), parameters(parameterTypes), null, owner, true, null));
    }

    private static List<VariableSymbol> parameters(ClassSymbol[] types) {
        List<VariableSymbol> parameters = new ArrayList<>();
        for (int i = 0; i < types.length; i++) {
            parameters.add(new VariableSymbol("p" + i, types[i], null));
        }
        return parameters;
    }
}
