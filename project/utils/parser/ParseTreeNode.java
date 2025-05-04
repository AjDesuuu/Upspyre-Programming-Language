package project.utils.parser;

import java.util.ArrayList;
import java.util.List;

import project.Token;

public class ParseTreeNode {
    private final String symbol;       // Grammar symbol or token type
    private final String value;       // Lexeme for leaf nodes (null for non-terminals)
    private final List<ParseTreeNode> children = new ArrayList<>();
    private final int ruleNumber;
    private final Token token;

    public ParseTreeNode(String symbol, String value, int ruleNumber, Token token) {
        this.symbol = symbol;
        this.value = value;
        this.ruleNumber = ruleNumber;
        this.token = token;
    }
    public ParseTreeNode(String symbol, String value, int ruleNumber) {
        this(symbol, value, ruleNumber, null);
    }

    public String getType() {
        // Remove wrapping "<>" if present
        if (symbol.startsWith("<") && symbol.endsWith(">")) {
            return symbol.substring(1, symbol.length() - 1);
        }
        return symbol;
    }

    public List<ParseTreeNode> getChildren() {
        return children;
    }

    public Token getToken() {
        return token;
    }
    
    public String getSymbol() {
        return symbol;
    }

    public String getValue() {
        return value;
    }
    public int getRuleNumber() {
        return ruleNumber;
    }

    public void addChild(ParseTreeNode child) {
        children.add(0, child); // Add to front to maintain correct order
    }

    public String toDot() {
        StringBuilder sb = new StringBuilder();
        String nodeId = "n" + System.identityHashCode(this);
        
        // Node definition
        String label = symbol;
        if (value != null) label += "\\n" + value;
        if (ruleNumber != -1) label += "\\n(R" + ruleNumber + ")";
        
        sb.append(String.format("  %s [label=\"%s\"];\n", nodeId, label));
        
        // Child connections
        for (ParseTreeNode child : children) {
            sb.append(child.toDot());
            sb.append(String.format("  %s -> %s;\n", nodeId, "n" + System.identityHashCode(child)));
        }
        
        return sb.toString();
    }
}