package project.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import project.utils.parser.*;
import project.utils.exception.*;
import project.utils.symbol.*;


public class LR1Generator {

    private Grammar grammar;
    private ParseTable parseTable;

    public LR1Generator(String grammarInput) throws AnalysisException {
        // Parse the grammar input
        this.grammar = parseGrammar(grammarInput);

        // Initialize the parse table
        grammar.initParseTable();
        
        this.parseTable = grammar.getParseTable();
        //System.out.println("here is the parse table" + parseTable.toString());
    }

    private Grammar parseGrammar(String grammarInput) throws AnalysisException {
        // Define terminal and non-terminal symbols
        Set<String> terminalSymbols = new HashSet<>(Arrays.asList(
                "START", "END", "IF", "OTHERWISE", "FOR", "REPEAT", "UNTIL", "CONTINUE", "STOP", "METHOD", "OUTPUT", "GET", "SHOW", "CHOOSE_WHAT", "CONVERT_TO",
        "NUMBER_TYPE", "DECIMAL_TYPE", "TEXT_TYPE", "BINARY_TYPE", "LIST_TYPE", "PAIR_MAP_TYPE", "PICK",
        "TRUE", "FALSE", "NONE",
        "LEN", "SORT", "KEY", "VALUE", "TO_TEXT",
        "IDENTIFIER", "NUMBER", "DECIMAL", "TEXT",
        "ASSIGN", "PLUS", "MINUS", "MULT", "DIV", "EXPONENT", "MOD", "FLOOR_DIV", "PLUS_ASSIGN", "MINUS_ASSIGN", "MULT_ASSIGN",
        "AND", "OR", "NOT", "EQ", "NEQ", "LT", "GT", "LEQ", "GEQ",
        "BITWISE_AND", "BITWISE_OR", "BITWISE_XOR", "BITWISE_NOT", "LSHIFT", "RSHIFT", "S_NOT", "QUOTE",
        "LPAREN", "RPAREN", "LCURLY", "RCURLY", "LBRACKET", "RBRACKET", "COMMA", "SEMI", "COLON", "DOT",
        "MCOMMENT", "SCOMMENT", "EOF", "ERROR","ε"
        ));

        Set<String> nonterminalSymbols = new HashSet<>(Arrays.asList(
        "<PROGRAM>", "<PROGRAM_KLEENE>", "<STMT>", "<SIMPLE_STMT>", "<COMPOUND_STMT>", 
        "<EXPRESSION>", "<TERM>", "<FACTOR>", "<BASE>", "<RELATIONAL_EXPR>", "<CONST>", 
        "<BIT_EXPR>", "<BITOR_EXPR>", "<BITXOR_EXPR>", "<BITAND_EXPR>", "<BITSHIFT_EXPR>", 
        "<BITNOT_EXPR>", "<BIT_BASE>", "<DECL_STMT>", "<IO_STATEMENT>", "<OUTPUT_STMT>", 
        "<INPUT_STMT>", "<CONV_EXPR>", "<ASSIGNMENT_STMT>", "<RETURN_STMT>", "<CONDITIONAL_STMT>", 
        "<BLOCK_STMT>", "<CONDITIONAL_EXPR>", "<LOGICOR_EXPR>", "<LOGICAND_EXPR>", "<LOGICNOT_EXPR>", 
        "<LOOP_STMT>", "<FOR_LOOP>", "<REPEAT_LOOP>", "<REPEAT_UNTIL>", "<FUNC_DECL>", 
        "<FUNC_DECL_OPT>", "<PARAM_LIST>", "<PARAM_LIST_GROUP>", "<FUNC_CALL>", "<FUNC_CALL_OPT>", 
        "<ARG_LIST>", "<ARG_LIST_GROUP>", "<CHOOSE_WHAT_STMT>", "<CHOOSE_WHAT_STMT_KLEENE>", 
        "<PICK_CASE>", "<COLLECTION_STMT>", "<LIST_DECL>", "<LIST_DECL_GROUP>", "<PAIR_MAP_DECL>", 
        "<PAIR_MAP_VAL>", "<PAIR_MAP_VAL_GROUP>", "<PAIR>", "<COLLECTION_ASSIGN>", "<COLLECTION_EXPR>", 
        "<LIST_VALUE>", "<PAIR_MAP_VALUE>", "<PAIR_MAP_KEY>", "<COLLECTION_METHOD>", "<DATA_TYPE>", 
        "<RELATIONAL_OP>", "<ADD_OP>", "<MULTI_OP>", "<EXP_OP>", "<ASSIGN_OP>", "<SHIFT_OP>", "<BINARY>","<CONDITIONAL_STMT_GROUP>",
        "<BLOCK_STMT_KLEENE>"
    ));

        String startSymbol = "<PROGRAM>";

        // Create grammar
        Grammar grammar = new Grammar(terminalSymbols, nonterminalSymbols, startSymbol);

        // Parse productions from the grammar input
        String[] lines = grammarInput.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("//")) continue; // Skip empty lines and comments

