package project.utils.parser;
import project.utils.symbol.AbstractSymbol;
import project.utils.symbol.AbstractTerminalSymbol;

/**
 * Represents an LR(1) item in the parser.
 * An item consists of a production rule, a position marker (dot), and a lookahead symbol.
 */

public class Item {

    private int Dot = 0;

    private final Production Production;

    private final AbstractTerminalSymbol LookAhead;

    /**
     * Creates a new item with the specified production and lookahead.
     * 
     * @param production The production rule
     * @param lookAhead The lookahead terminal symbol
     */

    public Item(Production production, AbstractTerminalSymbol lookAhead) {
        Production = production;
        LookAhead = lookAhead;
    }

    /// Returns the next symbol after the dot in the production rule.
    public AbstractSymbol getNextSymbol() {
        return Production.to().get(Dot);
    }

    public int getDot() {
        return Dot;
    }

    public Production getProduction() {
        return Production;
    }

    public AbstractTerminalSymbol getLookAhead() {
        return LookAhead;
    }

    /// Returns true if the item has not reached the end of the production rule.

    public boolean isNotEnded() {
        return Dot < Production.to().size() && !Production.to().get(0).getName().equals(AbstractTerminalSymbol.NULL);
    }

    // Creates a new item with the dot moved one position to the right.
    public Item getNextItem() {
        final Item item = new Item(Production, LookAhead);
        item.Dot = Dot + 1;
        return item;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Item) {
            final Item item = (Item) obj;
            return item.Dot == Dot && item.Production.equals(Production) && item.LookAhead.equals(LookAhead);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Production.hashCode() ^ Dot;
    }

    @Override
    public String toString() {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(Production.from());
        stringBuilder.append(" ->");
        for (int i = 0; i < Dot; ++i) {
            stringBuilder.append(" ");
            stringBuilder.append(Production.to().get(i));
        }
        stringBuilder.append(" ·");
        for (int i = Dot; i < Production.to().size(); i++) {
            stringBuilder.append(" ");
            stringBuilder.append(Production.to().get(i));
        }
        stringBuilder.append(", ");
        stringBuilder.append(LookAhead);
        return stringBuilder.toString();
    }
}
