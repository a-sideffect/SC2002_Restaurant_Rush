package rush.scenario;

import static rush.TestRunner.check;
import static rush.TestRunner.checkEquals;

import java.util.ArrayList;
import java.util.List;
import rush.domain.CustomerStatus;
import rush.domain.GameOutcome;
import rush.domain.Scoreboard;
import rush.ui.QuitRequested;

/**
 * Full-game scenarios run through the real controller, views and inputs.
 * Each turn asks: Happy Hour yes/no (only while unused), then Waiter, Host and Chef.
 */
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
        hostSeatsAndHappyHourPrompts();
        byteOrderMark();
    }

    /** The script lines for {@code turns} idle turns while Happy Hour is still unused. */
    private static void idleTurns(List<String> lines, int turns) {
        for (int i = 0; i < turns; i++) {
            lines.add("no");
            lines.add("wait");
            lines.add("wait");
            lines.add("wait");
        }
    }

    private static void victory() {
        GameHarness g = GameHarness.play(GameHarness.readScript("scripts/stage3-victory.txt"), "");
        Scoreboard s = g.restaurant.getScoreboard();
        checkEquals(GameOutcome.VICTORY, g.outcome, "victory script outcome");
        checkEquals(4, s.getPaidCount(), "victory paid");
        checkEquals(6480, s.getRevenueCents(), "victory revenue");
        checkEquals(338, s.getSatisfactionSum(), "victory satisfaction sum (avg 84.5)");
        checkEquals(0, s.getDepartures(), "victory departures");
        checkEquals(18, g.count(" END OF TURN "), "victory plays 18 turns");
        check(g.output.matches("(?s).*Avg satisfaction\\s+84\\.5.*"), "victory final shows 84.5");
        check(g.output.contains("*** VICTORY ***"), "victory banner");
        check(!g.output.contains("  X "), "victory script has no errors");
        check(g.output.chars().allMatch(ch -> ch < 128), "output is ASCII only");
        // VIP price, Critic's dish
        check(g.output.contains("Waiter takes C2's order: Burger $13.50"), "VIP price shown");
        check(g.output.contains("Waiter takes C4's order: Combo B"), "Critic orders the priciest item, a combo");
        // Happy Hour announced on turn 12 and ends two turns later
        check(g.output.contains("Happy Hour starts and lasts 2 turns."), "Happy Hour activation message");
        check(g.output.contains("HAPPY HOUR ACTIVE: 2 turn(s) left"), "board shows 2 turns left");
        check(g.output.contains("HAPPY HOUR ACTIVE: 1 turn(s) left"), "board shows 1 turn left");
        checkEquals(2, g.count("HAPPY HOUR ACTIVE"), "board shows Happy Hour for exactly two turns");
        check(g.count("Happy Hour: still available") > 0, "board shows it as available before use");
        checkEquals(11, g.count("Happy Hour> no"), "asked and declined on turns 1-11");
        checkEquals(1, g.count("Happy Hour> yes"), "accepted on turn 12");
        // after activation the question is no longer asked
        int yes = g.output.indexOf("Happy Hour> yes");
        check(g.output.indexOf("Happy Hour>", yes + 1) < 0, "no Happy Hour prompt after it was used");
    }

    private static void earlyStop() {
        GameHarness g = GameHarness.play(GameHarness.readScript("scripts/stage3-defeat-early-stop.txt"), "");
        Scoreboard s = g.restaurant.getScoreboard();
        checkEquals(GameOutcome.DEFEAT, g.outcome, "early-stop outcome");
        checkEquals(2, s.getPaidCount(), "early-stop paid");
        checkEquals(2250, s.getRevenueCents(), "early-stop revenue");
        checkEquals(174, s.getSatisfactionSum(), "early-stop satisfaction sum (avg 87.0)");
        checkEquals(1, s.getDepartures(), "the unattended Critic walks out");
        checkEquals(CustomerStatus.LEFT, g.restaurant.findCustomer(4).orElseThrow().getStatus(), "C4 left");
        check(g.output.contains("FAIL"), "early-stop shows FAIL rows");
        checkEquals(18, g.count("Happy Hour> no"), "Happy Hour never used, asked every turn");
    }

    private static void idle() {
        GameHarness g = GameHarness.play(GameHarness.readScript("scripts/stage3-defeat-idle.txt"), "");
        Scoreboard s = g.restaurant.getScoreboard();
        checkEquals(GameOutcome.DEFEAT, g.outcome, "idle outcome");
        checkEquals(0, s.getPaidCount(), "idle paid");
        checkEquals(0, s.getRevenueCents(), "idle revenue");
        checkEquals(2, s.getDepartures(), "idle departures (C1 and the Critic C4)");
        check(g.output.contains("Avg sat (paid) N/A"), "idle shows N/A");
        checkEquals(CustomerStatus.LEFT, g.restaurant.findCustomer(1).orElseThrow().getStatus(), "C1 left");
        checkEquals(CustomerStatus.LEFT, g.restaurant.findCustomer(4).orElseThrow().getStatus(), "C4 left");
        checkEquals(25, g.restaurant.findCustomer(2).orElseThrow().getSatisfaction(), "VIP C2 still waiting at 25");
        checkEquals(4, g.restaurant.findCustomer(3).orElseThrow().getSatisfaction(), "C3 about to leave at 4");
        int t13 = g.output.indexOf(" END OF TURN 13");
        int c1 = g.output.indexOf("C1 runs out of patience");
        check(t13 >= 0 && c1 > t13 && c1 < g.output.indexOf(" END OF TURN 14"), "C1 leaves at the end of turn 13");
        int t18 = g.output.indexOf(" END OF TURN 18");
        check(t18 >= 0 && g.output.indexOf("C4 runs out of patience") > t18, "Critic C4 leaves at the end of turn 18");
    }

    /** R-A4: invalid input explains itself and re-prompts without using the staff member's task. */
    private static void invalidInputDoesNotConsumeTurn() {
        String stdin = String.join("\n",
                "maybe",            // Happy Hour prompt: not yes or no
                "no",
                "serve 1",          // Waiter: C1 is still in the queue
                "seat 9",           // no such customer
                "seat 1 5",         // no such table
                "bogus",            // unknown
                "",                 // empty
                "cook 1",           // wrong role
                "seat 1 2",         // valid: C1 -> T2
                "order 1",          // Host prompt, wrong role
                "cook 1",           // Host prompt, wrong role
                "seat 3",           // Host: no such customer yet
                "wait",             // valid
                "order 1",          // Chef prompt, wrong role
                "cook 7",           // no such customer
                "cook 1",           // no order yet
                "wait",             // valid
                "") + "\n";
        GameHarness g = GameHarness.play(null, stdin);
        check(g.output.contains("Please answer yes or no."), "Happy Hour rejects other answers");
        check(g.output.contains("There is no customer C9."), "unknown customer message");
        check(g.output.contains("There is no table T5."), "unknown table message");
        check(g.output.contains("Unknown command 'bogus'"), "unknown command message");
        check(g.output.contains("Please type a command"), "empty line message");
        check(g.output.contains("'cook' is a Chef task. Waiter can"), "wrong role (waiter) message");
        check(g.output.contains("'order' is a Waiter task. Host can: seat, wait."), "wrong role (host) message");
        check(g.output.contains("'order' is a Waiter task. Chef can"), "wrong role (chef) message");
        check(g.output.contains("C1 has no order to cook yet."), "cook without order message");
        check(g.output.contains("is still in the queue") || g.output.contains("no order waiting"),
                "serve without order message");
        checkEquals(1, g.count("Seated C1 at T2."), "seat executed exactly once");
        checkEquals(18, g.count(" END OF TURN "), "game still plays 18 turns");
        check(g.output.contains("(input closed"), "EOF is reported");
    }

    private static void immediateEndOfInput() {
        GameHarness g = GameHarness.play(null, "");
        checkEquals(GameOutcome.DEFEAT, g.outcome, "EOF game is a defeat, not a crash");
        checkEquals(18, g.count(" END OF TURN "), "EOF game plays 18 turns");
        checkEquals(1, g.count("(input closed"), "EOF note printed once");
        checkEquals(0, g.count("HAPPY HOUR ACTIVE"), "EOF never starts Happy Hour");
    }

    private static void metaCommands() {
        // the first prompt of a game is the Happy Hour question
        String stdin = "help\nstatus\nmenu\n?\nquit\nn\nno\nhelp\nwait\nhelp\nwait\nhelp\nwait\n";
        GameHarness g = GameHarness.play(null, stdin);
        check(g.output.contains("Commands for the Happy Hour:"), "help (happy hour)");
        check(g.output.contains("Commands for the Waiter:"), "help (waiter)");
        check(g.output.contains("Commands for the Host:"), "help (host)");
        check(g.output.contains("Commands for the Chef:"), "help (chef)");
        check(g.output.contains("  Menu:" + System.lineSeparator() + "    Salad  $9.00  (1 unit)"), "menu command");
        check(g.output.contains("Combo A [Salad + Burger]  $21.60  (2 units)"), "menu lists combos with their parts");
        check(g.output.contains("Combo B [Salad + Pasta]  $24.30  (3 units)"), "menu lists the second combo");
        check(g.count(" TURN 1 / 18") >= 2, "status reprints the board");
        check(g.output.contains("Really quit?"), "quit asks for confirmation");
        check(g.output.contains("OK: Waiter waits."), "game continued after declining quit");
        boolean quit = false;
        try {
            GameHarness.play(null, "quit\ny\n");
        } catch (QuitRequested e) {
            quit = true;
        }
        check(quit, "confirmed quit throws QuitRequested");
    }

    private static void scriptFallsBackToStdin() {
        List<String> script = List.of("# only turn 1", "no", "seat 1 1", "wait", "", "# comment", "wait");
        GameHarness g = GameHarness.play(script, "no\nwait\nwait\nwait\n");
        checkEquals(18, g.count(" END OF TURN "), "fallback plays 18 turns");
        check(g.output.contains("Waiter> seat 1 1"), "script echoes the command");
        check(g.output.contains("OK: Seated C1 at T1."), "scripted seat happened");
    }

    /** A customer who leaves with an order pending: order cancelled, table freed, Chef cannot cook it. */
    private static void departureWhileOrderPending() {
        List<String> lines = new ArrayList<>(List.of("no", "seat 1 1", "wait", "wait",     // turn 1
                "no", "order 1", "wait", "wait"));                                           // turn 2
        idleTurns(lines, 11);                                                                // turns 3-13: C1 leaves
        lines.add("no");                                                                     // turn 14
        lines.add("wait");
        GameHarness g = GameHarness.play(lines, "wait\ncook 1\nwait\n");  // Host waits; Chef: rejected, then waits
        check(g.output.contains("C1's Salad is CANCELLED and cannot be cooked."), "cook cancelled order rejected");
        checkEquals(CustomerStatus.LEFT, g.restaurant.findCustomer(1).orElseThrow().getStatus(), "C1 left");
        check(g.restaurant.getTables().get(0).isFree(), "table freed on departure");
        checkEquals(0, g.restaurant.getScoreboard().getRevenueCents(), "no revenue from walk-out");
    }

    private static void fullTablesRejectSeating() {
        // C1 -> T1 (turn 1), C2 -> T2 (turn 4), C3 arrives turn 7 with both tables occupied
        List<String> lines = new ArrayList<>(List.of("no", "seat 1", "wait", "wait"));
        idleTurns(lines, 2);
        lines.addAll(List.of("no", "seat 2", "wait", "wait"));
        idleTurns(lines, 2);
        lines.addAll(List.of("no", "seat 3", "seat 3 1", "wait", "seat 3", "wait", "wait")); // waiter x2 rejected, host rejected
        GameHarness g = GameHarness.play(lines, "");
        check(g.output.contains("No free table. A table frees up when its customer pays or leaves."),
                "no free table message in full game");
        checkEquals(CustomerStatus.WAITING, g.restaurant.findCustomer(3).orElseThrow().getStatus(),
                "C3 stays queued");
        checkEquals(3, g.count("No free table."), "Waiter twice and Host once were refused");
    }

    /** The Host seats on its own prompt, so the Waiter can order in the same turn another customer was seated. */
    private static void hostSeatsAndHappyHourPrompts() {
        List<String> lines = new ArrayList<>(List.of("no", "seat 1 1", "wait", "wait"));  // turn 1: Waiter seats C1
        lines.addAll(List.of("no", "order 1", "wait", "cook 1"));                         // turn 2
        lines.addAll(List.of("no", "serve 1", "wait", "wait"));                           // turn 3
        lines.addAll(List.of("yes", "wait", "seat 2 2", "wait"));                         // turn 4: HH on; Host seats VIP C2
        GameHarness g = GameHarness.play(lines, "");
        check(g.output.contains("Host> seat 2 2"), "Host prompt is used");
        check(g.output.contains("OK: Seated C2 at T2."), "Host seated C2");
        check(g.output.contains("Host    : seat C2 at T2"), "board lists the Host's task");
        check(g.output.contains("Happy Hour starts and lasts 2 turns."), "Happy Hour started on turn 4");
        // C2 arrives on turn 4 at 100; recovery caps at 100
        check(g.output.contains("C2 100 -> 100"), "recovery is capped at 100");
        // turn 5: Happy Hour still active with one turn left; C2 is seated at 95 -> 100? (5 lost on turn 4)
        int t5 = g.output.indexOf(" TURN 5 / 18");
        check(t5 > 0 && g.output.substring(g.output.lastIndexOf("** HAPPY HOUR **", t5), t5).contains("1 turn(s) left"),
                "turn 5 recovery shows 1 turn left");
        // turn 6: no more recovery
        int t6 = g.output.indexOf(" TURN 6 / 18");
        check(!g.output.substring(g.output.indexOf(" END OF TURN 5"), t6).contains("** HAPPY HOUR **"),
                "no recovery on turn 6");
        check(!g.output.substring(t6).contains("HAPPY HOUR ACTIVE"), "board no longer shows it on turn 6");
    }

    /** Notepad and PowerShell can prepend a BOM; the first command must still be understood. */
    private static void byteOrderMark() {
        GameHarness typed = GameHarness.play(null, "\uFEFFno\nseat 1 1\nwait\nwait\n");
        check(typed.output.contains("OK: Seated C1 at T1."), "BOM before typed command is ignored");
        check(!typed.output.contains("Please answer yes or no."), "BOM does not break the first answer");
        GameHarness scripted = GameHarness.play(List.of("\uFEFFno", "seat 1 1", "wait", "wait"), "");
        check(scripted.output.contains("OK: Seated C1 at T1."), "BOM before scripted command is ignored");
        check(!scripted.output.contains("  X "), "BOM script produces no errors");
    }
}
