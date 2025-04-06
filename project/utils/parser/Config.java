package project.utils.parser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Config {

    private Set<String> nonterminalSymbols;
    private Set<String> terminalSymbols;
    private Set<String> ignoredSymbols;
    private String startSymbol;
    private List<String> productions;

    //Creates a new Config by reading grammar rules from the specified file path.
    public Config(String path) {
        nonterminalSymbols = new HashSet<>();
        terminalSymbols = new HashSet<>();
        ignoredSymbols = new HashSet<>();
        productions = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // Parse production rules
                if (line.contains("::=")) {
                    productions.add(line);
                    String[] parts = line.split("::=");
                    String lhs = parts[0].trim();
                    nonterminalSymbols.add(lhs);

                    String rhs = parts[1].trim();
                    String[] rhsSymbols = rhs.split("\\s+");
                    for (String symbol : rhsSymbols) {
                        if (symbol.equals("ε") || symbol.equals("")) continue;
                        if (!symbol.startsWith("<") && !symbol.endsWith(">")) {
                            terminalSymbols.add(symbol);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // the first nonterminal symbol is the start symbol
        startSymbol = nonterminalSymbols.iterator().next();
    }

    public Set<String> getNonterminalSymbols() {
        return nonterminalSymbols;
    }

    public Set<String> getTerminalSymbols() {
        return terminalSymbols;
    }

    public Set<String> getIgnoredSymbols() {
        return ignoredSymbols;
    }

    public String getStartSymbol() {
        return startSymbol;
    }

    public List<String> getProductions() {
        return productions;
    }
}