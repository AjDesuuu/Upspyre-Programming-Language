package project.interpreterComponents.utils;

import java.util.Stack;
import project.SymbolTable;
import project.SymbolDetails;
import project.TokenType;
import java.util.ArrayList;
import java.util.List;  

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
    
    public void pushScope() {
        int newScopeLevel = currentSymbolTable.getScopeLevel() + 1;
        SymbolTable newScope = new SymbolTable(newScopeLevel, currentSymbolTable);
        symbolTableStack.push(newScope);
        currentSymbolTable = newScope;
        allSymbolTables.add(newScope); // Track every scope ever created
        if (debugMode) {
            System.out.println("[DEBUG] Pushed new scope: " + newScopeLevel);
        }
    }
    public List<SymbolTable> getAllSymbolTables() {
        return allSymbolTables;
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
        SymbolTable current = currentSymbolTable;
        while (current != null) {
            SymbolDetails details = current.getIdentifierLocalScope(name);
            if (details != null) {
                return details;
            }
            current = current.getParent();
        }
        return null;
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
}