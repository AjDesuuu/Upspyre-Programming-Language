package project.utils.parser;
import project.utils.exception.AnalysisException;
import project.utils.symbol.AbstractSymbol;
import project.utils.symbol.AbstractTerminalSymbol;

import java.util.ArrayList;
import java.util.List;

public class Production {

    private AbstractSymbol From;

    private List<AbstractSymbol> To;

    protected Production() {
    }

    public Production(AbstractSymbol from, List<AbstractSymbol> to) {
        From = from;
        To = to;
    }

    public Production(Production production) {
        From = production.From;
        To = new ArrayList<>();
        To.addAll(production.To);
    }

    /// Creates a new production from a string representation.
    public static Production fromString(String input, Grammar grammar) throws AnalysisException {
        final Production production = new Production();
        if (!input.contains("::=")) {
            throw new AnalysisException(AnalysisException.ILL_FORMED_PRODUCTION, null);
        }
        final String[] parts = input.split("::=");
        if (parts.length != 2) {
            throw new AnalysisException(AnalysisException.ILL_FORMED_PRODUCTION, null);
        }
        final String[] fromStr = parts[0].trim().split(" +");
        final String[] toStr = parts[1].trim().split(" +");
        if (fromStr.length != 1) {
            throw new AnalysisException(AnalysisException.ILL_FORMED_PRODUCTION_LEFT, null);
        }
        try {
            production.From = grammar.getSymbolPool().getNonterminalSymbol(fromStr[0]);
        } catch (AnalysisException e) {
            throw new AnalysisException(AnalysisException.ILL_FORMED_PRODUCTION_LEFT, e);
        }
        production.To = new ArrayList<>();
        if (toStr.length == 1 && (toStr[0].equals(AbstractTerminalSymbol.NULL) || toStr[0].equals("ε"))) {
            production.To.add(grammar.getSymbolPool().getTerminalSymbol(AbstractTerminalSymbol.NULL));
            return production;
        } else if (toStr.length > 0 && toStr[0].length() > 0) {
            for (final String string : toStr) {
                try {
                    production.To.add(grammar.getSymbolPool().getSymbol(string));
                } catch (AnalysisException e) {
                    throw new AnalysisException(String.format(AnalysisException.SYMBOL_NOT_EXIST, string), e);
                }
            }
            return production;
        } else {
            throw new AnalysisException(AnalysisException.ILL_FORMED_PRODUCTION_RIGHT, null);
        }
    }

    public AbstractSymbol from() {
        return From;
    }

    public List<AbstractSymbol> to() {
        return To;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Production) {
            final Production production = (Production) obj;
            if (!From.equals(production.From) || To.size() != production.To.size()) {
                return false;
            }
            for (int i = 0; i < To.size(); i++) {
                if (!To.get(i).equals(production.To.get(i))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        result.append("Production: ");
        result.append(From.toString());
        result.append(" ->");
        for (final AbstractSymbol abstractSymbol : To) {
            result.append(" ");
            result.append(abstractSymbol.toString());
        }
        return result.toString();
    }

    
    @Override
    // Returns a hash code for the production.
    public int hashCode() {
        int hash = From.hashCode();
        for (final AbstractSymbol abstractSymbol : To) {
            hash ^= abstractSymbol.hashCode();
        }
        return hash;
    }
}
