package rush.domain;

import static rush.TestRunner.check;
import static rush.TestRunner.checkEquals;

public final class CustomerTest {
    private CustomerTest() { }

    public static void run() {
        Customer c = new RegularCustomer(1, 1);
        checkEquals(100, c.getSatisfaction(), "R1.1 starts at 100");
        checkEquals(CustomerStatus.WAITING, c.getStatus(), "R1.1 starts WAITING");
        checkEquals(8, c.getSatisfactionLossPerTurn(), "R5.1 regular loss");
        checkEquals("C1", c.getLabel(), "label");

        // Rule F1
        Menu menu = Menu.base();
        String[] expected = {"Salad", "Burger", "Pasta", "Salad", "Burger", "Pasta"};
        for (int i = 1; i <= 6; i++) {
            checkEquals(expected[i - 1], new RegularCustomer(i, 1).chooseDish(menu).getName(), "H5 F1 C" + i);
        }

        // R5.2 floor
        c.loseSatisfaction(150);
        checkEquals(0, c.getSatisfaction(), "R5.2 floors at 0");

        // illegal transitions are programmer errors
        boolean threw = false;
        try {
            new RegularCustomer(2, 1).pay();
        } catch (IllegalStateException e) {
            threw = true;
        }
        check(threw, "illegal transition throws");
    }
}
