package rush.domain;

import static rush.TestRunner.check;
import static rush.TestRunner.checkEquals;

public final class OrderTest {
    private OrderTest() { }

    public static void run() {
        Customer c = new RegularCustomer(3, 1);
        Order pasta = new Order(c, Menu.base().itemAt(2), 1800);
        checkEquals(OrderStatus.PLACED, pasta.getStatus(), "R2.3 starts PLACED");
        check(pasta.isCookable(), "PLACED is cookable");
        check(!pasta.advanceOneUnit(), "R3.1 first pasta unit not ready");
        checkEquals(OrderStatus.COOKING, pasta.getStatus(), "COOKING after 1 unit");
        checkEquals("1/2", pasta.progressLabel(), "progress label");
        check(pasta.advanceOneUnit(), "R3.1 second pasta unit ready");
        checkEquals(OrderStatus.READY, pasta.getStatus(), "READY after 2 units");
        check(!pasta.isCookable(), "READY not cookable");

        Order salad = new Order(c, Menu.base().itemAt(0), 900);
        check(salad.advanceOneUnit(), "R3.1 salad needs 1 cook");

        pasta.markServed();
        checkEquals(OrderStatus.SERVED, pasta.getStatus(), "served");
        boolean threw = false;
        try {
            pasta.cancel();
        } catch (IllegalStateException e) {
            threw = true;
        }
        check(threw, "served order cannot be cancelled");
    }
}
