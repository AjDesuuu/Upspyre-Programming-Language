package project;
// LexerTester.java
import java.io.FileNotFoundException;

public class LexerTester {
    public static void main(String[] args) {
        // Path to the input file
        String filePath = "project/test.up";

        try {
            // Create a lexer with the file path
            Lexer lexer = new Lexer(filePath);

            // Tokenize the input and print each token
            Token token;
            do {
                token = lexer.nextToken();
                System.out.println(token);
            } while (token.type != TokenType.EOF);
        } catch (FileNotFoundException e) {
            System.err.println("File not found: " + filePath);
        }
    }
}