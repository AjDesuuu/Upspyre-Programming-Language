package project.utils.symbol;

public abstract class AbstractSymbol {

    public static final int TERMINAL = 0x01;

    public static final int NONTERMINAL = 0xff;

    private String symbolName;

    public String getName() {
        return symbolName;
    }

    public void setName(String name) {
        symbolName = name;
    }

    public abstract int getType();

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AbstractSymbol) {
            final AbstractSymbol abstractSymbol = (AbstractSymbol) obj;
            return getType() == abstractSymbol.getType() && symbolName.equals(abstractSymbol.symbolName);
        }
        return false;
    }

    @Override
    public String toString() {
        return (getType() == TERMINAL ? "Terminal symbol: " : "Nonterminal symbol: ") + symbolName;
    }

    @Override
    public int hashCode() {
        return symbolName.hashCode();
    }
}
