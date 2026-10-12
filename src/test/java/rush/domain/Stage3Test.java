package rush.domain;

import static rush.TestRunner.check;
import static rush.TestRunner.checkEquals;

import java.util.List;
import rush.TestConfigs;

/** Stage 3: Host, Combo Meal and Happy Hour. */
public final class Stage3Test {
    private Stage3Test() { }

    public static void run() {
        host();
        combos();
        happyHourState();
        happyHourPricing();
        happyHourRecovery();
        happyHourEndToEnd();
    }

    private static Waiter waiter() { return new Waiter("W", new PriceCalculator()); }

    private static void host() {
        Restaurant r = new Restaurant(GameConfig.base());
        Host h = new Host("H");
        checkEquals("Host", h.getRole(), "Host role label");
        checkEquals(List.of(), h.describeAvailableTasks(r, 1), "nothing to seat before anyone arrives");
        r.admitArrivals(1);
        r.admitArrivals(4);
        checkEquals(List.of("seat C1 at T1", "seat C2 at T1"), h.describeAvailableTasks(r, 4), "seat offered per queued customer");

        ActionResult ok = new SeatTask(2, null).execute(h, r, 4);
        check(ok.success(), "Host seats a queued customer");
        checkEquals("Seated C2 at T1.", ok.message(), "Host seat message");
        checkEquals(CustomerStatus.SEATED, r.findCustomer(2).orElseThrow().getStatus(), "C2 seated");

        check(!new SeatTask(2, null).execute(h, r, 4).success(), "Host cannot seat a seated customer");
        check(!new SeatTask(9, null).execute(h, r, 4).success(), "Host cannot seat an unknown customer");
        check(!new SeatTask(1, 1).execute(h, r, 4).success(), "Host cannot use an occupied table");
        checkEquals("Table 1 is occupied by C2. Free tables: 2.", new SeatTask(1, 1).execute(h, r, 4).message(),
                "occupied-table hint, same rules as the Waiter");
        check(new SeatTask(1, 2).execute(h, r, 4).success(), "Host seats at a chosen table");
        checkEquals(List.of(), h.describeAvailableTasks(r, 4), "no seat offered when tables are full");
        check(WaitTask.INSTANCE.execute(h, r, 4).success(), "Host can wait");
        checkEquals("Host waits.", WaitTask.INSTANCE.execute(h, r, 4).message(), "Host wait message");
    }

    private static void combos() {
        MenuItem salad = new MenuItem("Salad", 900, 1);
        MenuItem burger = new MenuItem("Burger", 1500, 1);
        MenuItem pasta = new MenuItem("Pasta", 1800, 2);
        ComboMeal a = new ComboMeal("Combo A", salad, burger);
        ComboMeal b = new ComboMeal("Combo B", salad, pasta);
        checkEquals(2160, a.getPriceCents(), "Combo A is 90% of $24.00");
        checkEquals(2, a.getPrepUnits(), "Combo A units add up");
        checkEquals(2430, b.getPriceCents(), "Combo B is 90% of $27.00");
        checkEquals(3, b.getPrepUnits(), "Combo B units add up");
        checkEquals("Combo A [Salad + Burger]", a.getDescription(), "combo description lists its parts");
        checkEquals("Salad", salad.getDescription(), "plain dish description is its name");
        // rounding: 90% of 905 = 814.5 -> 815 (half up)
        checkEquals(815, new ComboMeal("X", new MenuItem("p", 405, 1), new MenuItem("q", 500, 1)).getPriceCents(),
                "combo price rounds half up");

        // first three customers are unaffected by the combos; later ones rotate through them
        Menu m = Menu.base();
        String[] expected = {"Salad", "Burger", "Pasta", "Combo A", "Combo B", "Salad", "Burger"};
        for (int id = 1; id <= expected.length; id++) {
            checkEquals(expected[id - 1], new RegularCustomer(id, 1).chooseDish(m).getName(), "rule F1 on 5 items, C" + id);
        }
        // the Critic prefers a combo when it is the priciest
        checkEquals("Combo B", new CriticCustomer(4, 10).chooseDish(m).getName(), "Critic takes the priciest combo");

        // a combo needs one cook per unit and uses the sum of both prices
        Restaurant r = new Restaurant(GameConfig.base());
        Waiter w = waiter();
        Chef chef = new Chef("C");
        for (int t : new int[] {1, 4, 7, 10}) {
            r.admitArrivals(t);
        }
        Customer c4 = r.findCustomer(4).orElseThrow();
        r.seatCustomer(4, null);
        ActionResult order = w.takeOrder(r, 4, 10);
        check(order.message().contains("Combo B") && order.message().contains("$24.30")
                && order.message().contains("3 units"), "order message shows combo, price and units");
        Order o = c4.getOrder().orElseThrow();
        check(!chef.cook(r, 4, 10).message().contains("READY"), "combo needs more than one cook");
        chef.cook(r, 4, 11);
        check(!o.isReady(), "still cooking after 2 of 3 units");
        check(chef.cook(r, 4, 12).message().contains("READY"), "combo ready after 3 units");
        checkEquals(3, o.getUnitsDone(), "three units done");
    }

