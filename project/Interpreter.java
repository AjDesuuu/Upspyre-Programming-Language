package project;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import project.utils.parser.ASTNode;
import project.utils.parser.ParseTreeNode; 

public class Interpreter {
    private SymbolTable symbolTable;

    private final Map<String, ASTNode> functions = new HashMap<>();
    private final Scanner scanner = new Scanner(System.in);

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
        // Handle nulls
        if (left == null || right == null) {
            throw new RuntimeException("Null operand in binary operation: " + left + " " + operator + " " + right);
        }
    
        // Promote integers to doubles if necessary
        if (left instanceof Integer && right instanceof Double) {
            left = ((Integer) left).doubleValue();
        }
        if (left instanceof Double && right instanceof Integer) {
            right = ((Integer) right).doubleValue();
        }
    
        // Handle PLUS (addition or string concatenation)
        if (operator.equals("PLUS") || operator.equals("+")) {
            if (left instanceof String || right instanceof String) {
                return String.valueOf(left) + String.valueOf(right);
            }
            if (left instanceof Double && right instanceof Double) {
                return (Double) left + (Double) right;
            }
            if (left instanceof Integer && right instanceof Integer) {
                return (Integer) left + (Integer) right;
            }
        }
    
        // Handle MINUS
        if (operator.equals("MINUS") || operator.equals("-")) {
            if (left instanceof Double && right instanceof Double) {
                return (Double) left - (Double) right;
            }
            if (left instanceof Integer && right instanceof Integer) {
                return (Integer) left - (Integer) right;
            }
            if (left instanceof Double && right instanceof Integer) {
                return (Double) left - (Integer) right;
            }
            if (left instanceof Integer && right instanceof Double) {
                return (Integer) left - (Double) right;
            }
        }
    
        // Handle MULTIPLICATION
        if (operator.equals("MULT") || operator.equals("*")) {
            if (left instanceof Double && right instanceof Double) {
                return (Double) left * (Double) right;
            }
            if (left instanceof Integer && right instanceof Integer) {
                return (Integer) left * (Integer) right;
            }
            if (left instanceof Double && right instanceof Integer) {
                return (Double) left * (Integer) right;
            }
            if (left instanceof Integer && right instanceof Double) {
                return (Integer) left * (Double) right;
            }
        }
    
        // Handle DIVISION
        if (operator.equals("DIV") || operator.equals("/")) {
            if (right instanceof Integer && ((Integer) right) == 0 ||
                right instanceof Double && ((Double) right) == 0.0) {
                throw new RuntimeException("Division by zero.");
            }
            // Always return a double for division
            return ((Number) left).doubleValue() / ((Number) right).doubleValue();
        }
    
        // Handle EXPONENT
        if (operator.equals("EXPONENT") || operator.equals("**")) {
            return Math.pow(((Number) left).doubleValue(), ((Number) right).doubleValue());
        }

        if (operator.equals("MOD") || operator.equals("%")) {
            if (right instanceof Integer && ((Integer) right) == 0) {
                throw new RuntimeException("Modulo by zero.");
            }
            if (left instanceof Integer && right instanceof Integer) {
                return (Integer) left % (Integer) right;
            }
            if (left instanceof Double && right instanceof Double) {
                return (Double) left % (Double) right;
            }
            if (left instanceof Integer && right instanceof Double) {
                return ((Integer) left) % (Double) right;
            }
            if (left instanceof Double && right instanceof Integer) {
                return (Double) left % (Integer) right;
            }
            throw new RuntimeException("Type mismatch for MOD: " + left.getClass() + " and " + right.getClass());
        }

