package project.utils.parser;

import project.utils.exception.AnalysisException;
import project.utils.symbol.AbstractNonterminalSymbol;
import project.utils.symbol.AbstractSymbol;
import project.utils.symbol.AbstractTerminalSymbol;
import project.utils.symbol.SymbolPool;

import java.util.*;

/**
 * Represents a context-free grammar and provides methods for parsing and analysis.
 * Handles grammar augmentation, FIRST set calculation, and LR parse table construction.
 */

public class Grammar {

    //augmented grammar start symbol
    public static final String START_SYMBOL = "_S";

    private AbstractNonterminalSymbol StartSymbol;

    //pool of symbols used in the grammar
    private final SymbolPool SymbolPool;

    private List<Production> Productions;

    private ParseTable ParseTable;

    // Constructor for Grammar class that initializes the grammar with a configuration object.
    public Grammar(Config config) throws AnalysisException {
        SymbolPool = new SymbolPool(config.getTerminalSymbols(), config.getNonterminalSymbols());
        initProductions(config.getProductions(), config.getStartSymbol());
    }

    public Grammar(Set<String> terminalSymbols, Set<String> nonterminalSymbols, String startSymbol)
            throws AnalysisException {
        //System.out.println("Terminal Symbols: " + terminalSymbols);
        //System.out.println("Non-terminal Symbols: " + nonterminalSymbols);
        //System.out.println("Start Symbol: " + startSymbol);
        SymbolPool = new SymbolPool(terminalSymbols, nonterminalSymbols);
        StartSymbol = SymbolPool.getNonterminalSymbol(startSymbol);
        Productions = new ArrayList<>();
        System.out.println("Start Symbol Object: " + StartSymbol);
    }

    public AbstractNonterminalSymbol getStartSymbol() {
        return StartSymbol;
    }

    public SymbolPool getSymbolPool() {
        return SymbolPool;
    }

    public List<Production> getProductions() {
        return Productions;
    }

    public void setProductions(List<? extends Production> productions) {
        this.Productions = new ArrayList<>(productions);
    }

    public ParseTable getParseTable() {
        return ParseTable;
    }

    /**
     * Initializes the LR parse table for this grammar.
     * Uses the LR(1) algorithm to construct states and transitions.
     * 
     * @throws AnalysisException If there are errors during parse table construction
     */

    public void initParseTable() throws AnalysisException {
        initSymbolProductions();
        initSymbolFirstSet();
        final List<ParseState> stateList = new ArrayList<>();
        final Map<ParseState, Integer> stateMap = new HashMap<>();

        // Create the start state with items derived from the start symbol

        final ParseState startState = new ParseState(this);
        for (final Production production : Productions) {
            if (production.from().equals(StartSymbol)) {
                startState.addItem(new Item(production, SymbolPool.getTerminalSymbol(AbstractTerminalSymbol.END)));
            }
        }
        startState.makeClosure();
        stateList.add(startState);
        stateMap.put(startState, 0);

        ParseTable parseTable = new ParseTable(this);
        for (int i = 0; i < stateList.size(); i++) {
            final Set<Item> items = stateList.get(i).getItems();
            final Map<AbstractSymbol, Set<Item>> groupedItems = new HashMap<>();

            // Group items by the next symbol
            for (final Item item : items) {
                if (item.isNotEnded()) {
                    if (!groupedItems.containsKey(item.getNextSymbol())) {
                        groupedItems.put(item.getNextSymbol(), new HashSet<>());
                    }
                    groupedItems.get(item.getNextSymbol()).add(item);
                } else {
                    if (!groupedItems.containsKey(SymbolPool.getTerminalSymbol(AbstractTerminalSymbol.NULL))) {
                        groupedItems.put(SymbolPool.getTerminalSymbol(AbstractTerminalSymbol.NULL), new HashSet<>());
                    }
                    groupedItems.get(SymbolPool.getTerminalSymbol(AbstractTerminalSymbol.NULL)).add(item);
                }
            }

            // Process each group of items to create transitions
            for (final AbstractSymbol abstractSymbol : groupedItems.keySet()) {
                if (abstractSymbol.equals(SymbolPool.getTerminalSymbol(AbstractTerminalSymbol.NULL))) {
                    for (final Item item : groupedItems.get(abstractSymbol)) {
                        parseTable.addTransition(i, item.getLookAhead(), item.getProduction());
                        if (item.getLookAhead().equals(SymbolPool.getTerminalSymbol(AbstractTerminalSymbol.END))
                                && item.getProduction().from().equals(StartSymbol)) {
                            parseTable.setAcceptState(i);
                            System.out.println("Accept State: " + i);
                        }
                    }
                } else {
                    // Create a new state by shifting over the next symbol
                    final ParseState parseState = new ParseState(this);
                    for (final Item item : groupedItems.get(abstractSymbol)) {
                        parseState.addItem(item.getNextItem());
                    }
                    parseState.makeClosure();
                    if (!stateMap.containsKey(parseState)) {
                        stateMap.put(parseState, stateList.size());
                        stateList.add(parseState);
                    }
                    parseTable.addTransition(i, abstractSymbol, stateMap.get(parseState));
                }
            }
        }
        ParseTable = parseTable;
    }

