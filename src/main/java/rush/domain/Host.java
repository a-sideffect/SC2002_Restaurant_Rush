package rush.domain;

import java.util.List;

/** Seats at most one queued customer per turn, freeing the Waiter for orders and serving. */
public class Host extends Staff {
    public Host(String name) { super(name); }

    @Override
    public String getRole() { return "Host"; }

    /** Uses the same seating rules as the Waiter. */
    public ActionResult seat(Restaurant r, int customerId, Integer tableNo) {
        return r.seatCustomer(customerId, tableNo);
    }

    @Override
    public List<String> describeAvailableTasks(Restaurant r, int turn) {
        List<Table> free = r.getFreeTables();
        if (free.isEmpty()) {
            return List.of();
        }
        return r.getQueue().stream()
                .map(c -> "seat " + c.getLabel() + " at T" + free.get(0).getNumber())
                .toList();
    }
}
