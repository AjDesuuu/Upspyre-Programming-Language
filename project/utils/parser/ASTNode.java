package project.utils.parser;

import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


public class ASTNode {
    private String type;
    private String value;
    private List<ASTNode> children;

    public ASTNode(String type) {
        this(type, null);
    }

    public ASTNode(String type, String value) {
        this.type = type;
        this.value = value;
        this.children = new ArrayList<>();
    }

    public void addChild(ASTNode child) {
        children.add(child);
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

    // Static method to convert CST to AST
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
                // Preserve the identifier's lexeme value
                return new ASTNode("IDENTIFIER", cstNode.getValue());

            case "NUMBER":
                // Preserve the number's lexeme value
                return new ASTNode("NUMBER", cstNode.getValue());  
            
            case "DECIMAL":
                // Preserve the decimal's lexeme value
                return new ASTNode("DECIMAL", cstNode.getValue());
            case "TRUE":
                // Preserve the boolean's lexeme value
                return new ASTNode("TRUE", cstNode.getValue());

            case "OUTPUT_STMT":
                // Create an OUTPUT node with the expression as its child
                ASTNode outputNode = new ASTNode("OUTPUT");
                ParseTreeNode expressionNode = cstNode.getChildren().get(2); // <EXPRESSION> is the 3rd child
                outputNode.addChild(fromCST(expressionNode));
                return outputNode;

            case "EXPRESSION":
            case "CONST":
                // Directly return the value of the expression or constant
                return fromCST(cstNode.getChildren().get(0));

            case "TEXT":
                // Leaf node for text
                return new ASTNode("TEXT", cstNode.getValue());

            default:
                // Recursively process other nodes
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
    }
    // Print the AST structure
    public void printAST(int depth) {
        // Indentation for readability
        for (int i = 0; i < depth; i++) {
            System.out.print("  ");
        }

        // Print the current node
        System.out.println(type + (value != null ? " (" + value + ")" : ""));

        // Recursively print children
        for (ASTNode child : children) {
            child.printAST(depth + 1);
        }
    }

    // Generate a Graphviz-compatible .dot representation of the AST
    public String toDot() {
        StringBuilder sb = new StringBuilder();
        String nodeId = "n" + System.identityHashCode(this);

        // Node definition
        String label = type;
        if (value != null) label += "\\n" + value;

        sb.append(String.format("  %s [label=\"%s\"];\n", nodeId, label));

        // Child connections
        for (ASTNode child : children) {
            sb.append(child.toDot());
            sb.append(String.format("  %s -> %s;\n", nodeId, "n" + System.identityHashCode(child)));
        }

        return sb.toString();
    }

    // Generate a PNG image of the AST
    public void generateImage(String outputFilePath) {
        try {
            // Step 1: Create a temporary .dot file
            File dotFile = File.createTempFile("ast", ".dot");
            try (FileWriter writer = new FileWriter(dotFile)) {
                writer.write("digraph AST {\n");
                writer.write(toDot());
                writer.write("}\n");
            }

            // Step 2: Use Graphviz to generate the PNG
            String command = String.format("dot -Tpng %s -o %s", dotFile.getAbsolutePath(), outputFilePath);
            Process process = Runtime.getRuntime().exec(command);
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new RuntimeException("Graphviz failed to generate the image. Ensure Graphviz is installed and available in PATH.");
            }

            System.out.println("AST image generated: " + outputFilePath);

            // Step 3: Delete the temporary .dot file
            dotFile.delete();

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to generate AST image: " + e.getMessage(), e);
        }
    }
}