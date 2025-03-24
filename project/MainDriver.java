package project;

import java.io.IOException;

public class MainDriver {
    public static void main(String[] args) {
        String fileName = "Show1.up";
        String filePath = getFilePath(fileName);
        SymbolTable symbolTable = new SymbolTable();

        try {
            Lexer lexer = new Lexer(filePath, symbolTable);
            Parser parser = new Parser(lexer);

            // Parser Prints the tokens
            parser.printTokens();
            //Lexer prints the symbol table
            System.out.println("\nSymbol Table:");
            symbolTable.printTable();
            

            System.out.println();
            parser.parse();
            
            //Generate image for parse tree, COMMENT THIS OUT IF YOU DON'T WANT TO GENERATE AN IMAGE
            parser.generateParseTreeImage(filePath + ".png");

        } catch (IOException|InterruptedException e) {
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