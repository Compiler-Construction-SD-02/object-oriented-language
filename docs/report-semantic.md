# Semantic Analysis

Project O, team 1991. Source language O (`.o91`), implementation Java 21, build Maven.
Repository: https://github.com/Compiler-Construction-SD-02/object-oriented-language

## 1. How the analyzer works

The parser checks the *form* of a program; the semantic analyzer checks its *meaning*. It runs
on the AST the parser built and never reads the source text again.

The work is split into two components, matching the two kinds of action required for this stage:

- `semantic/SemanticAnalyzer` - **checks**. It only reads the tree and reports errors; the tree
  is left untouched.
- `semantic/Optimizer` - **optimizations**. It rewrites the tree.

`Main` runs them in that order: lexer → parser → analyzer → optimizer → printed tree.

### Symbol table and scopes

The analyzer builds a symbol table: `ClassSymbol` (name, base, fields, methods, constructors),
`MethodSymbol` (parameter types, return type) and `VariableSymbol` (name and its computed type).
A `Scope` is a map of names with a link to the enclosing scope, so a name is looked up from the
innermost scope outwards; class fields are looked up separately, through the class symbol, since
they are visible in methods and in subclasses.

The standard library is not in the source file, so its classes are described by hand in
`semantic/Builtins`: `Integer`, `Real`, `Boolean` and the six containers, with the signatures of
`Plus`, `Less`, `Print`, `get`, `set`, `append` and the rest. Without that table nothing could be
checked, because every program in O is made of such calls.

### Four passes

1. **Declare classes.** In O a class may be used before it is declared (our test 12 relies on
   this), so no body can be checked until every class name is known.
2. **Resolve base classes** and detect inheritance cycles.
3. **Declare methods, constructors and fields.** Signatures first, because their types are class
   names and all classes are already known; field types come second, because they are computed
   from expressions that may call methods.
4. **Check the bodies.**

### Two details worth naming

**Errors are collected, not thrown.** The lexer and the parser stop at the first error, because
after a broken token stream nothing can be trusted. Here the tree already exists, so the analyzer
walks it to the end and reports every error at once.

**Induced errors are suppressed.** When a type cannot be determined, the analyzer returns an
internal `UNKNOWN` type instead of guessing. Every later check involving `UNKNOWN` silently
passes, so one real mistake does not produce a cascade of invented ones. A second internal type,
`VOID`, is the result of a method that declares no return type - it is what catches
`var x : a.Print()`.

**Resolving `Integer(0)` versus `IsOdd(n)`.** The parser cannot tell a constructor invocation from
a call to a method of the same class: both are "a name followed by arguments". The analyzer
resolves it through the symbol table - if a class with that name exists it is a constructor,
otherwise it looks for a method of the enclosing class.

## 2. Checks (the AST is not modified)

| # | Check | Example message |
|---|---|---|
| 1 | Names are declared before use (variables, fields, parameters, classes, methods, constructors) | `unknown name 'total'` |
| 2 | Type checking: assignments, variable initializers, arguments, return values, `if`/`while` conditions | `cannot assign Integer to 'flag' of type Boolean` |
| 3 | `return` is used correctly: a constructor returns no value, a method without a return type returns no value, a method with one returns a value on every path | `method 'Broken' must return a value on every path` |
| 4 | No duplicate declarations: classes, fields, parameters, local variables, method signatures (overloading is allowed, an exact repeat is not) | `method 'Same(Integer)' is already declared in class 'C'` |
| 5 | Inheritance is sound: the base class exists and there is no cycle | `class 'A' is part of an inheritance cycle` |
| 6 | Array bounds, where both the length and the index are known at compile time | `index 5 is out of bounds for array 'arr' of length 3` |

### Example: `examples/24_semantic_errors.o91`

```
class Main is
    method Broken(a: Integer) : Integer is
        var arr : ArrayInteger(Integer(3))
        arr.set(Integer(5), a)
        var flag : Boolean(true)
        flag := a
        if a then
            undefined.Print()
        end
        a.Foo()
    end
end
```

Output:

```
24_semantic_errors.o91:4:13-15: index 5 is out of bounds for array 'arr' of length 3
24_semantic_errors.o91:6:9-12: cannot assign Integer to 'flag' of type Boolean
24_semantic_errors.o91:7:9-10: condition of 'if' must be Boolean, found Integer
24_semantic_errors.o91:8:13-21: unknown name 'undefined'
24_semantic_errors.o91:10:11-13: class 'Integer' has no method 'Foo'
24_semantic_errors.o91:2:12-17: method 'Broken' must return a value on every path
6 semantic error(s)
```

Six independent mistakes in one syntactically valid program, each reported with its line and
column.

### Example: `examples/25_declaration_errors.o91`

```
25_declaration_errors.o91:7:7: class 'A' is already declared
25_declaration_errors.o91:10:7: unknown base class 'Missing'
25_declaration_errors.o91:1:7: class 'A' is part of an inheritance cycle
25_declaration_errors.o91:18:12-15: method 'Same(Integer)' is already declared in class 'C'
25_declaration_errors.o91:12:9-14: a constructor cannot return a value
25_declaration_errors.o91:22:17: class 'D' has no constructor applicable to (Integer, Integer)
6 semantic error(s)
```

