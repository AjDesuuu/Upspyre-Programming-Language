package project.interpreterComponents.utils;

import java.util.Stack;
import project.SymbolTable;
import project.SymbolDetails;
import project.TokenType;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SymbolTableManager {
    private SymbolTable currentSymbolTable;
    private final Stack<SymbolTable> symbolTableStack = new Stack<>();
    private boolean debugMode = false; // Set to true for debugging
    private final List<SymbolTable> allSymbolTables = new ArrayList<>(); // List to keep track of all symbol tables
    
    public SymbolTableManager(SymbolTable symbolTable, boolean debugMode) {
        this.currentSymbolTable = symbolTable;
        this.symbolTableStack.push(symbolTable);
        this.debugMode = debugMode;
        allSymbolTables.add(symbolTable);
    }
    
    /**
     * Push a new scope onto the stack
     * @param blockType Optional - the type of block (if, for, while, etc.)
     */
    public void pushScope(String blockType) {
        int newScopeLevel = currentSymbolTable.getScopeLevel() + 1;
        // Generate unique ID for this scope
        String scopeId = UUID.randomUUID().toString();
        
        SymbolTable newScope = new SymbolTable(newScopeLevel, currentSymbolTable);
        // Set block type and ID for better debugging
        newScope.setScopeType(blockType != null ? blockType : "generic");
        newScope.setScopeId(scopeId);
        
        symbolTableStack.push(newScope);
        currentSymbolTable = newScope;
        
        // Add this as a new scope, don't replace existing ones
        allSymbolTables.add(newScope);
        
        if (debugMode) {
            System.out.println("[DEBUG] Pushed new scope: " + newScopeLevel + 
                " (Type: " + newScope.getScopeType() + ", ID: " + scopeId + ")");
        }
    }
    
    // Overloaded method for backward compatibility
    public void pushScope() {
        pushScope(null);
    }
    
    public List<SymbolTable> getAllSymbolTables() {
        return allSymbolTables;
    }
    
    // Get only the non-empty symbol tables
    public List<SymbolTable> getNonEmptySymbolTables() {
        List<SymbolTable> nonEmpty = new ArrayList<>();
        for (SymbolTable table : allSymbolTables) {
            if (!table.isEmpty()) {
                nonEmpty.add(table);
            }
        }
        return nonEmpty;
    }
    
    // Get only symbol tables of a specific type
    public List<SymbolTable> getSymbolTablesByType(String blockType) {
        List<SymbolTable> result = new ArrayList<>();
        for (SymbolTable table : allSymbolTables) {
            if (blockType.equals(table.getScopeType())) {
                result.add(table);
            }
        }
        return result;
    }
    
    public void popScope() {
        if (!symbolTableStack.isEmpty() && symbolTableStack.size() > 1) {
            symbolTableStack.pop();
            currentSymbolTable = symbolTableStack.peek();
            if (debugMode) {
                System.out.println("[DEBUG] Popped scope, current level: " + currentSymbolTable.getScopeLevel());
            }
        }
    }
    
    public void addIdentifier(String name, TokenType type, Object value) {
        currentSymbolTable.addIdentifier(name, type, value);
    }
    
    public void addIdentifierToScope(SymbolTable scope, String name, TokenType type, Object value) {
        scope.addIdentifier(name, type, value);
    }
    
    public SymbolDetails getIdentifier(String name) {
        if (currentSymbolTable == null) {
            throw new InterpreterException("No active scope to search for identifier: " + name, 0);
        }
        // Use SymbolTable's getIdentifier to traverse parent scopes
        return currentSymbolTable.getIdentifier(name);
    }
    
    public void updateIdentifier(String name, Object value) {
        SymbolTable scope = findScopeWithIdentifier(name);
        if (scope == null) {
            throw new InterpreterException("Undefined variable: " + name, 0);
        }
        scope.updateIdentifier(name, value);
    }

    private SymbolTable findScopeWithIdentifier(String name) {
        SymbolTable current = currentSymbolTable;
        while (current != null) {
            if (current.hasIdentifier(name)) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }
    
    public SymbolTable getCurrentSymbolTable() {
        return currentSymbolTable;
    }
    
    public void setCurrentSymbolTable(SymbolTable symbolTable) {
        this.currentSymbolTable = symbolTable;
    }
    
    /**
     * Print a summary of all symbol tables organized by scope type
     */
    public void printSymbolTableSummary() {
        System.out.println("\nSymbol Table Summary:");
        System.out.println("=====================");
        
        // Group by scope type
    for (SymbolTable table : allSymbolTables) {
        if (!table.isEmpty()) {
            String scopeId = table.getScopeId();
            String scopeIdDisplay = (scopeId != null && scopeId.length() >= 8) 
                ? scopeId.substring(0, 8) 
                : scopeId; // Use the full ID if it's shorter than 8 characters

            System.out.println("Scope Level " + table.getScopeLevel() + 
                            " (" + table.getScopeType() + " - ID: " + 
                            scopeIdDisplay + "):");
            table.printTable();
            System.out.println();
        }
    }
    }
}