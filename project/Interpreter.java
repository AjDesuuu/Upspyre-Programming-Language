package project;


import java.util.ArrayList;
import java.util.List;

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
                case "PLUS" -> l + r;
                case "MINUS" -> l - r;
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
        //System.out.println("[DEBUG] Processing " + node.getType() + 
        //              ", children: " + node.getChildren().size());
        for (ASTNode child : node.getChildren()) {
            System.out.println("  - " + child.getType() + 
                                (child.getValue() != null ? " (" + child.getValue() + ")" : ""));
        }
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
                String variable = null;
                Object value = null;
                TokenType type = null;
            
                for (ASTNode child : node.getChildren()) {
                    switch (child.getType()) {
                        case "TEXT_TYPE":
                            type = TokenType.TEXT;
                            break;
                        case "NUMBER_TYPE":
                            type = TokenType.NUMBER;
                            break;
                        case "DECIMAL_TYPE":
                            type = TokenType.DECIMAL;
                            break;
                        case "BINARY_TYPE":
                            type = TokenType.BINARY_TYPE;
                            break;
                        case "IDENTIFIER":
                            variable = child.getValue();
                            break;
                        case "TEXT":
                        case "NUMBER":
                        case "DECIMAL":
                        case "TRUE":
                        case "FALSE":
                            value = evaluateASTNode(child);
                            break;
                        case "ASSIGN":
                            // Skip the "=" operator
                            break;
                        default:
                            // Handle complex expressions (e.g., "numberText = 5 + 3")
                            value = evaluateASTNode(child);
                            break;
                    }
                }
            
                if (variable != null && value != null) {
                    if (type == null) {
                        type = inferType(value);  // Infer type if not declared
                    }
                    symbolTable.addIdentifier(variable, type, value);  // Use the REAL value
                    System.out.println("Assigned " + variable + " = " + value);
                }
                break;
    
            case "OUTPUT":
                for (ASTNode child : node.getChildren()) {
                    if (child.getType().equals("IDENTIFIER")) {
                        String varName = child.getValue();  // Get actual variable name
                        SymbolDetails details = symbolTable.getIdentifier(varName);
                        if (details != null) {
                            System.out.println("Output: " + details.getValue());
                        } else {
                            System.out.println("Error: Undefined variable " + varName);
                        }
                    } else if (child.getType().equals("TEXT")) {
                        // Handle direct text output
                        System.out.println("Output: " + child.getValue());
                    } else if (child.getType().equals("LIST_VALUE")) {
                        Object result = evaluateASTNode(child);
                        System.out.println("Output: " + result);
                    }
                }
                break;
            
            case "CONDITIONAL_STMT":
                ASTNode conditionNode = null;
                ASTNode ifBlock = null;
                ASTNode otherwiseBlock = null;
            
                for (ASTNode child : node.getChildren()) {
                    switch (child.getType()) {
                        case "GT":
                        case "LT":
                        case "GTE":
                        case "LTE":
                        case "GEQ":
                        case "LEQ":
                        case "EQ":
                        case "NEQ":
                        case "RELATIONAL_EXPR":
                            conditionNode = child;
                            break;
                        case "BLOCK_STMT":
                            if (ifBlock == null) {
                                ifBlock = child;
                            }
                            break;
                        case "CONDITIONAL_STMT_GROUP":
                            // Look for OTHERWISE inside
                            for (ASTNode groupChild : child.getChildren()) {
                                if (groupChild.getType().equals("BLOCK_STMT")) {
                                    otherwiseBlock = groupChild;
                                }
                            }
                            break;
                    }
                }
            
                if (conditionNode == null) {
                    throw new RuntimeException("Error: Missing or invalid condition in IF statement.");
                }
            
                boolean conditionResult = (boolean) evaluateASTNode(conditionNode);
            
                if (conditionResult) {
                    executeASTNode(ifBlock);
                } else if (otherwiseBlock != null) {
                    executeASTNode(otherwiseBlock);
                }
                break;
            case "LIST_DECL":
                String listName = null;
                List<Object> elements = new ArrayList<>();
                TokenType listType = null;

                for (ASTNode child : node.getChildren()) {
                    switch (child.getType()) {
                        case "TEXT_TYPE":
                            listType = TokenType.TEXT;
                            break;
                        case "NUMBER_TYPE":
                            listType = TokenType.NUMBER;
                            break;
                        case "IDENTIFIER":
                            listName = child.getValue();
                            break;
                        case "TEXT":
                        case "NUMBER":
                            elements.add(evaluateASTNode(child));
                            break;
                        case "LIST_DECL_GROUP":
                            collectListElements(child, elements);  // Helper defined below
                            break;
                    }
                }

                if (listName != null && listType != null) {
                    symbolTable.addIdentifier(listName, listType, elements);
                    System.out.println("Assigned list " + listName + " = " + elements);
                }
                break;

            case "STOP":
                throw new BreakException();
            
            case "CONTINUE":
                throw new ContinueException();
            
            case "FOR_LOOP":
                ASTNode init = null;
                ASTNode condition = null;
                ASTNode increment = null;
                ASTNode body = null;
            
                for (ASTNode child : node.getChildren()) {
                    switch (child.getType()) {
                        case "ASSIGNMENT_STMT":
                            if (init == null) init = child;
                            else increment = child;
                            break;
                        case "LEQ":
                        case "LT":
                        case "GT":
                        case "GEQ":
                        case "EQ":
                        case "NEQ":
                            condition = child;
                            break;
                        case "BLOCK_STMT":
                            body = child;
                            break;
                    }
                }
            
                if (init == null || condition == null || increment == null || body == null) {
                    throw new RuntimeException("Incomplete FOR loop structure.");
                }
            
                executeASTNode(init);  // Run initialization once
            
                while (true) {
                    Object cond = evaluateASTNode(condition);
                    if (!(cond instanceof Boolean)) {
                        throw new RuntimeException("For-loop condition did not evaluate to a boolean.");
                    }
                    if (!(Boolean) cond) break;
            
                    try {
                        executeASTNode(body);
                    } catch (BreakException be) {
                        break;
                    } catch (ContinueException ce) {
                        // Skip increment, go straight to next iteration
                        executeASTNode(increment);
                        continue;
                    }
            
                    executeASTNode(increment);
                }
                break;
            default:
                // Process other nodes
                for (ASTNode child : node.getChildren()) {
                    executeASTNode(child);
                }
        }
    }
    private void collectListElements(ASTNode groupNode, List<Object> elements) {
        for (ASTNode child : groupNode.getChildren()) {
            switch (child.getType()) {
                case "TEXT":
                case "NUMBER":
                    elements.add(evaluateASTNode(child));
                    break;
                case "LIST_DECL_GROUP":
                    collectListElements(child, elements);  // Recursive
                    break;
            }
        }
    }
    private Object evaluateASTNode(ASTNode node) {
        switch (node.getType()) {
            case "NUMBER":
                return Integer.parseInt(node.getValue());
    
            case "TEXT":
                return node.getValue();
            case "GT": // Greater Than
            case "LT": // Less Than
            case "GTE": // Greater Than or Equal
            case "LTE": // Less Than or Equal
            case "EQ":
            case "GEQ":
            case "LEQ": // Equal
            case "NEQ": // Not Equal
                Object left = evaluateASTNode(node.getChildren().get(0));
                Object right = evaluateASTNode(node.getChildren().get(1));

                if (left instanceof Integer && right instanceof Integer) {
                    int l = (Integer) left;
                    int r = (Integer) right;
                    return switch (node.getType()) {
                        case "GT" -> l > r;
                        case "LT" -> l < r;
                        case "GTE", "GEQ" -> l >= r;
                        case "LTE", "LEQ" -> l <= r;
                        case "EQ" -> l == r;
                        case "NEQ" -> l != r;
                        default -> throw new RuntimeException("Unknown comparison operator: " + node.getType());
                    };
                }

                throw new RuntimeException("Type mismatch in relational operation.");

                        
            case "PLUS":
            case "MINUS":
            case "*":
            case "/":
                // Binary operation
                left = evaluateASTNode(node.getChildren().get(0));
                right = evaluateASTNode(node.getChildren().get(1));
                return evaluateBinaryOperation(left, node.getType(), right);
            case "DECIMAL":
                return Double.parseDouble(node.getValue());
            case "TRUE":
                return true;
            case "FALSE":
                return false;
            case "IDENTIFIER":
                // Retrieve the value of the identifier from the symbol table
                SymbolDetails details = symbolTable.getIdentifier(node.getValue());
                if (details == null) {
                    throw new RuntimeException("Undefined variable: " + node.getValue());
                }
                return details.getValue(); // Return the value of the identifier
    
            case "LIST_VALUE":
                String listVar = node.getChildren().get(0).getValue();  // colors
                Object indexVal = evaluateASTNode(node.getChildren().get(2)); // i
            
                if (!(indexVal instanceof Integer)) {
                    throw new RuntimeException("List index must be an integer.");
                }
            
                SymbolDetails listDetails = symbolTable.getIdentifier(listVar);
                if (listDetails == null || !(listDetails.getValue() instanceof List)) {
                    throw new RuntimeException("Undefined or invalid list: " + listVar);
                }
            
                List<?> list = (List<?>) listDetails.getValue();
                int index = (Integer) indexVal;
            
                if (index < 0 || index >= list.size()) {
                    throw new RuntimeException("List index out of bounds: " + index);
                }
            
                return list.get(index);
            default:
                throw new RuntimeException("Unsupported AST node type: " + node.getType());
        }
    }
    
}


