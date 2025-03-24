package project;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import project.utils.LR1Generator;
import project.utils.exception.AnalysisException;
import project.utils.parser.ParseTable;
import project.utils.parser.Transition;
import project.utils.symbol.AbstractSymbol;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class ParsingTableGenerator {
    public static HashMap<Integer, HashMap<String, String>> actionTable = new HashMap<>();
    public static HashMap<Integer, HashMap<String, String>> gotoTable = new HashMap<>();
    public static HashMap<Integer, GrammarProduction> productionTable = new HashMap<>();

    // Class to represent a grammar production
    public static class GrammarProduction {
        private String lhs; // Left-hand side of the production
        private List<String> rhs; // Right-hand side of the production

        public GrammarProduction(String lhs, List<String> rhs) {
            this.lhs = lhs;
            this.rhs = rhs;
        }

        public String getLhs() {
            return lhs;
        }

        public List<String> getRhs() {
            return rhs;
        }

        public int getRhsSize() {
            return rhs.size();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(lhs).append(" ::= ");
            for (String symbol : rhs) {
                sb.append(symbol).append(" ");
            }
            return sb.toString().trim();
        }
    }

    public static void generateParsingTables(String grammarFilePath) {
        try {
            // Read the grammar input from the file
            String grammarInput = new String(Files.readAllBytes(Paths.get(grammarFilePath)), StandardCharsets.UTF_8);

            // Create the LR1Generator
            LR1Generator lr1Generator = new LR1Generator(grammarInput);

            // Get the parse table from the LR1Generator
            ParseTable parseTable = lr1Generator.getParseTable();

            // Populate the actionTable and gotoTable from the parseTable
            for (Map.Entry<Integer, Map<AbstractSymbol, Transition>> entry : parseTable.getTable().entrySet()) {
                int state = entry.getKey();
                HashMap<String, String> actionRow = new HashMap<>();
                HashMap<String, String> gotoRow = new HashMap<>();

                for (Map.Entry<AbstractSymbol, Transition> symbolEntry : entry.getValue().entrySet()) {
                    AbstractSymbol symbol = symbolEntry.getKey();
                    Transition transition = symbolEntry.getValue();

                    if (symbol.getType() == AbstractSymbol.TERMINAL) {
                        actionRow.put(symbol.getName(), transition.toString());
                    } else {
                        gotoRow.put(symbol.getName(), transition.toString().substring(1));
                    }
                }

                actionTable.put(state, actionRow);
                gotoTable.put(state, gotoRow);
            }
            // Print the ACTION table
            System.out.println("\nACTION TABLE:");
            for (Map.Entry<Integer, HashMap<String, String>> entry : actionTable.entrySet()) {
                System.out.println("State " + entry.getKey() + " -> " + entry.getValue());
            }

            // Print the GOTO table
            System.out.println("\nGOTO TABLE:");
            for (Map.Entry<Integer, HashMap<String, String>> entry : gotoTable.entrySet()) {
                System.out.println("State " + entry.getKey() + " -> " + entry.getValue());
            }

            System.out.println("Parsing tables generated successfully.");
        } catch (IOException e) {
            System.out.println("Error reading grammar file: " + e.getMessage());
        } catch (AnalysisException e) {
            System.out.println("Error generating parsing table: " + e.getMessage());
        }
    }

    public static void generateProductionTable(String filePath) {
        try {
            FileReader fr = new FileReader(filePath);
            BufferedReader br = new BufferedReader(fr);
            String line;
            int ruleNumber = 1;
    
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("//"))
                    continue; // Skip empty lines and comments

                // Parse the production rule: LHS ::= RHS
                String[] parts = line.split("::=", 2);
                if (parts.length != 2) {
                    System.out.println("Invalid production format: " + line);
                    continue;
                }

                String lhs = parts[0].trim();
                String rhsString = parts[1].trim();

                // Handle epsilon (ε) explicitly
                List<String> rhs = new ArrayList<>();
                if (rhsString.equals("ε")) {
                    // If the RHS is ε, treat it as an empty production
                    rhs = new ArrayList<>();
                } else {
                    // Split the RHS into symbols (handling tokens enclosed in quotes as single
                    // symbols)
                    boolean inQuotes = false;
                    StringBuilder currentSymbol = new StringBuilder();

                    for (int i = 0; i < rhsString.length(); i++) {
                        char c = rhsString.charAt(i);

                        if (c == '"' || c == '\'') {
                            inQuotes = !inQuotes;
                            currentSymbol.append(c);
                        } else if (c == ' ' && !inQuotes) {
                            if (currentSymbol.length() > 0) {
                                rhs.add(currentSymbol.toString());
                                currentSymbol = new StringBuilder();
                            }
                        } else {
                            currentSymbol.append(c);
                        }
                    }

                    // Add the last symbol if it exists
                    if (currentSymbol.length() > 0) {
                        rhs.add(currentSymbol.toString());
                    }
                }

                // Store the production with its rule number
                productionTable.put(ruleNumber, new GrammarProduction(lhs, rhs));
                ruleNumber++;
            }

            br.close();
            fr.close();

            System.out.println("Generated " + ruleNumber + " grammar productions");
        } catch (IOException e) {
            System.out.println("Error reading productions file: " + e.getMessage());
        }
    }

    public static void generateOutputFIle(String outputFile) {
        ExcelExporter.exportToTxt(outputFile);
    }
}