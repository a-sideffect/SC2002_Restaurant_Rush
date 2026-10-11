package rush.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Seats customers, takes orders and serves food. At most one task per turn. */
public class Waiter extends Staff {
    private final PriceCalculator priceCalculator;

    public Waiter(String name, PriceCalculator priceCalculator) {
        super(name);
        this.priceCalculator = priceCalculator;
    }

    @Override
    public String getRole() { return "Waiter"; }

    /** R2.2: delegates to {@link Restaurant#seatCustomer} so the logic is not duplicated. */
    public ActionResult seat(Restaurant r, int customerId, Integer tableNo) {
        return r.seatCustomer(customerId, tableNo);
    }

    /** R2.3: the customer chooses the dish and the price is locked now. */
    public ActionResult takeOrder(Restaurant r, int customerId, int turn) {
        Optional<Customer> found = r.findCustomer(customerId);
        if (found.isEmpty()) {
            return ActionResult.fail("There is no customer C" + customerId + ".");
        }
        Customer c = found.get();
        if (c.getStatus() != CustomerStatus.SEATED) {
            if (c.getStatus() == CustomerStatus.ORDERED && c.getOrder().isPresent()) {
                Order o = c.getOrder().get();
                return ActionResult.fail(c.getLabel() + " has already ordered " + o.getItem().getName()
                        + " (" + o.getStatus() + " " + o.progressLabel() + ").");
            }
            if (c.getStatus() == CustomerStatus.WAITING) {
                return ActionResult.fail(c.getLabel() + " is still in the queue. Seat them first.");
            }
            return ActionResult.fail(c.getLabel() + " cannot order now (status: " + c.getStatus() + ").");
        }
        MenuItem dish = c.chooseDish(r.getMenu());
        int price = priceCalculator.quote(c, dish);
        c.placeOrder(new Order(c, dish, price, turn));
        return ActionResult.ok("Waiter takes " + c.getLabel() + "'s order: " + dish.getName() + " "
                + Money.format(price) + " (" + dish.getPrepUnits()
                + (dish.getPrepUnits() == 1 ? " unit)." : " units)."));
    }

    /** R2.4: only a READY order can be served. */
    public ActionResult serve(Restaurant r, int customerId, int turn) {
        Optional<Customer> found = r.findCustomer(customerId);
        if (found.isEmpty()) {
            return ActionResult.fail("There is no customer C" + customerId + ".");
        }
        Customer c = found.get();
        if (c.getStatus() != CustomerStatus.ORDERED || c.getOrder().isEmpty()) {
            return ActionResult.fail(c.getLabel() + " has no order waiting to be served (status: "
                    + c.getStatus() + ").");
        }
        Order o = c.getOrder().get();
        if (!o.isReady()) {
            return ActionResult.fail(c.getLabel() + "'s " + o.getItem().getName() + " is not READY ("
                    + o.progressLabel() + " units done).");
        }
        o.markServed();
        c.receiveFood(turn);
        r.getScoreboard().recordServed();
        return ActionResult.ok("Waiter serves " + o.getItem().getName() + " to " + c.getLabel() + ".");
    }

    @Override
    public List<String> describeAvailableTasks(Restaurant r, int turn) {
        List<String> tasks = new ArrayList<>();
        List<Table> free = r.getFreeTables();
        if (!free.isEmpty()) {
            for (Customer c : r.getQueue()) {
                tasks.add("seat " + c.getLabel() + " at T" + free.get(0).getNumber());
            }
        }
        for (Customer c : r.getAllCustomers()) {
            if (c.getStatus() == CustomerStatus.SEATED) {
                tasks.add("order " + c.getLabel());
            }
        }
        for (Order o : r.getReadyOrders()) {
            tasks.add("serve " + o.getCustomer().getLabel() + " (" + o.getItem().getName() + ")");
        }
        return tasks;
    }
}
