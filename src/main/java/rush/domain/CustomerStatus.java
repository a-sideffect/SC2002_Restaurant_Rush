package rush.domain;

/** Lifecycle of a customer. */
public enum CustomerStatus {
    WAITING, SEATED, ORDERED, EATING, READY_TO_PAY, PAID, LEFT;

    /** True for the states in which a customer loses satisfaction each turn. */
    public boolean isAwaitingService() {
        return this == WAITING || this == SEATED || this == ORDERED;
    }
}
