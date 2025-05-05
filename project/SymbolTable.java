package project;

import java.util.HashMap;
import java.util.Map;

public class SymbolTable {
    public Map<String, SymbolDetails> table;
    private SymbolTable parent = null;
    private int scopeLevel = 0;

    public SymbolTable() {
        table = new HashMap<>();
    }
    public SymbolTable(int scopeLevel, SymbolTable parent) {
        this.scopeLevel = scopeLevel;
        this.parent = parent;
        this.table = new HashMap<>(); 
    }
    public void setScopeLevel(int level) {
        this.scopeLevel = level;
    }

    public int getScopeLevel() {
        return scopeLevel;
    }
    
    // Add an identifier with type and value
    public void addIdentifier(String lexeme, TokenType type, Object value) {
        if (!table.containsKey(lexeme)) {
            table.put(lexeme, new SymbolDetails(lexeme, type, value,this.scopeLevel));
            System.out.println("Added new identifier: " + lexeme + " with value: " + value + " and type: " + type);
        } else {
            // Update value AND type when explicitly declared with a type
            SymbolDetails details = table.get(lexeme);
            details.setValue(value);
            details.setType(type); // This line is missing or not working
            System.out.println("Updated identifier: " + lexeme + " with new value: " + value + " and type: " + type);
        }
    }

    public SymbolDetails getIdentifier(String lexeme) {
        SymbolDetails details = table.get(lexeme);
        if (details != null) {
            return details;
        }
        // If not found in current scope and parent exists, check parent
        if (parent != null) {
            return parent.getIdentifier(lexeme);
        }
        return null;
    }
    public SymbolDetails getIdentifierLocalScope(String lexeme) {
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

    public void printTableHierarchical() {
        
        System.out.println("Scope Level " + scopeLevel + ":");
        System.out.println("|-----------------|--------------|-----------------|-------|");
        System.out.printf("| %-15s | %-12s | %-15s | %-5s |\n", "Lexeme", "Type", "Value", "Scope");
        System.out.println("|-----------------|--------------|-----------------|-------|");
        
        // Print current scope
        table.values().stream()
             .sorted((a, b) -> a.getLexeme().compareTo(b.getLexeme()))
             .forEach(details -> {
                 System.out.printf("| %-15s | %-12s | %-15s | %-5d |\n",
                     details.getLexeme(),
                     details.getType(),
                     details.getValue(),
                     details.getScopeLevel());
             });
        
        // Print parent scope
        // Print parent scope
    if (parent != null) {
        System.out.println();  // Add blank line for readability
        parent.printTableHierarchical();  // Just call the method directly
    }
    }

    public void printTableRecursive() {
        System.out.println("\nSymbol Table (Scope Level " + scopeLevel + "):");
        System.out.printf("| %-15s | %-12s | %-15s | %-5s |\n", "Lexeme", "Type", "Value", "Scope");
        System.out.println("|-----------------|--------------|-----------------|-------|");
        
        // Print current scope
        for (Map.Entry<String, SymbolDetails> entry : table.entrySet()) {
            SymbolDetails details = entry.getValue();
            System.out.printf("| %-15s | %-12s | %-15s | %-5d |\n",
                details.getLexeme(), details.getType(), details.getValue(), details.getScopeLevel());
        }
        
        // Print parent scope with indentation
        if (parent != null) {
            System.out.println("\nParent Scope:");
            parent.printTableRecursive();
        }
    }

    
}

