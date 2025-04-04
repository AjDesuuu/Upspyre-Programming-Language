package project.utils.parser;
import project.utils.exception.AnalysisException;
import project.utils.symbol.AbstractNonterminalSymbol;
import project.utils.symbol.AbstractSymbol;
import project.utils.symbol.AbstractTerminalSymbol;

import java.util.*;

public class ParseState {

    private final Set<Item> Items = new HashSet<>();

    private final Grammar Grammar;

    public ParseState(Grammar grammar) {
        Grammar = grammar;
    }

    public void addItem(Item item) {
        Items.add(item);
    }

    public Set<Item> getItems() {
        return Items;
    }

    public void makeClosure() {
        final List<Item> itemList = new ArrayList<>(Items);
        for (int i = 0; i < itemList.size(); i++) {
            final Item item = itemList.get(i);
            if (item.isNotEnded()) {
                final AbstractSymbol abstractSymbol = item.getNextSymbol();
                if (abstractSymbol.getType() == AbstractSymbol.NONTERMINAL) {
                    final List<AbstractSymbol> lookAheadSymbols = new ArrayList<>();
                    for (int j = item.getDot() + 1; j < item.getProduction().to().size(); j++) {
                        lookAheadSymbols.add(item.getProduction().to().get(j));
                    }
                    lookAheadSymbols.add(item.getLookAhead());
                    final Set<AbstractTerminalSymbol> headList = getHeadSet(lookAheadSymbols);
                    for (final Production production : ((AbstractNonterminalSymbol) abstractSymbol).getProductions()) {
                        for (final AbstractTerminalSymbol lookAheadSymbol : headList) {
                            final Item newItem = new Item(production, lookAheadSymbol);
                            if (!Items.contains(newItem)) {
                                Items.add(newItem);
                                itemList.add(newItem);
                            }
                        }
                    }
                }
            }
        }
    }

    private Set<AbstractTerminalSymbol> getHeadSet(List<AbstractSymbol> abstractSymbols) {
        final Set<AbstractTerminalSymbol> headSet = new HashSet<>();
        for (final AbstractSymbol abstractSymbol : abstractSymbols) {
            if (abstractSymbol.getType() == AbstractSymbol.NONTERMINAL) {
                headSet.addAll(((AbstractNonterminalSymbol) abstractSymbol).getFirstSet());
                if (!((AbstractNonterminalSymbol) abstractSymbol).isNullable()) {
                    break;
                }
            } else {
                headSet.add((AbstractTerminalSymbol) abstractSymbol);
                if (!abstractSymbol.getName().equals(AbstractTerminalSymbol.NULL)) {
                    break;
                }
            }
        }
        try {
            headSet.remove(Grammar.getSymbolPool().getTerminalSymbol(AbstractTerminalSymbol.NULL));
        } catch (AnalysisException e) {
            e.printStackTrace();
        }
        return headSet;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ParseState) {
            final ParseState parseState = (ParseState) obj;
            return Items.size() == parseState.Items.size() && Items.containsAll(parseState.Items);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        for (final Item item : Items) {
            hash ^= item.hashCode();
        }
        return hash;
    }

    @Override
    public String toString() {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Items in this state:\n");
        for (final Item item : Items) {
            stringBuilder.append(item);
            stringBuilder.append("\n");
        }
        return stringBuilder.toString();
    }
}