    private static void happyHourState() {
        HappyHour hh = new HappyHour();
        check(hh.isAvailable() && !hh.isActive(), "starts available and inactive");
        checkEquals(1000, hh.apply(1000), "no discount while inactive");
        check(hh.activate(), "first activation works");
        check(!hh.isAvailable() && hh.isActive(), "active and no longer available");
        checkEquals(2, hh.getRemainingTurns(), "lasts two turns");
        checkEquals(800, hh.apply(1000), "20% off while active");
        check(!hh.activate(), "cannot activate twice");
        hh.endTurn();
        checkEquals(1, hh.getRemainingTurns(), "one turn left after the first turn");
        check(hh.isActive(), "still active");
        hh.endTurn();
        check(!hh.isActive() && !hh.isAvailable(), "over after two turns and not available again");
        hh.endTurn();
        checkEquals(0, hh.getRemainingTurns(), "does not go negative");
        check(!hh.activate(), "still cannot activate after it ended");

        Restaurant r = new Restaurant(GameConfig.base());
        check(r.activateHappyHour().success(), "restaurant activation works");
        ActionResult again = r.activateHappyHour();
        check(!again.success(), "second activation is refused");
        checkEquals("Happy Hour has already been used this game.", again.message(), "refusal message");
    }

    private static void happyHourPricing() {
        PriceCalculator pc = new PriceCalculator();
        HappyHour off = new HappyHour();
        HappyHour on = new HappyHour();
        on.activate();
        MenuItem burger = Menu.base().itemAt(1);
        Customer regular = new RegularCustomer(1, 1);
        Customer vip = new VIPCustomer(2, 1);
        checkEquals(1500, pc.quote(regular, burger, off), "Regular, no Happy Hour");
        checkEquals(1200, pc.quote(regular, burger, on), "Regular with Happy Hour: 20% off");
        checkEquals(1350, pc.quote(vip, burger, off), "VIP, no Happy Hour");
        checkEquals(1080, pc.quote(vip, burger, on), "VIP then Happy Hour: 1500 -> 1350 -> 1080");
        checkEquals(1728, pc.quote(regular, Menu.base().itemAt(3), on), "Combo A with Happy Hour");
    }

