# Compiler for the object-oriented language **O**

A hand-written compiler (lexer → parser → semantic analyzer → JVM bytecode) for **O**, the
object-oriented language from *Project O* of the Compiler Construction course.
Implementation language: **Java 21**, build: **Maven**, target platform: **JVM**.

> **Status:** week 1 - language study, list of open questions to the specification, and a test
> suite that fixes the syntax we are going to accept. No compiler code yet.

---

## Repository layout

```
.
├── README.md                    ← language description + our design decisions (this file)
├── docs/
│   └── testsForFirstWeek.md     ← what every test case checks
├── examples/                    ← test programs in O (*.o91)
├── src/main/java/...            ← compiler sources (to be written)
└── pom.xml
```

Source files of the language use the extension **`.o91`**.

---

## 1. Language O in one page

O is a *pure* object-oriented language: a class is the only way to introduce a type, and there
are **no infix operators at all** - every arithmetic, logical and relational operation is an
ordinary method call in dot notation.

```
class Main is
    this() is
        var x : Integer(2)
        var y : Integer(3)
        var sum : x.Plus(y)          // instead of  x + y
        sum.Print()                  // 5
    end
end
```

A program is a sequence of class declarations. The entry point is **the constructor of one of
the classes**, chosen when the program is launched; its arguments come from the command line.
Creating that unnamed object and running its constructor body *is* the whole program run - when
the body ends, the program ends.

By convention in this repository the entry class is called `Main` and its entry constructor is
the parameterless `this()`.

---

## 2. Classes

### 2.1 The simplest class

```
class Answer is
    method Value : Integer is
        return Integer(42)
    end
end
```

* A class body is delimited by `is` … `end`.
* A class may declare three kinds of members: **variables**, **methods**, **constructors**.
* A class with an empty body is legal:

```
class Marker is
end
```

### 2.2 Member variables

```
var Identifier : Expression
```

The type is **never written explicitly** - it is inferred from the initializing expression.
This means a variable can never be uninitialized.

```
class Point is
    var x : Integer(0)          // inferred type: Integer
    var y : Integer(0)          // inferred type: Integer
    var visible : true          // inferred type: Boolean
    var scale : Real(1.0)       // inferred type: Real
end
```

Member variables are **read-only from the outside**: `p.x` may be read, but `p.x := 5` is
forbidden. State is changed only from inside the class's own methods (or methods of derived
classes).

```
class Main is
    this() is
        var p : Point()
        p.x.Print()             // OK - reading
        // p.x := Integer(5)    // ERROR - a member cannot be assigned from outside
    end
end
```

### 2.3 Constructors

```
this [ ( parameters ) ] is
    body
end
```

A class may declare several constructors, as long as they differ in their parameter lists.

```
class Counter is
    var value : Integer(0)
    var step  : Integer(1)

    this() is
    end

    this(start: Integer) is
        value := start
    end

    this(start: Integer, s: Integer) is
        value := start
        step  := s
    end

    method Next is
        value := value.Plus(step)
    end

    method Get : Integer => value
end
```

Objects are created by writing the class name with an argument list - there is no `new`:

```
var c1 : Counter()
var c2 : Counter(Integer(10))
var c3 : Counter(Integer(10), Integer(5))
```

### 2.4 Methods

```
method Identifier [ ( parameters ) ] [ : ReturnClass ]
is
    body
end
```

Parameters - unlike variables - **do** carry an explicit type: `Identifier : ClassName`.
The return type is optional; a method without one returns nothing and can only be used as a
statement.

```
class Rectangle is
    var width  : Integer(0)
    var height : Integer(0)

    this(w: Integer, h: Integer) is
        width  := w
        height := h
    end

    // full form
    method Area : Integer is
        return width.Mult(height)
    end

    // short form: the body is a single expression
    method Perimeter : Integer => width.Plus(height).Mult(Integer(2))

    // no return type - a pure command
    method Scale(k: Integer) is
        width  := width.Mult(k)
        height := height.Mult(k)
    end
end
```

**Short form.** `method Name(params) : Type => Expression` is sugar for a body consisting of a
single `return`.

**Calling a method of your own object.** Inside a method, members of the current object are
reachable by their bare name, or explicitly through `this`:

```
class Circleish is
    var r : Integer(1)

    method R : Integer => r
    method Diameter : Integer => R().Mult(Integer(2))         // implicit this
    method Area     : Integer => this.R().Mult(this.R())      // explicit this
end
```

A parameterless method is **declared without parentheses** but **called with empty
parentheses**: `method Area : Integer` → `shape.Area()`.

