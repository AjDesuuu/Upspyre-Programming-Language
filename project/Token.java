package project;
// Token.java
public class Token {
    TokenType type;
    String lexeme;
    int line;
    int position;

    public Token(TokenType type, String lexeme, int line, int position) {
        this.type = type;
        this.lexeme = lexeme;
        this.line = line;
        this.position = position;
    }
    public int getLine() {
        return line;
    }

    public int getPosition() {
        return position;
    }
    // Getter for the token type
    public TokenType getType() {
        return type;
    }

    @Override
    public String toString() {
        
        if (type == TokenType.ERROR) {
            return String.format("[%-12s] %s at line %d, position %d",type, lexeme, line, position);
        }
        
        return String.format("[%-12s] lexeme: %-15s Line:%4d  position:%3d", 
                            type, lexeme, line, position);
    }


}