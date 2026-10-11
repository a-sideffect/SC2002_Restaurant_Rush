package rush.domain;

/** Win conditions, checked once after the final turn (Rule R6.2). */
public record VictoryTargets(int minPaid, int minRevenueCents, int minAvgSatisfaction) {
    public static VictoryTargets base() { return new VictoryTargets(4, 4500, 60); }

    public boolean paidMet(Scoreboard s) { return s.getPaidCount() >= minPaid; }

    public boolean revenueMet(Scoreboard s) { return s.getRevenueCents() >= minRevenueCents; }

    /** Integer comparison only: satisfactionSum >= minAvg * paidCount (Rule R6.3). */
    public boolean satisfactionMet(Scoreboard s) {
        return s.getPaidCount() > 0 && s.getSatisfactionSum() >= minAvgSatisfaction * s.getPaidCount();
    }

    public GameOutcome evaluate(Scoreboard s) {
        boolean win = paidMet(s) && revenueMet(s) && satisfactionMet(s);
        return win ? GameOutcome.VICTORY : GameOutcome.DEFEAT;
    }
}