### 2.5 Overloading

Several methods may share a name if they differ in the number or the types of parameters. The
compiler chooses the one to call by comparing the argument types with the parameter types.

```
class Combiner is
    method Combine(a: Integer) : Integer          => a.Plus(Integer(1))
    method Combine(a: Real)    : Real             => a.Plus(Real(1.0))
    method Combine(a: Integer, b: Integer) : Integer => a.Plus(b)
end

class Main is
    this() is
        var c : Combiner()
        c.Combine(Integer(5)).Print()        // Integer version  → 6
        c.Combine(Real(5.0)).Print()         // Real version     → 6.0
        c.Combine(Integer(2), Integer(3)).Print()   // two-argument version → 5
    end
end
```

Constructors are overloaded by exactly the same rule.

### 2.6 Forward declarations

A method header without a body is a **forward declaration**: the method may be called before it
is fully defined, but a full definition with the same name and the same signature must appear
somewhere in the same class.

```
class Parity is
    method IsOdd(n: Integer) : Boolean        // forward: no body yet

    method IsEven(n: Integer) : Boolean is
        if n.Equal(Integer(0)) then
            return true
        end
        return IsOdd(n.Minus(Integer(1)))     // legal: IsOdd is declared above
    end

    method IsOdd(n: Integer) : Boolean is     // the actual definition
        if n.Equal(Integer(0)) then
            return false
        end
        return IsEven(n.Minus(Integer(1)))
    end
end
```

---

## 3. Inheritance and polymorphism

```
class Derived extends Base is ... end
```

A derived class contains everything the base class declares, plus its own members. Inheritance
is **transitive**: if `A` is a base of `B` and `B` is a base of `C`, then everything from `A` is
available in `C`.

```
class A is
    method Value : Integer => Integer(10)
end

class B extends A is
end

class C extends B is
    method DoubleValue : Integer => Value().Mult(Integer(2))   // Value() comes from A
end
```

A method of a derived class whose signature matches a base-class method **overrides** it. Which
one actually runs is decided at run time by the **dynamic** type of the object:

```
class Base is
    method Speak : Integer => Integer(1)
end

class Derived extends Base is
    method Speak : Integer => Integer(2)      // overrides Base.Speak
end

class Main is
    this() is
        var a : Base()        // static type: Base
        a := Derived()        // dynamic type: Derived
        a.Speak().Print()     // 2 - the Derived version runs
    end
end
```

A variable of a base type may hold an object of any derived type; the reverse is not allowed.

Full example - an array of shapes, each with its own `Area`:

```
class Shape is
    method Area : Integer => Integer(0)
end

class Square extends Shape is
    var side : Integer(0)
    this(s: Integer) is
        side := s
    end
    method Area : Integer => side.Mult(side)
end

class Rectangle extends Shape is
    var width  : Integer(0)
    var height : Integer(0)
    this(w: Integer, h: Integer) is
        width  := w
        height := h
    end
    method Area : Integer => width.Mult(height)
end

class Main is
    this() is
        var shapes : ArrayShape(Integer(2))
        shapes.set(Integer(0), Square(Integer(4)))
        shapes.set(Integer(1), Rectangle(Integer(3), Integer(5)))

        var total : Integer(0)
        var i : Integer(0)
        while i.Less(shapes.Length()) loop
            total := total.Plus(shapes.get(i).Area())   // dynamic dispatch
            i := i.Plus(Integer(1))
        end
        total.Print()                                   // 16 + 15 = 31
    end
end
```

---

## 4. Statements

O deliberately defines a minimal set of statements.

| Statement | Syntax |
|---|---|
| Assignment | `Identifier := Expression` |
| Loop | `while Expression loop Body end` |
| Conditional | `if Expression then Body [ else Body ] end` |
| Return | `return [ Expression ]` |
| Local declaration | `var Identifier : Expression` |

`while` is the only loop form; the condition must be `Boolean` and is re-evaluated before every
iteration, so the body may run zero times. `if` also requires a `Boolean` condition and is
closed by `end`, which removes any dangling-`else` ambiguity when conditionals are nested.

```
class Math is
    method Sum(n: Integer) : Integer is
        var i   : Integer(1)
        var acc : Integer(0)
        while i.LessEqual(n) loop
            acc := acc.Plus(i)
            i   := i.Plus(Integer(1))
        end
        return acc
    end

    method Sign(n: Integer) : Integer is
        if n.Greater(Integer(0)) then
            return Integer(1)
        else
            if n.Less(Integer(0)) then
                return Integer(0).UnaryMinus()
            else
                return Integer(0)
            end
        end
    end
end
```

