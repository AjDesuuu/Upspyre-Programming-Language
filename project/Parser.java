package project;

import java.util.HashMap;
import java.util.Stack;

public class Parser {
    private Lexer lexer;
    private Token currentToken;
    private Stack<Integer> stack;

    public Parser(Lexer lexer) {
        this.lexer = lexer;
        this.currentToken = lexer.nextToken(); // Initialize with the first token
        this.stack = new Stack<>();
        stack.push(0); // Start state

        // Load parsing table from CSV
        ParsingTableGenerator.generateParsingTables("GrammarProgrammer/output.csv");
    }
    public void reset() {
        this.lexer.reset();
        this.currentToken = lexer.nextToken(); // Re-initialize with the first token
        this.stack.clear();
        this.stack.push(0); // Reset to start state
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
    
            int state = stack.peek();
            String tokenType = currentToken.getType().toString();
            
            // Check action table
            HashMap<String, String> actionRow = ParsingTableGenerator.actionTable.get(state);
            if (actionRow == null || !actionRow.containsKey(tokenType)) {
                System.out.println("Syntax Error at token: " + currentToken);
                return;
            }
    
            String action = actionRow.get(tokenType);
            System.out.println("Current state: " + state + ", Current token: " + currentToken + ", Action: " + action);
    
            if (action.startsWith("s")) { // Shift action
                int nextState = Integer.parseInt(action.substring(1));
                stack.push(nextState);
                System.out.println("Shifting to state " + nextState + " with token: " + currentToken);
                currentToken = lexer.nextToken(); 
            } else if (action.startsWith("r")) { // Reduce action
                int ruleNumber = Integer.parseInt(action.substring(1));
                reduce(ruleNumber);
                System.out.println("Reducing by rule " + ruleNumber);
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
        // Implement reduction logic based on your grammar rules
        System.out.println("Reducing by rule " + ruleNumber);
    }

    public void printTokens() {
        while (currentToken.getType() != TokenType.EOF) {
            System.out.println(currentToken);
            currentToken = lexer.nextToken();
        }
        reset();
    }
}
