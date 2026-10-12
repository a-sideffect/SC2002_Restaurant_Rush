package rush.domain;

/**
 * A once-per-game promotion. When activated at the start of a turn it lasts two turns: orders taken while it is
 * active get 20% off (after any VIP discount), and customers awaiting service regain 15 satisfaction at the
 * start of each active turn.
 */
public class HappyHour {
    public static final int DURATION_TURNS = 2;
    public static final int DISCOUNT_PERCENT = 20;
    public static final int RECOVERY_POINTS = 15;

    private boolean used;
    private int remainingTurns;

    /** True until the Happy Hour has been activated. */
    public boolean isAvailable() { return !used; }

    public boolean isActive() { return remainingTurns > 0; }

    public int getRemainingTurns() { return remainingTurns; }

    /** The price after the Happy Hour discount, or unchanged if it is not active. */
    public int apply(int cents) {
        return isActive() ? Money.discounted(cents, DISCOUNT_PERCENT) : cents;
    }

    boolean activate() {
        if (used) {
            return false;
        }
        used = true;
        remainingTurns = DURATION_TURNS;
        return true;
    }

    /** Called once at the end of every turn. */
    void endTurn() {
        if (remainingTurns > 0) {
            remainingTurns--;
        }
    }
}