### Errors found in our own test suite

Running the analyzer over the 22 example programs written for the previous stages found two real
mistakes that the parser had accepted:

- `06_while_loop.o91` declares `sum` but prints `total` - `unknown name 'total'`;
- `03_arithmetic_via_method_calls.o91` calls `Plus` on a `Boolean` and passes a `Boolean` to
  `Integer.Plus` - two type errors.

## 3. Optimizations (the AST is modified)

AST nodes are immutable - all their fields are `final` - so the optimizer does not edit the tree
in place. Each method takes a node and returns either the same node or a new one, which means the
original tree survives and can be printed for comparison (`--diff`).

The passes are repeated until the tree stops changing, because they feed each other: constant
folding can make a condition constant, simplifying the `if` can remove the last use of a variable,
and that variable then becomes unused. One pass would not catch such a chain.

1. **Constant expression simplification.** `Integer(2).Plus(Integer(3))` becomes the literal `5`;
   `Integer(2).Less(Integer(3))` becomes `true`; `true.Not()` becomes `false`. Arithmetic,
   comparisons and boolean operators are folded for `Integer`, `Real` and `Boolean`. Division by
   a constant zero is deliberately *not* folded - that is a runtime error of the program, not a
   reason for the compiler to fail.
2. **Code simplification.** An `if` whose condition folded to a constant keeps only the branch
   that can run, and the branch's statements are spliced into the enclosing body. A `while` whose
   condition is constantly false is removed entirely.
3. **Unreachable code removal.** Statements that follow a `return` in the same body are dropped.
4. **Unused variable removal.** A variable that is never read and never assigned is removed,
   provided its initializer is side-effect free. Any method call counts as having side effects,
   even `Plus`, since the compiler cannot prove otherwise - so only literals and constructors of
   the three value classes qualify.

### Example: `examples/23_optimizations.o91`

```
class Helper is
    method Answer : Integer is
        return Integer(40).Plus(Integer(2))
        Integer(0).Print()
    end
end

class Main is
    this() is
        var limit : Integer(2).Plus(Integer(3))
        var unused : Integer(7)

        if Integer(2).Less(Integer(3)) then
            limit.Print()
        else
            Integer(0).Print()
        end

        while false loop
            limit.Print()
        end
    end
end
```

Tree before optimization (abbreviated):

```
class Helper
  method Answer() : Integer
    return
      call Plus
        new Integer
          int 40
        new Integer
          int 2
    call Print
      new Integer
        int 0
class Main
  this()
    var limit
      call Plus
        new Integer
          int 2
        new Integer
          int 3
    var unused
      new Integer
        int 7
    if
      call Less
        new Integer
          int 2
        new Integer
          int 3
    then
      call Print
        name limit
    else
      call Print
        new Integer
          int 0
    while
      bool false
    loop
      call Print
        name limit
```

Tree after optimization:

```
class Helper
  method Answer() : Integer
    return
      int 42
class Main
  this()
    var limit
      int 5
    call Print
      name limit
```

The compiler also reports what it did:

```
-- optimizations applied:
   3:28-31: folded a constant expression into 42
   3:9-14: removed 1 unreachable statement(s) after 'return'
   10:32-35: folded a constant expression into 5
   13:23-26: folded a constant expression into true
   13:9-10: 'if' condition is always true: kept the 'then' branch only
   19:9-13: removed a 'while' loop whose condition is always false
   11:13-18: removed unused variable 'unused'
```

A second, unplanned example comes from our own tests: `07_nested_loops.o91` declares `sum` and
never uses it, and the optimizer removes it.

## 4. How to run

```
mvn -q compile
java -cp target/classes com.innopolis.olang.Main examples/23_optimizations.o91
```

| Flag | Effect |
|---|---|
| *(none)* | check, optimize, print the tree and the list of optimizations |
| `--diff` | print the tree before and after optimization |
| `--no-optimize` | check only, print the unoptimized tree |
| `--tokens` | print the token stream and stop |

Semantic errors go to stderr as `file:line:column: message` and the exit code is 1.

## 5. Limitations

- Constant folding covers the standard value classes only. A user class has no known semantics,
  so `MyNumber(2).Plus(MyNumber(3))` is left alone.
- Unused variable removal is conservative, as described above, and does not account for a name
  being shadowed in a nested scope.
- Array bound checking works only when both the length and the index are literals; an index
  computed in a loop cannot be checked at compile time.
- Uninitialized-variable analysis is not implemented: it needs control flow analysis, which is a
  later stage.
- The analyzer collects semantic errors but the lexer and parser still stop at the first one, so
  a file with a syntax error reports exactly one problem.

## 6. Source files

```
semantic/SemanticAnalyzer.java   four passes and all the checks
semantic/Optimizer.java          the four tree rewrites
semantic/SymbolTable.java        classes by name, plus the VOID and UNKNOWN markers
semantic/Builtins.java           the standard library described by hand
semantic/ClassSymbol.java        a class: base, fields, methods, constructors
semantic/MethodSymbol.java       a method or constructor signature
semantic/VariableSymbol.java     a variable, parameter or field with its type
semantic/Scope.java              nested name lookup
semantic/SemanticError.java      one error with its position
```
