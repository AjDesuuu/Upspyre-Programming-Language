package project;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * A utility class that improves the readability of parser outputs
 * by providing cleaner formatting and removing redundancy.
 */
public class ParserOutputFormatter {
    private static int LINE_WIDTH = 80; // Now non-final to allow dynamic adjustment
    private static final boolean SHOW_DETAILED_MOVES = false; // Set to true for verbose mode
    
    private final List<String> messages = new ArrayList<>();
    private final List<Integer> contentLengths = new ArrayList<>(); // Track content lengths
    private int currentStep = 0;
    private boolean isSuccessful = false;
    
    /**
     * Records a shift action in the parsing process.
     * 
     * @param state Current state
     * @param nextState State after shifting
     * @param token Token being shifted
     */
    public void recordShift(int state, int nextState, Token token) {
        String message;
        if (SHOW_DETAILED_MOVES) {
            message = String.format("%-4d| SHIFT  | State %-3d → %-3d | Token: %-10s | Lexeme: %-15s | Line: %d:%d",
                    ++currentStep, state, nextState, token.getType(), 
                    token.lexeme, token.line, token.position);
        } else {
            message = String.format("%-4d| SHIFT  | %-3d → %-3d | %-10s | %s",
                    ++currentStep, state, nextState, token.getType(), token.lexeme);
        }
        messages.add(message);
        contentLengths.add(message.length());
    }
    
    /**
     * Records a reduce action in the parsing process.
     * 
     * @param state Current state
     * @param ruleNumber Grammar rule number used for reduction
     * @param production String representation of the production
     * @param nextState State after reduction and goto
     * @param nonTerminal The non-terminal resulting from reduction
     */
    public void recordReduce(int state, int ruleNumber, String production, 
                          int nextState, String nonTerminal) {
        String message;
        if (SHOW_DETAILED_MOVES) {
            message = String.format("%-4d| REDUCE | Rule %-3d | %-40s | State: %d → %d with %s", 
                    ++currentStep, ruleNumber, production, state, nextState, nonTerminal);
        } else {
            message = String.format("%-4d| REDUCE | Rule %-3d | %s", 
                    ++currentStep, ruleNumber, production);
        }
        messages.add(message);
        contentLengths.add(message.length());
    }
    
    /**
     * Records an error encountered during parsing.
     * 
     * @param state Current state
     * @param token The token that caused the error
     * @param expectedTokens Set of tokens that would have been valid
     */
    public void recordError(int state, Token token, String expectedTokens) {
        String message = String.format("%-4d| ERROR  | State %-3d | Token: %-10s | Expected: %s", 
                ++currentStep, state, token.getType(), expectedTokens);
        messages.add(message);
        contentLengths.add(message.length());
    }
    
    /**
     * Records an error recovery action.
     * 
     * @param action The type of recovery performed
     * @param details Additional details about the recovery
     */
    public void recordRecovery(String action, String details) {
        String message = String.format("%-4d| RECOVER| %-8s | %s", 
                ++currentStep, action, details);
        messages.add(message);
        contentLengths.add(message.length());
    }
    
    /**
     * Records a successful completion of parsing.
     */
    public void recordSuccess() {
        String message = String.format("%-4d| ACCEPT | Parsing completed successfully", 
                ++currentStep);
        messages.add(message);
        contentLengths.add(message.length());
        isSuccessful = true;
    }
    
    /**
     * Calculates the optimal width for the table based on content.
     * @return The width to use for the table
     */
    private int calculateOptimalWidth() {
        // Find the longest content
        int maxContentLength = contentLengths.stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(50);  // Default if no messages
        
        // Add padding for table borders
        int optimalWidth = maxContentLength + 4;  // 4 = space on each side + '│' characters
        
        // Minimum width to maintain readability
        return Math.max(optimalWidth, 80);
    }
    
    /**
     * Prints the formatted parsing trace.
     */
    public void printTrace() {
        // Determine optimal width for table
        LINE_WIDTH = calculateOptimalWidth();
        
        System.out.println(separator());
        System.out.println("│ " + centerText("PARSER TRACE OUTPUT", LINE_WIDTH - 4) + " │");
        System.out.println(separator());
        
        // Create header with appropriate width
        String header = "│ Step | Action | Details";
        System.out.println(header + " ".repeat(Math.max(0, LINE_WIDTH - header.length() - 1)) + "│");
        System.out.println(separator());
        
        for (String msg : messages) {
            System.out.println("│ " + msg + " ".repeat(Math.max(0, LINE_WIDTH - msg.length() - 4)) + " │");
        }
        
        System.out.println(separator());
        if (isSuccessful) {
            System.out.println("│ " + centerText("✓ PARSING SUCCESSFUL", LINE_WIDTH - 4) + " │");
        } else {
            System.out.println("│ " + centerText("✗ PARSING FAILED", LINE_WIDTH - 4) + " │");
        }
        System.out.println(separator());
    }
    
    /**
     * Prints a summary of errors encountered.
     * 
     * @param errorMessages List of error messages
     */
    public void printErrorSummary(List<String> errorMessages) {
        if (errorMessages.isEmpty()) {
            return;
        }
        
        // Calculate optimal width based on error messages too
        for (String error : errorMessages) {
            int errorLength = error.length() + 10; // Add some space for error numbering
            if (errorLength + 4 > LINE_WIDTH) {
                LINE_WIDTH = errorLength + 4;
            }
        }
        
        System.out.println(separator());
        System.out.println("│ " + centerText("ERROR SUMMARY", LINE_WIDTH - 4) + " │");
        System.out.println(separator());
        
        for (int i = 0; i < errorMessages.size(); i++) {
            String msg = "[Error " + (i+1) + "] " + errorMessages.get(i);
            
            // Split long messages across multiple lines
            while (msg.length() > LINE_WIDTH - 4) {
                String currentLine = msg.substring(0, LINE_WIDTH - 4);
                System.out.println("│ " + currentLine + " │");
                msg = "  " + msg.substring(LINE_WIDTH - 4);
            }
            
            System.out.println("│ " + msg + " ".repeat(Math.max(0, LINE_WIDTH - msg.length() - 4)) + " │");
        }
        
        System.out.println(separator());
    }
    
    /**
     * Creates a horizontal separator line.
     */
    private String separator() {
        return "├" + "─".repeat(LINE_WIDTH - 2) + "┤";
    }
    
    /**
     * Centers text in a field of the specified width.
     */
    private String centerText(String text, int width) {
        int padding = width - text.length();
        int padLeft = padding / 2;
        int padRight = padding - padLeft;
        return " ".repeat(padLeft) + text + " ".repeat(padRight);
    }
    
    /**
     * Creates a representation of the parsing stacks.
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