package rush;

import java.util.Objects;
import rush.domain.CustomerTest;
import rush.domain.FlowTest;
import rush.domain.MoneyTest;
import rush.domain.OrderTest;
import rush.domain.RestaurantTest;
import rush.domain.StaffTest;
import rush.domain.VictoryTargetsTest;
import rush.scenario.FuzzTest;
import rush.scenario.ScriptedRunTest;
import rush.ui.CommandParserTest;

/** Tiny dependency-free test harness. Exits non-zero if any check fails. */
public final class TestRunner {
    private static int passed;
    private static int failed;

    private TestRunner() { }

    public static void check(boolean cond, String name) {
        if (cond) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL: " + name);
        }
    }

    public static void checkEquals(Object expected, Object actual, String name) {
        check(Objects.equals(expected, actual), name + " expected=" + expected + " actual=" + actual);
    }

    public static void main(String[] args) {
        MoneyTest.run();
        CustomerTest.run();
        OrderTest.run();
        RestaurantTest.run();
        StaffTest.run();
        VictoryTargetsTest.run();
        FlowTest.run();
        try {
            CommandParserTest.run();
        } catch (Exception e) {
            check(false, "CommandParserTest threw " + e);
        }
        ScriptedRunTest.run();
        FuzzTest.run();
        System.out.printf("%d passed, %d failed%n", passed, failed);
        if (failed > 0) {
            System.exit(1);
        }
    }
}
