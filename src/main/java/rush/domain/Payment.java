package rush.domain;

/** Record of one payment collected by the Cashier. */
public record Payment(Customer customer, int amountCents, int recordedSatisfaction, int tableNumber) { }
