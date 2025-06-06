package project.utils.parser;

import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import project.Token;

public class ASTNode {
    private String type;
    private String value;
    private List<ASTNode> children;
    private ASTNode parent;
    private int lineNumber = 0;
    

    public ASTNode(String type) {
        this(type, null);
    }

    public ASTNode(String type, String value) {
        this.type = type;
        this.value = value;
        this.children = new ArrayList<>();
    }
    public ASTNode(String type, String value, Token token) {
        this.type = type;
        this.value = value;
        this.children = new ArrayList<>();
        this.lineNumber = (token != null) ? token.getLine() : 0;
    }
    public int getLineNumber() {
        return lineNumber;
    }

    public void addChild(ASTNode child) {
        child.setParent(this);
        children.add(child);
    }

    public ASTNode getParent() {
        return parent;
    }

    private void setParent(ASTNode parent) {
        this.parent = parent;
    }

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public List<ASTNode> getChildren() {
        return children;
    }

    

    public static ASTNode fromCST(ParseTreeNode cstNode) {
        if (cstNode == null) return null;
    
        // List of all terminal token types in your language
        final java.util.Set<String> TERMINALS = java.util.Set.of(
            "START", "END", "IF", "OTHERWISE", "FOR", "REPEAT", "UNTIL", "CONTINUE", "STOP", "METHOD", "OUTPUT", "GET", "SHOW", "CHOOSE_WHAT", "CONVERT_TO",
            "NUMBER_TYPE", "DECIMAL_TYPE", "TEXT_TYPE", "BINARY_TYPE", "LIST_TYPE", "PAIR_MAP_TYPE", "PICK",
            "TRUE", "FALSE", "NONE",
            "LEN", "SORT", "KEY", "VALUE", "TO_TEXT",
            "IDENTIFIER", "NUMBER", "DECIMAL", "TEXT",
            "ASSIGN", "PLUS", "MINUS", "MULT", "DIV", "EXPONENT", "MOD", "FLOOR_DIV", "PLUS_ASSIGN", "MINUS_ASSIGN", "MULT_ASSIGN",
            "AND", "OR", "NOT", "EQ", "NEQ", "LT", "GT", "LEQ", "GEQ",
            "BITWISE_AND", "BITWISE_OR", "BITWISE_XOR", "BITWISE_NOT", "LSHIFT", "RSHIFT", "S_NOT", "QUOTE",
            "LPAREN", "RPAREN", "LCURLY", "RCURLY", "LBRACKET", "RBRACKET", "COMMA", "SEMI", "COLON", "DOT"
        );
    
        String type = cstNode.getType();
    
        // If this is a terminal, always create an ASTNode with the token and line number
        if (TERMINALS.contains(type)) {
            return new ASTNode(type, cstNode.getValue(), cstNode.getToken());
        }
    
        // Helper: get the first non-zero line number from children
        int lineNumber = 0;
        if (cstNode.getToken() != null && cstNode.getToken().getLine() > 0) {
            lineNumber = cstNode.getToken().getLine();
        } else {
            for (ParseTreeNode child : cstNode.getChildren()) {
                if (child.getToken() != null && child.getToken().getLine() > 0) {
                    lineNumber = child.getToken().getLine();
                    break;
                }
            }
        }
        Token fakeToken = null;
        if (lineNumber > 0) {
            // Create a dummy token just for line number propagation
            fakeToken = new Token(null, null, lineNumber, 0);
        }
    
        switch (type) {
            case "PROGRAM":
                ASTNode programNode = new ASTNode("PROGRAM", null, fakeToken);
                for (ParseTreeNode child : cstNode.getChildren()) {
                    ASTNode childAST = fromCST(child);
                    if (childAST != null) {
                        programNode.addChild(childAST);
                    }
                }

                if (programNode.getLineNumber() == 0) {
                    programNode.lineNumber = findFirstNonZeroLine(cstNode);
                }
                return programNode;
    
            case "OUTPUT_STMT":
                ASTNode outputNode = new ASTNode("OUTPUT", null, fakeToken);
                if (cstNode.getChildren().size() >= 3) {
                    ASTNode exprAST = fromCST(cstNode.getChildren().get(2));
                    if (exprAST != null) outputNode.addChild(exprAST);
                }
                if (outputNode.getLineNumber() == 0) {
                    outputNode.lineNumber = findFirstNonZeroLine(cstNode);
                }
                return outputNode;
    
            case "IF":
                ASTNode ifNode = new ASTNode("IF", null, fakeToken);
                if (!cstNode.getChildren().isEmpty()) {
                    if (cstNode.getChildren().size() > 1) {
                        ASTNode conditionAST = fromCST(cstNode.getChildren().get(1));
                        if (conditionAST != null) ifNode.addChild(conditionAST);
                    }
                    if (cstNode.getChildren().size() > 2) {
                        ASTNode ifBlockAST = fromCST(cstNode.getChildren().get(2));
                        if (ifBlockAST != null) ifNode.addChild(ifBlockAST);
                    }
                    if (cstNode.getChildren().size() > 4) {
                        ASTNode otherwiseBlockAST = fromCST(cstNode.getChildren().get(4));
                        if (otherwiseBlockAST != null) ifNode.addChild(otherwiseBlockAST);
                    }
                    if (ifNode.getLineNumber() == 0) {
                        ifNode.lineNumber = findFirstNonZeroLine(cstNode);
                        
                    }
                }
                return ifNode;
    
            // Expression flattening for single-child nodes
            case "EXPRESSION":
            case "CONST":
                return fromCST(cstNode.getChildren().get(0));
    
            // Left-associative binary expressions
            case "LOGICOR_EXPR":
            case "LOGICAND_EXPR":
            case "RELATIONAL_EXPR":
            case "BITOR_EXPR":
            case "BITXOR_EXPR":
            case "BITAND_EXPR":
            case "BITSHIFT_EXPR":
            case "BIT_BASE":
            case "TERM":
                return buildLeftAssociativeBinaryExpressionAST(cstNode);
    
            // Right-associative binary expressions (e.g., exponentiation)
            case "FACTOR":
                return buildRightAssociativeBinaryExpressionAST(cstNode);
    
            case "BASE":
                if (cstNode.getChildren().size() == 1) {
                    return fromCST(cstNode.getChildren().get(0));
                } else if (cstNode.getChildren().size() == 3) {
                    return fromCST(cstNode.getChildren().get(1));
                }
                break;
    
            default:
                if (cstNode.getChildren().size() == 1) {
                    ASTNode childAST = fromCST(cstNode.getChildren().get(0));
                    // Propagate line number if this node has a token or child has one
                    if (childAST != null && lineNumber > 0) {
                        childAST.lineNumber = lineNumber;
                    }
                    return childAST;
                } else {
                    ASTNode defaultNode = new ASTNode(type, null, fakeToken);
                    for (ParseTreeNode child : cstNode.getChildren()) {
                        ASTNode childAST = fromCST(child);
                        if (childAST != null) {
                            defaultNode.addChild(childAST);
                        }
                    }
                    if (defaultNode.getLineNumber() == 0) {
                        defaultNode.lineNumber = findFirstNonZeroLine(cstNode);
                    }
                    return defaultNode;
                }
        }
    
        return null;
    }
    private static int findFirstNonZeroLine(ParseTreeNode node) {
        if (node.getToken() != null && node.getToken().getLine() > 0) {
            return node.getToken().getLine();
        }
        for (ParseTreeNode child : node.getChildren()) {
            int line = findFirstNonZeroLine(child);
            if (line > 0) return line;
        }
        return 0;
    }
    // Helper: Build left-associative binary operator AST for BIT_BASE, TERM, FACTOR
    private static ASTNode buildLeftAssociativeBinaryExpressionAST(ParseTreeNode node) {
        List<ParseTreeNode> children = node.getChildren();
        if (children.size() == 1) {
            return fromCST(children.get(0));
        }

        ASTNode left = fromCST(children.get(0));
        for (int i = 1; i < children.size(); i += 2) {
            ParseTreeNode opNode = children.get(i);
            String opType = extractOperatorTerminal(opNode);

            ASTNode opAST = new ASTNode(opType);
            ASTNode right = fromCST(children.get(i + 1));
            if (opNode.getToken() != null) {
                opAST.lineNumber = opNode.getToken().getLine();
            } else if (left != null && left.getLineNumber() > 0) {
                opAST.lineNumber = left.getLineNumber();
            } else if (right != null && right.getLineNumber() > 0) {
                opAST.lineNumber = right.getLineNumber();
            } else {
                opAST.lineNumber = 0;
            }
            

            opAST.addChild(left);
            opAST.addChild(right);
            left = opAST;
        }
        return left;
    }

