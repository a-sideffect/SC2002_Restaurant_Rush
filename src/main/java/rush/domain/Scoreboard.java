package rush.domain;

import java.util.OptionalDouble;

/** Running totals used for the displays and the victory check. */
public class Scoreboard {
    private int revenueCents;
    private int paidCount;
    private int satisfactionSum;
    private int servedCount;
    private int departures;

    public int getRevenueCents() { return revenueCents; }
    public int getPaidCount() { return paidCount; }
    public int getServedCount() { return servedCount; }
    public int getDepartures() { return departures; }
    public int getSatisfactionSum() { return satisfactionSum; }

    /** Average satisfaction of paid customers; empty if nobody has paid. */
    public OptionalDouble averageSatisfaction() {
        return paidCount == 0 ? OptionalDouble.empty()
                : OptionalDouble.of((double) satisfactionSum / paidCount);
    }

    void recordServed() { servedCount++; }

    void recordPayment(int cents, int satisfaction) {
        revenueCents += cents;
        paidCount++;
        satisfactionSum += satisfaction;
    }

    void recordDeparture() { departures++; }
}
