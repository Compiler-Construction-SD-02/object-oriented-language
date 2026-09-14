package com.innopolis.olang;

import com.innopolis.olang.lexer.LexicalException;
import com.innopolis.olang.lexer.Lexer;
import com.innopolis.olang.lexer.Token;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class Main {

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("usage: olang <file.o91>");
            System.exit(1);
        }

        Path file = Path.of(args[0]);
        String source = Files.readString(file);

        try {
            List<Token> tokens = new Lexer(source).tokenize();
            for (Token token : tokens) {
                System.out.println(token);
            }
            System.out.println("total tokens: " + tokens.size());
        } catch (LexicalException e) {
            System.err.println(file.getFileName() + ":" + e.getMessage());
            System.exit(1);
        }
    }
}
