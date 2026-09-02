# Object-oriented language on Java with handwritten parser for JVM

## Description of language O

A summary of the object-oriented language O specification.

### 1. Overall Program Structure

A program in O is a (possibly empty) sequence of class declarations. The entry point is the constructor of some class, chosen when the program is launched, together with the constructor's arguments. Execution begins by creating an unnamed instance of that class and transferring control to the constructor body; when the constructor body finishes, the whole program finishes.

### 2. Class - the Only Type Concept

In O, a class is the only way to define a type. Class declaration syntax:

```
class Name [ extends BaseClass ]
is
    { member declaration }
end
```

A class consists of simple member variables (defining the current state of an instance) and methods (defining behavior). A class may also define special constructor methods used to create instances.

### 3. Inheritance

A class may inherit from another (base) class, gaining everything defined in the base class in addition to its own members. Inheritance is transitive: if A is a base of B, and B is a base of C, then A is also (indirectly) a base of C.

Inheritance allows referring to a derived-class object through a base-type variable - the foundation of polymorphism: if a method in a derived class matches the signature of a base-class method, it overrides it, and at runtime the method actually invoked is determined by the object's dynamic type.


### 4. Class Member Variables

```
var Name : Expression
```

Important detail: the variable's type is not stated explicitly - it is inferred from the type of the initializing expression. Member variables are read-only from outside the class; they cannot be assigned directly via dot notation - state can only be changed through class methods.

### 5. Methods

```
method Name [ (parameters) ] [ : ReturnType ]
is
    body
end
```

If a method body is just a single returned expression, a short form is available:

```
method Name(parameters) : Type => Expression
```

The method body may be omitted in a declaration - this is a forward declaration (the method can be called before it is fully defined, but a full definition with the same name and signature must appear somewhere in the program).

Overloading is allowed - several methods can share a name as long as they differ in the number or types of parameters; the compiler picks the specific method based on matching arguments to parameters.

Parameters are declared as `Name : ClassName`.

### 6. Constructors

```
this [ (parameters) ]
is
    body
end
```

### 7. Statements

The minimal required set:

- **Assignment**: `Identifier := Expression`
- **Loop**: only one kind - `while Expression loop body end` (a conditional loop, executed zero or more times)
- **Conditional**: `if Expression then body [ else body ] end`
- **Return**: `return [ Expression ]`

### 8. Expressions

The expression structure in O is much simpler than in most languages - essentially just member access and method calls:

```
Expression : Primary | ConstructorInvocation | MethodCall | Expression.Expression
```

**Important: the language has no infix operators at all.** Everything is expressed through method calls in dot notation: `x.Plus(1)` instead of `x + 1`, `a.Less(b)` instead of `a < b`, and so on.

Object creation: `ClassName(arguments)` - e.g. `Derived(3)`.

Member access / method call: `object.member` or `object.method(arguments)` - these can be chained together.

**Primary** (basic building blocks of expressions): integer literal, real literal, boolean literal, or the keyword `this` (denotes the current object inside a method).

### 9. Standard Library Classes

The predefined class hierarchy:

```
Class
 ├─ AnyValue
 │   ├─ Integer
 │   ├─ Real
 │   └─ Boolean
 └─ AnyRef
     ├─ Array
     └─ List
```

These classes are available in every program without any explicit import. Each defines its own constructors and methods:

- **Integer**: constructors from Integer/Real; fields `Min`/`Max`; conversions `toReal`/`toBoolean`; unary minus; arithmetic (`Plus`, `Minus`, `Mult`, `Div`, `Rem`) overloaded for Integer and Real; comparisons (`Less`, `LessEqual`, `Greater`, `GreaterEqual`, `Equal`)
- **Real**: analogous to Integer, plus an `Epsilon` field, and `toInteger` conversion
- **Boolean**: constructor; `toInteger` conversion; logical operations `Or`, `And`, `Xor`, `Not`
- **Array[T]**: constructor taking a length; conversion to `List`; methods `Length`, `get(i)`, `set(i, v)`
- **List[T]**: several constructors; list operations (`append`, `head`, `tail`, and others not fully enumerated in the document)

### 10. Examples from the Document (syntax reference)

```
var a : Array[Integer](10)
a.set(i) := 55
x := a.get(i.Plus(1))
```

```
method MaxInt(a: Array[Integer]) : Integer is
    var max : Integer.Min
    var i : Integer(1)
    while i.Less(a.Length) loop
        if a.get(i).Greater(max) then max := get(i)
        end
        i := i.Plus(1)
    end
    return max
end
```