    /**
     * Initializes the production rules from string representations.
     * 
     * @param prodStrList List of production rule strings
     * @param startSymbol The start symbol for the grammar
     * @throws AnalysisException If a production rule is malformed
     */

    public void initProductions(List<String> prodStrList, String startSymbol) throws AnalysisException {
        Productions = new ArrayList<>();
        for (final String prodStr : prodStrList) {
            //System.out.println("Parsing Production: " + prodStr);

            Production production = Production.fromString(prodStr, this);
            Productions.add(production);
            //System.out.println("Parsed Production: " + production);
        }
        augmentGrammar(startSymbol);
    }

    /**
     * Augments the grammar by adding a new start symbol and production.
     * This ensures the grammar has a single production from the start symbol.
     * 
     * @param startSymbol The original start symbol
     * @throws AnalysisException If the start symbol does not exist in the grammar
     */

    private void augmentGrammar(String startSymbol) throws AnalysisException {
        if (!SymbolPool.getNonterminalSymbolNames().contains(startSymbol)) {
            throw new AnalysisException(AnalysisException.START_SYMBOL_NOT_TERMINAL, null);
        }
        final AbstractNonterminalSymbol oldStartSymbol = SymbolPool.getNonterminalSymbol(startSymbol);
        final AbstractNonterminalSymbol newStartSymbol = new AbstractNonterminalSymbol(START_SYMBOL);
        SymbolPool.addNonterminalSymbol(newStartSymbol);
        final List<AbstractSymbol> to = new ArrayList<>();
        to.add(oldStartSymbol);
        Productions.add(0, new Production(newStartSymbol, to));
        StartSymbol = newStartSymbol;

        //System.out.println("Augmented Grammar:");
        //System.out.println(this.toString());
    }

    // Initializes the productions for each non-terminal symbol in the grammar.
    private void initSymbolProductions() {
        for (final Production production : Productions) {
            final AbstractNonterminalSymbol from = (AbstractNonterminalSymbol) production.from();
            from.getProductions().add(production);

            //System.out.println("Non-terminal Symbol: " + from.getName() + ", Productions: " + from.getProductions());
        }

    }

    // Initializes the nullable symbols in the grammar.

    private void initSymbolNullable() throws AnalysisException {
        Set<AbstractSymbol> tmpNullableSymbols = new HashSet<>();
        final Set<AbstractSymbol> nullableSymbols = new HashSet<>();
        tmpNullableSymbols.add(SymbolPool.getTerminalSymbol(AbstractTerminalSymbol.NULL));

        final Map<AbstractSymbol, Set<Production>> symbolProductions = new HashMap<>();
        for (final Production production : Productions) {
            final AbstractSymbol from = production.from();
            if (!symbolProductions.containsKey(from)) {
                symbolProductions.put(from, new HashSet<>());
            }
            symbolProductions.get(from).add(new Production(production));
        }
        
        //Iteratively find nullable symbols
        while (!tmpNullableSymbols.isEmpty()) {
            final Set<AbstractSymbol> nextTmpNullableSymbols = new HashSet<>();
            for (final AbstractSymbol from : symbolProductions.keySet()) {
                for (final Production production : symbolProductions.get(from)) {
                    final List<AbstractSymbol> to = production.to();
                    for (final AbstractSymbol abstractSymbol : tmpNullableSymbols) {
                        to.remove(abstractSymbol);
                    }
                    if (to.size() <= 0) {
                        nextTmpNullableSymbols.add(from);
                        break;
                    }
                }
            }
            symbolProductions.keySet().removeAll(nextTmpNullableSymbols);
            nullableSymbols.addAll(nextTmpNullableSymbols);
            tmpNullableSymbols = nextTmpNullableSymbols;
        }

        //Mark all nullable symbols in the grammar
        for (final AbstractSymbol abstractSymbol : nullableSymbols) {
            ((AbstractNonterminalSymbol) abstractSymbol).setNullable(true);
        }

    }

    /**
     * Computes the FIRST sets for all non-terminal symbols in the grammar.
     * The FIRST set contains all terminals that can begin strings derived from a symbol.
     * 
     * @throws AnalysisException If there's an error during computation
     */

