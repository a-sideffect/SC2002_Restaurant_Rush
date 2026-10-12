package rush.scenario;

import static rush.TestRunner.check;
import static rush.TestRunner.checkEquals;

import java.util.List;
import rush.TestConfigs;
import rush.domain.CustomerStatus;
import rush.domain.GameOutcome;
import rush.domain.Scoreboard;
import rush.ui.QuitRequested;

/** Full-game scenarios run through the real controller, views and inputs. */
public final class ScriptedRunTest {
    private ScriptedRunTest() { }

    public static void run() {
        victory();
        earlyStop();
        idle();
        invalidInputDoesNotConsumeTurn();
        immediateEndOfInput();
        metaCommands();
        scriptFallsBackToStdin();
        departureWhileOrderPending();
        fullTablesRejectSeating();
        pastaCollision();
        byteOrderMark();
    }

    /** Notepad and PowerShell can prepend a BOM; the first command must still be understood. */
    private static void byteOrderMark() {
        GameHarness typed = GameHarness.play(null, "\uFEFFseat 1 1\nwait\n");
        check(typed.output.contains("OK: Waiter seats C1 at T1."), "BOM before typed command is ignored");
        GameHarness scripted = GameHarness.play(List.of("\uFEFFseat 1 1", "wait"), "");
        check(scripted.output.contains("OK: Waiter seats C1 at T1."), "BOM before scripted command is ignored");
        check(!scripted.output.contains("  X "), "BOM script produces no errors");
    }

    private static void victory() {
        GameHarness g = GameHarness.play(TestConfigs.regularOnly(), GameHarness.readScript("scripts/stage1-victory.txt"), "");
        Scoreboard s = g.restaurant.getScoreboard();
        checkEquals(GameOutcome.VICTORY, g.outcome, "victory script outcome");
        checkEquals(5, s.getPaidCount(), "victory paid");
        checkEquals(6600, s.getRevenueCents(), "victory revenue");
        checkEquals(396, s.getSatisfactionSum(), "victory satisfaction sum");
        checkEquals(0, s.getDepartures(), "victory departures");
        checkEquals(5, s.getServedCount(), "victory served");
        checkEquals(18, g.count(" END OF TURN "), "victory plays 18 turns");
        check(g.output.matches("(?s).*Avg satisfaction\\s+79\\.2.*"), "victory final shows 79.2");
        check(g.output.contains("*** VICTORY ***"), "victory banner");
        check(g.output.contains("Waiter takes C3's order: Pasta $18.00 (2 units)."), "D3 dish shown for C3");
        check(g.output.contains("Waiter takes C6's order: Pasta"), "C6 orders pasta");
        check(!g.output.contains("  X "), "victory script has no errors");
        checkEquals(CustomerStatus.ORDERED, g.restaurant.findCustomer(6).orElseThrow().getStatus(), "C6 unpaid R-A5");
        // ASCII-only output
        check(g.output.chars().allMatch(ch -> ch < 128), "output is ASCII only");
    }

    private static void earlyStop() {
        GameHarness g = GameHarness.play(TestConfigs.regularOnly(), GameHarness.readScript("scripts/stage1-defeat-early-stop.txt"), "");
        Scoreboard s = g.restaurant.getScoreboard();
        checkEquals(GameOutcome.DEFEAT, g.outcome, "early-stop outcome");
        checkEquals(3, s.getPaidCount(), "early-stop paid");
        checkEquals(4200, s.getRevenueCents(), "early-stop revenue");
        checkEquals(244, s.getSatisfactionSum(), "early-stop sum (avg 81.3)");
        checkEquals(0, s.getDepartures(), "early-stop departures");
        checkEquals(28, g.restaurant.findCustomer(4).orElseThrow().getSatisfaction(), "C4 sat");
        checkEquals(52, g.restaurant.findCustomer(5).orElseThrow().getSatisfaction(), "C5 sat");
        checkEquals(76, g.restaurant.findCustomer(6).orElseThrow().getSatisfaction(), "C6 sat");
        check(g.output.contains("Paid customers") && g.output.contains("FAIL"), "early-stop shows FAIL rows");
        check(g.output.matches("(?s).*Avg satisfaction\\s+81\\.3.*"), "early-stop avg 81.3");
    }

