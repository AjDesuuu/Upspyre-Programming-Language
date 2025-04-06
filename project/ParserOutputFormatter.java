package project;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * A utility class that improves the readability of parser outputs
 * by providing cleaner formatting and removing redundancy.
 */
public class ParserOutputFormatter {
    private static int LINE_WIDTH = 120;
    private static final boolean SHOW_DETAILED_MOVES = true;
    
    // Column widths for consistent formatting
    private static final int STEP_WIDTH = 8;
    private static final int ACTION_WIDTH = 8;
    private static final int STATE_WIDTH = 12;
    private static final int LINE_WIDTH_COL = 5;
    private static final int TOKEN_WIDTH = 10;
    private static final int DETAILS_WIDTH = 30;

    private final List<String> errorMessages = new ArrayList<>();
    private final List<String> messages = new ArrayList<>();
    private final List<Integer> contentLengths = new ArrayList<>();
    private int currentStep = 0;
    private boolean isSuccessful = false;
    private int currentLine = 1;
    private Token lastToken;
    private final List<Integer> lineNumbers = new ArrayList<>();
    private int endTokenLine = -1;
    
    /**
     * Records a shift action in the parsing process.
     */
    public void recordShift(int state, int nextState, Token token) {
        lineNumbers.add(token.line);
        String message = String.format(
            "%-" + STEP_WIDTH + "d| %-" + ACTION_WIDTH + "s | %-" + STATE_WIDTH + "s | %-" + 
            LINE_WIDTH_COL + "d | %-" + TOKEN_WIDTH + "s | %-" + DETAILS_WIDTH + "s",
            ++currentStep,
            "SHIFT",
            state + " -> " + nextState,
            token.line,
            token.getType(),
            token.lexeme
        );
        messages.add(message);
        contentLengths.add(message.length());
    }
    
    /**
     * Records a reduce action in the parsing process.
     */
    public void recordReduce(int state, int ruleNumber, String production, 
                          int nextState, String nonTerminal) {
        int line = (ruleNumber == 1 && endTokenLine != -1) ? endTokenLine : currentLine;
        lineNumbers.add(line);
        
        String message = String.format(
            "%-" + STEP_WIDTH + "d| %-" + ACTION_WIDTH + "s | %-" + STATE_WIDTH + "s | %-" + 
            LINE_WIDTH_COL + "s | %-" + TOKEN_WIDTH + "s | %-" + DETAILS_WIDTH + "s",
            ++currentStep,
            "REDUCE",
            state + " -> " + nextState,
            line > 0 ? String.valueOf(line) : "",
            "",
            production
        );
        messages.add(message);
        contentLengths.add(message.length());
    }
    
    /**
     * Records an error encountered during parsing.
     */
    public void recordError(int state, Token token, String expectedTokens) {
        lineNumbers.add(token.line);
        
        // Clean up expected tokens display
        String cleanExpected = expectedTokens.replace("[", "").replace("]", "");
        
        String message = String.format(
            "%-" + STEP_WIDTH + "d| %-" + ACTION_WIDTH + "s | %-" + STATE_WIDTH + "s | %-" + 
            LINE_WIDTH_COL + "d | %-" + TOKEN_WIDTH + "s | %-" + DETAILS_WIDTH + "s",
            ++currentStep,
            "ERROR",
            "State " + state,
            token.line,
            token.getType(),
            "Unexpected: " + token.getType() + " | Expected: " + cleanExpected
        );
        messages.add(message);
        contentLengths.add(message.length());
        
        // Store simplified error message for summary
        String errorMsg = String.format("Line %d:%d - Unexpected token: '%s'",
                                      token.line, token.position, 
                                      token.getType());
        errorMessages.add(errorMsg + "\n    Expected token/s type: '" + cleanExpected+"'");
    }

    public void updateCurrentLine(Token token) {
        if (token != null && (lastToken == null || token.line != lastToken.line)) {
            currentLine = token.line;
            lastToken = token;
        }

        if (token != null && token.getType() != null && token.getType().toString().equals("END")) {
            endTokenLine = token.line;
        }
    }

    public int getCurrentLine() {
        return currentLine;
    }
    
    /**
     * Records an error recovery action.
     */
    public void recordRecovery(String action, String details) {
        lineNumbers.add(currentLine);
        String message = String.format(
            "%-" + STEP_WIDTH + "d| %-" + ACTION_WIDTH + "s | %-" + STATE_WIDTH + "s | %-" + 
            LINE_WIDTH_COL + "d | %-" + TOKEN_WIDTH + "s | %-" + DETAILS_WIDTH + "s",
            ++currentStep,
            "RECOVER",
            action,
            currentLine,
            "",
            details
        );
        messages.add(message);
        contentLengths.add(message.length());
    }
    
