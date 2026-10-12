package rush.scenario;

import static rush.TestRunner.check;
import static rush.TestRunner.checkEquals;

import rush.domain.CustomerStatus;
import rush.domain.GameOutcome;
import rush.domain.Scoreboard;

/** Stage 2 reproducible runs, played through the real controller. */
public final class Stage2RunTest {
    private Stage2RunTest() { }

    public static void run() {
        victory();
        earlyStop();
        idle();
    }

    private static void victory() {
        GameHarness g = GameHarness.play(GameHarness.readScript("scripts/stage2-victory.txt"), "");
        Scoreboard s = g.restaurant.getScoreboard();
        checkEquals(GameOutcome.VICTORY, g.outcome, "stage 2 victory outcome");
        checkEquals(5, s.getPaidCount(), "stage 2 victory paid");
        checkEquals(7350, s.getRevenueCents(), "stage 2 victory revenue");
        checkEquals(378, s.getSatisfactionSum(), "stage 2 victory satisfaction sum (avg 75.6)");
        checkEquals(0, s.getDepartures(), "stage 2 victory departures");
        check(g.output.contains("Waiter takes C2's order: Burger $13.50"), "VIP price shown when ordering");
        check(g.output.contains("Waiter takes C4's order: Pasta $18.00"), "Critic orders the priciest dish");
        check(g.output.contains("Cashier collects $13.50 from C2 (satisfaction 90)"), "VIP pays $13.50");
        check(g.output.contains("C4 (Critic) arrives"), "Critic arrival announced");
        check(g.output.contains("C2 (VIP) arrives"), "VIP arrival announced");
        check(!g.output.contains("  X "), "stage 2 victory script has no errors");
    }

    private static void earlyStop() {
        GameHarness g = GameHarness.play(GameHarness.readScript("scripts/stage2-defeat-early-stop.txt"), "");
        Scoreboard s = g.restaurant.getScoreboard();
        checkEquals(GameOutcome.DEFEAT, g.outcome, "stage 2 early-stop outcome");
        checkEquals(3, s.getPaidCount(), "stage 2 early-stop paid");
        checkEquals(4050, s.getRevenueCents(), "stage 2 early-stop revenue");
        checkEquals(250, s.getSatisfactionSum(), "stage 2 early-stop satisfaction sum (avg 83.3)");
        checkEquals(1, s.getDepartures(), "the unattended Critic walks out");
        checkEquals(CustomerStatus.LEFT, g.restaurant.findCustomer(4).orElseThrow().getStatus(), "C4 left");
    }

    private static void idle() {
        GameHarness g = GameHarness.play(GameHarness.readScript("scripts/stage2-defeat-idle.txt"), "");
        Scoreboard s = g.restaurant.getScoreboard();
        checkEquals(GameOutcome.DEFEAT, g.outcome, "stage 2 idle outcome");
        checkEquals(0, s.getPaidCount(), "stage 2 idle paid");
        checkEquals(2, s.getDepartures(), "C1 and the Critic C4 leave");
        checkEquals(CustomerStatus.LEFT, g.restaurant.findCustomer(1).orElseThrow().getStatus(), "C1 left");
        checkEquals(CustomerStatus.LEFT, g.restaurant.findCustomer(4).orElseThrow().getStatus(), "C4 left");
        // the VIP is more patient: 100 - 5*15 turns of waiting = 25 at the end
        checkEquals(25, g.restaurant.findCustomer(2).orElseThrow().getSatisfaction(), "VIP still waiting at 25");
        int t13 = g.output.indexOf(" END OF TURN 13");
        int c1 = g.output.indexOf("C1 runs out of patience");
        check(t13 >= 0 && c1 > t13 && c1 < g.output.indexOf(" END OF TURN 14"), "C1 leaves at the end of turn 13");
        int t18 = g.output.indexOf(" END OF TURN 18");
        int c4 = g.output.indexOf("C4 runs out of patience");
        check(t18 >= 0 && c4 > t18, "Critic C4 leaves at the end of turn 18");
    }
}
