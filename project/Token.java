package project;
// Token.java
class Token {
    TokenType type;
    String lexeme;
    int line;
    int position;

    Token(TokenType type, String lexeme, int line, int position) {
        this.type = type;
        this.lexeme = lexeme;
        this.line = line;
        this.position = position;
    }
    // Getter for the token type
    public TokenType getType() {
        return type;
    }

    @Override
    public String toString() {
        lexeme = "'"+lexeme+"'"; //wraps the lexeme in single quotes
        if (type == TokenType.ERROR) {
            return String.format("[%-10s] %s at line %d, position %d",type, lexeme, line, position);
        }
        
        return String.format("[%-10s] lexeme: %-15s Line:%4d  position:%3d", 
                            type, lexeme, line, position);
    }


}