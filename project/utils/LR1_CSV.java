package project.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import project.utils.exception.AnalysisException;

//THIS IS USED TO GENERATE THE CSV OF THE ACTION AND GOTO TABLE
public class LR1_CSV {
    public static void main(String[] args) {
        try {
            // Read grammar input from a file (e.g., format.txt)
            String grammarInput = Files.readString(Paths.get("GrammarProgrammer/expanded.txt"));

            // Create the LR(1) generator
            LR1Generator generator = new LR1Generator(grammarInput);

            // Export the action and goto tables to a CSV file
            generator.exportToCSV("output.csv");
            System.out.println("Action and Goto tables exported to output.csv");
        } catch (IOException e) {
            System.err.println("Error reading grammar file: " + e.getMessage());
        } catch (AnalysisException e) {
            System.err.println("Error generating parsing table: " + e.getMessage());
        }
    }
}