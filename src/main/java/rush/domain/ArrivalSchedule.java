package rush.domain;

import java.util.List;

/** When customers arrive and which kind of customer each arrival is. */
public class ArrivalSchedule {

    /** Creates the customer for one arrival. */
    @FunctionalInterface
    public interface CustomerFactory {
        Customer create(int id, int arrivalTurn);
    }

    /** One planned arrival. */
    public record ScheduledArrival(int turn, CustomerFactory factory) { }

    private final List<ScheduledArrival> arrivals;

    public ArrivalSchedule(List<ScheduledArrival> arrivals) {
        this.arrivals = List.copyOf(arrivals);
    }

    /**
     * Base setting: one customer on turns 1, 4, 7, 10, 13 and 16. The turn-4 arrival is a VIP and the
     * turn-10 arrival is a Critic; everyone else is Regular.
     */
    public static ArrivalSchedule base() {
        return new ArrivalSchedule(List.of(
                new ScheduledArrival(1, RegularCustomer::new),
                new ScheduledArrival(4, VIPCustomer::new),
                new ScheduledArrival(7, RegularCustomer::new),
                new ScheduledArrival(10, CriticCustomer::new),
                new ScheduledArrival(13, RegularCustomer::new),
                new ScheduledArrival(16, RegularCustomer::new)));
    }

    public List<ScheduledArrival> arrivalsAt(int turn) {
        return arrivals.stream().filter(a -> a.turn() == turn).toList();
    }

    public int totalArrivals() { return arrivals.size(); }

    /** The distinct turns on which someone arrives, ascending. */
    public List<Integer> getArrivalTurns() {
        return arrivals.stream().map(ScheduledArrival::turn).distinct().sorted().toList();
    }
}
