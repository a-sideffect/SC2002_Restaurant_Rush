package rush.domain;

import static rush.TestRunner.check;
import static rush.TestRunner.checkEquals;

import java.util.Optional;

public final class StaffTest {
    private StaffTest() { }

    public static void run() {
        taskDescriptions();
        waiterValidation();
        chefValidation();
        cashierRules();
    }

    /** The board's "staff tasks available" lines come from describeAvailableTasks. */
    private static void taskDescriptions() {
        Restaurant r = new Restaurant(GameConfig.base());
        Waiter w = new Waiter("W", new PriceCalculator());
        Chef chef = new Chef("C");
        Cashier cashier = new Cashier("K");
        checkEquals(java.util.List.of(), w.describeAvailableTasks(r, 1), "no tasks before anyone arrives");
        checkEquals(java.util.List.of("no one ready to pay"), cashier.describeAvailableTasks(r, 1), "idle cashier");
        r.admitArrivals(1);
        checkEquals(java.util.List.of("seat C1 at T1"), w.describeAvailableTasks(r, 1), "seat offered");
        w.seat(r, 1, 2);
        checkEquals(java.util.List.of("order C1"), w.describeAvailableTasks(r, 1), "order offered once seated");
        w.takeOrder(r, 1, 2);
        checkEquals(java.util.List.of("cook C1 Salad (0/1)"), chef.describeAvailableTasks(r, 2), "cook offered");
        checkEquals(java.util.List.of(), w.describeAvailableTasks(r, 2), "nothing to serve until READY");
        chef.cook(r, 1, 2);
        checkEquals(java.util.List.of(), chef.describeAvailableTasks(r, 2), "READY order is not cookable");
        checkEquals(java.util.List.of("serve C1 (Salad)"), w.describeAvailableTasks(r, 3), "serve offered when READY");
        w.serve(r, 1, 3);
        r.finishEating(4);
        checkEquals(java.util.List.of("will charge C1 ($9.00) this turn"), cashier.describeAvailableTasks(r, 5),
                "cashier previews next payment");
        checkEquals(java.util.List.of("no one ready to pay"), cashier.describeAvailableTasks(r, 4),
                "not eligible on the turn it became READY_TO_PAY");

        // with both tables taken, seat is not offered
        Restaurant full = new Restaurant(GameConfig.base());
        for (int t = 1; t <= 7; t += 3) {
            full.admitArrivals(t);
        }
        full.seatCustomer(1, null);
        full.seatCustomer(2, null);
        checkEquals(java.util.List.of("order C1", "order C2"), w.describeAvailableTasks(full, 7),
                "no seat offered when tables are full");
    }

    private static void waiterValidation() {
        Restaurant r = new Restaurant(GameConfig.base());
        Waiter w = new Waiter("W", new PriceCalculator());
        Chef chef = new Chef("C");
        r.admitArrivals(1);

        check(!w.takeOrder(r, 1, 1).success(), "R2.3 order requires SEATED");
        check(!w.serve(r, 1, 1).success(), "serve requires an order");
        check(w.seat(r, 1, 1).success(), "seat ok");

        ActionResult order = w.takeOrder(r, 1, 2);
        check(order.success(), "R2.3 order ok");
        check(order.message().contains("Salad") && order.message().contains("$9.00"), "D3 dish shown");
        Order o = r.findCustomer(1).orElseThrow().getOrder().orElseThrow();
        checkEquals(900, o.getLockedPriceCents(), "R2.3 price locked");
        check(!w.takeOrder(r, 1, 2).success(), "cannot order twice");
        check(w.takeOrder(r, 1, 2).message().startsWith("C1 has already ordered Salad (PLACED 0/1)"),
                "already-ordered message");

        check(!w.serve(r, 1, 2).success(), "R2.4 cannot serve PLACED order");
        check(chef.cook(r, 1, 2).success(), "R3.3 chef cooks order taken same turn");
        check(!chef.cook(r, 1, 2).success(), "R3.1 cooking READY order fails");
        checkEquals("C1's Salad is READY and does not need cooking.", chef.cook(r, 1, 2).message(), "cook message");
        check(w.serve(r, 1, 3).success(), "R2.4 serve READY order");
        checkEquals(CustomerStatus.EATING, r.findCustomer(1).orElseThrow().getStatus(), "EATING");
        checkEquals(1, r.getScoreboard().getServedCount(), "served counted");
        check(!chef.cook(r, 1, 3).success(), "cook SERVED order fails");
        check(!w.seat(r, 9, null).success(), "failed action reports failure (not consumed)");
        check(WaitTask.INSTANCE.execute(w, r, 3).success(), "R2.5 wait always valid");
    }

    private static void chefValidation() {
        Restaurant r = new Restaurant(GameConfig.base());
        Waiter w = new Waiter("W", new PriceCalculator());
        Chef chef = new Chef("C");
        r.admitArrivals(1);
        check(!chef.cook(r, 1, 1).success(), "cook with no order fails");
        check(!chef.cook(r, 7, 1).success(), "cook unknown customer fails");
        // Pasta customer is C3; build via a Pasta-only config
        Restaurant pr = new Restaurant(FlowTest.pastaConfig());
        pr.admitArrivals(1);
        w.seat(pr, 1, 1);
        w.takeOrder(pr, 1, 2);
        checkEquals("Chef cooks Pasta for C1 (1/2).", chef.cook(pr, 1, 2).message(), "R3.1 pasta first cook");
        check(!w.serve(pr, 1, 2).success(), "cannot serve COOKING");
        check(w.serve(pr, 1, 2).message().contains("not READY (1/2 units done)"), "serve not-ready message");
        check(chef.cook(pr, 1, 3).message().contains("READY"), "R3.1 pasta second cook ready");
    }

    private static void cashierRules() {
        Restaurant r = new Restaurant(GameConfig.base());
        Waiter w = new Waiter("W", new PriceCalculator());
        Chef chef = new Chef("C");
        Cashier cashier = new Cashier("K");
        // C1 (Salad) and C2 (Burger) become READY_TO_PAY on the same turn
        r.admitArrivals(1);
        r.admitArrivals(4);
        w.seat(r, 1, 1);
        w.takeOrder(r, 1, 2);
        w.seat(r, 2, 2);
        w.takeOrder(r, 2, 3);
        chef.cook(r, 1, 3);
        chef.cook(r, 2, 4);
        w.serve(r, 1, 5);
        w.serve(r, 2, 5);
        check(r.finishEating(5).isEmpty(), "R4.4 not ready to pay on the serve turn");
        checkEquals(2, r.finishEating(6).size(), "R4.4 both ready to pay next turn");
        check(cashier.collect(r, 6).isEmpty(), "R4.1 cannot pay on the turn it became READY_TO_PAY");
        Optional<Payment> first = cashier.collect(r, 7);
        check(first.isPresent(), "R4.2 one payment");
        checkEquals(1, first.orElseThrow().customer().getId(), "R4.2 tie broken by lower id");
        checkEquals(900, first.orElseThrow().amountCents(), "R4.3 locked price");
        checkEquals(100, first.orElseThrow().recordedSatisfaction(), "R4.3 satisfaction recorded");
        check(r.getTables().get(0).isFree(), "R4.3 table freed");
        checkEquals(900, r.getScoreboard().getRevenueCents(), "R4.3 revenue");
        checkEquals(1, r.getScoreboard().getPaidCount(), "only one paid");
        checkEquals(2, cashier.collect(r, 8).orElseThrow().customer().getId(), "other pays next turn");
        check(cashier.collect(r, 9).isEmpty(), "nobody left to pay");
    }
}
