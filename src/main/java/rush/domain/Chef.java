package rush.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Prepares one unit of one order per turn. */
public class Chef extends Staff {
    public Chef(String name) { super(name); }

    @Override
    public String getRole() { return "Chef"; }

    /** R3.1: advances the customer's order by one preparation unit. */
    public ActionResult cook(Restaurant r, int customerId) {
        Optional<Customer> found = r.findCustomer(customerId);
        if (found.isEmpty()) {
            return ActionResult.fail("There is no customer C" + customerId + ".");
        }
        Customer c = found.get();
        if (c.getOrder().isEmpty()) {
            return ActionResult.fail(c.getLabel() + " has no order to cook yet.");
        }
        Order o = c.getOrder().get();
        if (!o.isCookable()) {
            String reason = o.isReady() ? " is READY and does not need cooking."
                    : " is " + o.getStatus() + " and cannot be cooked.";
            return ActionResult.fail(c.getLabel() + "'s " + o.getItem().getName() + reason);
        }
        boolean nowReady = o.advanceOneUnit();
        String name = o.getItem().getName();
        return ActionResult.ok(nowReady
                ? "Chef finishes " + name + " for " + c.getLabel() + " (READY)."
                : "Chef cooks " + name + " for " + c.getLabel() + " (" + o.progressLabel() + ").");
    }

    @Override
    public List<String> describeAvailableTasks(Restaurant r, int turn) {
        List<String> tasks = new ArrayList<>();
        for (Order o : r.getCookableOrders()) {
            tasks.add("cook " + o.getCustomer().getLabel() + " " + o.getItem().getName()
                    + " (" + o.progressLabel() + ")");
        }
        return tasks;
    }
}