    private void initSymbolFirstSet() throws AnalysisException {
        initSymbolNullable();
        initSymbolFollowSet();

        //System.out.println("Nullable Symbols:");
        for (AbstractNonterminalSymbol symbol : SymbolPool.getNonterminalSymbols()) {
            if (symbol.isNullable()) {
                //System.out.println(symbol.getName() + " is nullable");
            }
        }

        // Initialize FIRST sets for all non-terminal symbols
        for (AbstractNonterminalSymbol symbol : SymbolPool.getNonterminalSymbols()) {
            symbol.setFirstSet(new HashSet<>()); // Ensure the FIRST set is initialized
        }

        // Track symbol connections for propagating FIRST sets
        final Map<AbstractNonterminalSymbol, Set<AbstractNonterminalSymbol>> connections = new HashMap<>();
        final Map<AbstractNonterminalSymbol, Set<AbstractTerminalSymbol>> firstSets = new HashMap<>();
        final Map<AbstractNonterminalSymbol, Set<AbstractTerminalSymbol>> tmpFirstSets = new HashMap<>();
        final AbstractTerminalSymbol nullSymbol = SymbolPool.getTerminalSymbol(AbstractTerminalSymbol.NULL);

        // Analyze productions to build initial FIRST sets
        for (final Production production : Productions) {
            for (final AbstractSymbol abstractSymbol : production.to()) {
                final AbstractNonterminalSymbol from = (AbstractNonterminalSymbol) production.from();
                if (abstractSymbol.getType() == AbstractSymbol.NONTERMINAL) {
                    final AbstractNonterminalSymbol abstractNonterminalSymbol = (AbstractNonterminalSymbol) abstractSymbol;
                    if (!connections.containsKey(abstractNonterminalSymbol)) {
                        connections.put(abstractNonterminalSymbol, new HashSet<>());
                    }
                    connections.get(abstractNonterminalSymbol).add(from);
                    if (!(abstractNonterminalSymbol.isNullable())) {
                        break;
                    }
                } else if (!abstractSymbol.equals(nullSymbol)) {
                    final AbstractTerminalSymbol abstractTerminalSymbol = (AbstractTerminalSymbol) abstractSymbol;
                    if (!tmpFirstSets.containsKey(from)) {
                        tmpFirstSets.put(from, new HashSet<>());
                    }
                    tmpFirstSets.get(from).add(abstractTerminalSymbol);
                    break;
                } else {
                    break;
                }
            }
        }

        // Propagate FIRST sets through symbol connections until fixed point
        while (!tmpFirstSets.isEmpty()) {
            final Map<AbstractNonterminalSymbol, Set<AbstractTerminalSymbol>> newTmpFirstSets = new HashMap<>();
            for (final AbstractNonterminalSymbol abstractNonterminalSymbol : tmpFirstSets.keySet()) {
                if (!firstSets.containsKey(abstractNonterminalSymbol)) {
                    firstSets.put(abstractNonterminalSymbol, new HashSet<>());
                }
                firstSets.get(abstractNonterminalSymbol).addAll(tmpFirstSets.get(abstractNonterminalSymbol));
                if (connections.containsKey(abstractNonterminalSymbol)) {
                    for (final AbstractNonterminalSymbol target : connections.get(abstractNonterminalSymbol)) {
                        if (!newTmpFirstSets.containsKey(target)) {
                            newTmpFirstSets.put(target, new HashSet<>());
                        }
                        newTmpFirstSets.get(target).addAll(tmpFirstSets.get(abstractNonterminalSymbol));
                    }
                }
            }
            tmpFirstSets.clear();
            for (final AbstractNonterminalSymbol abstractNonterminalSymbol : newTmpFirstSets.keySet()) {
                if (firstSets.containsKey(abstractNonterminalSymbol)) {
                    newTmpFirstSets.get(abstractNonterminalSymbol).removeAll(firstSets.get(abstractNonterminalSymbol));
                }
                if (newTmpFirstSets.get(abstractNonterminalSymbol).size() > 0) {
                    tmpFirstSets.put(abstractNonterminalSymbol, newTmpFirstSets.get(abstractNonterminalSymbol));
                }
            }
        }

        // Finalize FIRST sets and handle nullable symbols
        for (final AbstractNonterminalSymbol abstractNonterminalSymbol : SymbolPool.getNonterminalSymbols()) {
            if (!firstSets.containsKey(abstractNonterminalSymbol)) {
                firstSets.put(abstractNonterminalSymbol, new HashSet<>());
            }
            if (abstractNonterminalSymbol.isNullable()) {
                firstSets.get(abstractNonterminalSymbol).add(nullSymbol);
            }
            abstractNonterminalSymbol.setFirstSet(firstSets.get(abstractNonterminalSymbol));
        }
        System.out.println("FIRST Sets:");
        System.out.println(getFirstSetsCSV());
        System.out.println("FOLLOW Sets:");
        System.out.println(getFollowSetsCSV());
    }

