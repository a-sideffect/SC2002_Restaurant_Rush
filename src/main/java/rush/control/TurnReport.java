package rush.control;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import rush.domain.Customer;
import rush.domain.Payment;

/**
 * What happened in a turn's automatic phase.
 *
 * @param payment the payment the Cashier collected, if any
 * @param finishedEating customers who finished eating and are now ready to pay
 * @param satisfactionBefore satisfaction of each customer awaiting service before the loss was applied
 * @param departed customers who left unhappy
 */
public record TurnReport(Optional<Payment> payment, List<Customer> finishedEating,
                         Map<Customer, Integer> satisfactionBefore, List<Customer> departed) { }
