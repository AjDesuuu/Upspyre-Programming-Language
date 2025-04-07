package project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import project.ParsingTableGenerator.GrammarProduction;
import project.utils.parser.ParseTreeNode;

/**
 * LR Parser implementation for syntax analysis.
 * Uses shift-reduce parsing with action and goto tables generated from a grammar.
 */
public class Parser {
    private Lexer lexer;
    private Token currentToken;
    private Stack<Integer> stateStack;
    private Stack<String> symbolStack;
    private String inputGrammar = "GrammarProgrammer/expanded.txt";
    private final Stack<ParseTreeNode> treeStack = new Stack<>();
    private final ParserOutputFormatter outputFormatter = new ParserOutputFormatter();
    
    // Track parsing errors for summary reporting
    private final List<String> errorMessages = new ArrayList<>();

    // Tokens to be ignored during parsing (comments)
    private static final Set<TokenType> TOKENS_TO_IGNORE = Set.of(
            TokenType.MCOMMENT,
            TokenType.SCOMMENT);

    /**
     * Initializes the parser with a lexer and prepares the parsing tables.
     * @param lexer The lexer to provide tokens for parsing
     */
    public Parser(Lexer lexer) {
        this.lexer = lexer;
        this.currentToken = lexer.nextToken(); // Initialize with the first token
        this.stateStack = new Stack<>();
        this.symbolStack = new Stack<>();
        stateStack.push(0); // Start state

        // Load parsing table from grammar file
        ParsingTableGenerator.generateParsingTables(inputGrammar);
        ParsingTableGenerator.generateProductionTable(inputGrammar);
    }

    /**
     * Resets the parser to its initial state to allow reuse.
     */
    public void reset() {
        this.lexer.reset();
        this.currentToken = lexer.nextToken(); // Re-initialize with the first token
        this.stateStack.clear();
        this.symbolStack.clear();
        this.treeStack.clear();
        this.errorMessages.clear();
        this.stateStack.push(0); // Reset to start state
    }

    /**
     * Performs the full parsing process using LR parsing algorithm.
     * Handles errors with recovery strategies.
     */
    public void parse() {
        // Print initial parsing setup information
        //System.out.println("Start Symbol: " + ParsingTableGenerator.productionTable.get(1));
        outputFormatter.updateCurrentLine(currentToken);

        int errorCount = 0;
        final int MAX_ERRORS = 10; // Limit error count to prevent infinite loops

        while (errorCount < MAX_ERRORS) {
            outputFormatter.updateCurrentLine(currentToken);
            // Skip comment tokens to focus on actual code
            while (TOKENS_TO_IGNORE.contains(currentToken.getType())) {
                currentToken = lexer.nextToken();
                outputFormatter.updateCurrentLine(currentToken);
            }

            int state = stateStack.peek();
            String tokenType = currentToken.getType().toString();

            // Check action table for the current state and token
            HashMap<String, String> actionRow = ParsingTableGenerator.actionTable.get(state);
            if (actionRow == null || !actionRow.containsKey(tokenType)) {
                // Syntax error detected - no valid action for current state and token
                errorCount++;
                String errorMsg = String.format("Syntax Error at line %d, position %d: %s", 
                                               currentToken.line, currentToken.position, currentToken);
                
                // Add to error collection for summary
                errorMessages.add(errorMsg);
                
                // Record the error in the formatter
                outputFormatter.recordError(state, currentToken, 
                    (actionRow != null ? actionRow.keySet().toString() : "none"));
                
                // Try error recovery strategies
                if (!tryPhraseLevelRecovery(state, tokenType)) {
                    // If phrase-level recovery fails, use panic mode
                    panicModeRecovery();
                }
                
                continue;
            }

            // Valid action found - execute it
            String action = actionRow.get(tokenType);

            if (action.startsWith("s")) { 
                // Shift action - move to next token
                outputFormatter.updateCurrentLine(currentToken);
                int nextState = Integer.parseInt(action.substring(1));
                stateStack.push(nextState);
                symbolStack.push(tokenType);
                
                // Record the shift action
                outputFormatter.recordShift(state, nextState, currentToken);
                
                handleShift(currentToken);
                currentToken = lexer.nextToken();
            } else if (action.equals("acc")) { 
                // Accept - parsing completed successfully
                outputFormatter.recordSuccess();
                
                // Build final parse tree by combining all nodes
                while (treeStack.size() > 1) {
                    ParseTreeNode last = treeStack.pop();
                    treeStack.peek().addChild(last);
                }
                
                // Print the trace and error summary
                outputFormatter.printTrace();
                if (!errorMessages.isEmpty()) {
                    outputFormatter.printErrorSummary();
                }
                
                System.out.println("Parse tree nodes: " + treeStack.size());
                return;
            } else if (action.startsWith("r")) { 
                // Reduce action - apply grammar rule
                int ruleNumber = Integer.parseInt(action.substring(1));
                
                // Get production information for logging
                GrammarProduction production = ParsingTableGenerator.productionTable.get(ruleNumber);
                String lhs = production.getLhs();
                
                // Store current state for logging
                int currentState = stateStack.peek();
                
                // Perform the reduction
                reduce(ruleNumber);
                
                // Record the reduction (current state is now different after reduce)
                outputFormatter.recordReduce(
                    currentState, 
                    ruleNumber, 
                    production.toString(),
                    stateStack.peek(),
                    lhs
                );
            } else {
                // Unexpected action - should not happen with valid tables
                errorMessages.add("Unexpected action: " + action);
                return;
            }
        }
        
        // Reached max errors - print summary and exit
        System.out.println("Maximum error count reached. Stopping parsing.");
        outputFormatter.printTrace();
        outputFormatter.printErrorSummary();
    }

