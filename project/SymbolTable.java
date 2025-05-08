package project;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SymbolTable {
    public Map<String, SymbolDetails> table;
    private SymbolTable parent = null;
    private int scopeLevel = 0;
    private String scopeType = "generic";
    private String scopeId = "";
    private final Set<String> usedVariables = new HashSet<>();
    

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

    // Mark a variable as used
    public void markVariableAsUsed(String lexeme) {
        usedVariables.add(lexeme);
    }

    // Check if a variable is used in the current scope
    public boolean isVariableUsed(String lexeme) {
        return usedVariables.contains(lexeme);
    }

    // Get the set of used variables
    public Set<String> getUsedVariables() {
        return usedVariables;
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
        System.out.println("Searching for identifier: " + lexeme);
        SymbolDetails details = table.get(lexeme);
        
        if (details != null) {
            // Variable found in the current scope
            markVariableAsUsed(lexeme);
            return details;
        }
        // If not found in current scope, check parent scope
        if (parent != null) {
            SymbolDetails parentDetails = parent.getIdentifier(lexeme);
            if (parentDetails != null) {
                // Mark the variable as used in the current scope
                markVariableAsUsed(lexeme);
            }
            return parentDetails;
        }
        
        return null; // Variable not found
    }
    public SymbolDetails getIdentifierLocalScope(String lexeme) {
        return table.get(lexeme);
    }

    public String getScopeType() {
        return scopeType;
    }
    
    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }
    
    public String getScopeId() {
        return scopeId;
    }
    
    public void setScopeId(String scopeId) {
        this.scopeId = scopeId;
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
    public boolean isEmpty() {
        return getTable().isEmpty();
    }
    public Map<String, SymbolDetails> getTable() {
        return table;
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
        System.out.println("Scope Level " + this.getScopeLevel() + " (" + this.getScopeType() + "):");
        System.out.println("|-----------------|--------------|-----------------|-------|");
        System.out.printf("| %-15s | %-12s | %-15s | %-5s |\n", "Lexeme", "Type", "Value", "Scope");
        System.out.println("|-----------------|--------------|-----------------|-------|");
    
        // Print current scope variables
        table.values().stream()
             .sorted((a, b) -> a.getLexeme().compareTo(b.getLexeme()))
             .forEach(details -> {
                 System.out.printf("| %-15s | %-12s | %-15s | %-5d |\n",
                     details.getLexeme(),
                     details.getType(),
                     details.getValue(),
                     this.getScopeLevel());
             });
    
        // Recursively print inherited variables from all parent scopes
        SymbolTable parentScope = this.getParent();
        while (parentScope != null) {
            final SymbolTable currentScope = parentScope; // Make effectively final
            currentScope.getTable().values().stream()
                .filter(details -> !table.containsKey(details.getLexeme())) // Only inherited variables
                .filter(details -> isVariableUsed(details.getLexeme()))
                .sorted((a, b) -> a.getLexeme().compareTo(b.getLexeme()))
                .forEach(details -> {
                    System.out.printf("| %-15s | %-12s | %-15s | %-5d |\n",
                        details.getLexeme(),
                        details.getType(),
                        details.getValue(),
                        currentScope.getScopeLevel()); // Show originating scope level
                });
            parentScope = parentScope.getParent(); // Move to the next parent
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

