package rush.domain;

/** One customer's order. The price is locked when the order is taken. */
public class Order {
    private final Customer customer;
    private final MenuItem item;
    private final int lockedPriceCents;
    private final int takenTurn;
    private int unitsDone;
    private int readyTurn = -1;
    private OrderStatus status = OrderStatus.PLACED;

    Order(Customer customer, MenuItem item, int lockedPriceCents, int takenTurn) {
        this.customer = customer;
        this.item = item;
        this.lockedPriceCents = lockedPriceCents;
        this.takenTurn = takenTurn;
    }

    public Customer getCustomer() { return customer; }
    public MenuItem getItem() { return item; }
    public int getLockedPriceCents() { return lockedPriceCents; }
    public int getTakenTurn() { return takenTurn; }
    public int getReadyTurn() { return readyTurn; }
    public int getUnitsDone() { return unitsDone; }
    public int getRemainingUnits() { return item.getPrepUnits() - unitsDone; }
    public OrderStatus getStatus() { return status; }

    /** True while the Chef still has units to prepare (PLACED or COOKING). */
    public boolean isCookable() {
        return status == OrderStatus.PLACED || status == OrderStatus.COOKING;
    }

    public boolean isReady() { return status == OrderStatus.READY; }

    /** Progress as "done/total", e.g. "1/2". */
    public String progressLabel() { return unitsDone + "/" + item.getPrepUnits(); }

    /** Advances preparation by one unit; returns true if the order just became READY. */
    boolean advanceOneUnit(int turn) {
        if (!isCookable()) {
            throw new IllegalStateException("Order is " + status + " and cannot be cooked");
        }
        unitsDone++;
        if (unitsDone == item.getPrepUnits()) {
            status = OrderStatus.READY;
            readyTurn = turn;
            return true;
        }
        status = OrderStatus.COOKING;
        return false;
    }

    void markServed() {
        requireStatus(OrderStatus.READY);
        status = OrderStatus.SERVED;
    }

    void markPaid() {
        requireStatus(OrderStatus.SERVED);
        status = OrderStatus.PAID;
    }

    /** Cancels an order that has not been served yet (the customer left). */
    void cancel() {
        if (!isCookable() && !isReady()) {
            throw new IllegalStateException("Order is " + status + " and cannot be cancelled");
        }
        status = OrderStatus.CANCELLED;
    }

    private void requireStatus(OrderStatus expected) {
        if (status != expected) {
            throw new IllegalStateException("Order is " + status + ", expected " + expected);
        }
    }
}