Local variables may be declared anywhere in a body, including inside a loop:

```
while j.Less(n) loop
    var temp : a.get(j)          // declared and typed on every iteration
    ...
end
```

---

## 5. Expressions

An expression is built from just two constructions: **member access** and **method call**,
both written in dot notation and freely chainable.

```
Expression : Primary { . Identifier [ Arguments ] }
Primary    : IntegerLiteral | RealLiteral | BooleanLiteral | this | Identifier
           | ClassName Arguments          // constructor invocation
```

```
x := a.get(i.Plus(Integer(1)))
b := p.Left().Distance(p.Right()).Greater(Real(0.5))
```

**Literals are objects of library classes.** An integer literal is an `Integer`, a real literal
is a `Real`, `true` / `false` are `Boolean`. Therefore `4` and `Integer(4)` denote exactly the same
object and are interchangeable.

**Style convention.** In this repository we always write the explicit constructor form, including
array lengths and indices, so that every value in a program is visibly an object:

```
var a : ArrayInteger(Integer(5))
a.set(Integer(0), Integer(10))
a.get(Integer(0)).Print()
```

`this` inside a method body denotes the object the method was called on.

Because there are no operators, everything reads as a call:

| Usual notation | O |
|---|---|
| `a + b` | `a.Plus(b)` |
| `a - b` | `a.Minus(b)` |
| `a * b` | `a.Mult(b)` |
| `a / b` | `a.Div(b)` |
| `a % b` | `a.Rem(b)` |
| `-a` | `a.UnaryMinus()` |
| `a < b` | `a.Less(b)` |
| `a >= b` | `a.GreaterEqual(b)` |
| `a == b` | `a.Equal(b)` |
| `a && b` | `a.And(b)` |
| `!a` | `a.Not()` |

---

## 6. Standard library

The library classes are available in every program with no import:

```
Class
 ├─ AnyValue
 │   ├─ Integer
 │   ├─ Real
 │   └─ Boolean
 └─ AnyRef
     ├─ Array…
     └─ List…
```

| Class | Members |
|---|---|
| `Integer` | constructors from `Integer` / `Real`; `Min`, `Max`; `toReal`, `toBoolean`; `UnaryMinus`; `Plus`, `Minus`, `Mult`, `Div`, `Rem` (overloaded for `Integer` and `Real`); `Less`, `LessEqual`, `Greater`, `GreaterEqual`, `Equal` |
| `Real` | same shape as `Integer`, plus `Epsilon` and `toInteger` |
| `Boolean` | constructor; `toInteger`; `Or`, `And`, `Xor`, `Not` |
| `Array…` | constructor taking the length; `Length()`; `get(i)`; `set(i, v)`; `toList()` |
| `List…` | constructors (empty / one element / element + count); `append(v)`, `head()`, `tail()` |

```
class Main is
    this() is
        var a : ArrayInteger(Integer(5))
        a.set(Integer(0), Integer(10))
        a.set(Integer(1), Integer(20))
        a.get(Integer(0)).Print()
        a.Length().Print()

        var l : ListInteger()
        l := l.append(Integer(1))
        l := l.append(Integer(2))
        l.head().Print()
        l.tail().head().Print()
    end
end
```

---

## 7. Our design decisions (deviations from *Project O*)

The specification leaves several things to the implementer, and one feature is explicitly out of
scope for the course. This section records what **this** implementation does.

### 7.1 No generics - monomorphic containers instead

The specification describes generic classes (`Array[T]`, `List[T]`, `class C[T]`), but marks
them as *not a subject for implementation within the Compiler Construction course*. We do not
implement them: **there is no `[...]` type syntax in our grammar at all.**

Instead, a container is a plain class whose name encodes the element type:

> **Naming rule:** `Array` / `List` + the exact name of the element class.

| Generic form | Our form |
|---|---|
| `Array[Integer]` | `ArrayInteger` |
| `Array[Real]` | `ArrayReal` |
| `Array[Boolean]` | `ArrayBoolean` |
| `Array[Shape]` | `ArrayShape` |
| `List[Integer]` | `ListInteger` |
| `List[Real]` | `ListReal` |

The container classes for the built-in element types are part of the standard library.
`Array<UserClass>` / `List<UserClass>` are generated by the compiler on demand, from the same
template, when it sees such a name used in the program. Since `ArrayShape` stores `Shape`
references, it also accepts objects of classes derived from `Shape` - so polymorphic collections
still work (see the `shapes` example above and `examples/19_array_polymorphism.o91`).

### 7.2 `Print` - our extension

