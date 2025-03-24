package project;

import java.util.HashMap;
import java.util.Stack;
import project.ParsingTableGenerator.GrammarProduction;

public class Parser {
    private Lexer lexer;
    private Token currentToken;
    private Stack<Integer> stateStack;
    private Stack<String> symbolStack;
    private String inputGrammar = "GrammarProgrammer/expanded.txt";

    public Parser(Lexer lexer) {
        this.lexer = lexer;
        this.currentToken = lexer.nextToken(); // Initialize with the first token
        this.stateStack = new Stack<>();
        this.symbolStack = new Stack<>();
        stateStack.push(0); // Start state

        

        // Load parsing table from CSV
        ParsingTableGenerator.generateParsingTables(inputGrammar);
        ParsingTableGenerator.generateProductionTable(inputGrammar);
        
    }
    
    public void reset() {
        this.lexer.reset();
        this.currentToken = lexer.nextToken(); // Re-initialize with the first token
        this.stateStack.clear();
        this.symbolStack.clear();
        this.stateStack.push(0); // Reset to start state
    }

    public void parse() {
        while (true) {
            // Skip comment tokens
            while(currentToken.getType().toString().equals("COMMENT")) {
                currentToken = lexer.nextToken();
            }
            
            // Stop parsing if EOF is reached
            if (currentToken.getType() == TokenType.EOF) {
                System.out.println("Parsing complete.");
                return;
            }
    
            int state = stateStack.peek();
            String tokenType = currentToken.getType().toString();
            
            // Check action table
            HashMap<String, String> actionRow = ParsingTableGenerator.actionTable.get(state);
            if (actionRow == null || !actionRow.containsKey(tokenType)) {
                System.out.println("Syntax Error at token: " + currentToken);
                System.out.println("Expected one of: " + (actionRow != null ? actionRow.keySet() : "none"));
                return;
            }
    
            String action = actionRow.get(tokenType);
            System.out.println("Current state: " + state + ", Current token: " + currentToken + ", Action: " + action);
    
            if (action.startsWith("s")) { // Shift action
                int nextState = Integer.parseInt(action.substring(1));
                stateStack.push(nextState);
                symbolStack.push(tokenType);
                System.out.println("Shifting to state " + nextState + " with token: " + currentToken);
                currentToken = lexer.nextToken(); 
            } else if (action.startsWith("r")) { // Reduce action
                int ruleNumber = Integer.parseInt(action.substring(1));
                reduce(ruleNumber-1);
            } else if (action.equals("acc")) { // Accept
                System.out.println("Parsing successful!");
                return;
            } else {
                System.out.println("Unexpected action: " + action);
                return;
            }
        }
    }

    private void reduce(int ruleNumber) {
        // Get the production for this rule number
        GrammarProduction production = ParsingTableGenerator.productionTable.get(ruleNumber);
        System.out.println("Rule Number:"+ruleNumber);
        
        if (production == null) {
            System.out.println("Error: No production found for rule " + ruleNumber);
            return;
        }
        
        String lhs = production.getLhs().replaceAll("[<>]", "");
        int rhsSize = production.getRhsSize();
        
        System.out.println("Reducing by rule " + ruleNumber + ": " + production);
        
        // Pop rhsSize symbols and states from the stacks
        for (int i = 0; i < rhsSize; i++) {
            symbolStack.pop();
            stateStack.pop();
        }
        
        // Get the current state after popping
        int currentState = stateStack.peek();
        
        // Push the LHS non-terminal onto the symbol stack
        symbolStack.push(lhs);
        
        // Look up the goto action for this non-terminal
        HashMap<String, String> gotoRow = ParsingTableGenerator.gotoTable.get(currentState);
        if (gotoRow == null || !gotoRow.containsKey(lhs)) {
            System.out.println("Error: No goto action for non-terminal " + lhs + " in state " + currentState);
            return;
        }
        
        // Get the next state from the goto table
        int nextState = Integer.parseInt(gotoRow.get(lhs));
        
        // Push the next state onto the state stack
        stateStack.push(nextState);
        
        System.out.println("Goto state " + nextState + " with non-terminal " + lhs);
    }

    public void printTokens() {
        while (currentToken.getType() != TokenType.EOF) {
            System.out.println(currentToken);
            currentToken = lexer.nextToken();
        }
        reset();
    }
}