    private static ASTNode buildRightAssociativeBinaryExpressionAST(ParseTreeNode node) {
        List<ParseTreeNode> children = node.getChildren();

        if (children.size() == 2 && "MINUS".equals(children.get(0).getType())) {
            ASTNode minusNode = new ASTNode("MINUS");
            minusNode.addChild(fromCST(children.get(1)));
            return minusNode;
        }
        
        // Base case: If there's only one child, return that node as is
        if (children.size() == 1) {
            return fromCST(children.get(0));
        }

        // The right-associative rule should start from the last operator
        String opType = extractOperatorTerminal(children.get(children.size() - 2));

        // Recursively process the right side of the expression first (right-to-left)
        ParseTreeNode rightNode = children.get(children.size() - 1);
        ASTNode right = fromCST(rightNode);

        // Recursively process the left side of the expression (right-associative)
        // The left side is everything before the last operator
        List<ParseTreeNode> leftChildren = children.subList(0, children.size() - 2);

        // Create a new ParseTreeNode for the left children, preserving the parent rule number
        ParseTreeNode leftNode = new ParseTreeNode(node.getSymbol(), null, node.getRuleNumber());
        for (ParseTreeNode leftChild : leftChildren) {
            leftNode.addChild(leftChild);
        }

        ASTNode left = buildRightAssociativeBinaryExpressionAST(leftNode);

        // Build the operator AST and return it
        ASTNode opAST = new ASTNode(opType);
        opAST.addChild(left);
        opAST.addChild(right);
        return opAST;
    }



