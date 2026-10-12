package rush.domain;

/** A VIP: patient (loses 5 satisfaction per turn) and gets 10% off the bill. */
public class VIPCustomer extends Customer {
    public static final int DISCOUNT_PERCENT = 10;

    public VIPCustomer(int id, int arrivalTurn) {
        super(id, arrivalTurn);
    }

    @Override
    public int getSatisfactionLossPerTurn() { return 5; }

    @Override
    public String getTypeLabel() { return "VIP"; }

    @Override
    public int adjustBill(int baseCents) {
        return Money.discounted(baseCents, DISCOUNT_PERCENT);
    }
}
