package project.utils.symbol;

public class NonterminalSymbol extends Symbol {

    public NonterminalSymbol(AbstractNonterminalSymbol abstractNonterminalSymbol) {
        super(abstractNonterminalSymbol.getName());
        setAbstractSymbol(abstractNonterminalSymbol);
    }
}
