package project;

import java.io.IOException;
import project.utils.parser.ParseTreeNode; // Ensure this import matches the actual location of ParseTreeNode
import project.interpreterComponents.InterpreterN;

public class MainDriver {
    public static void main(String[] args) {
        String fileName = "Show8.up";
        String filePath = getFilePath(fileName);
        SymbolTable symbolTable = new SymbolTable();

        try {
            Lexer lexer = new Lexer(filePath, symbolTable);
            Parser parser = new Parser(lexer);

            System.out.println("\nLexer Tokens:");
            parser.printTokens();

            System.out.println("\nSymbol Table:");
            symbolTable.printTable();

            System.out.println("\nParsing...");
            parser.parse();

            ParseTreeNode parseTree = parser.getParseTree();
            if (parseTree == null) {
                System.err.println("Error: Failed to generate parse tree.");
                return;
            }
            parser.generateParseTreeImage(filePath + ".png");

           

            System.out.println("\nInterpreting...");
            InterpreterN interpreter = new InterpreterN(symbolTable);
            interpreter.interpret(parseTree);

            System.out.println("\nUpdated Symbol Table:");
            symbolTable.printTable();

        } catch (IOException | InterruptedException e) {
            System.err.println("File not found: " + filePath);
        }
    }

    private static String getFilePath(String fileName) {
        if (fileName.startsWith("S")) {
            return "TestFiles/ShowcaseFiles/" + fileName;
        } else if (fileName.startsWith("E")) {
            return "TestFiles/ErrorFiles/" + fileName;
        } else {
            return fileName;
        }
    }
}