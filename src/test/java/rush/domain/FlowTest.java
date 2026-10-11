package rush.domain;

import static rush.TestRunner.check;
import static rush.TestRunner.checkEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Whole-flow tests that drive the domain by hand in the same phase order the controller will use.
 */
public final class FlowTest {
    private FlowTest() { }

    /** A game with a single Regular customer arriving on turn 1 who always orders Pasta. */
    public static GameConfig pastaConfig() {
        return new GameConfig("Test", 18, 2,
                new Menu(List.of(new MenuItem("Pasta", 1800, 2))),
                new ArrivalSchedule(List.of(new ArrivalSchedule.ScheduledArrival(1, RegularCustomer::new))),
                VictoryTargets.base());
    }

    /** One turn's automatic phase; returns the payment, if any. */
    private static Optional<Payment> autoPhase(Restaurant r, Cashier cashier, int turn) {
        Optional<Payment> p = cashier.collect(r, turn);
        r.finishEating(turn);
        r.applyEndOfTurnLoss(turn);
        return p;
    }

    public static void run() {
        singlePastaCustomer();
        lateArrivalCannotPay();
        fullVictoryLine();
    }

    /** Brief example: seat T1, order T2, serve T4; satisfaction 92, 84, 76, 76, 76; pays on T6. */
    private static void singlePastaCustomer() {
        Restaurant r = new Restaurant(pastaConfig());
        Waiter w = new Waiter("W", new PriceCalculator());
        Chef chef = new Chef("C");
        Cashier cashier = new Cashier("K");
        Customer c = null;
        List<Integer> sats = new ArrayList<>();
        Optional<Payment> payment = Optional.empty();
        int paidTurn = -1;
        for (int turn = 1; turn <= 6; turn++) {
            r.admitArrivals(turn);
            if (turn == 1) {
                c = r.findCustomer(1).orElseThrow();
                w.seat(r, 1, 1);
            } else if (turn == 2) {
                w.takeOrder(r, 1, turn);
                chef.cook(r, 1, turn);
            } else if (turn == 3) {
                chef.cook(r, 1, turn);
            } else if (turn == 4) {
                w.serve(r, 1, turn);
            }
            Optional<Payment> p = autoPhase(r, cashier, turn);
            if (p.isPresent()) {
                payment = p;
                paidTurn = turn;
            }
            sats.add(c.getSatisfaction());
        }
        checkEquals(List.of(92, 84, 76, 76, 76, 76), sats, "brief example satisfaction sequence");
        check(payment.isPresent(), "brief example pays");
        checkEquals(6, paidTurn, "brief example pays on turn 6");
        checkEquals(1800, payment.orElseThrow().amountCents(), "brief example amount");
        checkEquals(76, payment.orElseThrow().recordedSatisfaction(), "brief example recorded satisfaction");
    }

    /** R-A5: the turn-16 arrival cannot pay within 18 turns, even with perfect play. */
    private static void lateArrivalCannotPay() {
        Restaurant r = new Restaurant(GameConfig.base());
        Waiter w = new Waiter("W", new PriceCalculator());
        Chef chef = new Chef("C");
        Cashier cashier = new Cashier("K");
        for (int turn = 1; turn <= 18; turn++) {
            r.admitArrivals(turn);
            if (turn == 16) {
                w.seat(r, 6, null);
            } else if (turn == 17) {
                w.takeOrder(r, 6, turn);
                chef.cook(r, 6, turn);
            } else if (turn == 18) {
                w.serve(r, 6, turn);
            }
            autoPhase(r, cashier, turn);
        }
        check(r.findCustomer(6).orElseThrow().getStatus() != CustomerStatus.PAID, "R-A5 C6 not paid by turn 18");
        checkEquals(0, r.getScoreboard().getPaidCount(), "R-A5 nobody paid");
    }

    /** The hand-traced optimal line from the plan (section 3.3): 5 paid, $66.00, avg 79.2. */
    private static void fullVictoryLine() {
        Restaurant r = new Restaurant(GameConfig.base());
        Waiter w = new Waiter("W", new PriceCalculator());
        Chef chef = new Chef("C");
        Cashier cashier = new Cashier("K");
        // waiter commands by turn; chef commands by turn
        String[] waiter = {"seat 1 1", "order 1", "serve 1", "seat 2 2", "order 2", "serve 2",
            "seat 3 1", "order 3", "wait", "serve 3", "seat 4 2", "order 4", "serve 4",
            "seat 5 1", "order 5", "serve 5", "seat 6 2", "order 6"};
        String[] cook = {"-", "1", "-", "-", "2", "-", "-", "3", "3", "-", "-", "4", "-",
            "-", "5", "-", "-", "6"};
        for (int turn = 1; turn <= 18; turn++) {
            r.admitArrivals(turn);
            String[] a = waiter[turn - 1].split(" ");
            ActionResult res = switch (a[0]) {
                case "seat" -> w.seat(r, Integer.parseInt(a[1]), Integer.parseInt(a[2]));
                case "order" -> w.takeOrder(r, Integer.parseInt(a[1]), turn);
                case "serve" -> w.serve(r, Integer.parseInt(a[1]), turn);
                default -> ActionResult.ok("wait");
            };
            check(res.success(), "victory line waiter turn " + turn + ": " + res.message());
            if (!cook[turn - 1].equals("-")) {
                ActionResult cr = chef.cook(r, Integer.parseInt(cook[turn - 1]), turn);
                check(cr.success(), "victory line chef turn " + turn + ": " + cr.message());
            }
            autoPhase(r, cashier, turn);
        }
        Scoreboard s = r.getScoreboard();
        checkEquals(5, s.getPaidCount(), "victory line paid");
        checkEquals(6600, s.getRevenueCents(), "victory line revenue");
        checkEquals(396, s.getSatisfactionSum(), "victory line satisfaction sum (avg 79.2)");
        checkEquals(0, s.getDepartures(), "victory line departures");
        checkEquals(GameOutcome.VICTORY, r.getConfig().targets().evaluate(s), "victory line outcome");
    }
}
