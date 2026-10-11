package rush.domain;

import java.util.List;

/** A staff member. The start-of-turn board loops over staff, so new roles appear automatically. */
public abstract class Staff {
    private final String name;

    protected Staff(String name) { this.name = name; }

    public String getName() { return name; }

    /** Role label shown on the board, e.g. "Waiter". */
    public abstract String getRole();

    /** Short descriptions of what this staff member could do right now. */
    public abstract List<String> describeAvailableTasks(Restaurant r, int turn);
}
