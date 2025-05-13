package project.interpreterComponents;

import project.interpreterComponents.utils.InterpreterException;
import project.interpreterComponents.utils.SymbolTableManager;
import project.interpreterComponents.utils.TypeChecker;
import project.TokenType;
import project.SymbolDetails;
import project.utils.parser.ASTNode;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class Evaluator {
    public final SymbolTableManager symbolTableManager;
    public final TypeChecker typeChecker;
    private final boolean debugMode;
    private Executor executor;

    public Evaluator(SymbolTableManager symbolTableManager, boolean debugMode) {
        this.symbolTableManager = symbolTableManager;
        this.typeChecker = new TypeChecker();
        this.debugMode = debugMode;
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public Object evaluateASTNode(ASTNode node) {
        if (node == null) {
            throw new InterpreterException("Cannot evaluate null node", 0);
        }

        if (debugMode) {
            System.out.println("[DEBUG] Evaluating node: " + node.getType() +
                (node.getValue() != null ? " (" + node.getValue() + ")" : "") +
                ", children: " + node.getChildren().size());
        }

        switch (node.getType()) {
            case "NUMBER":
                // Check if the parent node is expecting a DECIMAL_TYPE
                ASTNode parent = node.getParent();
                if (parent != null && parent.getType().equals("DECIMAL_TYPE")) {
                    return Double.parseDouble(node.getValue());
                }
                return Integer.parseInt(node.getValue());
    
            case "TEXT":
                return node.getValue();
            case "NONE":
                return null;
            case "GT": case "LT": case "GTE": case "LTE": 
            case "EQ": case "GEQ": case "LEQ": case "NEQ":
                return evaluateRelationalExpr(node);

            case "PLUS": case "MINUS": case "MULT": case "DIV":
            case "EXPONENT": case "MOD":
                if (node.getType().equals("MINUS") && node.getChildren().size() == 1) {
                    // Handle unary minus
                    Object operand = evaluateASTNode(node.getChildren().get(0));
                    if (operand instanceof Integer) {
                        return -((Integer) operand);
                    } else if (operand instanceof Double) {
                        return -((Double) operand);
                    } else {
                        throw new InterpreterException("Unary minus requires a numeric operand", getNodeLineNumber(node));
                    }
                } else {
                    Object left = evaluateASTNode(node.getChildren().get(0));
                    Object right = evaluateASTNode(node.getChildren().get(1));
                    return evaluateBinaryOperation(left, node.getType(), right, node);
                }
            case "DECIMAL":
                return Double.parseDouble(node.getValue());
            
            case "TRUE":
                return true;
            case "FALSE":
                return false;
            
            case "BITWISE_AND": case "BITWISE_OR": case "BITWISE_XOR":
            case "LSHIFT": case "RSHIFT":
                Object left = evaluateASTNode(node.getChildren().get(0));
                Object right = evaluateASTNode(node.getChildren().get(1));
                return evaluateBinaryOperation(left, node.getType(), right, node);
            
            case "BITNOT_EXPR":
                Object operand = evaluateASTNode(node.getChildren().get(1));
                typeChecker.checkType(operand, Integer.class, "BITNOT_EXPR operand must be an integer");
                return ~((Integer) operand);

            case "AND": case "OR":
                Object leftLogic = evaluateASTNode(node.getChildren().get(0));
                Object rightLogic = evaluateASTNode(node.getChildren().get(1));
                return evaluateBinaryOperation(leftLogic, node.getType(), rightLogic, node);
            
            case "LOGICNOT_EXPR":
                operand = evaluateASTNode(node.getChildren().get(1));
                typeChecker.checkType(operand, Boolean.class, "NOT operand must be boolean");
                return !(Boolean) operand;

            case "IDENTIFIER":
                String varName = node.getValue();
                SymbolDetails details = symbolTableManager.getIdentifier(varName);
                if (details == null) {
                    throw new InterpreterException("Undefined variable: " + varName, getNodeLineNumber(node));
                }
                if (!details.isExplicitlyDeclared()) {
                    throw new InterpreterException("Variable '" + varName + "' used before declaration", getNodeLineNumber(node));
                }
                if (details.getValue() == null) {
                    throw new InterpreterException("Variable '" + varName + "' is uninitialized", getNodeLineNumber(node));
                }
                symbolTableManager.getCurrentSymbolTable().markVariableAsUsed(varName);
                return details.getValue();

            case "FUNC_CALL":
                return executor.evaluateFunctionCall(node);
            case "LIST_VALUE":
            case "PAIR_MAP_VALUE":
            case "PAIR_MAP_KEY":
                return evaluateListValue(node);

            case "COLLECTION_METHOD":
                return evaluateCollectionMethod(node);
            
            case "CONV_EXPR":
                return evaluateConversionExpr(node);

            
            default:
                throw new InterpreterException("Unsupported AST node type for evaluation: " + node.getType(), getNodeLineNumber(node));
        }
    }

    private Object evaluateRelationalExpr(ASTNode node) {
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
                default -> throw new InterpreterException("Unknown comparison operator: " + node.getType(), getNodeLineNumber(node));
            };
        }

        throw new InterpreterException("Type mismatch in relational operation", getNodeLineNumber(node));
    }

    private Object evaluateListValue(ASTNode node) {
        String collectionName = node.getChildren().get(0).getValue();
        Object indexOrKey;


        SymbolDetails collectionDetails = symbolTableManager.getIdentifier(collectionName);
        if (collectionDetails == null) {
            throw new InterpreterException("Undefined variable: " + collectionName, getNodeLineNumber(node));
        }

        Object collection = collectionDetails.getValue();
        TokenType collectionType = collectionDetails.getType();

        if (collectionType == TokenType.PAIR_MAP_TYPE && node.getType().equals("LIST_VALUE")) {
            throw new InterpreterException(
                "Cannot access pair_map '" + collectionName + "' using list syntax. Use '" + collectionName + ".value[key]' instead.",
                getNodeLineNumber(node)
            );
        }
        // If the collection is a list but accessed as a pair_map, throw an error
        if (collectionType == TokenType.LIST_TYPE && node.getType().equals("PAIR_MAP_VALUE")) {
            throw new InterpreterException(
                "Cannot access list '" + collectionName + "' using pair_map syntax. Use '" + collectionName + "[index]' instead.",
                getNodeLineNumber(node)
            );
        }

        // Handle list access
        if (collection instanceof List) {
            indexOrKey = evaluateASTNode(node.getChildren().get(2));;
            if (!(indexOrKey instanceof Integer)) {
                throw new InterpreterException(
                        "TypeError: Index must be a 'number', got '" + inferType(indexOrKey) + "'",
                        getNodeLineNumber(node)
                );
            }
            List<?> list = (List<?>) collection;
            int index = (Integer) indexOrKey;

            if (index < 0 || index >= list.size()) {
                throw new InterpreterException("List index out of bounds: " + index, getNodeLineNumber(node));
            }
            return list.get(index);
        } else if (collection instanceof Map) {
            indexOrKey = evaluateASTNode(node.getChildren().get(4));;
            Map<?, ?> map = (Map<?, ?>) collection;
            // Handle cases where the key is an integer
            if (node.getType().equals("PAIR_MAP_KEY")) {
                if (!(indexOrKey instanceof Integer)) {
                    throw new InterpreterException(
                            "Map key index must be an integer, got: " + (indexOrKey == null ? "null" : indexOrKey.getClass().getSimpleName()),
                            getNodeLineNumber(node)
                    );
                }

                int index = (Integer) indexOrKey;

                // Convert the map's key set to a list to access by index
                List<?> keys = new ArrayList<>(map.keySet());

                if (index < 0 || index >= keys.size()) {
                    throw new InterpreterException(
                            "Map key index out of bounds: " + index,
                            getNodeLineNumber(node)
                    );
                }

                // Return the key at the specified index
                return keys.get(index);
            }

            // Handle cases where the value is requested
            if (node.getType().equals("PAIR_MAP_VALUE")) {
                if (!map.containsKey(indexOrKey)) {
                    throw new InterpreterException("Map key not found: " + indexOrKey, getNodeLineNumber(node));
                }
                // Return the value associated with the key
                return map.get(indexOrKey);
            }

            throw new InterpreterException("Unsupported map operation for: " + node.getType(), getNodeLineNumber(node));
        }

        throw new InterpreterException("Unsupported collection type for: " + collectionName, getNodeLineNumber(node));
    }

    private Object evaluateCollectionMethod(ASTNode node) {
        String methodTarget = node.getChildren().get(0).getValue();
        String methodName = node.getChildren().get(2).getType();
    
        SymbolDetails targetDetails = symbolTableManager.getIdentifier(methodTarget);
        if (targetDetails == null) {
            throw new InterpreterException("Undefined variable: " + methodTarget, getNodeLineNumber(node));
        }
        Object targetValue = targetDetails.getValue();
    
        switch (methodName) {
            case "ADD":
                if (targetValue instanceof List<?> l) {
                    @SuppressWarnings("unchecked")
                    List<Object> list = (List<Object>) l;
                    Object value = this.evaluateASTNode(node.getChildren().get(4));
                    list.add(value);

                    return null;
                }
                throw new InterpreterException("add() only supported for lists", getNodeLineNumber(node));
            case "REMOVE":
                if (targetValue instanceof List<?> list) {
                    Object idx = this.evaluateASTNode(node.getChildren().get(4));
                    if (!(idx instanceof Integer))
                        throw new InterpreterException("remove() index must be a number", getNodeLineNumber(node));
                    return list.remove((int) idx);
                }
                if (targetValue instanceof Map<?, ?> map) {
                    Object key = this.evaluateASTNode(node.getChildren().get(4));
                    return map.remove(key);
                }
                throw new InterpreterException("remove() only supported for lists and maps", getNodeLineNumber(node));
            case "CONTAINS":
                Object arg = this.evaluateASTNode(node.getChildren().get(4));
                if (targetValue instanceof List<?> list) {
                    return list.contains(arg);
                }
                if (targetValue instanceof Map<?, ?> map) {
                    return map.containsKey(arg);
                }
                throw new InterpreterException("contains() only supported for lists and maps", getNodeLineNumber(node));
            case "CLEAR":
                if (targetValue instanceof List<?> list) {
                    list.clear();
                    return null;
                }
                if (targetValue instanceof Map<?, ?> map) {
                    map.clear();
                    return null;
                }
                throw new InterpreterException("clear() only supported for lists and maps", getNodeLineNumber(node));
            case "KEYS":
                if (targetValue instanceof Map<?, ?> map) {
                    return new ArrayList<>(map.keySet());
                }
                throw new InterpreterException("keys() only supported for maps", getNodeLineNumber(node));
            case "VALUES":
                if (targetValue instanceof Map<?, ?> map) {
                    return new ArrayList<>(map.values());
                }
                throw new InterpreterException("values() only supported for maps", getNodeLineNumber(node));
            case "LEN":
                if (targetValue instanceof List) {
                    return ((List<?>) targetValue).size();
                }
                if (targetValue instanceof Map) {
                    return ((Map<?, ?>) targetValue).size();
                }
                if(targetValue instanceof String) {
                    return ((String) targetValue).length();
                }
                throw new InterpreterException(
                    "TypeError: len() not supported for type '" +
                    (targetValue == null ? "null" : inferType(targetValue).name().toLowerCase().replace("_type", "")) + "'",
                    getNodeLineNumber(node)
                );
            case "SORT":
                if (targetValue instanceof List list) {
                    // Only sort if elements are Comparable
                    if (!list.isEmpty() && !(list.get(0) instanceof Comparable)) {
                        throw new InterpreterException("List elements are not comparable for sort()", getNodeLineNumber(node));
                    }
                    list.sort(null);
                    return null;
                }
                throw new InterpreterException("sort() only supported for lists", getNodeLineNumber(node));
            case "TO_TEXT":
                if (targetValue instanceof List) {
                    return listToText((List<?>) targetValue);
                }
                if (targetValue instanceof Map) {
                    return mapToText((Map<?, ?>) targetValue);
                }
                return String.valueOf(targetValue);
            default:
                throw new InterpreterException("Unknown collection method: " + methodName, getNodeLineNumber(node));
        }
    }
    
    // Helper for list to text
    private String listToText(List<?> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i));
            if (i < list.size() - 1) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }
    
    // Helper for map to text
    private String mapToText(Map<?, ?> map) {
        StringBuilder sb = new StringBuilder("{");
        int i = 0;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue());
            if (i < map.size() - 1) sb.append(", ");
            i++;
        }
        sb.append("}");
        return sb.toString();
    }

    private Object evaluateConversionExpr(ASTNode node) {
        String sourceVar = null;
        TokenType targetType = null;
        
        for (ASTNode child : node.getChildren()) {
            if (child.getType().equals("IDENTIFIER")) {
                sourceVar = child.getValue();
            } else if (child.getType().equals("NUMBER_TYPE")) {
                targetType = TokenType.NUMBER;
            } else if (child.getType().equals("DECIMAL_TYPE")) {
                targetType = TokenType.DECIMAL;
            }  else if (child.getType().equals("TEXT_TYPE")) {
                targetType = TokenType.TEXT;
            }
        }
        
        if (sourceVar == null || targetType == null) {
            throw new InterpreterException("Invalid conversion expression", getNodeLineNumber(node));
        }
        
        SymbolDetails srcDetails = symbolTableManager.getIdentifier(sourceVar);
        if (srcDetails == null) {
            throw new InterpreterException("Undefined variable: " + sourceVar, getNodeLineNumber(node));
        }
        
        Object srcValue = srcDetails.getValue();
        
        TokenType srcType = srcDetails.getType();
        if (srcType == targetType) {
            return srcValue; // No conversion needed
        }
        if ((srcType == TokenType.TEXT && (targetType == TokenType.NUMBER || targetType == TokenType.DECIMAL)) ||
            ((srcType == TokenType.NUMBER || srcType == TokenType.DECIMAL) && targetType == TokenType.TEXT)) {
            try {
                return typeChecker.convertIfNeeded(srcValue, targetType);
            } catch (Exception e) {
                throw new InterpreterException("Conversion error: " + e.getMessage(), getNodeLineNumber(node));
            }
        }
        throw new InterpreterException("Unsupported type conversion: " + srcType + " to " + targetType, getNodeLineNumber(node));
    }



    public Object evaluateBinaryOperation(Object left, String operator, Object right, ASTNode node) {
        int lineNumber = getNodeLineNumber(node); // In a real implementation, get actual line number

        if (left == null || right == null) {
            throw new InterpreterException("Null operand in binary operation: " + left + " " + operator + " " + right, lineNumber);
        }

        // Type conversion logic
        if (left instanceof Integer && right instanceof Double) {
            left = ((Integer) left).doubleValue();
        }
        if (left instanceof Double && right instanceof Integer) {
            right = ((Integer) right).doubleValue();
        }

        switch (operator) {
            case "PLUS", "+":
                if (left instanceof String || right instanceof String) {
                    return String.valueOf(left) + String.valueOf(right);
                }
                if (left instanceof Double && right instanceof Double) {
                    return (Double) left + (Double) right;
                }
                if (left instanceof Integer && right instanceof Integer) {
                    return (Integer) left + (Integer) right;
                }
                break;
                
            case "MINUS", "-":
                if (node.getChildren().size() == 1) {
                    Object operand = evaluateASTNode(node.getChildren().get(0));
                    if (operand instanceof Integer) {
                        return -((Integer) operand);
                    } else if (operand instanceof Double) {
                        return -((Double) operand);
                    } else {
                        throw new InterpreterException("Unary minus requires a numeric operand", getNodeLineNumber(node));
                    }
                }
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
                break;
                
            case "MULT", "*":
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
                break;
                
            case "DIV", "/":
                if (right instanceof Integer && ((Integer) right) == 0 ||
                    right instanceof Double && ((Double) right) == 0.0) {
                    throw new InterpreterException("Division by zero", lineNumber);
                }
                return ((Number) left).doubleValue() / ((Number) right).doubleValue();
                
            case "EXPONENT", "**":
                return Math.pow(((Number) left).doubleValue(), ((Number) right).doubleValue());
                
            case "MOD", "%":
                if (right instanceof Integer && ((Integer) right) == 0) {
                    throw new InterpreterException("Modulo by zero", lineNumber);
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
                break;
                
            case "BITWISE_AND", "&":
                typeChecker.checkType(left, Integer.class, "Bitwise AND requires integer operands");
                typeChecker.checkType(right, Integer.class, "Bitwise AND requires integer operands");
                return (Integer) left & (Integer) right;
                
            case "BITWISE_OR", "|":
                typeChecker.checkType(left, Integer.class, "Bitwise OR requires integer operands");
                typeChecker.checkType(right, Integer.class, "Bitwise OR requires integer operands");
                return (Integer) left | (Integer) right;
                
            case "BITWISE_XOR", "^":
                typeChecker.checkType(left, Integer.class, "Bitwise XOR requires integer operands");
                typeChecker.checkType(right, Integer.class, "Bitwise XOR requires integer operands");
                return (Integer) left ^ (Integer) right;
                
            case "LSHIFT", "<<":
                typeChecker.checkType(left, Integer.class, "Left shift requires integer operands");
                typeChecker.checkType(right, Integer.class, "Left shift requires integer operands");
                return (Integer) left << (Integer) right;
                
            case "RSHIFT", ">>":
                typeChecker.checkType(left, Integer.class, "Right shift requires integer operands");
                typeChecker.checkType(right, Integer.class, "Right shift requires integer operands");
                return (Integer) left >> (Integer) right;
                
            case "AND":
                typeChecker.checkType(left, Boolean.class, "AND requires boolean operands");
                typeChecker.checkType(right, Boolean.class, "AND requires boolean operands");
                return (Boolean) left && (Boolean) right;
                
            case "OR":
                typeChecker.checkType(left, Boolean.class, "OR requires boolean operands");
                typeChecker.checkType(right, Boolean.class, "OR requires boolean operands");
                return (Boolean) left || (Boolean) right;
        }

        throw new InterpreterException(
            "Unsupported operator or type mismatch: " + operator +
            " (" + inferType(left) + ", " + inferType(right) + ")",
            lineNumber
        );
    }

    private int getNodeLineNumber(ASTNode node) {
        if (node == null) return 0;
        return node.getLineNumber();
    }

    public TokenType inferType(Object value) {
        if (value instanceof Integer) return TokenType.NUMBER;
        if (value instanceof Double) return TokenType.DECIMAL;
        if (value instanceof String) return TokenType.TEXT;
        if (value instanceof Boolean) return TokenType.BINARY_TYPE;
        if (value instanceof List) return TokenType.LIST_TYPE;
        if (value instanceof Map) return TokenType.PAIR_MAP_TYPE;
        throw new InterpreterException("Unknown type for value: " + value, 0);
    }
}