        if (operator.equals("BITWISE_AND") || operator.equals("&")) {
            if (left instanceof Integer && right instanceof Integer) {
                return (Integer) left & (Integer) right;
            }
            throw new RuntimeException("Bitwise AND requires integer operands.");
        }
        if (operator.equals("BITWISE_OR") || operator.equals("|")) {
            if (left instanceof Integer && right instanceof Integer) {
                return (Integer) left | (Integer) right;
            }
            throw new RuntimeException("Bitwise OR requires integer operands.");
        }
        if (operator.equals("BITWISE_XOR") || operator.equals("^")) {
            if (left instanceof Integer && right instanceof Integer) {
                return (Integer) left ^ (Integer) right;
            }
            throw new RuntimeException("Bitwise XOR requires integer operands.");
        }
        if (operator.equals("LSHIFT") || operator.equals("<<")) {
            if (left instanceof Integer && right instanceof Integer) {
                return (Integer) left << (Integer) right;
            }
            throw new RuntimeException("Left shift requires integer operands.");
        }
        if (operator.equals("RSHIFT") || operator.equals(">>")) {
            if (left instanceof Integer && right instanceof Integer) {
                return (Integer) left >> (Integer) right;
            }
            throw new RuntimeException("Right shift requires integer operands.");
        }
        if (operator.equals("AND")) {
            if (left instanceof Boolean && right instanceof Boolean) {
                return (Boolean) left && (Boolean) right;
            }
            throw new RuntimeException("AND requires boolean operands.");
        }
        if (operator.equals("OR")) {
            if (left instanceof Boolean && right instanceof Boolean) {
                return (Boolean) left || (Boolean) right;
            }
            throw new RuntimeException("OR requires boolean operands.");
        }
        
    
        throw new RuntimeException("Unsupported operator or type mismatch: " + operator + " (" + left.getClass() + ", " + right.getClass() + ")");
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
                    if (type == TokenType.DECIMAL && value instanceof Integer) {
                        value = ((Integer) value).doubleValue(); // Convert Integer to Double
                    }
                    if (type == TokenType.NUMBER && value instanceof Double) {
                        value = ((Double) value).intValue(); // Cast Double to Integer
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
                    } else if (child.getType().equals("LIST_VALUE") ||  child.getType().equals("COLLECTION_METHOD")) {
                        Object result = evaluateASTNode(child);
                        System.out.println("Output: " + result);
                    } else if (child.getType().equals("PLUS")) {
                        Object leftVal = evaluateASTNode(child.getChildren().get(0));
                        Object rightVal = evaluateASTNode(child.getChildren().get(1));
                        if (leftVal instanceof Integer && rightVal instanceof Integer) {
                            System.out.println("Output: " + ((Integer) leftVal + (Integer) rightVal));
                        } else {
                            // If either is not an integer, treat as string concat
                            System.out.println("Output: " + leftVal.toString() + rightVal.toString());
                        }
                    }
                }
                break;
            case "BLOCK_STMT":
                for (ASTNode child : node.getChildren()) {
                    executeASTNode(child);
                }
                break;
            case "BLOCK_STMT_KLEENE":
                for (ASTNode child : node.getChildren()) {
                    executeASTNode(child);
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
            
            case "REPEAT_UNTIL":
                ASTNode repeatBlock = null;
                condition = null;
            
                for (ASTNode child : node.getChildren()) {
                    if ("BLOCK_STMT".equals(child.getType())) {
                        repeatBlock = child;
                    } else if (child.getType().matches("GT|LT|LEQ|GEQ|EQ|NEQ")) {
                        condition = child;
                    }
                }
            
                if (repeatBlock == null || condition == null) {
                    throw new RuntimeException("REPEAT_UNTIL missing block or condition");
                }
            
                while (true) {
                    try {
                        executeASTNode(repeatBlock);
                    } catch (BreakException be) {
                        break;
                    } catch (ContinueException ce) {
                        // skip to condition check
                    }
            
                    Object condVal = evaluateASTNode(condition);
                    if (!(condVal instanceof Boolean)) {
                        throw new RuntimeException("REPEAT_UNTIL condition must evaluate to boolean.");
                    }
                    if ((Boolean) condVal) {
                        break;
                    }
                }
                break;

            case "REPEAT_LOOP":
                ASTNode repeatCondition = null;
                repeatBlock = null;
            
                // Extract the condition and block from the REPEAT_LOOP node
                for (ASTNode child : node.getChildren()) {
                    switch (child.getType()) {
                        case "LT":
                        case "GT":
                        case "LEQ":
                        case "GEQ":
                        case "EQ":
                        case "NEQ":
                            repeatCondition = child;
                            break;
                        case "BLOCK_STMT":
                            repeatBlock = child;
                            break;
                    }
                }
            
                if (repeatCondition == null || repeatBlock == null) {
                    throw new RuntimeException("REPEAT_LOOP missing condition or block.");
                }
            
                // Execute the loop
                while (true) {
                    Object conditionValue = evaluateASTNode(repeatCondition);
                    if (!(conditionValue instanceof Boolean)) {
                        throw new RuntimeException("REPEAT_LOOP condition must evaluate to a boolean.");
                    }
            
                    if (!(Boolean) conditionValue) {
                        break; // Exit the loop if the condition is false
                    }
            
                    try {
                        executeASTNode(repeatBlock);
                    } catch (BreakException be) {
                        break; // Handle "stop" statement
                    } catch (ContinueException ce) {
                        // Skip to the next iteration
                        continue;
                    }
                }
                break;
            

            case "FUNC_DECL":
                String functionName = null;
                ASTNode functionBody = null;
            
                for (ASTNode child : node.getChildren()) {
                    if (child.getType().equals("IDENTIFIER")) {
                        functionName = child.getValue();
                    } else if (child.getType().equals("BLOCK_STMT")) {
                        functionBody = child;
                    }
                }
            
                if (functionName != null && functionBody != null) {
                    functions.put(functionName, node); // Store the entire function node
                    System.out.println("Function declared: " + functionName);
                } else {
                    throw new RuntimeException("Invalid function declaration.");
                }
                break;
            
            case "PAIR_MAP_DECL":
                String mapName = null;
                Map<Object, Object> map = new HashMap<>();
                TokenType keyType = null;
                TokenType valueType = null;

                for (ASTNode child : node.getChildren()) {
                    switch (child.getType()) {
                        case "TEXT_TYPE":
                            if (keyType == null) {
                                keyType = TokenType.TEXT;
                            } else {
                                valueType = TokenType.TEXT;
                            }
                            break;
                        case "NUMBER_TYPE":
                            if (keyType == null) {
                                keyType = TokenType.NUMBER;
                            } else {
                                valueType = TokenType.NUMBER;
                            }
                            break;
                        case "IDENTIFIER":
                            mapName = child.getValue();
                            break;
                        case "PAIR_MAP_VAL":
                            // Recursively collect all PAIR nodes
                            List<ASTNode> pairNodes = new ArrayList<>();
                            collectPairNodes(child, pairNodes);
                            for (ASTNode pairNode : pairNodes) {
                                List<ASTNode> pairChildren = pairNode.getChildren();
                                ASTNode keyNode = null;
                                ASTNode valueNode = null;
                                for (ASTNode pc : pairChildren) {
                                    String t = pc.getType();
                                    if (t.equals("TEXT") || t.equals("NUMBER") || t.equals("IDENTIFIER")) {
                                        if (keyNode == null) keyNode = pc;
                                        else valueNode = pc;
                                    }
                                }
                                if (keyNode != null && valueNode != null) {
                                    Object key = evaluateASTNode(keyNode);
                                    value = evaluateASTNode(valueNode);
                                    map.put(key, value);
                                }
                            }
                            break;
                        default:
                            // Skip grammar tokens like LPAREN, RPAREN, COLON, etc.
                            break;
                    }
                }

                if (mapName != null && keyType != null && valueType != null) {
                    symbolTable.addIdentifier(mapName, TokenType.PAIR_MAP_TYPE, map);
                    System.out.println("Assigned map " + mapName + " = " + map);
                } else {
                    throw new RuntimeException("Invalid map declaration.");
                }
                break;

            case "RETURN_STMT":
                Object returnValue = null;
                for (ASTNode child : node.getChildren()) {
                    returnValue = evaluateASTNode(child);
                }
                throw new ReturnException(returnValue); 

            case "INPUT_STMT":
                // Example CST: get text numberText();
                String inputVarName = null;
                TokenType inputType = null;
                for (ASTNode child : node.getChildren()) {
                    switch (child.getType()) {
                        case "TEXT_TYPE":
                            inputType = TokenType.TEXT;
                            break;
                        case "NUMBER_TYPE":
                            inputType = TokenType.NUMBER;
                            break;
                        case "IDENTIFIER":
                            inputVarName = child.getValue();
                            break;
                    }
                }
                if (inputVarName != null && inputType != null) {
                    System.out.print("> "); // Prompt
                    String userInput = scanner.nextLine();
                    value = userInput;
                    if (inputType == TokenType.NUMBER) {
                        try {
                            value = Integer.parseInt(userInput);
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid number input, storing as text.");
                        }
                    }
                    symbolTable.addIdentifier(inputVarName, inputType, value);
                }
                break;
            
            case "CHOOSE_WHAT_STMT":
                // Evaluate the condition (e.g., the value inside "choose_what (3)")
                conditionNode = node.getChildren().get(2); // NUMBER (3)
                Object conditionValue = evaluateASTNode(conditionNode);
            
                // Recursively collect all PICK_CASE nodes
                List<ASTNode> pickCases = new ArrayList<>();
                collectPickCases(node, pickCases);
            
                boolean caseMatched = false;
                for (ASTNode pickCase : pickCases) {
                    ASTNode pickConditionNode = pickCase.getChildren().get(1); // NUMBER (1), NUMBER (3), etc.
                    Object pickConditionValue = evaluateASTNode(pickConditionNode);
            
                    if (conditionValue.equals(pickConditionValue)) {
                        ASTNode blockStmt = pickCase.getChildren().get(3); // BLOCK_STMT
                        executeASTNode(blockStmt);
                        caseMatched = true;
                        break;
                    }
                }
            
                if (!caseMatched) {
                    System.out.println("No matching case found for choose_what condition: " + conditionValue);
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
    private void collectPairNodes(ASTNode node, List<ASTNode> pairs) {
        if (node.getType().equals("PAIR")) {
            pairs.add(node);
        } else {
            for (ASTNode child : node.getChildren()) {
                collectPairNodes(child, pairs);
            }
        }
    }
    private void collectPickCases(ASTNode node, List<ASTNode> pickCases) {
        if (node.getType().equals("PICK_CASE")) {
            pickCases.add(node);
        } else {
            for (ASTNode child : node.getChildren()) {
                collectPickCases(child, pickCases);
            }
        }
    }

    private List<ASTNode> getParamIdentifiers(ASTNode paramList) {
        List<ASTNode> params = new ArrayList<>();
        
        for (ASTNode child : paramList.getChildren()) {
            if (child.getType().equals("IDENTIFIER")) {
                params.add(child);
            } else if (child.getType().equals("PARAM_LIST_GROUP")) {
                params.addAll(getParamIdentifiers(child));
            }
        }
        
        return params;
    }

    private List<ASTNode> getArgNodes(ASTNode argList) {
        List<ASTNode> args = new ArrayList<>();
        
        for (ASTNode child : argList.getChildren()) {
            if (child.getType().equals("NUMBER") || 
                child.getType().equals("TEXT") || 
                child.getType().equals("IDENTIFIER")) {
                args.add(child);
            } else if (child.getType().equals("ARG_LIST_GROUP")) {
                args.addAll(getArgNodes(child));
            }
        }
        
        return args;
    }
    
    private Object evaluateASTNode(ASTNode node) {
        switch (node.getType()) {
            case "NUMBER":
                // Check if the parent node is expecting a DECIMAL_TYPE
                ASTNode parent = node.getParent(); // Assuming you have a way to get the parent node
                if (parent != null && parent.getType().equals("DECIMAL_TYPE")) {
                    return Double.parseDouble(node.getValue());
                }
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
            case "MULT":
            case "DIV":
                // Binary operation
                left = evaluateASTNode(node.getChildren().get(0));
                right = evaluateASTNode(node.getChildren().get(1));
                return evaluateBinaryOperation(left, node.getType(), right);
            case "EXPONENT":
                Object leftExp = evaluateASTNode(node.getChildren().get(0));
                Object rightExp = evaluateASTNode(node.getChildren().get(1));
                return evaluateBinaryOperation(leftExp, "EXPONENT", rightExp);
            case "DECIMAL":
                return Double.parseDouble(node.getValue());
            case "TRUE":
                return true;
            case "FALSE":
                return false;
            
            case "BITWISE_AND":
                left = evaluateASTNode(node.getChildren().get(0));
                right = evaluateASTNode(node.getChildren().get(1));
                return evaluateBinaryOperation(left, "BITWISE_AND", right);
            
            case "BITWISE_OR":
                left = evaluateASTNode(node.getChildren().get(0));
                right = evaluateASTNode(node.getChildren().get(1));
                return evaluateBinaryOperation(left, "BITWISE_OR", right);
            
            case "BITWISE_XOR":
                left = evaluateASTNode(node.getChildren().get(0));
                right = evaluateASTNode(node.getChildren().get(1));
                return evaluateBinaryOperation(left, "BITWISE_XOR", right);
            
            case "BITNOT_EXPR":
                // BITWISE_NOT is a unary operator
                Object operand = evaluateASTNode(node.getChildren().get(1));
                return ~((Integer) operand);
            
            case "LSHIFT":
                left = evaluateASTNode(node.getChildren().get(0));
                right = evaluateASTNode(node.getChildren().get(1));
                return evaluateBinaryOperation(left, "LSHIFT", right);
            
            case "RSHIFT":
                left = evaluateASTNode(node.getChildren().get(0));
                right = evaluateASTNode(node.getChildren().get(1));
                return evaluateBinaryOperation(left, "RSHIFT", right);

            case "AND":
                Object leftAnd = evaluateASTNode(node.getChildren().get(0));
                Object rightAnd = evaluateASTNode(node.getChildren().get(1));
                return evaluateBinaryOperation(leftAnd, "AND", rightAnd);
            
            case "OR":
                Object leftOr = evaluateASTNode(node.getChildren().get(0));
                Object rightOr = evaluateASTNode(node.getChildren().get(1));
                return evaluateBinaryOperation(leftOr, "OR", rightOr);
            
            case "LOGICNOT_EXPR":
                // LOGICNOT_EXPR: NOT <expr>
                operand = evaluateASTNode(node.getChildren().get(1));
                if (!(operand instanceof Boolean)) {
                    throw new RuntimeException("NOT operand must be boolean.");
                }
                return !(Boolean) operand;
            case "IDENTIFIER":
                // Retrieve the value of the identifier from the symbol table
                SymbolDetails details = symbolTable.getIdentifier(node.getValue());
                if (details == null) {
                    throw new RuntimeException("Undefined variable: " + node.getValue());
                }
                return details.getValue(); // Return the value of the identifier
    
            case "LIST_VALUE":
                String collectionName = node.getChildren().get(0).getValue(); // e.g., fruits or studentAges
                Object indexOrKey = evaluateASTNode(node.getChildren().get(2)); // e.g., 1 or "Mark"
            
                // Retrieve the collection from the symbol table
                SymbolDetails collectionDetails = symbolTable.getIdentifier(collectionName);
                if (collectionDetails == null) {
                    throw new RuntimeException("Undefined variable: " + collectionName);
                }
            
                Object collection = collectionDetails.getValue();
            
                // Handle list access
                if (collection instanceof List) {
                    if (!(indexOrKey instanceof Integer)) {
                        throw new RuntimeException("List index must be an integer.");
                    }
                    List<?> list = (List<?>) collection;
                    int index = (Integer) indexOrKey;
            
                    if (index < 0 || index >= list.size()) {
                        throw new RuntimeException("List index out of bounds: " + index);
                    }
                    return list.get(index);
                }
            
                // Handle map access
                if (collection instanceof Map) {
                    if (!(indexOrKey instanceof String)) {
                        throw new RuntimeException("Map key must be a string.");
                    }
                    Map<?, ?> map = (Map<?, ?>) collection;
                    if (!map.containsKey(indexOrKey)) {
                        throw new RuntimeException("Map key not found: " + indexOrKey);
                    }
                    return map.get(indexOrKey);
                }
            
                throw new RuntimeException("Variable is neither a list nor a map: " + collectionName);

            case "COLLECTION_METHOD":
                String methodTarget = node.getChildren().get(0).getValue(); // e.g., i
                String methodName = node.getChildren().get(2).getType();    // TO_TEXT, etc.
    
                // Fetch the actual variable value from the symbol table
                SymbolDetails targetDetails = symbolTable.getIdentifier(methodTarget);
                if (targetDetails == null) {
                    throw new RuntimeException("Undefined variable: " + methodTarget);
                }
                Object targetValue = targetDetails.getValue();
    
                // Handle the TO_TEXT method
                if ("TO_TEXT".equals(methodName)) {
                    return String.valueOf(targetValue); // Convert the value to a string
                }
    
                throw new RuntimeException("Unknown method: " + methodName);
            
            case "CONV_EXPR":
                // Structure: IDENTIFIER (source), DOT, CONVERT_TO, LPAREN, NUMBER_TYPE/TEXT_TYPE, RPAREN
                String sourceVar = null;
                TokenType targetType = null;
                // Find the source variable and target type
                for (ASTNode child : node.getChildren()) {
                    if (child.getType().equals("IDENTIFIER")) {
                        sourceVar = child.getValue();
                    } else if (child.getType().equals("NUMBER_TYPE")) {
                        targetType = TokenType.NUMBER;
                    } else if (child.getType().equals("TEXT_TYPE")) {
                        targetType = TokenType.TEXT;
                    }
                }
                if (sourceVar == null || targetType == null) {
                    throw new RuntimeException("Invalid conversion expression.");
                }
                SymbolDetails srcDetails = symbolTable.getIdentifier(sourceVar);
                if (srcDetails == null) {
                    throw new RuntimeException("Undefined variable: " + sourceVar);
                }
                Object srcValue = srcDetails.getValue();
                // Perform conversion
                if (targetType == TokenType.NUMBER) {
                    try {
                        return Integer.parseInt(srcValue.toString());
                    } catch (NumberFormatException e) {
                        throw new RuntimeException("Cannot convert value to number: " + srcValue);
                    }
                } else if (targetType == TokenType.TEXT) {
                    return srcValue.toString();
                } else {
                    throw new RuntimeException("Unsupported conversion type.");
                }

            case "MOD":
                left = evaluateASTNode(node.getChildren().get(0));
                right = evaluateASTNode(node.getChildren().get(1));
                return evaluateBinaryOperation(left, "MOD", right);

            case "FUNC_CALL":
                String calledFunctionName = node.getChildren().get(0).getValue();
                ASTNode argList = node.getChildren().get(2);
            
                ASTNode functionNode = functions.get(calledFunctionName);
                if (functionNode == null) {
                    throw new RuntimeException("Undefined function: " + calledFunctionName);
                }
            
                ASTNode paramList = functionNode.getChildren().get(3);
            
                List<ASTNode> params = getParamIdentifiers(paramList);
                List<ASTNode> args = getArgNodes(argList);

                if (params.size() != args.size()) {
                    throw new RuntimeException("Argument count mismatch for function: " + calledFunctionName);
                }
            
                Map<String, Object> tempSymbolTable = new HashMap<>();
                for (int i = 0; i < params.size(); i++) {
                    String paramName = params.get(i).getValue();
                    Object argValue = evaluateASTNode(args.get(i));
                    tempSymbolTable.put(paramName, argValue);
                }
            
                SymbolTable newSymbolTable = new SymbolTable();
                for (Map.Entry<String, Object> entry : tempSymbolTable.entrySet()) {
                    String name = entry.getKey();
                    Object value = entry.getValue();
                    TokenType type = inferType(value);
                    newSymbolTable.addIdentifier(name, type, value);
                }
            
                SymbolTable oldSymbolTable = symbolTable;
                symbolTable = newSymbolTable;
            
                Object returnValue = null;
                try {
                    for (ASTNode child : functionNode.getChildren()) {
                        if (child.getType().equals("BLOCK_STMT")) {
                            try {
                                executeASTNode(child);
                            } catch (ReturnException re) {
                                returnValue = re.value;
                                break;
                            }
                        }
                    }
                } finally {
                    symbolTable = oldSymbolTable;
                }
            
                return returnValue;
            
            case "OUTPUT":
                // If OUTPUT is used as an expression (e.g., in a function), return the value
                if (!node.getChildren().isEmpty()) {
                    Object outputValue = evaluateASTNode(node.getChildren().get(0));
                    throw new ReturnException(outputValue);
                }
                return null;

            default:
                throw new RuntimeException("Unsupported AST node type: " + node.getType());
        }
    }
    
}