    @Override
    public String toString() {
        final StringBuilder stringBuilder = new StringBuilder("Productions in this grammar:");
        for (final Production production : Productions) {
            stringBuilder.append("\n");
            stringBuilder.append(production);
        }
        return stringBuilder.toString();
    }

    public String getFirstSetsCSV() {
        StringBuilder csv = new StringBuilder();
        csv.append("Nonterminal,FIRST Set\n");
        for (AbstractNonterminalSymbol symbol : SymbolPool.getNonterminalSymbols()) {
            csv.append(symbol.getName()).append(",");
            Set<AbstractTerminalSymbol> firstSet = symbol.getFirstSet();
            for (AbstractTerminalSymbol terminal : firstSet) {
                csv.append(terminal.getName()).append(" ");
            }
            csv.append("\n");
        }
        return csv.toString();
    }

    /**
     * Computes the FOLLOW sets for all non-terminal symbols in the grammar.
     */
    private void initSymbolFollowSet() throws AnalysisException {
        // Initialize FOLLOW sets
        Map<AbstractNonterminalSymbol, Set<AbstractTerminalSymbol>> followSets = new HashMap<>();
        for (AbstractNonterminalSymbol symbol : SymbolPool.getNonterminalSymbols()) {
            followSets.put(symbol, new HashSet<>());
        }
        
        // Add $ to FOLLOW of start symbol
        AbstractTerminalSymbol endSymbol = SymbolPool.getTerminalSymbol(AbstractTerminalSymbol.END);
        followSets.get(StartSymbol).add(endSymbol);
        
        boolean changed;
        do {
            changed = false;
            for (Production production : Productions) {
                AbstractNonterminalSymbol A = (AbstractNonterminalSymbol) production.from();
                List<AbstractSymbol> beta = production.to();
                
                // Traverse the right-hand side of the production
                for (int i = 0; i < beta.size(); i++) {
                    if (beta.get(i).getType() == AbstractSymbol.NONTERMINAL) {
                        AbstractNonterminalSymbol B = (AbstractNonterminalSymbol) beta.get(i);
                        
                        // Get FIRST of the symbols after B
                        Set<AbstractTerminalSymbol> firstBeta = new HashSet<>();
                        boolean nullable = true;
                        for (int j = i + 1; j < beta.size() && nullable; j++) {
                            if (beta.get(j).getType() == AbstractSymbol.NONTERMINAL) {
                                firstBeta.addAll(((AbstractNonterminalSymbol) beta.get(j)).getFirstSet());
                            } else {
                                firstBeta.add((AbstractTerminalSymbol) beta.get(j));
                            }
                            
                            // Remove ε from FIRST set (if present)
                            firstBeta.remove(SymbolPool.getTerminalSymbol(AbstractTerminalSymbol.NULL));
                            
                            // Check if current symbol is nullable
                            nullable = beta.get(j).getType() == AbstractSymbol.NONTERMINAL && 
                                    ((AbstractNonterminalSymbol) beta.get(j)).isNullable();
                        }
                        
                        // Add FIRST(beta) to FOLLOW(B)
                        int oldSize = followSets.get(B).size();
                        followSets.get(B).addAll(firstBeta);
                        if (followSets.get(B).size() > oldSize) {
                            changed = true;
                        }
                        
                        // If all symbols after B are nullable, add FOLLOW(A) to FOLLOW(B)
                        if (nullable) {
                            oldSize = followSets.get(B).size();
                            followSets.get(B).addAll(followSets.get(A));
                            if (followSets.get(B).size() > oldSize) {
                                changed = true;
                            }
                        }
                    }
                }
            }
        } while (changed);
        
        // Store the FOLLOW sets in the symbols
        for (AbstractNonterminalSymbol symbol : SymbolPool.getNonterminalSymbols()) {
            symbol.setFollowSet(followSets.get(symbol));
        }
    }

    public String getFollowSetsCSV() {
        StringBuilder csv = new StringBuilder();
        csv.append("Nonterminal,FOLLOW Set\n");
        for (AbstractNonterminalSymbol symbol : SymbolPool.getNonterminalSymbols()) {
            csv.append(symbol.getName()).append(",");
            Set<AbstractTerminalSymbol> followSet = symbol.getFollowSet();
            for (AbstractTerminalSymbol terminal : followSet) {
                csv.append(terminal.getName()).append(" ");
            }
            csv.append("\n");
        }
        return csv.toString();
    }

}
