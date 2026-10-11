package rush.domain;

import static rush.TestRunner.checkEquals;

public final class MoneyTest {
    private MoneyTest() { }

    public static void run() {
        checkEquals("$0.00", Money.format(0), "H7 zero");
        checkEquals("$0.05", Money.format(5), "H7 five cents");
        checkEquals("$18.30", Money.format(1830), "H7 1830");
        checkEquals("$66.00", Money.format(6600), "H7 6600");
        checkEquals("Pasta ($18.00, 2 units)", Menu.base().itemAt(2).toString(), "MenuItem toString");
        checkEquals(3, Menu.base().size(), "H3 menu size");
        checkEquals(900, Menu.base().itemAt(0).getPriceCents(), "H3 salad price");
        checkEquals(1500, Menu.base().itemAt(1).getPriceCents(), "H3 burger price");
        checkEquals(2, Menu.base().itemAt(2).getPrepUnits(), "H3 pasta prep");
    }
}
