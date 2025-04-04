package project.utils.symbol;

import project.utils.exception.AnalysisException;
import project.utils.parser.Grammar;

import java.util.*;

public class SymbolPool {

    private Map<String, AbstractTerminalSymbol> absTerminalSymbols;

    private Map<String, AbstractNonterminalSymbol> absNonterminalSymbols;

    public SymbolPool(Set<String> terminalSymbols, Set<String> nonterminalSymbols)
            throws AnalysisException {
        final Map<String, String> keywords = Map.of(
                AbstractTerminalSymbol.NULL, "an empty string",
                AbstractTerminalSymbol.END, "the end of a production",
                Grammar.START_SYMBOL, "the start symbol of the augmented grammar");
        for (final String name : keywords.keySet()) {
            if (terminalSymbols.contains(name) || nonterminalSymbols.contains(name)) {
                throw new AnalysisException(
                        String.format(AnalysisException.INVALID_NAME, name, keywords.get(name)), null);
            }
        }
        initTerminalSymbols(terminalSymbols);
        initNonterminalSymbols(nonterminalSymbols);
    }

    private void initTerminalSymbols(Set<String> terminalSymbols) {
        absTerminalSymbols = new HashMap<>();
        for (final String name : terminalSymbols) {
            absTerminalSymbols.put(name, new AbstractTerminalSymbol(name));
        }
        absTerminalSymbols.put(AbstractTerminalSymbol.NULL, AbstractTerminalSymbol.Null());
        absTerminalSymbols.put(AbstractTerminalSymbol.END, AbstractTerminalSymbol.End());
    }

    private void initNonterminalSymbols(Set<String> nonterminalSymbols) {
        absNonterminalSymbols = new HashMap<>();
        for (final String name : nonterminalSymbols) {
            absNonterminalSymbols.put(name, new AbstractNonterminalSymbol(name));
        }
    }

    public Set<AbstractTerminalSymbol> getTerminalSymbols() {
        return new HashSet<>(absTerminalSymbols.values());
    }

    public AbstractTerminalSymbol getTerminalSymbol(String name) throws AnalysisException {
        if (absTerminalSymbols.containsKey(name)) {
            return absTerminalSymbols.get(name);
        }
        throw new AnalysisException(String.format(AnalysisException.TERMINAL_SYMBOL_NOT_EXIST, name), null);
    }

    public Set<String> getNonterminalSymbolNames() {
        return absNonterminalSymbols.keySet();
    }

    public Set<AbstractNonterminalSymbol> getNonterminalSymbols() {
        return new HashSet<>(absNonterminalSymbols.values());
    }

    public AbstractNonterminalSymbol getNonterminalSymbol(String name) throws AnalysisException {
        if (absNonterminalSymbols.containsKey(name)) {
            return absNonterminalSymbols.get(name);
        }
        throw new AnalysisException(String.format(AnalysisException.NONTERMINAL_SYMBOL_NOT_EXIST, name), null);
    }

    public void addNonterminalSymbol(AbstractNonterminalSymbol abstractNonterminalSymbol) {
        if (!absNonterminalSymbols.containsKey(abstractNonterminalSymbol.getName())) {
            absNonterminalSymbols.put(abstractNonterminalSymbol.getName(), abstractNonterminalSymbol);
        }
    }

    public AbstractSymbol getSymbol(String name) throws AnalysisException {
        if (absTerminalSymbols.containsKey(name)) {
            return absTerminalSymbols.get(name);
        } else if (absNonterminalSymbols.containsKey(name)) {
            return absNonterminalSymbols.get(name);
        }
        throw new AnalysisException(String.format(AnalysisException.SYMBOL_NOT_EXIST, name), null);
    }
}
