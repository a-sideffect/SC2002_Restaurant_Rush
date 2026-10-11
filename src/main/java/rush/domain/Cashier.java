package rush.domain;

import java.util.List;
import java.util.Optional;

/** Automatic: collects payment from at most one eligible customer per turn, oldest first. */
public class Cashier extends Staff {
    public Cashier(String name) { super(name); }

    @Override
    public String getRole() { return "Cashier"; }

    /** R4.1-R4.3: charges the oldest customer who became READY_TO_PAY on an earlier turn. */
    public Optional<Payment> collect(Restaurant r, int turn) {
        Optional<Customer> next = nextEligible(r, turn);
        if (next.isEmpty()) {
            return Optional.empty();
        }
        Customer c = next.get();
        int amount = c.getOrder().orElseThrow().getLockedPriceCents();
        int tableNo = c.getTable().orElseThrow().getNumber();
        r.completePayment(c);
        return Optional.of(new Payment(c, amount, c.getRecordedSatisfaction().orElseThrow(), tableNo));
    }

    @Override
    public List<String> describeAvailableTasks(Restaurant r, int turn) {
        return nextEligible(r, turn)
                .map(c -> List.of("will charge " + c.getLabel() + " ("
                        + Money.format(c.getOrder().orElseThrow().getLockedPriceCents()) + ") this turn"))
                .orElse(List.of("no one ready to pay"));
    }

    private Optional<Customer> nextEligible(Restaurant r, int turn) {
        return r.getReadyToPay().stream().filter(c -> c.getReadyToPayTurn() < turn).findFirst();
    }
}
