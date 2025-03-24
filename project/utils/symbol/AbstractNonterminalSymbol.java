package project.utils.symbol;
import project.utils.parser.Production;

import java.util.*;

public class AbstractNonterminalSymbol extends AbstractSymbol {

    private boolean mNullable;

    private Set<AbstractTerminalSymbol> mFirstSet;

    private final Set<Production> mProductions = new HashSet<>();

    public AbstractNonterminalSymbol(String name) {
        setName(name);
        this.mFirstSet = new HashSet<>();
    }

    public boolean isNullable() {
        return mNullable;
    }

    public void setNullable(boolean nullable) {
        mNullable = nullable;
    }

    public Set<AbstractTerminalSymbol> getFirstSet() {
        return mFirstSet;
    }

    public void setFirstSet(Set<AbstractTerminalSymbol> firstSet) {
        mFirstSet = firstSet;
    }

    public Set<Production> getProductions() {
        return mProductions;
    }

    @Override
    public int getType() {
        return NONTERMINAL;
    }
}
