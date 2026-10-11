package rush.domain;

/** Do nothing this turn. Valid for both the Waiter and the Chef, and always succeeds. */
public final class WaitTask implements WaiterTask, ChefTask {
    public static final WaitTask INSTANCE = new WaitTask();

    private WaitTask() { }

    @Override
    public ActionResult execute(Waiter w, Restaurant r, int turn) {
        return ActionResult.ok("Waiter waits.");
    }

    @Override
    public ActionResult execute(Chef c, Restaurant r, int turn) {
        return ActionResult.ok("Chef waits.");
    }

    @Override
    public String describe() { return "wait"; }
}
