package com.innopolis.olang.semantic;

import com.innopolis.olang.lexer.Span;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Класс: имя, родитель, поля, методы, конструкторы.
 */
public final class ClassSymbol {

    private final String name;

    /** Имя родителя как написано в исходнике. null, если extends нет. */
    private final String baseName;

    /** Родитель после того, как нашли эту строку в таблице классов*/
    @Setter
    private ClassSymbol base;

    private final Map<String, VariableSymbol> fields = new LinkedHashMap<>();

    private final List<MethodSymbol> methods = new ArrayList<>();

    private final List<MethodSymbol> constructors = new ArrayList<>();

    /** Встроенный класс стандартной библиотеки или нет. */
    private final boolean builtin;

    private final Span span;

    public ClassSymbol(String name, String baseName, boolean builtin, Span span) {
        this.name = name;
        this.baseName = baseName;
        this.builtin = builtin;
        this.span = span;
    }

    public String getName() {
        return name;
    }

    public String getBaseName() {
        return baseName;
    }

    public ClassSymbol getBase() {
        return base;
    }

    public Map<String, VariableSymbol> getFields() {
        return fields;
    }

    public List<MethodSymbol> getMethods() {
        return methods;
    }

    public List<MethodSymbol> getConstructors() {
        return constructors;
    }

    public boolean isBuiltin() {
        return builtin;
    }

    public Span getSpan() {
        return span;
    }

    /**
     * Является ли этот класс самим other или его наследником.
     * Идём вверх по цепочке родителей и ищем other.
     */
    public boolean isSubclassOf(ClassSymbol other) {
        for (ClassSymbol current = this; current != null; current = current.base) {
            if (current == other) {
                return true;
            }
        }
        return false;
    }

    /**
     * Ищет поле по имени: сначала в этом классе, потом у родителя и выше.
     * Наследник видит поля родителя, поэтому одного своего класса мало.
     * Не нашлось нигде - null, и зовущий выдаёт ошибку.
     */
    public VariableSymbol findField(String fieldName) {
        for (ClassSymbol current = this; current != null; current = current.base) {
            VariableSymbol field = current.fields.get(fieldName);
            if (field != null) {
                return field;
            }
        }
        return null;
    }

    /**
     * Все методы с таким именем: свои и унаследованные.
     * Список, а не один метод, потому что есть перегрузка - выбор
     * по типам аргументов делает resolve в анализаторе.
     * Идём снизу вверх, поэтому метод наследника встречается первым
     * и перекрывает родительский с такими же параметрами.
     */
    public List<MethodSymbol> findMethods(String methodName) {
        List<MethodSymbol> found = new ArrayList<>();
        for (ClassSymbol current = this; current != null; current = current.base) {
            for (MethodSymbol method : current.methods) {
                if (method.getName().equals(methodName) && notOverridden(found, method)) {
                    found.add(method);
                }
            }
        }
        return found;
    }

    /**
     * Не перекрыт ли этот метод уже найденным.
     * Идём снизу вверх, поэтому всё найденное раньше - ближе к объекту.
     * Совпали типы параметров - это переопределение, родительский пропускаем.
     * Разные параметры - это перегрузка, берём оба.
     */
    private boolean notOverridden(List<MethodSymbol> alreadyFound, MethodSymbol candidate) {
        return alreadyFound.stream().noneMatch(method -> method.sameParameterTypes(candidate));
    }

    @Override
    public String toString() {
        return name;
    }
}
