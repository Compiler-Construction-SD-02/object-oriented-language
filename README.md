# Compiler for the object-oriented language **O**

## Repository layout

```
.
├── README.md                    language description + our design decisions (this file)
├── docs/
│   └── testsForFirstWeek.md     what every test case checks
├── examples/                    test programs in O (*.o91)
├── src/main/java/...            compiler sources (to be written)
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

Every class also has a parameterless constructor even when it declares none of its own - see §7.6.

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

class AreaSummer is
    var total : Integer(0)

    method Add(s: Shape) is
        total := total.Plus(s.Area())      // s is declared Shape; the derived Area runs
    end

    method Total : Integer => total
end

class Main is
    this() is
        var summer : AreaSummer()
        var s : Shape()

        s := Square(Integer(4))
        summer.Add(s)                                   // dynamic dispatch: Square.Area
        summer.Add(Rectangle(Integer(3), Integer(5)))   // dynamic dispatch: Rectangle.Area

        summer.Total().Print()                          // 16 + 15 = 31
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
     ├─ ArrayInteger, ArrayReal, ArrayBoolean
     └─ ListInteger,  ListReal,  ListBoolean
```

| Class | Members |
|---|---|
| `Integer` | constructors from `Integer` / `Real`; `Min`, `Max`; `toReal`, `toBoolean`; `UnaryMinus`; `Plus`, `Minus`, `Mult`, `Div`, `Rem` (overloaded for `Integer` and `Real`); `Less`, `LessEqual`, `Greater`, `GreaterEqual`, `Equal` |
| `Real` | same shape as `Integer`, plus `Epsilon` and `toInteger` |
| `Boolean` | constructor; `toInteger`; `Or`, `And`, `Xor`, `Not` |
| `ArrayInteger`, `ArrayReal`, `ArrayBoolean` | constructor taking the length; `Length()`; `get(i)`; `set(i, v)`; `toList()` |
| `ListInteger`, `ListReal`, `ListBoolean` | constructors (empty / one element / element + count); `append(v)`, `head()`, `tail()` |

These six container classes are the complete set - see §7.1.

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

## 7. Our design decisions

Each item is a rule our compiler follows.

### 7.1 No generics

There is no `[...]` type syntax. Containers are a fixed, closed set of six library classes:

| kind | classes |
|---|---|
| arrays | `ArrayInteger`, `ArrayReal`, `ArrayBoolean` |
| lists | `ListInteger`, `ListReal`, `ListBoolean` |

Containers of user-defined classes do not exist - `ArrayShape` is an unknown class. Generating a
container per element type would cost a full class-generation machinery for a feature the course
excludes.

Consequence: polymorphism is tested through base-typed variables (test 15) and base-typed
parameters (tests 19 and 20), not through a collection. A container of `AnyRef` would not
substitute: `get` would return `AnyRef`, whose static type declares no user method, and O has no
type casts.

### 7.2 `Print`

`Integer`, `Real` and `Boolean` have a `Print` method: it writes the value and a line break and
returns nothing. Our addition - the language has no other I/O, and without it test programs produce
nothing observable.

### 7.3 Expression statements

A bare expression is a statement:

```
Statement : Assignment | WhileLoop | IfStatement | ReturnStatement | Expression
```

Without it a method declared without a return type could never be called: its result fits no
assignment, and no other statement form accepts a call.

### 7.4 `Identifier` in `Primary`

A variable reference is a basic element of an expression:

```
Primary : IntegerLiteral | RealLiteral | BooleanLiteral | this | Identifier | ClassName Arguments
```

Without it a bare `a` parses as a constructor invocation of a class named `a`, and name analysis
then searches the class table instead of the variable scope.

### 7.5 Constructor invocation vs. method call

`C()` and `Value()` are syntactically identical. The parser builds one node for both and name
analysis resolves it: a class name means object creation, a method name means a call on `this`, a
variable name means a read, anything else is an error.

### 7.6 Every class has a parameterless constructor

Every class has a parameterless constructor. When the program does not declare one, the compiler
supplies it with an empty body; the class's own field initializers still run. Declaring other
constructors does not remove it, so both `Square(Integer(4))` and `Square()` are valid.

### 7.7 Initial contents of an array

Every cell is filled at creation with a fresh object of the element class:

| Array | Every cell holds |
|---|---|
| `ArrayInteger(Integer(n))` | `Integer(0)` |
| `ArrayReal(Integer(n))` | `Real(0.0)` |
| `ArrayBoolean(Integer(n))` | `Boolean(false)` |

Consequence: **the language has no null.** Reading a cell that was never written returns a real
object, so no null check is ever needed and no null dereference can occur. The price is that array
creation constructs all of its elements eagerly.

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
