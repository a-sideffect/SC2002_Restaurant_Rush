package rush.domain;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * A restaurant customer. Subclasses vary behaviour by overriding the polymorphic methods,
 * so no other class ever needs to check which kind of customer it is dealing with.
 */
public abstract class Customer {
    public static final int MAX_SATISFACTION = 100;

    private final int id;
    private final int arrivalTurn;
    private int satisfaction = MAX_SATISFACTION;
    private CustomerStatus status = CustomerStatus.WAITING;
    private Order order;
    private Table table;
    private int servedTurn = -1;
    private int readyToPayTurn = -1;
    private int recordedSatisfaction = -1;

    protected Customer(int id, int arrivalTurn) {
        this.id = id;
        this.arrivalTurn = arrivalTurn;
    }

    // ---- polymorphic behaviour ----

    /** Satisfaction lost at the end of each turn while awaiting service. */
    public abstract int getSatisfactionLossPerTurn();

    /** Human-readable customer type, e.g. "Regular". */
    public abstract String getTypeLabel();

    /**
     * Rule F1: customer Cn orders menu item (n - 1) mod menuSize, so the base menu gives
     * Salad, Burger, Pasta, Salad, Burger, Pasta for C1..C6.
     */
    public MenuItem chooseDish(Menu menu) {
        return menu.itemAt((id - 1) % menu.size());
    }

    /** The bill for a dish at the given list price. Regular customers pay full price. */
    public int adjustBill(int baseCents) {
        return baseCents;
    }

    // ---- queries ----

    public int getId() { return id; }
    public String getLabel() { return "C" + id; }
    public int getArrivalTurn() { return arrivalTurn; }
    public int getSatisfaction() { return satisfaction; }
    public CustomerStatus getStatus() { return status; }
    public Optional<Order> getOrder() { return Optional.ofNullable(order); }
    public Optional<Table> getTable() { return Optional.ofNullable(table); }
    public int getServedTurn() { return servedTurn; }
    public int getReadyToPayTurn() { return readyToPayTurn; }

    /** Satisfaction recorded at payment time; empty until the customer has paid. */
    public OptionalInt getRecordedSatisfaction() {
        return recordedSatisfaction < 0 ? OptionalInt.empty() : OptionalInt.of(recordedSatisfaction);
    }

    // ---- package-private transitions; an illegal call is a programmer error ----

    void seatAt(Table t) {
        require(CustomerStatus.WAITING);
        this.table = t;
        this.status = CustomerStatus.SEATED;
    }

    void placeOrder(Order o) {
        require(CustomerStatus.SEATED);
        this.order = o;
        this.status = CustomerStatus.ORDERED;
    }

    void receiveFood(int turn) {
        require(CustomerStatus.ORDERED);
        this.servedTurn = turn;
        this.status = CustomerStatus.EATING;
    }

    void becomeReadyToPay(int turn) {
        require(CustomerStatus.EATING);
        this.readyToPayTurn = turn;
        this.status = CustomerStatus.READY_TO_PAY;
    }

    void pay() {
        require(CustomerStatus.READY_TO_PAY);
        this.recordedSatisfaction = satisfaction;
        this.table = null;
        this.status = CustomerStatus.PAID;
    }

    void loseSatisfaction(int amount) {
        satisfaction = Math.max(0, satisfaction - amount);
    }

    void leave() {
        if (!status.isAwaitingService()) {
            throw new IllegalStateException(getLabel() + " cannot leave from " + status);
        }
        this.table = null;
        this.status = CustomerStatus.LEFT;
    }

    private void require(CustomerStatus expected) {
        if (status != expected) {
            throw new IllegalStateException(getLabel() + " is " + status + ", expected " + expected);
        }
    }

    @Override
    public String toString() {
        return getLabel() + " (" + getTypeLabel() + ", sat " + satisfaction + ", " + status + ")";
    }
}
