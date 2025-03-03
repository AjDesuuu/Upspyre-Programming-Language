package project;

import java.util.HashMap;
import java.util.Map;

public class SymbolTable {
    private Map<String, SymbolDetails> table;

    public SymbolTable() {
        table = new HashMap<>();
    }

    public void addIdentifier(String lexeme, TokenType type) {
        if (!table.containsKey(lexeme)) {
            table.put(lexeme, new SymbolDetails(lexeme, type));
        }
    }

    public SymbolDetails getIdentifier(String lexeme) {
        return table.get(lexeme);
    }

    public boolean containsIdentifier(String lexeme) {
        return table.containsKey(lexeme);
    }

    public void printTable() {
        System.out.printf("| %-12s | %-12s |\n", "Lexeme", "Type");
        for (Map.Entry<String, SymbolDetails> entry : table.entrySet()) {
            SymbolDetails details = entry.getValue();
            System.out.printf("| %-12s | %-12s |\n", details.getLexeme(), details.getType());
        }
    }
}

class SymbolDetails {
    private String lexeme;
    private TokenType type;

    public SymbolDetails(String lexeme, TokenType type) {
        this.lexeme = lexeme;
        this.type = type;
    }

    public String getLexeme() {
        return lexeme;
    }

    public TokenType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "Identifier: " + lexeme + ", Type: " + type;
    }
}