    /**
     * Records a successful completion of parsing.
     */
    public void recordSuccess() {
        int line = endTokenLine != -1 ? endTokenLine : currentLine;
        lineNumbers.add(line);
        
        String message = String.format(
            "%-" + STEP_WIDTH + "d| %-" + ACTION_WIDTH + "s | %-" + STATE_WIDTH + "s | %-" + 
            LINE_WIDTH_COL + "s | %-" + TOKEN_WIDTH + "s | %-" + DETAILS_WIDTH + "s",
            ++currentStep,
            "ACCEPT",
            "",
            "",
            "",
            "Parsing completed successfully"
        );
        messages.add(message);
        contentLengths.add(message.length());
        isSuccessful = true;
    }
    
    
    
    /**
     * Prints the formatted parsing trace.
     */
    public void printTrace() {
    
        System.out.println(String.format("%-" + LINE_WIDTH + "s", "PARSER TRACE OUTPUT"));
        System.out.println("-".repeat(LINE_WIDTH));
        
        // Header with new column order
        String header = String.format(
            "%-" + STEP_WIDTH + "s| %-" + ACTION_WIDTH + "s | %-" + STATE_WIDTH + "s | %-" + 
            LINE_WIDTH_COL + "s | %-" + TOKEN_WIDTH + "s | %-" + DETAILS_WIDTH + "s",
            "Step", "Action", "States", "Line", "Token", "Details"
        );
        System.out.println(header);
        System.out.println("-".repeat(LINE_WIDTH));
        
        int lastLineNumber = -1;
        for (int i = 0; i < messages.size(); i++) {
            String msg = messages.get(i);
            int lineNum = lineNumbers.get(i);
            
            if (lineNum != lastLineNumber && lineNum > 0) {
                System.out.println("-".repeat(LINE_WIDTH));
                System.out.println("Current Line: " + lineNum);
                lastLineNumber = lineNum;
            }
            
            System.out.println(msg);
        }
        
        System.out.println("-".repeat(LINE_WIDTH));
        System.out.println(isSuccessful ? "[/] PARSING SUCCESSFUL" : "[X] PARSING FAILED");
        System.out.println("-".repeat(LINE_WIDTH));
    }
    
    /**
     * Prints a summary of errors encountered.
     * 
     * @param errorMessages List of error messages
     */
    public void printErrorSummary() {
        if (errorMessages.isEmpty()) {
            return;
        }
    
        
        
        System.out.println("\n" + centerText("ERROR SUMMARY", LINE_WIDTH));
        System.out.println("-".repeat(LINE_WIDTH));
        
        for (int i = 0; i < errorMessages.size(); i++) {
            String[] parts = errorMessages.get(i).split("\n");
            
            System.out.println("Error " + (i+1) + ":");
            System.out.println("  " + parts[0]);  // Location and unexpected token
            System.out.println("  " + parts[1]);  // Expected tokens
            
            if (i < errorMessages.size() - 1) {
                System.out.println("-".repeat(Math.min(LINE_WIDTH, 30)));
            }
        }
        
        System.out.println("-".repeat(LINE_WIDTH));
    }
    
    // Helper method to center text
    private String centerText(String text, int width) {
        int padding = (width - text.length()) / 2;
        if (padding <= 0) return text;
        return " ".repeat(padding) + text + " ".repeat(padding);
    }
    
    /**
     * Creates a string representation of the parsing stacks.
     * 
     * @param stateStack The state stack
     * @param symbolStack The symbol stack
     * @return String representation of the current stack state
     */
    public String formatStacks(Stack<Integer> stateStack, Stack<String> symbolStack) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("States: [");
        for (int i = 0; i < stateStack.size(); i++) {
            if (i > 0) sb.append(" ");
            sb.append(stateStack.get(i));
            if (sb.length() > 30) {
                sb.append("...]");
                break;
            }
        }
        if (!sb.toString().endsWith("]")) {
            sb.append("]");
        }
        
        sb.append(" | Symbols: [");
        for (int i = 0; i < symbolStack.size(); i++) {
            if (i > 0) sb.append(" ");
            sb.append(symbolStack.get(i));
            if (sb.length() > 70) {
                sb.append("...]");
                break;
            }
        }
        if (!sb.toString().endsWith("]")) {
            sb.append("]");
        }
        
        return sb.toString();
    }
}