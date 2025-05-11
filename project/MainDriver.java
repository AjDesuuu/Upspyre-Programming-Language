package project;

import java.io.IOException;

import project.utils.parser.ParseTreeNode; 
import project.interpreterComponents.InterpreterN;

public class MainDriver {
    public static void main(String[] args) {
        String fileName = "Show18.up";
        String filePath = getFilePath(fileName);
        SymbolTable lexicalSymbolTable = new SymbolTable();
        SymbolTable symbolTable = new SymbolTable(0,null);

        try {
            Lexer lexer = new Lexer(filePath, lexicalSymbolTable);
            Parser parser = new Parser(lexer);

            System.out.println("\nLexer Tokens:");
            parser.printTokens();

            System.out.println("\nSymbol Table(lexer phase):");
            lexicalSymbolTable.printTable();

            System.out.println("\nParsing...");
            parser.parse();

            ParseTreeNode parseTree = parser.getParseTree();
            if (parseTree == null) {
                System.err.println("Error: Failed to generate parse tree.");
                return;
            }
            parser.generateParseTreeImage(filePath + ".png");
            

           

            System.out.println("\nInterpreting...");
            InterpreterN interpreter = new InterpreterN(symbolTable,true);
            interpreter.interpret(parseTree);

            
            System.out.println("\nUpdated Symbol Table:");
            interpreter.printUpdatedSymbolTable();
            

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