    private static void idle() {
        GameHarness g = GameHarness.play(TestConfigs.regularOnly(), GameHarness.readScript("scripts/stage1-defeat-idle.txt"), "");
        Scoreboard s = g.restaurant.getScoreboard();
        checkEquals(GameOutcome.DEFEAT, g.outcome, "idle outcome");
        checkEquals(0, s.getPaidCount(), "idle paid");
        checkEquals(0, s.getRevenueCents(), "idle revenue");
        checkEquals(2, s.getDepartures(), "idle departures (C1, C2)");
        check(g.output.contains("Avg sat (paid) N/A"), "idle shows N/A");
        checkEquals(CustomerStatus.LEFT, g.restaurant.findCustomer(1).orElseThrow().getStatus(), "C1 left");
        checkEquals(CustomerStatus.LEFT, g.restaurant.findCustomer(2).orElseThrow().getStatus(), "C2 left");
        checkEquals(CustomerStatus.WAITING, g.restaurant.findCustomer(3).orElseThrow().getStatus(), "C3 still waiting");
        // C1 leaves at the end of turn 13: the report must say so on that turn
        int t13 = g.output.indexOf(" END OF TURN 13");
        int leave = g.output.indexOf("C1 runs out of patience");
        check(t13 >= 0 && leave > t13 && leave < g.output.indexOf(" END OF TURN 14"), "C1 leaves at end of turn 13");
    }

    /** R-A4: invalid input explains itself and re-prompts without using the staff member's task. */
    private static void invalidInputDoesNotConsumeTurn() {
        String stdin = String.join("\n",
                "serve 1",          // turn 1 waiter: no such order
                "seat 9",           // no such customer
                "seat 1 5",         // no such table
                "bogus",            // unknown
                "",                 // empty
                "cook 1",           // wrong role
                "seat 1 2",         // valid: C1 -> T2
                "order 1",          // chef prompt, wrong role
                "cook 7",           // no such customer
                "cook 1",           // no order yet
                "wait",             // valid
                "") + "\n";
        GameHarness g = GameHarness.play(null, stdin);
        // after the valid turn-1 commands the input runs dry -> remaining prompts wait
        check(g.output.contains("There is no customer C9."), "unknown customer message");
        check(g.output.contains("There is no table T5."), "unknown table message");
        check(g.output.contains("Unknown command 'bogus'"), "unknown command message");
        check(g.output.contains("Please type a command"), "empty line message");
        check(g.output.contains("'cook' is a Chef task"), "wrong role (waiter) message");
        check(g.output.contains("'order' is a Waiter task"), "wrong role (chef) message");
        check(g.output.contains("C1 has no order to cook yet."), "cook without order message");
        check(g.output.contains("C1's order") || g.output.contains("not READY") || g.output.contains("no order waiting"),
                "serve without order message");
        checkEquals(1, g.count("Waiter seats C1 at T2."), "seat executed exactly once");
        checkEquals(18, g.count(" END OF TURN "), "game still plays 18 turns");
        check(g.output.contains("(input closed"), "EOF is reported");
    }

    private static void immediateEndOfInput() {
        GameHarness g = GameHarness.play(null, "");
        checkEquals(GameOutcome.DEFEAT, g.outcome, "EOF game is a defeat, not a crash");
        checkEquals(18, g.count(" END OF TURN "), "EOF game plays 18 turns");
        checkEquals(1, g.count("(input closed"), "EOF note printed once");
    }

