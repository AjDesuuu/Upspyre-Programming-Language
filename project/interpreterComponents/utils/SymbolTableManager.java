package project.interpreterComponents.utils;

import java.util.Stack;
import project.SymbolTable;
import project.SymbolDetails;
import project.TokenType;

public class SymbolTableManager {
    private SymbolTable currentSymbolTable;
    private final Stack<SymbolTable> symbolTableStack = new Stack<>();
    
    public SymbolTableManager(SymbolTable symbolTable) {
        this.currentSymbolTable = symbolTable;
    }
    
    public void pushScope() {
        symbolTableStack.push(currentSymbolTable);
        SymbolTable newScope = new SymbolTable();
        newScope.setParent(currentSymbolTable);
        currentSymbolTable = newScope;
    }
    
    public void popScope() {
        if (symbolTableStack.isEmpty()) {
            throw new InterpreterException("Cannot pop global scope", 0);
        }
        currentSymbolTable = symbolTableStack.pop();
    }
    
    public void addIdentifier(String name, TokenType type, Object value) {
        SymbolDetails existing = currentSymbolTable.getIdentifier(name);
        if (existing == null) {
            // Only add if not already present
            currentSymbolTable.addIdentifier(name, type, value);
        } else {
            // Update both value AND type
            currentSymbolTable.updateIdentifier(name, type, value);
        }
    }
    
    public void addIdentifierToScope(SymbolTable scope, String name, TokenType type, Object value) {
        scope.addIdentifier(name, type, value);
    }
    
    public SymbolDetails getIdentifier(String name) {
        return currentSymbolTable.getIdentifier(name);
    }
    
    public void updateIdentifier(String name, Object value) {
        SymbolTable scope = findScopeWithIdentifier(name);
        if (scope == null) {
            throw new InterpreterException("Undefined variable: " + name, 0);
        }
        
        SymbolDetails details = scope.getIdentifier(name);
        if (details != null) {
            // Perform type checking before updating
            if (value instanceof Integer && details.getType() == TokenType.DECIMAL) {
                // Auto-convert int to double if needed
                scope.updateIdentifier(name, ((Integer) value).doubleValue());
            } else if (value instanceof Double && details.getType() == TokenType.NUMBER) {
                // Truncate double to int if assigned to int variable
                scope.updateIdentifier(name, ((Double) value).intValue());
            } else {
                scope.updateIdentifier(name, value);
            }
        }
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