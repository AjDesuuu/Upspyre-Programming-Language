package project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Set;
import java.util.Stack;
import project.ParsingTableGenerator.GrammarProduction;
import project.utils.parser.ParseTreeNode;

public class Parser {
    private Lexer lexer;
    private Token currentToken;
    private Stack<Integer> stateStack;
    private Stack<String> symbolStack;
    private String inputGrammar = "GrammarProgrammer/expanded.txt";
    private final Stack<ParseTreeNode> treeStack = new Stack<>();

    private static final Set<TokenType> TOKENS_TO_IGNORE = Set.of(
    TokenType.MCOMMENT,
    TokenType.SCOMMENT
);
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
        
        System.out.println("Start Symbol: " + ParsingTableGenerator.productionTable.get(1));
        System.out.println("Accepting State: " + ParsingTableGenerator.actionTable.get(0));
        System.out.println("Start State: " + ParsingTableGenerator.gotoTable.get(0));

        while (true) {
            // Skip comment tokens
            while (TOKENS_TO_IGNORE.contains(currentToken.getType())) {
                currentToken = lexer.nextToken();
            }
            
            if (currentToken.getType() == TokenType.EOF) {
                
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
                handleShift(currentToken);

                System.out.println("Shifting to state " + nextState + " with token: " + currentToken);
                currentToken = lexer.nextToken(); 
            } else if (action.startsWith("r")) { // Reduce action
                int ruleNumber = Integer.parseInt(action.substring(1));
                reduce(ruleNumber-1);
            } else if (action.equals("EOF")) { // Accept
                System.out.println("Parsing successful!");
                while (treeStack.size() > 1) {
                    ParseTreeNode last = treeStack.pop();
                    treeStack.peek().addChild(last);
                }
                System.out.println("Parse tree nodes: " + treeStack.size());
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
        
        //System.out.println("Rule Number:"+ruleNumber);
        
        if (production == null) {
            System.out.println("Error: No production found for rule " + ruleNumber);
            return;
        }
      
        String lhs = production.getLhs();
        int rhsSize = production.getRhsSize();
        
        System.out.println("Reducing by rule " + ruleNumber + ": " + production);

        ParseTreeNode node = new ParseTreeNode(lhs, null, ruleNumber);
        
        // Pop rhsSize symbols and states from the stacks
        for (int i = 0; i < rhsSize; i++) {
            symbolStack.pop();
            stateStack.pop();
            node.addChild(treeStack.pop()); 
        }
        
        
        treeStack.push(node);
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
    private void handleShift(Token token) {
        treeStack.push(new ParseTreeNode(
            token.getType().toString(),
            token.lexeme,
            -1  // -1 indicates terminal node
        ));
        //System.out.println("Pushed terminal: " + token.getType());
        
    }
    public ParseTreeNode getParseTree() {
        if (treeStack.size() != 1) {
            System.err.println("Warning: Incomplete parse tree - stack has " + treeStack.size() + " nodes");
        }
        return treeStack.isEmpty() ? null : treeStack.peek();
    }

    public String getParseTreeDot() {
        ParseTreeNode root = getParseTree();
        if (root == null) return "";
        return "digraph ParseTree {\n" +
               "  node [shape=box, fontname=\"Courier\"];\n" +
               "  edge [arrowhead=vee];\n" +
               root.toDot() +
               "}";
    }
    public void generateParseTreeImage(String outputPath) throws IOException, InterruptedException {
        // 1. Get the DOT representation
        String dotContent = getParseTreeDot();
        
        // 2. Write to temporary file
        String dotFilePath = "parse_tree.dot";
        Files.write(Paths.get(dotFilePath), dotContent.getBytes());
        
        // 3. Execute Graphviz to generate PNG
        ProcessBuilder pb = new ProcessBuilder("dot", "-Tpng", dotFilePath, "-o", outputPath);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        int exitCode = process.waitFor();
        
        if (exitCode != 0) {
            System.err.println("Error generating parse tree image. Is Graphviz installed?");
        } else {
            System.out.println("Parse tree image saved to: " + outputPath);
        }
    }


    public void printTokens() {
        while (currentToken.getType() != TokenType.EOF) {
            System.out.println(currentToken);
            currentToken = lexer.nextToken();
        }
        reset();
    }
}