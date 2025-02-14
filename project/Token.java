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

    @Override
    public String toString() {
        return String.format("[%s],'%s',%d,%d", type, lexeme, line, position);
    }
}