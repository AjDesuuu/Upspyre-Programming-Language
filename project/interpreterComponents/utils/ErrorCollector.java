package project.interpreterComponents.utils;

import java.util.ArrayList;
import java.util.List;

public class ErrorCollector {
    private final List<String> errors = new ArrayList<>();
    private static final int LINE_WIDTH = 120;

    public void addError(String message) {
        errors.add(message);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public void printErrors() {
        if (!hasErrors()) return;

        System.out.println("\n" + centerText("INTERPRETER ERROR SUMMARY", LINE_WIDTH));
        System.out.println("-".repeat(LINE_WIDTH));
        for (int i = 0; i < errors.size(); i++) {
            String[] parts = errors.get(i).split(":", 2);
            System.out.println("Error " + (i + 1) + ":");
            if (parts.length == 2) {
                System.out.println("  Location: " + parts[0].trim());
                System.out.println("  " + parts[1].trim());
            } else {
                System.out.println("  " + errors.get(i));
            }
            if (i < errors.size() - 1) {
                System.out.println("-".repeat(Math.min(LINE_WIDTH, 30)));
            }
        }
        System.out.println("-".repeat(LINE_WIDTH));
    }

    private String centerText(String text, int width) {
        int padding = (width - text.length()) / 2;
        if (padding <= 0) return text;
        return " ".repeat(padding) + text + " ".repeat(padding);
    }

    public void clear() {
        errors.clear();
    }
}