    /**
     * Prints a summarized report of all parsing errors encountered.
     */
  

    /**
     * Performs a reduction operation according to a grammar rule.
     * This implements the "reduce" part of shift-reduce parsing.
     * 
     * @param ruleNumber The grammar rule number to apply
     */
    private void reduce(int ruleNumber) {
        // Get the production for this rule number
        GrammarProduction production = ParsingTableGenerator.productionTable.get(ruleNumber);

        if (production == null) {
            System.out.println("Error: No production found for rule " + ruleNumber);
            return;
        }

        String lhs = production.getLhs();
        int rhsSize = production.getRhsSize();

        // Create a new parse tree node for this production
        ParseTreeNode node = new ParseTreeNode(lhs, null, ruleNumber);

        // Pop rhsSize symbols and states from the stacks
        // Build the parse tree bottom-up by adding children in reverse order
        for (int i = 0; i < rhsSize; i++) {
            symbolStack.pop();
            stateStack.pop();
            node.addChild(treeStack.pop());
        } 

        // Push the new node onto the tree stack
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
    }

    /**
     * Handles a shift operation by creating a terminal node in the parse tree.
     * 
     * @param token The token to shift
     */
    private void handleShift(Token token) {
        // Create a terminal node and push it onto the tree stack
        treeStack.push(new ParseTreeNode(
                token.getType().toString(),
                token.lexeme,
                -1 // -1 indicates terminal node
        ));
    }

    /**
     * Returns the completed parse tree after successful parsing.
     * 
     * @return The root node of the parse tree, or null if parsing failed
     */
    public ParseTreeNode getParseTree() {
        if (treeStack.size() != 1) {
            System.err.println("Warning: Incomplete parse tree - stack has " + treeStack.size() + " nodes");
        }
        return treeStack.isEmpty() ? null : treeStack.peek();
    }

    /**
     * Generates a DOT representation of the parse tree for visualization.
     * 
     * @return String containing DOT format representation of the parse tree
     */
    public String getParseTreeDot() {
        ParseTreeNode root = getParseTree();
        if (root == null)
            return "";
        return "digraph ParseTree {\n" +
                "  node [shape=box, fontname=\"Courier\"];\n" +
                "  edge [arrowhead=vee];\n" +
                root.toDot() +
                "}";
    }

    /**
     * Generates a graphical visualization of the parse tree using Graphviz.
     * 
     * @param outputPath The file path where the image should be saved
     * @throws IOException If there's an error writing files
     * @throws InterruptedException If the Graphviz process is interrupted
     */
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