The specification defines no input/output at all, which makes test programs impossible to
observe. We add one method to the library classes:

```
method Print          // prints the value and a line break, returns nothing
```

It is available on `Integer`, `Real` and `Boolean`. This is **our addition, not part of
Project O**, and it is used only in test programs.

### 7.3 Expression statements

The grammar in the specification allows only assignment, loop, conditional and return as
statements - a bare method call such as `total.Print()` or `counter.Next()` is not derivable.
Since methods without a return type would otherwise be uncallable, we add:

```
Statement : ... | Expression
```

### 7.4 Identifiers in `Primary`

`Primary` in the specification is `IntegerLiteral | RealLiteral | BooleanLiteral | this` - a plain
variable reference is not among the basic building blocks of an expression, even though the
document's own examples use variables everywhere (`x := a.get(i.Plus(1))`).

Strictly speaking such an expression is still derivable, but only through the wrong production:
`ConstructorInvocation : ClassName [ Arguments ]`, where `ClassName : Identifier` and `Arguments`
are optional, turns a bare `a` into "create an object of class `a`" - syntactically accepted,
semantically nonsense.

We therefore add `Identifier` to `Primary`, so that reading a variable has a production of its own.

### 7.5 Constructor invocation vs. plain identifier

Both a constructor invocation and a variable reference start with an identifier. Our parser
resolves it during name analysis: an identifier that names a **class** and is followed by an
argument list is a constructor invocation; otherwise it is a variable or a method of `this`.

### 7.6 Implicit default constructor

The specification never says what happens to a class that declares no constructor at all, yet its
own examples - and ours - write `Base()`, `Derived()`, `C()`.

We adopt the Java rule: **a class that declares no constructor gets an implicit parameterless one**
with an empty body, which only runs the member-variable initializers. A class that declares at
least one constructor gets no implicit one, so for the `Square` above `Square(Integer(4))` is valid
while `Square()` is an error.

### 7.7 Initial contents of an array

`ArrayInteger(Integer(5))` allocates five cells - the specification does not say what is in them.

Every cell is filled with a **freshly created object of the element class, built by that class's
parameterless constructor**:

| Array | Every cell initially holds |
|---|---|
| `ArrayInteger(Integer(n))` | `Integer(0)` |
| `ArrayReal(Integer(n))` | `Real(0.0)` |
| `ArrayBoolean(Integer(n))` | `Boolean(false)` |
| `ArrayFoo(Integer(n))` | `Foo()` |

If the element class declares constructors but no parameterless one, creating an array of it is a
compile-time error - there would be nothing to fill the cells with.

The pleasant consequence: **the language has no null**. Reading a cell that was never written
returns a real object, so no null check is ever needed and no null dereference can occur. The price
is that array creation eagerly constructs all of its elements.

---

## 8. Grammar we accept

The specification's grammar with the corrections from §7 applied: no generic type syntax, no
separate `FunctionCall` production (it made the grammar cyclic), `Identifier` added to `Primary`,
and a bare expression allowed as a statement.

```
Program             : { ClassDeclaration }

ClassDeclaration    : class ClassName [ extends ClassName ] is { MemberDeclaration } end
ClassName           : Identifier

MemberDeclaration   : VariableDeclaration | MethodDeclaration | ConstructorDeclaration

VariableDeclaration : var Identifier : Expression

MethodDeclaration   : MethodHeader [ MethodBody ]
MethodHeader        : method Identifier [ Parameters ] [ : ClassName ]
MethodBody          : is Body end
                    | => Expression

ConstructorDeclaration : this [ Parameters ] is Body end

Parameters          : ( ParameterDeclaration { , ParameterDeclaration } )
ParameterDeclaration: Identifier : ClassName

Body                : { VariableDeclaration | Statement }

Statement           : Assignment
                    | WhileLoop
                    | IfStatement
                    | ReturnStatement
                    | Expression                 // 7.3, our extension

Assignment          : Identifier := Expression
WhileLoop           : while Expression loop Body end
IfStatement         : if Expression then Body [ else Body ] end
ReturnStatement     : return [ Expression ]

Expression          : Primary { . Identifier [ Arguments ] }
Primary             : IntegerLiteral | RealLiteral | BooleanLiteral
                    | this
                    | Identifier                 // 7.4, our extension
                    | ClassName Arguments        // constructor invocation
Arguments           : ( [ Expression { , Expression } ] )
```

Keywords: `class`, `extends`, `is`, `end`, `var`, `method`, `this`, `while`, `loop`, `if`,
`then`, `else`, `return`, `true`, `false`.

Comments: `//` to end of line.
