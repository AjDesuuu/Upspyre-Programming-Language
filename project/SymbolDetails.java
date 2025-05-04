package project;

// Updated SymbolDetails class to include a value field
public class SymbolDetails {
    private String lexeme;
    private TokenType type;
    private Object value;
    private boolean isDeclared;
    private boolean isExplicitlyDeclared;

    public SymbolDetails(String lexeme, TokenType type, Object value) {
        this.lexeme = lexeme;
        this.type = type;
        this.value = value;
        //WARNINGLY: This is a temporary fix. The isDeclared field should be set based on the context of the identifier.
        this.isDeclared = (type != null);
        this.isExplicitlyDeclared = false;
    }

    public String getLexeme() {
        return lexeme;
    }

    public TokenType getType() {
        return type;
    }

    public void setType(TokenType type) {
        this.type = type;
        this.isDeclared = (type != null);
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    // Add these methods
    public boolean isDeclared() {
        return isDeclared;
    }

    public void setDeclared(boolean declared) {
        isDeclared = declared;
    }
    public boolean isExplicitlyDeclared() {
        return isExplicitlyDeclared;
    }

    public void setExplicitlyDeclared(boolean explicitlyDeclared) {
        this.isExplicitlyDeclared = explicitlyDeclared;
    }

    @Override
    public String toString() {
        return "Identifier: " + lexeme + ", Type: " + type + ", Value: " + value;
    }
}