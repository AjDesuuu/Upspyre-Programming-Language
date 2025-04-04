package project.utils.symbol;
import project.utils.parser.Production;

import java.util.*;

public class AbstractNonterminalSymbol extends AbstractSymbol {

    private boolean isNullable;

    private Set<AbstractTerminalSymbol> FirstSet;

    private final Set<Production> Prod = new HashSet<>();

    public AbstractNonterminalSymbol(String name) {
        setName(name);
        this.FirstSet = new HashSet<>();
    }

    public boolean isNullable() {
        return isNullable;
    }

    public void setNullable(boolean nullable) {
        isNullable = nullable;
    }

    public Set<AbstractTerminalSymbol> getFirstSet() {
        return FirstSet;
    }

    public void setFirstSet(Set<AbstractTerminalSymbol> firstSet) {
        FirstSet = firstSet;
    }

    public Set<Production> getProductions() {
        return Prod;
    }

    @Override
    public int getType() {
        return NONTERMINAL;
    }
}
