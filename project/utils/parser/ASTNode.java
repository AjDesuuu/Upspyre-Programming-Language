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
    private int lineNumber;
    

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

        switch (cstNode.getType()) {
            case "PROGRAM":
                ASTNode programNode = new ASTNode("PROGRAM");
                for (ParseTreeNode child : cstNode.getChildren()) {
                    ASTNode childAST = fromCST(child);
                    if (childAST != null) {
                        programNode.addChild(childAST);
                    }
                }
                return programNode;

            case "IDENTIFIER":
                return new ASTNode("IDENTIFIER", cstNode.getValue(), cstNode.getToken());

            case "NUMBER":
                return new ASTNode("NUMBER", cstNode.getValue(), cstNode.getToken());

            case "DECIMAL":
                return new ASTNode("DECIMAL", cstNode.getValue(), cstNode.getToken());

            case "TRUE":
                return new ASTNode("TRUE", cstNode.getValue(), cstNode.getToken());
            
            case "FALSE":
                return new ASTNode("FALSE", cstNode.getValue(), cstNode.getToken());
            
            case "OUTPUT_STMT":
                ASTNode outputNode = new ASTNode("OUTPUT");
                ParseTreeNode expressionNode = cstNode.getChildren().get(2);
                outputNode.addChild(fromCST(expressionNode));
                return outputNode;

           
            case "IF":
                ASTNode ifNode = new ASTNode("IF");
                if (!cstNode.getChildren().isEmpty()) {
                    // Extract the condition (assuming it's the second child of the IF node in the CST)
                    if (cstNode.getChildren().size() > 1) {
                        ParseTreeNode conditionCST = cstNode.getChildren().get(1); // Adjust index if necessary
                        ASTNode conditionAST = fromCST(conditionCST);
                        if (conditionAST != null) {
                            ifNode.addChild(conditionAST); // Add the condition as a child of the IF node
                        } else {
                            throw new RuntimeException("Error: Failed to parse condition in IF statement.");
                        }
                    }
            
                    // Extract the IF block (assuming it's the third child of the IF node in the CST)
                    if (cstNode.getChildren().size() > 2) {
                        ParseTreeNode ifBlockCST = cstNode.getChildren().get(2);
                        ASTNode ifBlockAST = fromCST(ifBlockCST);
                        if (ifBlockAST != null) {
                            ifNode.addChild(ifBlockAST); // Add the IF block as a child of the IF node
                        }
                    }
            
                    // Extract the OTHERWISE block (if it exists, assuming it's the fifth child in the CST)
                    if (cstNode.getChildren().size() > 4) {
                        ParseTreeNode otherwiseBlockCST = cstNode.getChildren().get(4);
                        ASTNode otherwiseBlockAST = fromCST(otherwiseBlockCST);
                        if (otherwiseBlockAST != null) {
                            ifNode.addChild(otherwiseBlockAST); // Add the OTHERWISE block as a child of the IF node
                        }
                    }
                }
                return ifNode;

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

            case "TEXT":
                return new ASTNode("TEXT", cstNode.getValue(), cstNode.getToken());

            
                
            default:
                if (cstNode.getChildren().size() == 1) {
                    return fromCST(cstNode.getChildren().get(0));
                } else {
                    ASTNode defaultNode = new ASTNode(cstNode.getType());
                    for (ParseTreeNode child : cstNode.getChildren()) {
                        ASTNode childAST = fromCST(child);
                        if (childAST != null) {
                            defaultNode.addChild(childAST);
                        }
                    }
                    return defaultNode;
                }
        }

        return null;
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

            opAST.addChild(left);
            opAST.addChild(right);
            left = opAST;
        }
        return left;
    }

    private static ASTNode buildRightAssociativeBinaryExpressionAST(ParseTreeNode node) {
        List<ParseTreeNode> children = node.getChildren();

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
                writer.write("digraph AST {\n");
                writer.write(toDot());
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
