package rush.domain;

/** Waiter task: take a seated customer's order. */
public record TakeOrderTask(int customerId) implements WaiterTask {
    @Override
    public ActionResult execute(Waiter w, Restaurant r, int turn) {
        return w.takeOrder(r, customerId);
    }

    @Override
    public String describe() { return "order C" + customerId; }
}
