package project.interpreterComponents;


import project.interpreterComponents.utils.InterpreterException;
import project.SymbolTable;
import project.interpreterComponents.utils.SymbolTableManager;
import project.utils.parser.ASTNode;
import project.utils.parser.ParseTreeNode;
import java.util.Scanner;

public class InterpreterN {
    private final Evaluator evaluator;
    private final Executor executor;
    private final SymbolTableManager symbolTableManager;
    private final Scanner scanner;
    private final boolean debugMode;

    public InterpreterN(SymbolTable symbolTable) {
        this(symbolTable, false); // default: debug off
    }

    public InterpreterN(SymbolTable symbolTable, boolean debugMode) {
        this.scanner = new Scanner(System.in);
        this.symbolTableManager = new SymbolTableManager(symbolTable, debugMode);
        this.debugMode = debugMode;
        this.evaluator = new Evaluator(symbolTableManager, debugMode);
        this.executor = new Executor(symbolTableManager, evaluator, scanner, debugMode);
        
        this.executor.setEvaluator(this.evaluator);
    }
    public SymbolTableManager getSymbolTableManager() {
        return symbolTableManager;
    }

    public void interpret(ParseTreeNode root) {
        if (root == null) {
            throw new InterpreterException("Parse tree is null. Cannot interpret.", 0);
        }

        System.out.println("Converting CST to AST...");
        ASTNode astRoot = ASTNode.fromCST(root);

        System.out.println("Generated AST:");
        astRoot.printAST(0);

        System.out.println("\nGenerating AST image...");
        astRoot.generateImage("ast.png");

        System.out.println("\nStarting interpretation...");
        try {
            
            executor.executeASTNode(astRoot);
        } catch (InterpreterException e) {
            System.err.println("Interpreter error at line " + e.getLineNumber() + ": " + e.getMessage());
            return;
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return;
        }
    }
}