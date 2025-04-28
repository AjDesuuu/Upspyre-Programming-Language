package project;


import project.utils.parser.ASTNode;
import project.utils.parser.ParseTreeNode; 

public class Interpreter {
    private SymbolTable symbolTable;

    public Interpreter(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    public void interpret(ParseTreeNode root) {
        if (root == null) {
            System.err.println("Error: Parse tree is null. Cannot interpret.");
            return;
        }
    
        

        System.out.println("Converting CST to AST...");
        ASTNode astRoot = ASTNode.fromCST(root);

        System.out.println("Generated AST:");
        astRoot.printAST(0);

        System.out.println("\nGenerating AST image...");
        astRoot.generateImage("ast.png");

        System.out.println("\nStarting interpretation...");
        executeASTNode(astRoot);
    }



    private Object evaluateBinaryOperation(Object left, String operator, Object right) {
        if (left instanceof Integer && right instanceof Integer) {
            int l = (Integer) left;
            int r = (Integer) right;
            return switch (operator) {
                case "+" -> l + r;
                case "-" -> l - r;
                case "*" -> l * r;
                case "/" -> l / r;
                default -> throw new RuntimeException("Unsupported operator: " + operator);
            };
        }
        throw new RuntimeException("Type mismatch in binary operation.");
    }



    private TokenType inferType(Object value) {
        if (value instanceof Integer) return TokenType.NUMBER;
        if (value instanceof String) return TokenType.TEXT;
        throw new RuntimeException("Unknown type for value: " + value);
    }

    private void executeASTNode(ASTNode node) {
        switch (node.getType()) {
            case "PROGRAM":
            case "PROGRAM_KLEENE":
            case "STMT":
            case "START":
            case "END":
                // Process structural nodes
                for (ASTNode child : node.getChildren()) {
                    executeASTNode(child);
                }
                break;
    
            case "ASSIGNMENT_STMT":
                // Handle both forms of assignment
                String variable = null;
                Object value = null;
                TokenType type = null;
    
                for (ASTNode child : node.getChildren()) {
                    switch (child.getType()) {
                        case "TEXT_TYPE":
                            type = TokenType.TEXT;
                            break;
                        case "IDENTIFIER":
                            variable = child.getValue();
                            break;
                        case "TEXT":
                            value = child.getValue();
                            if (value != null) {
                                try {
                                    value = Integer.parseInt((String)value);
                                } catch (NumberFormatException e) {
                                    // Keep as string if not a number
                                }
                            }
                            break;
                        case "ASSIGN":
                            // Skip assignment operator
                            break;
                        default:
                            // Handle complex expressions
                            value = evaluateASTNode(child);
                            break;
                    }
                }
    
                if (variable != null) {
                    if (type == null) {
                        // For assignments without type declaration, infer type from value
                        type = inferType(value);
                    }
                    symbolTable.addIdentifier(variable, type, value);
                    System.out.println("Assigned " + variable + " = " + value);
                }
                break;
    
            case "OUTPUT":
                // Handle output statements
                for (ASTNode child : node.getChildren()) {
                    if (child.getType().equals("IDENTIFIER")) {
                        SymbolDetails details = symbolTable.getIdentifier(child.getValue());
                        if (details != null) {
                            System.out.println("Output: " + details.getValue());
                        } else {
                            System.out.println("Error: Undefined variable " + child.getValue());
                        }
                    } else {
                        // Handle direct value output
                        Object outputValue = evaluateASTNode(child);
                        System.out.println("Output: " + outputValue);
                    }
                }
                break;
    
            default:
                // Process other nodes
                for (ASTNode child : node.getChildren()) {
                    executeASTNode(child);
                }
        }
    }
    private Object evaluateASTNode(ASTNode node) {
        switch (node.getType()) {
            case "NUMBER":
                return Integer.parseInt(node.getValue());
    
            case "TEXT":
                return node.getValue();
    
            case "+":
            case "-":
            case "*":
            case "/":
                // Binary operation
                Object left = evaluateASTNode(node.getChildren().get(0));
                Object right = evaluateASTNode(node.getChildren().get(1));
                return evaluateBinaryOperation(left, node.getType(), right);
    
            case "IDENTIFIER":
                // Retrieve the value of the identifier from the symbol table
                SymbolDetails details = symbolTable.getIdentifier(node.getValue());
                if (details == null) {
                    throw new RuntimeException("Undefined variable: " + node.getValue());
                }
                return details.getValue(); // Return the value of the identifier
    
            default:
                throw new RuntimeException("Unsupported AST node type: " + node.getType());
        }
    }
    
}
