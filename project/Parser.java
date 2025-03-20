package project;

public class Parser {
    private Lexer lexer;
    private Token currentToken;

    public Parser(Lexer lexer) {
        this.lexer = lexer;
        this.currentToken = lexer.nextToken(); // Initialize with the first token
    }

    public void printTokens() {
        while (currentToken.getType() != TokenType.EOF) {
            // Print the current token (or perform other parsing actions)
            System.out.println(currentToken);

            // Move to the next token
            currentToken = lexer.nextToken();
        }
    }

    // Additional LR parsing methods will go here
    // For example:
    // - shift()
    // - reduce()
    // - accept()
    // - error recovery, etc.
}