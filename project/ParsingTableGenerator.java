package project;

import java.io.*;
import java.util.HashMap;

public class ParsingTableGenerator {
    public static HashMap<Integer, HashMap<String, String>> actionTable = new HashMap<>();
    public static HashMap<Integer, HashMap<String, String>> gotoTable = new HashMap<>();

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
}