    /**
     * Attempts phrase-level error recovery by inserting a common token
     * that could help continue parsing.
     * 
     * @param state Current parser state
     * @param tokenType Current token type
     * @return true if recovery successful, false otherwise
     */
    private boolean tryPhraseLevelRecovery(int state, String tokenType) {
        // Try inserting common tokens that would allow progress
        String[] commonTokens = {"SEMICOLON", "RPAREN", "RBRACE", "COMMA"};
        
        // First check if action table exists for this state
        HashMap<String, String> actionRow = ParsingTableGenerator.actionTable.get(state);
        if (actionRow == null) {
            return false;
        }
        
        // Try each common token to see if inserting it would allow parsing to continue
        for (String possibleToken : commonTokens) {
            if (actionRow.containsKey(possibleToken)) {
                String action = actionRow.get(possibleToken);
                if (action != null && !action.isEmpty()) {
                    outputFormatter.recordRecovery("INSERT", "Inserting missing " + possibleToken);
                    performAction(action, possibleToken);
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Implements panic mode error recovery by skipping tokens until
     * a synchronization point is found.
     */
    private void panicModeRecovery() {
        // Keep track of starting token for error reporting
        outputFormatter.updateCurrentLine(currentToken);
        Token errorToken = currentToken;
        int tokensSkipped = 0;
        
        
        // Skip tokens until we find one that can be processed in the current state
        while (currentToken.getType() != TokenType.EOF) {
            outputFormatter.updateCurrentLine(currentToken);
            int state = stateStack.peek();
            HashMap<String, String> actionRow = ParsingTableGenerator.actionTable.get(state);
            
            // Check if current token can be processed in current state
            if (actionRow != null && actionRow.containsKey(currentToken.getType().toString())) {
                outputFormatter.recordRecovery("SYNC", 
                    "Found synchronization point at token " + currentToken + " after skipping " + tokensSkipped + " tokens");
                break;
            }
            
            tokensSkipped++;
            currentToken = lexer.nextToken();
        }
        
        if (currentToken.getType() == TokenType.EOF) {
            outputFormatter.recordRecovery("FAIL", 
                "Recovery failed - reached end of file after error at line " + 
                errorToken.line + ", position " + errorToken.position);
        }
        
        // Pop states until we find a valid state for current token
        int statesPopped = 0;
        while (stateStack.size() > 1) {
            int state = stateStack.peek();
            HashMap<String, String> actionRow = ParsingTableGenerator.actionTable.get(state);
            if (actionRow != null && actionRow.containsKey(currentToken.getType().toString())) {
                break;
            }
            stateStack.pop();
            symbolStack.pop();
            statesPopped++;
        }
        
        if (statesPopped > 0) {
            outputFormatter.recordRecovery("DISCARD", "Popped " + statesPopped + " states to reach recovery state");
        }
    }

    /**
     * Performs the specified parsing action with the given token type.
     * Used during error recovery to simulate tokens.
     * 
     * @param action The action to perform (shift or reduce)
     * @param tokenType The token type to use
     */
    private void performAction(String action, String tokenType) {
        if (action.startsWith("s")) { // Shift action
            int nextState = Integer.parseInt(action.substring(1));
            stateStack.push(nextState);
            symbolStack.push(tokenType);
            // Create a dummy token for the inserted token
            Token dummyToken = new Token(
                TokenType.valueOf(tokenType),
                "", // empty lexeme
                currentToken.line, // use current line
                currentToken.position // use current position
            );
            handleShift(dummyToken);
        } else if (action.startsWith("r")) { // Reduce action
            int ruleNumber = Integer.parseInt(action.substring(1));
            reduce(ruleNumber);
        }
    }
    
    /**
     * Utility method to print all tokens from the lexer.
     * Useful for debugging the lexical analysis.
     */
    /**
 * Prints tokens in two formats:
 * 1. First in a line-aligned format similar to the original program structure
 * 2. Then in the detailed format with all token information
 */
    public void printTokens() {
        
        reset();
        
        System.out.println("=== Tokens (Aligned with Source) ===");
        
        // Track current line to handle line breaks
        int currentLine = 1;
        
        // Print aligned version first
        while (currentToken.getType() != TokenType.EOF) {
            // Skip comments if needed
            if (TOKENS_TO_IGNORE.contains(currentToken.getType())) {
                currentToken = lexer.nextToken();
                continue;
            }
            
            // Handle line breaks
            if (currentToken.line > currentLine) {
                // Print newlines for empty lines if needed
                while (currentLine < currentToken.line) {
                    System.out.println();
                    currentLine++;
                }
            }
            
            // Print the token type in brackets
            System.out.print("[" + currentToken.getType() + "]");
            
            // Add space between tokens on same line (except for certain tokens)
            if (!currentToken.getType().toString().matches("RPAREN|LPAREN|RBACE|LBRACE|SEMI")) {
                System.out.print(" ");
            }
            
            currentToken = lexer.nextToken();
        }
        
        System.out.println("\n\n=== Detailed Token Information ===");
        
        // Now print detailed version
        reset();  // Use reset() again to start from beginning
        while (currentToken.getType() != TokenType.EOF) {
            if (!TOKENS_TO_IGNORE.contains(currentToken.getType())) {
                System.out.println(currentToken);
            }
            currentToken = lexer.nextToken();
        }
        
        // Restore original state by resetting and fast-forwarding
        reset();
        
}
}