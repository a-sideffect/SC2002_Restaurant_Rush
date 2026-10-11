package rush.domain;

/** The standard customer: loses 8 satisfaction per turn while awaiting service. */
public class RegularCustomer extends Customer {
    public RegularCustomer(int id, int arrivalTurn) {
        super(id, arrivalTurn);
    }

    @Override
    public int getSatisfactionLossPerTurn() { return 8; }

    @Override
    public String getTypeLabel() { return "Regular"; }
}
