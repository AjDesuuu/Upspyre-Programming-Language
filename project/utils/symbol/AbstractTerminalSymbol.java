package project.utils.symbol;

public class AbstractTerminalSymbol extends AbstractSymbol {

    public static final String NULL = "null";

    public static final String END = "EOF";

    public AbstractTerminalSymbol(String name) {
        setName(name);
    }

    public static AbstractTerminalSymbol Null() {
        return new AbstractTerminalSymbol(NULL);
    }

    public static AbstractTerminalSymbol End() {
        return new AbstractTerminalSymbol(END);
    }

    @Override
    public int getType() {
        return TERMINAL;
    }
}
