# O Language - Test Suite

**1. Minimal class with a method**
A single class with a single method returning a constant. Tests: basic `class` / `method` / `end` syntax.

**2. Variables and type inference**
`var` declarations with different initializers (`Integer`, `Real`, `Boolean`) - no explicit type is written. Tests: type inference from the initializing expression.

**3. Arithmetic via method calls**
`a.Plus(b)`, `a.Minus(b)`, `a.Mult(b)`, `a.Div(b)` on both Integer and Real. Tests: built-in arithmetic methods and their overloads (Integer vs Real).

**4. Comparisons and Boolean**
`Less`, `Greater`, `Equal`, plus logical `And` / `Or` / `Not`. Tests: Boolean handling and built-in comparison methods.

**5. Conditional statement**
`if ... then ... else ... end`, including a nested `if`. Tests: conditional constructs and nesting.

**6. While loop**
A simple counter from 1 to N accumulating a sum. Tests: `while ... loop ... end`.

**7. Nested loops**
Two nested `while` loops (e.g., iterating over index pairs). Tests: loop nesting, correct `end` matching.

**8. Return with multiple exit paths**
A method with several `return` statements inside different `if` branches. Tests: `return`, early method termination.

**9. Class with a field and constructor**
A class with a `var` field, a constructor `this(...)`, and a method reading the field. Tests: constructors, encapsulation (fields are not directly writable from outside).

**10. Creating and using an object**
The main class creates an object of another class via `ClassName(...)` and calls its methods. Tests: `ConstructorInvocation`, the program's entry point.

**11. Method overloading**
A class with multiple methods sharing a name but differing in parameters (e.g., `Combine(a: Integer)` and `Combine(a: Real)`). Tests: overload resolution based on argument types.

**12. Forward method declaration**
Method A calls method B, which is declared below it; B is first declared as forward (no body), then fully defined later. Tests: forward declarations.

**13. Simple inheritance**
A `Derived extends Base` class that uses an inherited method without overriding it. Tests: `extends`, access to base-class methods.

**14. Method overriding**
`Derived` overrides a `Base` method with the same signature. Tests: overriding.

**15. Polymorphism via a base-typed variable**
`var a : Base(...)`, then `a := Derived(...)`, followed by a method call - the `Derived` version should run. Tests: dynamic dispatch.

**16. Transitive inheritance**
Three classes in a chain: `A → B → C`, where `C` uses something declared in `A`. Tests: transitivity of inheritance.

**17. Working with Array**
Create an `Array[Integer]`, fill it via `set`, read via `get`, use `Length`. Tests: `Array[T]`.

**18. Working with List**
Create a `List`, use `append`, `head`, `tail`. Tests: `List[T]`.

**19. Array of user-defined objects with polymorphism**
An `Array[Base]` actually holding a mix of `Derived1` and `Derived2` objects; a loop calls the overridden method on each. Tests: Array + inheritance + polymorphism combined - the most representative OOP test case.

**20. Full mini-program**
Something like: a `Shape` class with an `Area` method, subclasses `Circle` / `Square` / `Rectangle`, each overriding `Area`; the main class creates several shapes and sums their areas in a loop using `Plus`. Tests: everything together - classes, inheritance, polymorphism, loops, arithmetic, arrays.

**21. Array sorting (e.g. bubble sort)**
A method that sorts an `Array[Integer]` in place (or returns a sorted copy), using nested `while` loops, `Greater`/`Less` comparisons, and `get`/`set` for element access and swapping. Tests: nested loops + comparisons + array read/write + assignment all working together - a good end-to-end algorithmic test beyond the isolated feature tests above.