package project.interpreterComponents.utils;

public class InterpreterException extends RuntimeException {
    private final int lineNumber;
    
    public InterpreterException(String message, int lineNumber) {
        super(message);
        this.lineNumber = lineNumber;
    }
    
    public int getLineNumber() {
        return lineNumber;
    }
}