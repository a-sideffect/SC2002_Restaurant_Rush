package rush.domain;

/** A command for the Waiter, built by the UI and executed by the domain. */
public interface WaiterTask {
    ActionResult execute(Waiter w, Restaurant r, int turn);

    String describe();
}
