package project;

import java.io.FileNotFoundException;

public class LexerTester {
    public static void main(String[] args) {
        String filePath = "TestFiles/ShowcaseFiles/Show3.up";
        SymbolTable symbolTable = new SymbolTable();

        try {
            Lexer lexer = new Lexer(filePath, symbolTable);
            Token token;

            do {
                token = lexer.nextToken();
                System.out.println(token);
            } while (token.getType() != TokenType.EOF);

            System.out.println("\nSymbol Table:");
            symbolTable.printTable();
        } catch (FileNotFoundException e) {
            System.err.println("File not found: " + filePath);
        }
    }
}