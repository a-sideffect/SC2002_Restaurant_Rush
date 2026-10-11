package rush.domain;

/** Waiter task: seat a queued customer, at a given table or the lowest free one (null). */
public record SeatTask(int customerId, Integer tableNo) implements WaiterTask {
    @Override
    public ActionResult execute(Waiter w, Restaurant r, int turn) {
        return w.seat(r, customerId, tableNo);
    }

    @Override
    public String describe() {
        return "seat C" + customerId + (tableNo == null ? "" : " at T" + tableNo);
    }
}
