package rush.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The restaurant's state: tables, the waiting queue, all customers and the scoreboard.
 * It also owns the phases that need no staff member (arrivals, eating, satisfaction loss).
 * It never prints or reads input; it returns values for the view to format.
 */
public class Restaurant {
    private final GameConfig config;
    private final List<Table> tables = new ArrayList<>();
    private final List<Customer> queue = new ArrayList<>();
    private final List<Customer> customers = new ArrayList<>();
    private final Scoreboard scoreboard = new Scoreboard();

    public Restaurant(GameConfig config) {
        this.config = config;
        for (int i = 1; i <= config.tableCount(); i++) {
            tables.add(new Table(i));
        }
    }

    // ---- queries ----

    public GameConfig getConfig() { return config; }
    public Menu getMenu() { return config.menu(); }
    public Scoreboard getScoreboard() { return scoreboard; }
    public List<Table> getTables() { return Collections.unmodifiableList(tables); }
    public List<Customer> getQueue() { return Collections.unmodifiableList(queue); }
    public List<Customer> getAllCustomers() { return Collections.unmodifiableList(customers); }

    public List<Table> getFreeTables() {
        return tables.stream().filter(Table::isFree).toList();
    }

    public Optional<Customer> findCustomer(int id) {
        return customers.stream().filter(c -> c.getId() == id).findFirst();
    }

    /** Orders the Chef can still work on (PLACED or COOKING), in customer arrival order. */
    public List<Order> getCookableOrders() {
        return customers.stream()
                .flatMap(c -> c.getOrder().stream())
                .filter(Order::isCookable)
                .toList();
    }

    /** READY orders the Waiter can serve, in customer arrival order. */
    public List<Order> getReadyOrders() {
        return customers.stream()
                .filter(c -> c.getStatus() == CustomerStatus.ORDERED)
                .flatMap(c -> c.getOrder().stream())
                .filter(Order::isReady)
                .toList();
    }

    /** Customers in READY_TO_PAY, oldest first (ties broken by lower id). */
    public List<Customer> getReadyToPay() {
        return customers.stream()
                .filter(c -> c.getStatus() == CustomerStatus.READY_TO_PAY)
                .sorted(Comparator.comparingInt(Customer::getReadyToPayTurn)
                        .thenComparingInt(Customer::getId))
                .toList();
    }

    // ---- phases ----

    /** Step 1 (R1.*): creates this turn's arrivals and appends them to the queue. */
    public List<Customer> admitArrivals(int turn) {
        List<Customer> arrived = new ArrayList<>();
        for (ArrivalSchedule.ScheduledArrival a : config.arrivals().arrivalsAt(turn)) {
            Customer c = a.factory().create(customers.size() + 1, turn);
            customers.add(c);
            queue.add(c);
            arrived.add(c);
        }
        return arrived;
    }

    /**
     * Rule R2.2: seats a queued customer at a free table. A null table number means the
     * lowest-numbered free table. Shared by every staff member who can seat customers.
     */
    public ActionResult seatCustomer(int customerId, Integer tableNoOrNull) {
        Optional<Customer> found = findCustomer(customerId);
        if (found.isEmpty()) {
            return ActionResult.fail("There is no customer C" + customerId + ".");
        }
        Customer c = found.get();
        if (c.getStatus() != CustomerStatus.WAITING) {
            return ActionResult.fail(c.getLabel() + " is not waiting in the queue (status: "
                    + c.getStatus() + ").");
        }
        List<Table> free = getFreeTables();
        if (free.isEmpty()) {
            return ActionResult.fail("No free table. A table frees up when its customer pays or leaves.");
        }
        Table target;
        if (tableNoOrNull == null) {
            target = free.get(0);
        } else {
            Optional<Table> t = tables.stream().filter(x -> x.getNumber() == tableNoOrNull).findFirst();
            if (t.isEmpty()) {
                return ActionResult.fail("There is no table T" + tableNoOrNull + ".");
            }
            if (!t.get().isFree()) {
                Customer occupant = t.get().getOccupant().orElseThrow();
                return ActionResult.fail("Table " + tableNoOrNull + " is occupied by " + occupant.getLabel()
                        + ". Free tables: " + describeTableNumbers(free) + ".");
            }
            target = t.get();
        }
        queue.remove(c);
        target.occupy(c);
        c.seatAt(target);
        return ActionResult.ok("Waiter seats " + c.getLabel() + " at T" + target.getNumber() + ".");
    }

    /** Step 4b (R4.4): customers served on an earlier turn finish eating. */
    public List<Customer> finishEating(int turn) {
        List<Customer> done = new ArrayList<>();
        for (Customer c : customers) {
            if (c.getStatus() == CustomerStatus.EATING && c.getServedTurn() < turn) {
                c.becomeReadyToPay(turn);
                done.add(c);
            }
        }
        return done;
    }

    /** Step 5 (R5.*): applies satisfaction loss and returns the customers who left. */
    public List<Customer> applyEndOfTurnLoss(int turn) {
        List<Customer> departed = new ArrayList<>();
        List<Customer> awaiting = customers.stream()
                .filter(c -> c.getStatus().isAwaitingService())
                .collect(Collectors.toCollection(ArrayList::new));
        for (Customer c : awaiting) {
            c.loseSatisfaction(c.getSatisfactionLossPerTurn());
            if (c.getSatisfaction() == 0) {
                handleDeparture(c);
                departed.add(c);
            }
        }
        return departed;
    }

    /** Rule R4.3, called by the Cashier. */
    void completePayment(Customer c) {
        Order order = c.getOrder().orElseThrow();
        Table table = c.getTable().orElseThrow();
        c.pay();
        order.markPaid();
        table.free();
        scoreboard.recordPayment(order.getLockedPriceCents(), c.getRecordedSatisfaction().orElseThrow());
    }

    private void handleDeparture(Customer c) {
        c.getOrder().ifPresent(o -> {
            if (o.isCookable() || o.isReady()) {
                o.cancel();
            }
        });
        c.getTable().ifPresent(Table::free);
        queue.remove(c);
        c.leave();
        scoreboard.recordDeparture();
    }

    private static String describeTableNumbers(List<Table> list) {
        return list.stream().map(t -> String.valueOf(t.getNumber())).collect(Collectors.joining(", "));
    }
}
