package project;

import java.util.HashMap;
import java.util.Map;

public class SymbolTable {
    public Map<String, SymbolDetails> table;
    private SymbolTable parent = null;

    public SymbolTable() {
        table = new HashMap<>();
    }
    
    // Add an identifier with type and value
    public void addIdentifier(String lexeme, TokenType type, Object value) {
        if (!table.containsKey(lexeme)) {
            table.put(lexeme, new SymbolDetails(lexeme, type, value));
            System.out.println("Added new identifier: " + lexeme + " with value: " + value + " and type: " + type);
        } else {
            // Update value AND type when explicitly declared with a type
            SymbolDetails details = table.get(lexeme);
            details.setValue(value);
            details.setType(type); // This line is missing or not working
            System.out.println("Updated identifier: " + lexeme + " with new value: " + value + " and type: " + type);
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
    public void setParent(SymbolTable parent) {
        this.parent = parent;
    }
    
    public SymbolTable getParent() {
        return parent;
    }
    
    public boolean hasIdentifier(String lexeme) {
        return table.containsKey(lexeme);
    }
    
    public void updateIdentifier(String lexeme, Object value) {
        SymbolDetails details = table.get(lexeme);
        if (details != null) {
            details.setValue(value);
        }
    }
    // Add this new method to update both value and type
    public void updateIdentifier(String lexeme, TokenType type, Object value) {
        SymbolDetails details = table.get(lexeme);
        if (details != null) {
            details.setValue(value);
            details.setType(type);
        }
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

