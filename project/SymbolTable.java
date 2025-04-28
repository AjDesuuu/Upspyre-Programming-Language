package project;

import java.util.HashMap;
import java.util.Map;

public class SymbolTable {
    private Map<String, SymbolDetails> table;

    public SymbolTable() {
        table = new HashMap<>();
    }

    // Add an identifier with type and value
    public void addIdentifier(String lexeme, TokenType type, Object value) {
        if (!table.containsKey(lexeme)) {
            table.put(lexeme, new SymbolDetails(lexeme, type, value));
            System.out.println("Added new identifier: " + lexeme + " with value: " + value);
        } else {
            SymbolDetails details = table.get(lexeme);
            details.setValue(value);
            System.out.println("Updated identifier: " + lexeme + " with new value: " + value);
        }
    }

    // Retrieve an identifier's details
    public SymbolDetails getIdentifier(String lexeme) {
        return table.get(lexeme);
    }

    // Check if an identifier exists
    public boolean containsIdentifier(String lexeme) {
        return table.containsKey(lexeme);
    }

    // Print the symbol table
    public void printTable() {
        System.out.printf("| %-15s | %-12s | %-15s |\n", "Lexeme", "Type", "Value");
        for (Map.Entry<String, SymbolDetails> entry : table.entrySet()) {
            SymbolDetails details = entry.getValue();
            System.out.printf("| %-15s | %-12s | %-15s |\n", details.getLexeme(), details.getType(), details.getValue());
        }
    }
}

// Updated SymbolDetails class to include a value field
class SymbolDetails {
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