    private static void happyHourRecovery() {
        Restaurant r = new Restaurant(TestConfigs.regularOnly());
        Waiter w = waiter();
        r.admitArrivals(1);
        r.admitArrivals(4);
        r.admitArrivals(7);
        check(r.applyHappyHourRecovery().isEmpty(), "no recovery before activation");
        r.seatCustomer(1, null);
        w.takeOrder(r, 1, 2);
        r.findCustomer(1).orElseThrow().loseSatisfaction(40);      // C1: ordered, 60
        r.findCustomer(2).orElseThrow().loseSatisfaction(95);      // C2: waiting, 5
        r.findCustomer(3).orElseThrow().loseSatisfaction(10);      // C3: waiting, 90
        check(r.applyHappyHourRecovery().isEmpty(), "still no recovery while inactive");
        checkEquals(60, r.findCustomer(1).orElseThrow().getSatisfaction(), "C1 unchanged while inactive");

        r.activateHappyHour();
        List<SatisfactionChange> changes = r.applyHappyHourRecovery();
        checkEquals(3, changes.size(), "all three awaiting customers recover");
        checkEquals(75, r.findCustomer(1).orElseThrow().getSatisfaction(), "C1 60 -> 75");
        checkEquals(20, r.findCustomer(2).orElseThrow().getSatisfaction(), "C2 5 -> 20");
        checkEquals(100, r.findCustomer(3).orElseThrow().getSatisfaction(), "C3 90 -> 100 (capped)");
        checkEquals(90, changes.get(2).before(), "change record keeps the old value");
        checkEquals(100, changes.get(2).after(), "change record keeps the new value");

        // customers who are eating, paid or gone do not recover
        Restaurant r2 = new Restaurant(TestConfigs.regularOnly());
        Waiter w2 = waiter();
        Chef chef = new Chef("C");
        r2.admitArrivals(1);
        r2.seatCustomer(1, null);
        w2.takeOrder(r2, 1, 2);
        chef.cook(r2, 1, 2);
        r2.findCustomer(1).orElseThrow().loseSatisfaction(30);
        w2.serve(r2, 1, 3);
        r2.activateHappyHour();
        check(r2.applyHappyHourRecovery().isEmpty(), "an eating customer does not recover");
        checkEquals(70, r2.findCustomer(1).orElseThrow().getSatisfaction(), "eating customer unchanged");

        Restaurant r3 = new Restaurant(TestConfigs.regularOnly());
        r3.admitArrivals(1);
        Customer gone = r3.findCustomer(1).orElseThrow();
        for (int i = 0; i < 13; i++) {
            r3.applyEndOfTurnLoss(i + 1);
        }
        checkEquals(CustomerStatus.LEFT, gone.getStatus(), "customer left");
        r3.activateHappyHour();
        check(r3.applyHappyHourRecovery().isEmpty(), "a customer who left does not recover");
        checkEquals(0, gone.getSatisfaction(), "and stays at 0");
    }

    /** Orders taken during Happy Hour are cheaper; orders taken before it keep their old price. */
    private static void happyHourEndToEnd() {
        Restaurant r = new Restaurant(TestConfigs.regularOnly());
        Waiter w = waiter();
        Chef chef = new Chef("C");
        Cashier cashier = new Cashier("K");
        r.admitArrivals(1);
        r.admitArrivals(4);
        r.seatCustomer(1, null);
        r.seatCustomer(2, null);
        w.takeOrder(r, 1, 4);                       // before Happy Hour: Salad $9.00
        r.activateHappyHour();
        w.takeOrder(r, 2, 4);                       // during: Burger $15.00 -> $12.00
        checkEquals(900, r.findCustomer(1).orElseThrow().getOrder().orElseThrow().getLockedPriceCents(),
                "existing order keeps its price");
        checkEquals(1200, r.findCustomer(2).orElseThrow().getOrder().orElseThrow().getLockedPriceCents(),
                "new order gets 20% off");
        chef.cook(r, 1, 4);
        chef.cook(r, 2, 5);
        w.serve(r, 1, 5);
        w.serve(r, 2, 6);
        r.finishEating(6);
        r.finishEating(7);
        cashier.collect(r, 7);
        cashier.collect(r, 8);
        checkEquals(2100, r.getScoreboard().getRevenueCents(), "revenue is $9.00 + $12.00");
        r.endHappyHourTurn();
        r.endHappyHourTurn();
        check(!r.getHappyHour().isActive(), "Happy Hour ended");
    }
}
