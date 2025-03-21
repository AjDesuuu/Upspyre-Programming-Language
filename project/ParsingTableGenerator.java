package project;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

    public static void generateParsingTables(String filePath) {
        StringBuilder csvData = new StringBuilder();

        try {
            FileReader fr = new FileReader(filePath);
            BufferedReader br = new BufferedReader(fr);
            String line;

            while ((line = br.readLine()) != null) {
                csvData.append(line).append("\n");
            }

            br.close();
            fr.close();
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
            return;
        }

        String[] lines = csvData.toString().split("\n");
        String[] headers = lines[0].split(",");

        int dollarIndex = -1;
        for (int i = 0; i < headers.length; i++) {
            if (headers[i].trim().equals("$")) {
                dollarIndex = i;
                break;
            }
        }

        if (dollarIndex == -1) {
            System.out.println("Error: '$' column not found!");
            return;
        }

        for (int i = 1; i < lines.length; i++) {
            String[] values = lines[i].split(",", -1);
            if (values.length == 0 || values[0].trim().isEmpty()) continue;

            int state;
            try {
                state = Integer.parseInt(values[0].trim());
            } catch (NumberFormatException e) {
                System.out.println("Skipping invalid state at line " + (i + 1));
                continue;
            }

            HashMap<String, String> actionRow = new HashMap<>();
            HashMap<String, String> gotoRow = new HashMap<>();

            for (int j = 1; j < headers.length; j++) {
                if (j >= values.length) continue;

                String value = values[j].trim();
                if (!value.isEmpty()) {
                    if (j < dollarIndex) {
                        actionRow.put(headers[j].trim(), value);
                    } else {
                        gotoRow.put(headers[j].trim(), value);
                    }
                }
            }

            actionTable.put(state, actionRow);
            gotoTable.put(state, gotoRow);
        }
    }
    
    public static void generateProductionTable(String filePath) {
        try {
            FileReader fr = new FileReader(filePath);
            BufferedReader br = new BufferedReader(fr);
            String line;
            int ruleNumber = 0;
    
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("//")) continue; // Skip empty lines and comments
    
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
                    // Split the RHS into symbols (handling tokens enclosed in quotes as single symbols)
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
    public static void generateOutputFIle(String outputFile){
        ExcelExporter.exportToTxt(outputFile);
    }
}