package project.interpreterComponents;



import project.TokenType;
import project.interpreterComponents.utils.InterpreterException;
import project.interpreterComponents.utils.SymbolTableManager;
import project.interpreterComponents.utils.BreakException;
import project.interpreterComponents.utils.ContinueException;
import project.interpreterComponents.utils.ErrorCollector;
import project.interpreterComponents.utils.ReturnException;
import project.interpreterComponents.utils.ErrorCollector;
import project.SymbolDetails;
import project.SymbolTable;
import project.utils.parser.ASTNode;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class Executor {
    private final SymbolTableManager symbolTableManager;
    private Evaluator evaluator;
    private final Scanner scanner;
    private final Map<String, ASTNode> functions = new HashMap<>();
    private final boolean debugMode;
    private final ErrorCollector errorCollector;
    private Object returnValue;
    private int loopDepth = 0;

    public Executor(SymbolTableManager symbolTableManager, Evaluator evaluator, Scanner scanner, boolean debugMode) {
        this.symbolTableManager = symbolTableManager;
        this.evaluator = evaluator;
        this.scanner = scanner;
        this.debugMode = debugMode;
        this.errorCollector = new ErrorCollector();
    }

    public ErrorCollector getErrorCollector() {
        return errorCollector;
    }
    
    public void setEvaluator(Evaluator evaluator) {
        this.evaluator = evaluator;
    }

    public void executeASTNode(ASTNode node) {
        if (node == null) {
            throw new InterpreterException("Cannot execute null node", 0);
        }

        if (debugMode) {
            System.out.println("[DEBUG] Executing node: " + node.getType() +
                (node.getValue() != null ? " (" + node.getValue() + ")" : "") +
                ", children: " + node.getChildren().size());
        }


        switch (node.getType()) {
            case "PROGRAM":
            case "PROGRAM_KLEENE":
            case "STMT":
            case "START":
            case "END":
                for (ASTNode child : node.getChildren()) {
                    try {
                        executeASTNode(child);
                    } catch (ContinueException | BreakException ce) {
                        throw ce;
                    } catch (ReturnException re) {
                        throw re;   
                    }catch (InterpreterException e) {
                        errorCollector.addError("Interpreter error at line " + e.getLineNumber() + ": " + e.getMessage());
                    } catch (Exception e) {
                        String msg = e.getMessage();
                        if (msg == null || msg.trim().isEmpty()) msg = e.getClass().getSimpleName();
                        errorCollector.addError("Unexpected error: " + msg);
                    }
                }
                break;
                
            case "ASSIGNMENT_STMT":
                executeAssignment(node);
                break;

            case "DECL_STMT":
                executeDeclaration(node);
                break;
    
            case "OUTPUT":
                executeOutput(node);
                break;

            case "BLOCK_STMT":
            case "BLOCK_STMT_KLEENE":
                for (ASTNode child : node.getChildren()) {
                    executeASTNode(child);
                }
                break;

            case "CONDITIONAL_STMT":
                executeConditional(node);
                break;
            
            case "FUNC_CALL":
                Object returnValue = evaluateFunctionCall(node);
                if (returnValue != null) {
                    System.out.println("Function returned: " + returnValue);
                }
                break;

            case "LIST_DECL":
                executeListDeclaration(node);
                break;

            case "STOP":
                if (loopDepth > 0) {
                    throw new BreakException();
                } else {
                    throw new InterpreterException("'stop' statement not allowed outside of a breakable block", getNodeLineNumber(node));
                }
            
            case "CONTINUE":
                if (loopDepth > 0) {
                    throw new ContinueException();
                } else {
                    throw new InterpreterException("'continue' statement not allowed outside of a loop", getNodeLineNumber(node));
                }
            
            case "FOR_LOOP":
                executeForLoop(node);
                break;
            
            case "REPEAT_UNTIL":
                executeRepeatUntil(node);
                break;
            
            case "REPEAT_LOOP":
                executeRepeatLoop(node);
                break;
            
            case "FUNC_DECL":
                executeFunctionDeclaration(node);
                break;
            
            case "PAIR_MAP_DECL":
                executePairMapDeclaration(node);
                break;

            case "RETURN_STMT":
                executeReturnStatement(node);
                break;

            case "INPUT_STMT":
                executeInputStatement(node);
                break;
            case "COLLECTION_METHOD":
                evaluator.evaluateASTNode(node);
                break;
            case "CHOOSE_WHAT_STMT":
                executeChooseWhatStatement(node);
                break;
            case "COLLECTION_ASSIGN":
                executeCollectionAssignment(node);
                break;
            default:
                // Process other nodes
                for (ASTNode child : node.getChildren()) {
                    executeASTNode(child);
                }
        }
    }

    private void executeAssignment(ASTNode node) {
        String variable = null;
        Object value = null;
        TokenType type = null;
        boolean isForLoopInit = node.getParent() != null && node.getParent().getType().equals("FOR_LOOP");
    
        // Extract information from children
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
                    if (variable == null) {
                        variable = child.getValue();
                    } else {
                        SymbolDetails rhsDetails = symbolTableManager.getIdentifier(child.getValue());
                        if (rhsDetails == null) {
                            throw new InterpreterException(
                                "Variable '" + child.getValue() + "' is not defined.",
                                getNodeLineNumber(node)
                            );
                        }
                        value = rhsDetails.getValue();
                    }
                    break;
                case "TRUE":
                    value = true;
                    break;
                case "FALSE":
                    value = false;
                    break;
                case "ASSIGN":
                    break;
                default:
                    value = evaluator.evaluateASTNode(child);
                    break;
            }
        }
    
        // Handle value validation and type checking
        if (variable == null) {
            throw new InterpreterException(
                "Invalid assignment: missing variable name",
                getNodeLineNumber(node)
            );
        }
    
        if (value == null) {
            // Optionally: print a warning for number types
            System.out.println("Warning: Assigned 'none' to variable '" + variable + "' of type " + type);
            // Allow assignment of 'none' (null) to any type
            symbolTableManager.addIdentifier(variable, type, null);
            SymbolDetails details = symbolTableManager.getIdentifier(variable);
            if (details != null) {
                details.setExplicitlyDeclared(true);
            }
            symbolTableManager.getCurrentSymbolTable().markVariableAsUsed(variable);
            System.out.println("Assigned " + variable + " = none");
            return;
        }
    
        // Get or verify type
        if (type == null) {
            // Assignment to existing variable
            SymbolDetails existing = symbolTableManager.getIdentifier(variable);
            if (existing == null) {
                throw new InterpreterException(
                    "Variable '" + variable + "' must be declared before assignment.",
                    getNodeLineNumber(node)
                );
            }
            type = existing.getType();
        }
    
        // Type checking
        TokenType valueType = evaluator.inferType(value);
        // Allow implicit conversion between number and decimal
        if (type == TokenType.NUMBER && (valueType == TokenType.NUMBER || valueType == TokenType.DECIMAL)) {
            if (value instanceof Double) {
                value = ((Double) value).intValue();
            }
        } else if (type == TokenType.DECIMAL && (valueType == TokenType.NUMBER || valueType == TokenType.DECIMAL)) {
            if (value instanceof Integer) {
                value = ((Integer) value).doubleValue();
            }
        } else if (type != valueType) {
            throw new InterpreterException(
                "Type mismatch: cannot assign value of type " + valueType + " to variable '" + variable + "' of type " + type,
                getNodeLineNumber(node)
            );
        }
    
        // Type conversion if needed
        if (type == TokenType.DECIMAL && value instanceof Integer) {
            value = ((Integer) value).doubleValue();
        }
        if (type == TokenType.NUMBER && value instanceof Double) {
            value = ((Double) value).intValue();
        }
    
        // Update or declare the variable
        if (node.getChildren().stream().anyMatch(c -> c.getType().endsWith("_TYPE"))) {

            // Check for redeclaration in current scope
            // Check all visible scopes for redeclaration
            //TODO DECIDE IF WE WANT RESHADOWING
            SymbolDetails existing;
            if (isForLoopInit) {
                // For for-loop variable declarations, check all visible scopes (Java behavior)
                existing = symbolTableManager.getIdentifier(variable);
            } else {
                // For other declarations, only check current scope (allow shadowing)
                existing = symbolTableManager.getCurrentSymbolTable().getIdentifierLocalScope(variable);
            }
            if (existing != null && existing.isExplicitlyDeclared()) {
                throw new InterpreterException(
                    "Variable '" + variable + "' is already declared in this or an outer scope",
                    getNodeLineNumber(node)
                );
            }


            symbolTableManager.addIdentifier(variable, type, value);
            SymbolDetails details = symbolTableManager.getIdentifier(variable);
            if (details != null) {
                details.setExplicitlyDeclared(true);
            }
        } else {
            symbolTableManager.updateIdentifier(variable, value);
        }
        symbolTableManager.getCurrentSymbolTable().markVariableAsUsed(variable);

        System.out.println("Assigned " + variable + " = " + value);
    }

    private void executeCollectionAssignment(ASTNode node) {
        // node: COLLECTION_ASSIGN -> [LIST_VALUE, ASSIGN, value]
        ASTNode collectionValueNode = node.getChildren().get(0);
        ASTNode valueNode = node.getChildren().get(2);

        // Evaluate the list and index
        String listName = collectionValueNode.getChildren().get(0).getValue();
        Object index;
        Object value = evaluator.evaluateASTNode(valueNode);

        // Check if the list exists
        SymbolDetails listDetails = symbolTableManager.getIdentifier(listName);
        if (listDetails == null) {
            throw new InterpreterException("Undefined variable: " + listName, getNodeLineNumber(node));
        }
        Object listObj = listDetails.getValue();

        // Only support List for now
        if (listObj instanceof List) {
            index = evaluator.evaluateASTNode(collectionValueNode.getChildren().get(2));
            // Ensure index is an Integer
            if (!(index instanceof Integer)) {
                throw new InterpreterException("List index must be a number, got: " + (index == null ? "null" : index.getClass().getSimpleName()), getNodeLineNumber(node));
            }
            List<Object> list = (List<Object>) listObj;
            int idx = (Integer) index;
            if (idx < 0 || idx >= list.size()) {
                throw new InterpreterException("List index out of bounds: " + idx, getNodeLineNumber(node));
            }
            // Type check the value
            TokenType listType = listDetails.getType();
            TokenType valueType = evaluator.inferType(value);
            if (listType != valueType) {
                throw new InterpreterException(
                    "Type mismatch: cannot assign value of type " + valueType + " to list of type " + listType,
                    getNodeLineNumber(node)
                );
            }
            list.set(idx, value);
            return;
        } else if (listObj instanceof Map) {
            index = evaluator.evaluateASTNode(collectionValueNode.getChildren().get(4));
            Map<Object, Object> map = (Map<Object, Object>) listObj;

            // Handle PAIR_MAP_VALUE
            if (collectionValueNode.getType().equals("PAIR_MAP_VALUE")) {
                map.put(index, value);
                return;
            }

            // Handle PAIR_MAP_KEY (not typically allowed, but included for completeness)
            if (collectionValueNode.getType().equals("PAIR_MAP_KEY")) {
                throw new InterpreterException("Cannot assign to a map key directly: " + listObj, getNodeLineNumber(node));
            }

            throw new InterpreterException("Unsupported map operation for assignment: " + listName, getNodeLineNumber(node));
        }


        throw new InterpreterException("Variable is not a list: " + listName, getNodeLineNumber(node));
    }
    
    private void executeDeclaration(ASTNode node) {
        String varName = null;
        TokenType type = null;
    
        for (ASTNode child : node.getChildren()) {
            switch (child.getType()) {
                case "TEXT_TYPE":
                    type = TokenType.TEXT;
                    break;
                case "NUMBER_TYPE":
                    type = TokenType.NUMBER;
                    break;
                case "IDENTIFIER":
                    varName = child.getValue();
                    break;
            }
        }
    
        if (varName == null || type == null) {
            throw new InterpreterException(
                "Invalid declaration: missing variable name or type",
                getNodeLineNumber(node)
            );
        }
    
        // Check only in current scope
        SymbolDetails existing = symbolTableManager.getCurrentSymbolTable().getIdentifierLocalScope(varName);
        if (existing != null && existing.isExplicitlyDeclared()) {
            throw new InterpreterException(
                "Variable '" + varName + "' is already declared in current scope",
                getNodeLineNumber(node)
            );
        }
    
        symbolTableManager.addIdentifier(varName, type, null);
        SymbolDetails details = symbolTableManager.getCurrentSymbolTable().getIdentifier(varName);
        if (details != null) {
            details.setExplicitlyDeclared(true);
        }
    }

    private void executeOutput(ASTNode node) {
        StringBuilder output = new StringBuilder();
        
    
        for (ASTNode child : node.getChildren()) {
            switch (child.getType()) {
                case "TEXT":
                    // Append text directly
                    output.append(child.getValue());
                    break;
    
                case "IDENTIFIER":
                    // Retrieve the value of the identifier
                    String varName = child.getValue();
                    SymbolDetails details = symbolTableManager.getCurrentSymbolTable().getIdentifier(varName);
    
                    if (details == null || !details.isExplicitlyDeclared()) {
                        throw new InterpreterException(
                            "Cannot show undefined variable: " + varName,
                            getNodeLineNumber(node)
                        );
                    }
    
                    if (details.getValue() == null) {
                        throw new InterpreterException(
                            "Cannot show uninitialized variable: " + varName,
                            getNodeLineNumber(node)
                        );
                    }

                    symbolTableManager.getCurrentSymbolTable().markVariableAsUsed(varName);
    
                    output.append(details.getValue());
                    break;
                default:
                    // Evaluate expressions (e.g., PLUS nodes)
                    Object result = evaluator.evaluateASTNode(child);
                    output.append(result);
                  
                    break;
            }
        }
    
        // Print the final output
        System.out.println("Output: " + output.toString());
    }

    private void executeConditional(ASTNode node) {
        ASTNode conditionNode = null;
        ASTNode ifBlock = null;
        ASTNode otherwiseBlock = null;
    
        for (ASTNode child : node.getChildren()) {
            switch (child.getType()) {
                case "GT", "LT", "GTE", "LTE", "GEQ", "LEQ", "EQ", "NEQ", "RELATIONAL_EXPR","TRUE","FALSE","IDENTIFIER", "AND", "OR", "NOT":
                    conditionNode = child;
                    break;
                case "BLOCK_STMT":
                    if (ifBlock == null) {
                        ifBlock = child;
                    }
                    break;
                case "CONDITIONAL_STMT_GROUP":
                    for (ASTNode groupChild : child.getChildren()) {
                        if (groupChild.getType().equals("BLOCK_STMT")) {
                            otherwiseBlock = groupChild;
                        }
                    }
                    break;
            }
        }
    
        if (conditionNode == null) {
            throw new InterpreterException("Missing or invalid condition in IF statement", getNodeLineNumber(node));
        }
    
        boolean conditionResult = (boolean) evaluator.evaluateASTNode(conditionNode);
    
        if (conditionResult) {
            symbolTableManager.pushScope("IF_BLOCK");
            
            try {
                // Access variables in the IF_BLOCK
                ifBlock.getChildren().forEach(child -> {
                    if (child.getType().equals("IDENTIFIER")) {
                        String varName = child.getValue();
                        SymbolDetails details = symbolTableManager.getIdentifier(varName);
                        if (details != null) {
                            symbolTableManager.getCurrentSymbolTable().markVariableAsUsed(varName);
                        }
                    }
                });
                executeASTNode(ifBlock);
            } finally {
                
                symbolTableManager.popScope();
            }
        } else if (otherwiseBlock != null) {
            symbolTableManager.pushScope("OTHERWISE_BLOCK");
            
            try {
                // Access variables in the OTHERWISE_BLOCK
                otherwiseBlock.getChildren().forEach(child -> {
                    if (child.getType().equals("IDENTIFIER")) {
                        String varName = child.getValue();
                        SymbolDetails details = symbolTableManager.getIdentifier(varName);
                        if (details != null) {
                            symbolTableManager.getCurrentSymbolTable().markVariableAsUsed(varName);
                        }
                    }
                });
                executeASTNode(otherwiseBlock);
            } finally {
                
                symbolTableManager.popScope();
            }
        }
    }

    private void executeListDeclaration(ASTNode node) {
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
                case "DECIMAL_TYPE":
                    listType = TokenType.DECIMAL;
                    break;
                case "IDENTIFIER":
                    listName = child.getValue();
                    break;
                case "TEXT":
                case "NUMBER":
                case "DECIMAL":
                    elements.add(evaluator.evaluateASTNode(child));
                    break;
                case "LIST_ELEMENTS":
                    collectListElements(child, elements);
                    break;
            }
        }

        // Enforce type consistency for all elements
        if (listType != null) {
            for (Object elem : elements) {
                TokenType elemType = evaluator.inferType(elem);
                if (elemType != listType) {
                    throw new InterpreterException(
                        "List elements must all be of type " + listType + ", but found " + elemType,
                        getNodeLineNumber(node)
                    );
                }
            }
        }

        if (listName != null && listType != null) {
            symbolTableManager.addIdentifier(listName, TokenType.LIST_TYPE, elements);
            SymbolDetails details = symbolTableManager.getIdentifier(listName);
            if (details != null) {
                details.setExplicitlyDeclared(true);
                details.setElementType(listType); // If you have this method/field
            }
            System.out.println("Assigned list " + listName + " = " + elements);
        }
    }

    private void executeForLoop(ASTNode node) {
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
            throw new InterpreterException("Incomplete FOR loop structure", getNodeLineNumber(node));
        }
    
        // Only push one scope for the loop variable and body
        symbolTableManager.pushScope("FOR_LOOP");
        loopDepth++;
        try {
            executeASTNode(init);  // Declare loop variable in this scope

            String loopVariable = init.getChildren().get(0).getValue();
            symbolTableManager.getCurrentSymbolTable().markVariableAsUsed(loopVariable);
    
            while (true) {
                Object cond = evaluator.evaluateASTNode(condition);
                if (!(cond instanceof Boolean)) {
                    throw new InterpreterException("For-loop condition did not evaluate to a boolean", getNodeLineNumber(node));
                }
                if (!(Boolean) cond) break;
    
                try {
                    executeASTNode(body);
                } catch (ContinueException ce) {
                    // skip increment, go to next iteration
                    executeASTNode(increment);
                    symbolTableManager.getCurrentSymbolTable().markVariableAsUsed(loopVariable);
                    continue;
                } catch (BreakException be) {
                    break;
                }
                executeASTNode(increment);

                symbolTableManager.getCurrentSymbolTable().markVariableAsUsed(loopVariable);
            }
        } finally {
            loopDepth--;
            symbolTableManager.popScope(); // Pop the loop variable's scope
        }
    }
    

    private void executeRepeatUntil(ASTNode node) {
        ASTNode repeatBlock = null;
        ASTNode condition = null;
    
        for (ASTNode child : node.getChildren()) {
            if ("BLOCK_STMT".equals(child.getType())) {
                repeatBlock = child;
            } else if (!"REPEAT".equals(child.getType()) && 
                       !"UNTIL".equals(child.getType()) && 
                       !"LPAREN".equals(child.getType()) && 
                       !"RPAREN".equals(child.getType())) {
                // Accept any non-keyword, non-block node as the condition
                condition = child;
            }
        }
    
        if (repeatBlock == null || condition == null) {
            throw new InterpreterException("REPEAT_UNTIL missing block or condition", getNodeLineNumber(node));
        }
        // Evaluate the condition before entering the loop
        Object condVal = evaluator.evaluateASTNode(condition);
        if (!(condVal instanceof Boolean)) {
            throw new InterpreterException("REPEAT_UNTIL condition must evaluate to binary", getNodeLineNumber(node));
        }
        if ((Boolean) condVal) {
            System.out.println("Warning: repeat-until loop will not execute because the condition is already true.");
            return;
        }
    
        symbolTableManager.pushScope("REPEAT_UNTIL");
        loopDepth++;
        int maxIterations = 10; // or any reasonable limit
        int iterations = 0;
        try {
            while (true) {
                try {
                    executeASTNode(repeatBlock);
                } catch (ContinueException ce) {
                    // continue to next iteration
                    continue;
                } catch (BreakException be) {
                    // break out of the loop
                    break;
                }
    
                condVal = evaluator.evaluateASTNode(condition);
                if (!(condVal instanceof Boolean)) {
                    throw new InterpreterException("REPEAT_UNTIL condition must evaluate to binary", getNodeLineNumber(node));
                }
                if ((Boolean) condVal) {
                    break;
                }
                iterations++;
                if (iterations > maxIterations) {
                    System.out.println("Warning: repeat-until loop exceeded maximum iterations (" + maxIterations + "). Possible infinite loop.");
                    break;
                }
            }
        } finally {
            loopDepth--;
            symbolTableManager.popScope();
        }
    }

    private void executeRepeatLoop(ASTNode node) {
        ASTNode repeatCondition = null;
        ASTNode repeatBlock = null;
    
        for (ASTNode child : node.getChildren()) {
            if ("BLOCK_STMT".equals(child.getType())) {
                repeatBlock = child;
            } else if (!"REPEAT".equals(child.getType()) &&
                       !"LPAREN".equals(child.getType()) &&
                       !"RPAREN".equals(child.getType())) {
                // Accept any non-keyword, non-block node as the condition
                repeatCondition = child;
            }
        }
    
        if (repeatCondition == null || repeatBlock == null) {
            throw new InterpreterException("REPEAT_LOOP missing condition or block", getNodeLineNumber(node));
        }
        // Evaluate the condition before entering the loop
        Object conditionValue = evaluator.evaluateASTNode(repeatCondition);
        if (!(conditionValue instanceof Boolean)) {
            throw new InterpreterException("REPEAT_LOOP condition must evaluate to a binary", getNodeLineNumber(node));
        }
        if (!(Boolean) conditionValue) {
            System.out.println("Warning: repeat loop will not execute because the condition is initially false.");
            return;
        }

        
    
        symbolTableManager.pushScope("REPEAT_LOOP");
        loopDepth++;
        int maxIterations = 10; // safeguard
        int iterations = 0;
        try {
            while (true) {
                conditionValue = evaluator.evaluateASTNode(repeatCondition);
                if (!(conditionValue instanceof Boolean)) {
                    throw new InterpreterException("REPEAT_LOOP condition must evaluate to a binary", getNodeLineNumber(node));
                }
    
                if (!(Boolean) conditionValue) {
                    break; // Exit the loop if the condition is false
                }
                
                try {
                    executeASTNode(repeatBlock);
                } catch (ContinueException ce) {
                    // Just continue to the next iteration
                    continue;
                } catch (BreakException be) {
                    // Break out of the loop
                    break;
                }
                iterations++;
                if (iterations > maxIterations) {
                    System.out.println("Warning: repeat loop exceeded maximum iterations (" + maxIterations + "). Possible infinite loop.");
                    break;
                }
               
            }
        } finally {
            loopDepth--;
            symbolTableManager.popScope();
        }
    }

    

    private void executeFunctionDeclaration(ASTNode node) {
        String functionName = null;
        ASTNode paramList = null;
        ASTNode functionBody = null;

        for (ASTNode child : node.getChildren()) {
            if (child.getType().equals("IDENTIFIER")) {
                functionName = child.getValue();
            } else if (child.getType().equals("PARAM_LIST")) {
                paramList = child;
            } else if (child.getType().equals("BLOCK_STMT")) {
                functionBody = child;
            }
        }

        if (functionName == null || functionBody == null) {
            throw new InterpreterException("Invalid function declaration", getNodeLineNumber(node));
        }

        // Build parameter signature string (e.g., "TEXT_TYPE," or "TEXT_TYPE,COMMA,TEXT_TYPE,")
        List<ASTNode> params = paramList != null ? getParamIdentifiers(paramList) : new ArrayList<>();
        StringBuilder signatureBuilder = new StringBuilder();
        for (ASTNode param : params) {
            ASTNode parent = param.getParent();
            List<ASTNode> siblings = parent.getChildren();
            int idx = siblings.indexOf(param);
            ASTNode typeNode = null;
            // Look backwards for the nearest type node
            for (int i = idx - 1; i >= 0; i--) {
                String t = siblings.get(i).getType();
                if (t.equals("NUMBER_TYPE") || t.equals("TEXT_TYPE") || t.equals("DECIMAL_TYPE")) {
                    typeNode = siblings.get(i);
                    break;
                }
            }
            String typeString = null;
            if (typeNode != null) {
                switch (typeNode.getType()) {
                    case "NUMBER_TYPE": typeString = "NUMBER"; break;
                    case "TEXT_TYPE": typeString = "TEXT"; break;
                    case "DECIMAL_TYPE": typeString = "DECIMAL"; break;
                    default: typeString = typeNode.getType();
                }
            } else {
                typeString = "UNKNOWN";
            }
            signatureBuilder.append(typeString).append(",");
        }
        String paramSignature = signatureBuilder.toString();
        symbolTableManager.addIdentifier(functionName, TokenType.METHOD, paramSignature, getNodeLineNumber(node));

        String compositeKey = functionName + "|" + paramSignature;

        // Store function with composite key
        functions.put(compositeKey, node);
        
        System.out.println("Function declared: " + functionName + " with signature: " + paramSignature);
    }

    private void executePairMapDeclaration(ASTNode node) {
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
                            Object key = evaluator.evaluateASTNode(keyNode);
                            Object value = evaluator.evaluateASTNode(valueNode);
    
                            // Enforce type consistency for key and value
                            TokenType actualKeyType = evaluator.inferType(key);
                            TokenType actualValueType = evaluator.inferType(value);
                            if (keyType != null && actualKeyType != keyType) {
                                throw new InterpreterException(
                                    "Pair map keys must all be of type " + keyType + ", but found " + actualKeyType,
                                    getNodeLineNumber(node)
                                );
                            }
                            if (valueType != null && actualValueType != valueType) {
                                throw new InterpreterException(
                                    "Pair map values must all be of type " + valueType + ", but found " + actualValueType,
                                    getNodeLineNumber(node)
                                );
                            }
    
                            map.put(key, value);
                        }
                    }
                    break;
            }
        }

        if (mapName != null && keyType != null && valueType != null) {
            symbolTableManager.addIdentifier(mapName, TokenType.PAIR_MAP_TYPE, map);
            System.out.println("Assigned map " + mapName + " = " + map);
        } else {
            throw new InterpreterException("Invalid map declaration", getNodeLineNumber(node));
        }
    }

    private void executeReturnStatement(ASTNode node) {
        Object returnValue = null;
        
        // Skip the OUTPUT node and directly evaluate the expression that follows it
        for (ASTNode child : node.getChildren()) {
            if (!child.getType().equals("OUTPUT")) {
                returnValue = evaluator.evaluateASTNode(child);
            }
        }
        
        throw new ReturnException(returnValue);
    }

    private void executeInputStatement(ASTNode node) {
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

        // Ensure both variable name and type are defined
        if (inputVarName != null && inputType != null) {
            SymbolDetails details = symbolTableManager.getIdentifier(inputVarName);

            // Check if variable is already declared
            if (details != null) {
                // Ensure the types match
                if (details.getType() != inputType) {
                    throw new InterpreterException(
                            "Type mismatch: cannot assign value of type " + inputVarName + " to variable '" + details.getType() + "' of type " + inputType,
                            getNodeLineNumber(node)
                    );
                }
            } else {
                // Declare the variable if not already declared
                symbolTableManager.addIdentifier(inputVarName, inputType, null);
                details = symbolTableManager.getIdentifier(inputVarName);
                if (details != null) {
                    details.setExplicitlyDeclared(true);
                }
            }

            // Prompt the user for input
            System.out.print("> ");
            String userInput = scanner.nextLine();
            Object value = userInput;

            // Parse input value if the required type is NUMBER
            if (inputType == TokenType.NUMBER) {
                try {
                    value = Integer.parseInt(userInput);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid number input, storing as text.");
                }
            }

            // Update the variable with the new value
            symbolTableManager.updateIdentifier(inputVarName, value);
        }
    }

    private void executeChooseWhatStatement(ASTNode node) {
        ASTNode conditionNode = node.getChildren().get(2);
        Object conditionValue = evaluator.evaluateASTNode(conditionNode);
    
        List<ASTNode> pickCases = new ArrayList<>();
        collectPickCases(node, pickCases);
    
        boolean caseMatched = false;
        for (ASTNode pickCase : pickCases) {
            ASTNode pickConditionNode = pickCase.getChildren().get(1);
            Object pickConditionValue = evaluator.evaluateASTNode(pickConditionNode);
    
            if (conditionValue.equals(pickConditionValue)) {
                ASTNode blockStmt = pickCase.getChildren().get(3);
                executeASTNode(blockStmt);
                caseMatched = true;
                break;
            }
        }
    
        if (!caseMatched) {
            System.out.println("No matching case found for choose_what condition: " + conditionValue);
        }
    }

    public Object evaluateFunctionCall(ASTNode node) {
        String functionName = node.getChildren().get(0).getValue();
        ASTNode argListNode = null;
        Object returnValue = null;

        // Find the ARG_LIST node
        for (ASTNode child : node.getChildren()) {
            if (child.getType().equals("ARG_LIST")) {
                argListNode = child;
                break;
            }
        }

        // If no ARG_LIST node, treat as empty argument list
        List<ASTNode> args = new ArrayList<>();
        if (argListNode != null) {
            args = getArgNodes(argListNode);
        }
        // Build argument signature string
        
        StringBuilder argSignatureBuilder = new StringBuilder();
        for (ASTNode arg : args) {
            // Infer type for each argument
            Object argValue = evaluator.evaluateASTNode(arg);
            TokenType argType = evaluator.inferType(argValue);
            argSignatureBuilder.append(argType.toString()).append(",");
        }
        String argSignature = argSignatureBuilder.toString();

        // Lookup function using composite key
        String compositeKey = functionName + "|" + argSignature;
        ASTNode functionNode = functions.get(compositeKey);
        if (functionNode == null) {
            throw new InterpreterException("Undefined function: " + functionName, getNodeLineNumber(node));
        }

        // Find parameter list and block statement in function declaration
        ASTNode paramList = null;
        ASTNode blockStmt = null;
        for (ASTNode child : functionNode.getChildren()) {
            if (child.getType().equals("PARAM_LIST")) {
                paramList = child;
            } else if (child.getType().equals("BLOCK_STMT")) {
                blockStmt = child;
            }
        }

        if (blockStmt == null) {
            throw new InterpreterException("Invalid function definition for: " + functionName, getNodeLineNumber(node));
        }

        // Get parameters and arguments
        List<ASTNode> params = paramList != null ? getParamIdentifiers(paramList) : new ArrayList<>();
        args = argListNode != null ? getArgNodes(argListNode) : new ArrayList<>();

        if (params.size() != args.size()) {
            throw new InterpreterException(
                "Argument count mismatch for function: " + functionName +
                ". Expected: " + params.size() + ", Got: " + args.size(),
                getNodeLineNumber(node)
            );
        }

        // Create a new scope for the function

        symbolTableManager.pushScope("FUNCTION_CALL");

        try {
            // Map arguments to parameters
            for (int i = 0; i < params.size(); i++) {
                ASTNode param = params.get(i);
                String paramName = param.getValue();
                Object argValue = evaluator.evaluateASTNode(args.get(i));
                TokenType type = evaluator.inferType(argValue);

                // Add parameter to the new scope
                symbolTableManager.addIdentifier(paramName, type, argValue);
                SymbolDetails details = symbolTableManager.getIdentifier(paramName);
                if (details != null) {
                    details.setExplicitlyDeclared(true);
                }
            }
            // Execute function body
            executeASTNode(blockStmt);
        } catch (ReturnException re) {
            returnValue = re.value;
        } finally {
            symbolTableManager.popScope(); // Always restore previous scope
        }

        return returnValue;
    }

    private void collectListElements(ASTNode groupNode, List<Object> elements) {
        for (ASTNode child : groupNode.getChildren()) {
            switch (child.getType()) {
                case "TEXT":
                case "NUMBER":
                case "DECIMAL":
                    elements.add(evaluator.evaluateASTNode(child));
                    break;
                case "LIST_DECL_GROUP":
                    collectListElements(child, elements);
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

    private int getNodeLineNumber(ASTNode node) {
        if (node == null) return 0;
        return node.getLineNumber();
    }
}