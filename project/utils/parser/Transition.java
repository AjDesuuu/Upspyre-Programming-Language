package project.utils.parser;

public class Transition {

    public static final int SHIFT = 0x01;

    public static final int GOTO = 0x02;

    public static final int REDUCE = 0x03;

    private final int Action;

    private int NextState;

    private Production ReduceProduction;

    private int Index;

    /// Constructor for shift and goto actions
    Transition(int action, int nextState) {
        Action = action;
        NextState = nextState;
    }

    /// Constructor for reduce action
    Transition(Production reduceProduction, int index) {
        Action = REDUCE;
        ReduceProduction = reduceProduction;
        Index = index;
    }

    public int getAction() {
        return Action;
    }

    /// Returns the next state for shift or goto actions.
    public Integer getNextState() {
        return Action == SHIFT || Action == GOTO ? NextState : null;
    }

    public Production getReduceProduction() {
        return ReduceProduction;
    }

    @Override
    public String toString() {
        if (Action == REDUCE) {
            return "r" + (Index);
        } else {
            return "s" + NextState;
        }
    }
}
