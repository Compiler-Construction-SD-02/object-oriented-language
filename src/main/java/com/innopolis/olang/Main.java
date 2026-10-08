package com.innopolis.olang;

import com.innopolis.olang.lexer.LexicalException;
import com.innopolis.olang.lexer.Lexer;
import com.innopolis.olang.lexer.Token;
import com.innopolis.olang.parser.AstPrinter;
import com.innopolis.olang.parser.Parser;
import com.innopolis.olang.parser.SyntaxException;
import com.innopolis.olang.parser.ast.Program;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Точка входа компилятора.
 * Прогоняет файл через лексер и парсер и печатает дерево.
 * С флагом --tokens сначала печатает ещё и поток токенов.
 */
public final class Main {

    public static void main(String[] args) throws IOException {
        if (args.length < 1 || args.length > 2) {
            System.err.println("usage: olang [--tokens] <file.o91>");
            System.exit(1);
        }

        boolean printTokens = args.length == 2 && args[0].equals("--tokens");
        Path file = Path.of(args[args.length - 1]);
        String source = Files.readString(file);

        try {
            List<Token> tokens = new Lexer(source).tokenize();

            if (printTokens) {
                for (Token token : tokens) {
                    System.out.println(token);
                }
                System.out.println("total tokens: " + tokens.size());
                System.out.println();
            }

            Program program = new Parser(tokens).parseProgram();
            System.out.print(new AstPrinter().print(program));

        } catch (LexicalException | SyntaxException e) {
            System.err.println(file.getFileName() + ":" + e.getMessage());
            System.exit(1);
        }
    }
}
