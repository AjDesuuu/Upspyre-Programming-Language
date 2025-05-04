package project;

// Updated SymbolDetails class to include a value field
public class SymbolDetails {
    private String lexeme;
    private TokenType type;
    private Object value;

    public SymbolDetails(String lexeme, TokenType type, Object value) {
        this.lexeme = lexeme;
        this.type = type;
        this.value = value;
    }

    public String getLexeme() {
        return lexeme;
    }

    public TokenType getType() {
        return type;
    }

    public void setType(TokenType type) {
        this.type = type;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "Identifier: " + lexeme + ", Type: " + type + ", Value: " + value;
    }
}