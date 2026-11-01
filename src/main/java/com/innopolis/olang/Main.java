package com.innopolis.olang;

import com.innopolis.olang.lexer.LexicalException;
import com.innopolis.olang.lexer.Lexer;
import com.innopolis.olang.lexer.Token;
import com.innopolis.olang.parser.AstPrinter;
import com.innopolis.olang.parser.Parser;
import com.innopolis.olang.parser.SyntaxException;
import com.innopolis.olang.parser.ast.Program;
import com.innopolis.olang.semantic.Optimizer;
import com.innopolis.olang.semantic.SemanticAnalyzer;
import com.innopolis.olang.semantic.SemanticError;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Точка входа компилятора.
 *
 * Конвейер: лексер - парсер - семантический анализ - оптимизация - печать дерева.
 *
 * Флаги:
 *   --tokens напечатать поток токенов и выйти;
 *   --no-optimize проверить, но дерево не трогать;
 *   --diff напечатать дерево до и после оптимизации.
 */
public final class Main {

    public static void main(String[] args) throws IOException {
        if (args.length < 1 || args.length > 2) {
            System.err.println("usage: olang [--tokens | --no-optimize | --diff] <file.o91>");
            System.exit(1);
        }

        String flag = args.length == 2 ? args[0] : "";
        Path file = Path.of(args[args.length - 1]);
        String source = Files.readString(file);
        String name = String.valueOf(file.getFileName());

        try {
            List<Token> tokens = new Lexer(source).tokenize();

            if (flag.equals("--tokens")) {
                for (Token token : tokens) {
                    System.out.println(token);
                }
                System.out.println("total tokens: " + tokens.size());
                return;
            }

            Program program = new Parser(tokens).parseProgram();

            // Семантика: проверки, которые дерево не меняют.
            List<SemanticError> errors = new SemanticAnalyzer().analyze(program);
            if (!errors.isEmpty()) {
                for (SemanticError error : errors) {
                    System.err.println(name + ":" + error);
                }
                System.err.println(errors.size() + " semantic error(s)");
                System.exit(1);
            }

            if (flag.equals("--no-optimize")) {
                System.out.print(new AstPrinter().print(program));
                return;
            }

            if (flag.equals("--diff")) {
                System.out.println("-- before optimization");
                System.out.print(new AstPrinter().print(program));
                System.out.println();
            }

            // Оптимизация: преобразования, которые дерево меняют.
            Optimizer optimizer = new Optimizer();
            Program optimized = optimizer.optimize(program);

            if (flag.equals("--diff")) {
                System.out.println("-- after optimization");
            }
            System.out.print(new AstPrinter().print(optimized));

            if (!optimizer.getApplied().isEmpty()) {
                System.out.println();
                System.out.println("-- optimizations applied:");
                for (String message : optimizer.getApplied()) {
                    System.out.println("   " + message);
                }
            }

        } catch (LexicalException | SyntaxException e) {
            System.err.println(name + ":" + e.getMessage());
            System.exit(1);
        }
    }
}