            String[] parts = line.split("::=");
            if (parts.length != 2) {
                throw new AnalysisException("Invalid production format: " + line, null);
            }

            String lhs = parts[0].trim();
            String rhs = parts[1].trim();

            List<AbstractSymbol> rhsSymbols = new ArrayList<>();
            for (String symbol : rhs.split(" ")) {
                symbol = symbol.trim();
                if (symbol.isEmpty()) continue;

                if (terminalSymbols.contains(symbol)) {
                    rhsSymbols.add(grammar.getSymbolPool().getTerminalSymbol(symbol));
                } else if (nonterminalSymbols.contains(symbol)) {
                    rhsSymbols.add(grammar.getSymbolPool().getNonterminalSymbol(symbol));
                } else {
                    throw new AnalysisException("Unknown symbol in production: " + symbol, null);
                }
            }

            grammar.getProductions().add(new Production(grammar.getSymbolPool().getNonterminalSymbol(lhs), rhsSymbols));
        }

        return grammar;
    }

    public void exportToCSV(String filePath) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            // Write action table
            writer.write("Action Table\n");
            writeActionTable(writer);

            // Write goto table
            writer.write("\nGoto Table\n");
            writeGotoTable(writer);
        }
    }

    private void writeActionTable(FileWriter writer) throws IOException {
        // Write headers
        writer.write("State,");
        for (AbstractSymbol symbol : grammar.getSymbolPool().getTerminalSymbols()) {
            writer.write(symbol.getName() + ",");
        }
        writer.write("\n");

        // Write rows
        for (Map.Entry<Integer, Map<AbstractSymbol, Transition>> entry : parseTable.getTable().entrySet()) {
            writer.write(entry.getKey() + ",");
            for (AbstractSymbol symbol : grammar.getSymbolPool().getTerminalSymbols()) {
                Transition transition = entry.getValue().get(symbol);
                if (transition != null) {
                    writer.write(transition.toString() + ",");
                } else {
                    writer.write(",");
                }
            }
            writer.write("\n");
        }
    }
    public ParseTable getParseTable() {
        return this.parseTable;
    }
   
    

    private void writeGotoTable(FileWriter writer) throws IOException {
        // Write headers
        writer.write("State,");
        for (AbstractSymbol symbol : grammar.getSymbolPool().getNonterminalSymbols()) {
            writer.write(symbol.getName() + ",");
        }
        writer.write("\n");

        // Write rows
        for (Map.Entry<Integer, Map<AbstractSymbol, Transition>> entry : parseTable.getTable().entrySet()) {
            writer.write(entry.getKey() + ",");
            for (AbstractSymbol symbol : grammar.getSymbolPool().getNonterminalSymbols()) {
                Transition transition = entry.getValue().get(symbol);
                if (transition != null) {
                    writer.write(transition.toString() + ",");
                } else {
                    writer.write(",");
                }
            }
            writer.write("\n");
        }
    }

}