package rush.domain;

import static rush.TestRunner.check;
import static rush.TestRunner.checkEquals;

import java.util.List;

public final class RestaurantTest {
    private RestaurantTest() { }

    public static void run() {
        arrivals();
        seating();
        lossAndDeparture();
    }

    private static void arrivals() {
        Restaurant r = new Restaurant(GameConfig.base());
        int[] arrivalTurns = {1, 4, 7, 10, 13, 16};
        int total = 0;
        for (int t = 1; t <= 18; t++) {
            List<Customer> arrived = r.admitArrivals(t);
            boolean expected = false;
            for (int a : arrivalTurns) {
                expected |= a == t;
            }
            checkEquals(expected ? 1 : 0, arrived.size(), "R1.1 arrivals on turn " + t);
            total += arrived.size();
        }
        checkEquals(6, total, "six arrivals");
        checkEquals(6, r.getAllCustomers().size(), "six customers");
        checkEquals(6, r.getQueue().size(), "all queued (none seated)");
        checkEquals("C6", r.getAllCustomers().get(5).getLabel(), "R1.2 ids in order");
        checkEquals(2, r.getTables().size(), "H2 two tables");
    }

    private static void seating() {
        Restaurant r = new Restaurant(GameConfig.base());
        r.admitArrivals(1);
        ActionResult noSuch = r.seatCustomer(9, null);
        check(!noSuch.success(), "seat unknown customer fails");
        checkEquals("There is no customer C9.", noSuch.message(), "unknown customer message");

        ActionResult ok = r.seatCustomer(1, 1);
        check(ok.success(), "R2.2 seat succeeds");
        checkEquals(CustomerStatus.SEATED, r.findCustomer(1).orElseThrow().getStatus(), "SEATED");
        check(r.getQueue().isEmpty(), "removed from queue");
        check(!r.getTables().get(0).isFree(), "table occupied");

        check(!r.seatCustomer(1, 2).success(), "cannot seat an already seated customer");
        checkEquals("C1 is not waiting in the queue (status: SEATED).",
                r.seatCustomer(1, 2).message(), "status message");

        for (int t = 2; t <= 4; t++) {
            r.admitArrivals(t);
        }
        // C2 arrived on turn 4
        ActionResult occupied = r.seatCustomer(2, 1);
        checkEquals("Table 1 is occupied by C1. Free tables: 2.", occupied.message(), "occupied hint");
        check(r.seatCustomer(2, null).success(), "default table is lowest free");
        checkEquals(2, r.findCustomer(2).orElseThrow().getTable().orElseThrow().getNumber(), "went to T2");

        r.admitArrivals(7);
        ActionResult none = r.seatCustomer(3, null);
        check(!none.success(), "no free table fails");
        check(none.message().startsWith("No free table"), "no free table message");
        check(r.findCustomer(3).orElseThrow().getStatus() == CustomerStatus.WAITING, "C3 unchanged");
    }

    private static void lossAndDeparture() {
        Restaurant r = new Restaurant(GameConfig.base());
        Waiter w = new Waiter("W", new PriceCalculator());
        r.admitArrivals(1);
        r.seatCustomer(1, 1);
        w.takeOrder(r, 1);
        Customer c = r.findCustomer(1).orElseThrow();
        // 100 -> 92 -> ... -> 4 after 12 turns, 0 on the 13th
        for (int i = 1; i <= 12; i++) {
            check(r.applyEndOfTurnLoss(i).isEmpty(), "R5.1 nobody leaves yet, loss " + i);
        }
        checkEquals(4, c.getSatisfaction(), "R5.1 after 12 losses");
        List<Customer> left = r.applyEndOfTurnLoss(13);
        checkEquals(1, left.size(), "R5.3 customer leaves at 0");
        checkEquals(0, c.getSatisfaction(), "R5.2 floored");
        checkEquals(CustomerStatus.LEFT, c.getStatus(), "LEFT");
        checkEquals(OrderStatus.CANCELLED, c.getOrder().orElseThrow().getStatus(), "R5.3 order cancelled");
        check(r.getTables().get(0).isFree(), "R5.3 table freed");
        checkEquals(1, r.getScoreboard().getDepartures(), "departure counted");
        checkEquals(0, r.getScoreboard().getRevenueCents(), "R-A3 no revenue");
        check(r.getCookableOrders().isEmpty(), "cancelled order not cookable");
        check(r.applyEndOfTurnLoss(14).isEmpty(), "R5.4 left customers lose nothing more");
    }
}
