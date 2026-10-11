package rush.domain;

import static rush.TestRunner.checkEquals;

import java.util.OptionalDouble;

public final class VictoryTargetsTest {
    private VictoryTargetsTest() { }

    private static Scoreboard board(int paid, int revenue, int satEach) {
        Scoreboard s = new Scoreboard();
        for (int i = 0; i < paid; i++) {
            s.recordPayment(revenue / paid, satEach);
        }
        return s;
    }

    public static void run() {
        VictoryTargets t = VictoryTargets.base();
        checkEquals(GameOutcome.VICTORY, t.evaluate(board(4, 4500, 60)), "R6.2 exact boundaries win");
        checkEquals(GameOutcome.DEFEAT, t.evaluate(board(3, 4500, 90)), "R6.2 3 paid loses");
        checkEquals(GameOutcome.DEFEAT, t.evaluate(board(4, 4499, 90)), "R6.2 $44.99 loses");
        checkEquals(GameOutcome.DEFEAT, t.evaluate(board(4, 4500, 59)), "R6.2 avg 59 loses");

        Scoreboard s = new Scoreboard();
        s.recordPayment(1200, 60);
        s.recordPayment(1200, 60);
        s.recordPayment(1200, 60);
        s.recordPayment(1200, 59);
        checkEquals(GameOutcome.DEFEAT, t.evaluate(s), "R6.3 avg 59.75 loses");

        Scoreboard empty = new Scoreboard();
        checkEquals(OptionalDouble.empty(), empty.averageSatisfaction(), "D2 N/A with no payers");
        checkEquals(GameOutcome.DEFEAT, t.evaluate(empty), "no payers loses");
    }
}