    private static void metaCommands() {
        String stdin = "help\nstatus\nmenu\n?\nquit\nn\nwait\nwait\n";
        GameHarness g = GameHarness.play(null, stdin);
        check(g.output.contains("Commands for the Waiter:"), "help (waiter)");
        check(g.output.contains("Menu: Salad $9.00"), "menu command");
        check(g.count(" TURN 1 / 18") >= 2, "status reprints the board");
        check(g.output.contains("Really quit?"), "quit asks for confirmation");
        // 'n' answers the confirmation, so the game continues and "wait" is accepted
        check(g.output.contains("OK: Waiter waits."), "game continued after declining quit");
        boolean quit = false;
        try {
            GameHarness.play(null, "quit\ny\n");
        } catch (QuitRequested e) {
            quit = true;
        }
        check(quit, "confirmed quit throws QuitRequested");
        boolean chefHelp = GameHarness.play(null, "wait\nhelp\nwait\n").output.contains("Commands for the Chef:");
        check(chefHelp, "help (chef)");
    }

    private static void scriptFallsBackToStdin() {
        List<String> script = List.of("# only turn 1", "seat 1 1", "wait", "", "# comment");
        GameHarness g = GameHarness.play(script, "wait\nwait\n");
        checkEquals(18, g.count(" END OF TURN "), "fallback plays 18 turns");
        check(g.output.contains("Waiter> seat 1 1"), "script echoes the command");
        check(g.output.contains("OK: Waiter seats C1 at T1."), "scripted seat happened");
    }

    /** A customer who leaves with an order pending: order cancelled, table freed, Chef cannot cook it. */
    private static void departureWhileOrderPending() {
        StringBuilder sb = new StringBuilder("# turn 1\nseat 1 1\nwait\n# turn 2\norder 1\nwait\n");
        for (int turn = 3; turn <= 13; turn++) {
            sb.append("wait\nwait\n");
        }
        sb.append("wait\n"); // turn 14 waiter
        GameHarness g = GameHarness.play(List.of(sb.toString().split("\n")),
                "cook 1\nwait\n");  // turn 14 chef: first attempt is rejected, then wait
        check(g.output.contains("C1's Salad is CANCELLED and cannot be cooked."), "cook cancelled order rejected");
        checkEquals(CustomerStatus.LEFT, g.restaurant.findCustomer(1).orElseThrow().getStatus(), "C1 left");
        check(g.restaurant.getTables().get(0).isFree(), "table freed on departure");
        checkEquals(0, g.restaurant.getScoreboard().getRevenueCents(), "no revenue from walk-out");
    }

    private static void fullTablesRejectSeating() {
        // C1 -> T1 (t1), C2 -> T2 (t4), C3 arrives t7 with both tables occupied
        StringBuilder sb = new StringBuilder();
        sb.append("seat 1\nwait\n");                    // t1
        sb.append("wait\nwait\nwait\nwait\n");          // t2, t3
        sb.append("seat 2\nwait\n");                    // t4
        sb.append("wait\nwait\nwait\nwait\n");          // t5, t6
        sb.append("seat 3\n");                          // t7: rejected (no free table)
        sb.append("seat 3 1\n");                        // rejected again (T1 occupied)
        sb.append("wait\nwait\n");
        GameHarness g = GameHarness.play(List.of(sb.toString().split("\n")), "");
        check(g.output.contains("No free table. A table frees up when its customer pays or leaves."),
                "no free table message in full game");
        checkEquals(CustomerStatus.WAITING, g.restaurant.findCustomer(3).orElseThrow().getStatus(),
                "C3 stays queued");
    }

    /** One Pasta delays the next customers (plan 3.1): serving C3 on turn 10 collides with C4's seating. */
    private static void pastaCollision() {
        GameHarness g = GameHarness.play(TestConfigs.regularOnly(), GameHarness.readScript("scripts/stage1-victory.txt"), "");
        // on turn 10 the board must offer both seat C4 and serve C3 (the collision)
        int t10 = g.output.indexOf(" TURN 10 / 18");
        int t11 = g.output.indexOf(" TURN 11 / 18");
        String board = g.output.substring(t10, t11);
        check(board.contains("seat C4") && board.contains("serve C3 (Pasta)"), "turn 10 collision offered");
        check(board.contains("Revenue $24.00"), "turn 10 revenue $24.00");
        check(board.contains("Avg satisfaction (paid) 84.0"), "turn 10 average 84.0");
    }
}
