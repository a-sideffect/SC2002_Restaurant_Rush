package rush.domain;

import static rush.TestRunner.check;
import static rush.TestRunner.checkEquals;

/** Stage 2: VIP and Critic customers, the bill hook and percentage rounding. */
public final class CustomerTypesTest {
    private CustomerTypesTest() { }

    public static void run() {
        money();
        vip();
        critic();
        baseSchedule();
    }

    private static void money() {
        checkEquals(135, Money.percentOf(1350, 10), "10% of 1350");
        checkEquals(91, Money.percentOf(905, 10), "10% of 905 rounds half up (90.5 -> 91)");
        checkEquals(90, Money.percentOf(904, 10), "10% of 904 rounds down (90.4 -> 90)");
        checkEquals(0, Money.percentOf(4, 10), "10% of 4 cents (0.4) is 0");
        checkEquals(1, Money.percentOf(5, 10), "10% of 5 cents (0.5) is 1");
        checkEquals(810, Money.discounted(900, 10), "salad with 10% off");
        checkEquals(1350, Money.discounted(1500, 10), "burger with 10% off");
        checkEquals(1620, Money.discounted(1800, 10), "pasta with 10% off");
        checkEquals(1000, Money.discounted(1000, 0), "0% off changes nothing");
    }

    private static void vip() {
        Customer v = new VIPCustomer(2, 4);
        checkEquals(5, v.getSatisfactionLossPerTurn(), "VIP loses 5 per turn");
        checkEquals("VIP", v.getTypeLabel(), "VIP label");
        checkEquals(1350, v.adjustBill(1500), "VIP bill is 10% off");
        checkEquals(1500, new RegularCustomer(1, 1).adjustBill(1500), "Regular pays full price");
        // price is locked through the shared PriceCalculator
        PriceCalculator pc = new PriceCalculator();
        checkEquals(1350, pc.quote(v, Menu.base().itemAt(1), new HappyHour()), "quote for VIP burger");
        checkEquals(1500, pc.quote(new RegularCustomer(1, 1), Menu.base().itemAt(1), new HappyHour()), "quote for Regular burger");
        // F1 is unchanged for a VIP
        checkEquals("Burger", v.chooseDish(Menu.base()).getName(), "VIP C2 follows rule F1");

        // end to end: the VIP's order and payment use the discounted price
        Restaurant r = new Restaurant(GameConfig.base());
        Waiter w = new Waiter("W", pc);
        Chef chef = new Chef("C");
        Cashier cashier = new Cashier("K");
        r.admitArrivals(1);
        r.admitArrivals(4);
        checkEquals(VIPCustomer.class, r.findCustomer(2).orElseThrow().getClass(), "C2 is the VIP in the base run");
        w.seat(r, 2, null);
        ActionResult order = w.takeOrder(r, 2);
        check(order.message().contains("$13.50"), "order message shows the discounted price");
        chef.cook(r, 2);
        w.serve(r, 2, 5);
        r.finishEating(6);
        checkEquals(1350, cashier.collect(r, 7).orElseThrow().amountCents(), "VIP pays the discounted amount");
        checkEquals(1350, r.getScoreboard().getRevenueCents(), "revenue counts the discounted amount");

        // VIP loses 5 per turn and survives longer than a Regular
        Customer waiting = new VIPCustomer(9, 1);
        for (int i = 0; i < 19; i++) {
            waiting.loseSatisfaction(waiting.getSatisfactionLossPerTurn());
        }
        checkEquals(5, waiting.getSatisfaction(), "VIP is at 5 after 19 turns");
    }

    private static void critic() {
        Customer c = new CriticCustomer(4, 10);
        checkEquals(12, c.getSatisfactionLossPerTurn(), "Critic loses 12 per turn");
        checkEquals("Critic", c.getTypeLabel(), "Critic label");
        checkEquals("Combo B", c.chooseDish(Menu.base()).getName(), "Critic orders the priciest item on the menu");
        checkEquals("Combo A", new RegularCustomer(4, 10).chooseDish(Menu.base()).getName(),
                "a Regular C4 would order Combo A (rule F1)");
        checkEquals(2430, c.adjustBill(2430), "Critic pays full price");

        Menu tie = new Menu(java.util.List.of(new MenuItem("A", 500, 1), new MenuItem("B", 500, 1)));
        checkEquals("A", c.chooseDish(tie).getName(), "price tie goes to the first listed item");
        Menu single = new Menu(java.util.List.of(new MenuItem("Only", 700, 1)));
        checkEquals("Only", c.chooseDish(single).getName(), "single-item menu");

        // a Critic who waits too long leaves: 12 per turn from 100 hits 0 on the 9th loss
        Customer waiting = new CriticCustomer(9, 1);
        for (int i = 0; i < 8; i++) {
            waiting.loseSatisfaction(waiting.getSatisfactionLossPerTurn());
        }
        checkEquals(4, waiting.getSatisfaction(), "Critic is at 4 after 8 losses");
        waiting.loseSatisfaction(waiting.getSatisfactionLossPerTurn());
        checkEquals(0, waiting.getSatisfaction(), "Critic floors at 0");
    }

    private static void baseSchedule() {
        Restaurant r = new Restaurant(GameConfig.base());
        for (int t = 1; t <= 18; t++) {
            r.admitArrivals(t);
        }
        checkEquals(6, r.getAllCustomers().size(), "still six arrivals");
        int vips = 0;
        int critics = 0;
        for (Customer c : r.getAllCustomers()) {
            if (c.getTypeLabel().equals("VIP")) {
                vips++;
            }
            if (c.getTypeLabel().equals("Critic")) {
                critics++;
            }
        }
        checkEquals(1, vips, "base run has one VIP");
        checkEquals(1, critics, "base run has one Critic");
        checkEquals(4, r.findCustomer(2).orElseThrow().getArrivalTurn(), "VIP arrives on turn 4");
        checkEquals(10, r.findCustomer(4).orElseThrow().getArrivalTurn(), "Critic arrives on turn 10");
    }
}