    private static String extractOperatorTerminal(ParseTreeNode opNode) {
        if (opNode.getChildren().isEmpty()) {
            return opNode.getType(); // terminal
        } else {
            return extractOperatorTerminal(opNode.getChildren().get(0)); // dig until terminal
        }
    }


    public void printAST(int depth) {
        for (int i = 0; i < depth; i++) {
            System.out.print("  ");
        }
        System.out.println(type + (value != null ? " (" + value + ")" : ""));
        for (ASTNode child : children) {
            child.printAST(depth + 1);
        }
    }

    public String toDot() {
        StringBuilder sb = new StringBuilder();
        String nodeId = "n" + System.identityHashCode(this);
    
        // Node label: type and value (if present)
        String label = type;
        if (value != null) label += "\\n" + value;
    
        sb.append(String.format("  %s [label=\"%s\"];\n", nodeId, label));
    
        for (ASTNode child : children) {
            sb.append(child.toDot());
            sb.append(String.format("  %s -> %s;\n", nodeId, "n" + System.identityHashCode(child)));
        }
    
        return sb.toString();
    }

    public void generateImage(String outputFilePath) {
        try {
            File dotFile = File.createTempFile("ast", ".dot");
            try (FileWriter writer = new FileWriter(dotFile)) {
                // Match the parse tree's DOT header
                writer.write("digraph AST {\n");
                writer.write("  node [shape=box, fontname=\"Courier\"];\n");
                writer.write("  edge [arrowhead=vee];\n");
                writer.write(this.toDot());
                writer.write("}\n");
            }
    
            String command = String.format("dot -Tpng %s -o %s", dotFile.getAbsolutePath(), outputFilePath);
            Process process = Runtime.getRuntime().exec(command);
            int exitCode = process.waitFor();
    
            if (exitCode != 0) {
                throw new RuntimeException("Graphviz failed to generate the image. Ensure Graphviz is installed and available in PATH.");
            }
    
            System.out.println("AST image generated: " + outputFilePath);
            dotFile.delete();
    
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to generate AST image: " + e.getMessage(), e);
        }
    }
}
