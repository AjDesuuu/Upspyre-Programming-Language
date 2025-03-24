package project.utils.parser;

import project.utils.symbol.Symbol;


import java.util.ArrayList;
import java.util.List;

public class ParseTreeNode {

    private final String mId = IDGen.next();

    private final Symbol mSymbol;

    private final List<ParseTreeNode> mChildren = new ArrayList<>();

    public ParseTreeNode(Symbol symbol) {
        mSymbol = symbol;
    }

    public String getId() {
        return mId;
    }

    public Symbol getSymbol() {
        return mSymbol;
    }

    public List<ParseTreeNode> getChildren() {
        return mChildren;
    }

    public void addChildren(ParseTreeNode node) {
        mChildren.add(node);
    }
}
