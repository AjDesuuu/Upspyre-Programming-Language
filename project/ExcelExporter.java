package project;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ExcelExporter {

    public static void exportToTxt(String filePath) {
        try (FileWriter writer = new FileWriter(filePath)) {
            // Write action table
            writer.write("Action Table\n");
            writeActionTable(writer);

            // Write goto table
            writer.write("\nGoto Table\n");
            writeGotoTable(writer);

            // Write grammar table
            writer.write("\nGrammar Table\n");
            writeGrammarTable(writer);

            System.out.println("Text file generated successfully: " + filePath);
        } catch (IOException e) {
            System.out.println("Error writing text file: " + e.getMessage());
        }
    }

    private static void writeActionTable(FileWriter writer) throws IOException {
        // Write headers
        writer.write("State,");
        for (String symbol : ParsingTableGenerator.actionTable.values().iterator().next().keySet()) {
            writer.write(symbol + ",");
        }
        writer.write("\n");

        // Write rows
        for (Map.Entry<Integer, HashMap<String, String>> entry : ParsingTableGenerator.actionTable.entrySet()) {
            writer.write(entry.getKey() + ",");
            for (String value : entry.getValue().values()) {
                writer.write(value + ",");
            }
            writer.write("\n");
        }
    }

    private static void writeGotoTable(FileWriter writer) throws IOException {
        // Write headers
        writer.write("State,");
        for (String symbol : ParsingTableGenerator.gotoTable.values().iterator().next().keySet()) {
            writer.write(symbol + ",");
        }
        writer.write("\n");

        // Write rows
        for (Map.Entry<Integer, HashMap<String, String>> entry : ParsingTableGenerator.gotoTable.entrySet()) {
            writer.write(entry.getKey() + ",");
            for (String value : entry.getValue().values()) {
                writer.write(value + ",");
            }
            writer.write("\n");
        }
    }

    private static void writeGrammarTable(FileWriter writer) throws IOException {
        // Write headers
        writer.write("Rule Number,LHS,RHS\n");

        // Write rows
        for (Map.Entry<Integer, ParsingTableGenerator.GrammarProduction> entry : ParsingTableGenerator.productionTable.entrySet()) {
            writer.write(entry.getKey() + ",");
            writer.write(entry.getValue().getLhs() + ",");
            writer.write(String.join(" ", entry.getValue().getRhs()) + "\n");
        }
    }
}