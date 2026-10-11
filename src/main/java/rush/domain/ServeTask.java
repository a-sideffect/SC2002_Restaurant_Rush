package rush.domain;

/** Waiter task: serve a customer's READY order. */
public record ServeTask(int customerId) implements WaiterTask {
    @Override
    public ActionResult execute(Waiter w, Restaurant r, int turn) {
        return w.serve(r, customerId, turn);
    }

    @Override
    public String describe() { return "serve C" + customerId; }
}
