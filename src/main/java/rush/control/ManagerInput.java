package rush.control;

import rush.domain.ChefTask;
import rush.domain.WaiterTask;

/**
 * Source of the manager's decisions. The controller depends on this abstraction, so the same
 * game runs from the console, from a replay script, or from a test.
 */
public interface ManagerInput {
    /** Returns a syntactically valid Waiter task; help/status/menu are handled inside. */
    WaiterTask nextWaiterTask(int turn);

    ChefTask nextChefTask(int turn);
}
