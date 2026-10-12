package rush.domain;

/** Chef task: advance a customer's order by one preparation unit. */
public record CookTask(int customerId) implements ChefTask {
    @Override
    public ActionResult execute(Chef c, Restaurant r, int turn) {
        return c.cook(r, customerId);
    }

    @Override
    public String describe() { return "cook C" + customerId; }
}
