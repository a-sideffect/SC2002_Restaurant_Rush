package rush.domain;

/** A Critic: impatient (loses 12 satisfaction per turn) and always orders the most expensive dish. */
public class CriticCustomer extends Customer {
    public CriticCustomer(int id, int arrivalTurn) {
        super(id, arrivalTurn);
    }

    @Override
    public int getSatisfactionLossPerTurn() { return 12; }

    @Override
    public String getTypeLabel() { return "Critic"; }

    /** The highest-priced item; on a tie, the one listed first. */
    @Override
    public MenuItem chooseDish(Menu menu) {
        MenuItem best = menu.itemAt(0);
        for (MenuItem item : menu.getItems()) {
            if (item.getPriceCents() > best.getPriceCents()) {
                best = item;
            }
        }
        return best;
    }
}
