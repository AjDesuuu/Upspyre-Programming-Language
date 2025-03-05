package project;

import java.io.FileNotFoundException;

public class LexerTester {
    public static void main(String[] args) {
        String fileName ="Error1.up";
        String filePath = getFilePath(fileName);
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
    private static String getFilePath(String fileName) {
        if (fileName.startsWith("S")) {
            return "TestFiles/ShowcaseFiles/" + fileName;
        } else if (fileName.startsWith("E")) {
            return "TestFiles/ErrorFiles/" + fileName;
        } else {
            throw new IllegalArgumentException("Invalid file name: " + fileName);
        }
    }
}