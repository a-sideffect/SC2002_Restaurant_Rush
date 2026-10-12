package rush.domain;

/** Do nothing this turn. Valid for the Waiter, Host and Chef, and always succeeds. */
public final class WaitTask implements WaiterTask, HostTask, ChefTask {
    public static final WaitTask INSTANCE = new WaitTask();

    private WaitTask() { }

    @Override
    public ActionResult execute(Waiter w, Restaurant r, int turn) {
        return ActionResult.ok("Waiter waits.");
    }

    @Override
    public ActionResult execute(Host h, Restaurant r, int turn) {
        return ActionResult.ok("Host waits.");
    }

    @Override
    public ActionResult execute(Chef c, Restaurant r, int turn) {
        return ActionResult.ok("Chef waits.");
    }

    @Override
    public String describe() { return "wait"; }
}
