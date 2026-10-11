package rush.ui;

import static rush.TestRunner.check;
import static rush.TestRunner.checkEquals;

import rush.domain.CookTask;
import rush.domain.SeatTask;
import rush.domain.ServeTask;
import rush.domain.TakeOrderTask;
import rush.domain.WaitTask;

public final class CommandParserTest {
    private static final CommandParser P = new CommandParser();

    private CommandParserTest() { }

    public static void run() throws Exception {
        valid();
        waiterErrors();
        chefErrors();
    }

    private static void valid() throws Exception {
        checkEquals(new SeatTask(3, null), P.parseWaiter("seat 3"), "seat default table");
        checkEquals(new SeatTask(4, 2), P.parseWaiter("seat 4 2"), "seat with table");
        checkEquals(new SeatTask(3, 1), P.parseWaiter("SEAT c3 T1"), "mixed case and prefixes");
        checkEquals(new SeatTask(3, 1), P.parseWaiter("   seat    C3    t1   "), "extra whitespace");
        checkEquals(new TakeOrderTask(2), P.parseWaiter("order 2"), "order");
        checkEquals(new ServeTask(5), P.parseWaiter("Serve C5"), "serve");
        check(P.parseWaiter("wait") == WaitTask.INSTANCE, "waiter wait");
        check(P.parseWaiter("w") == WaitTask.INSTANCE, "waiter w");
        checkEquals(new CookTask(2), P.parseChef("cook 2"), "cook");
        checkEquals(new CookTask(6), P.parseChef("COOK c6"), "cook case");
        check(P.parseChef("wait") == WaitTask.INSTANCE, "chef wait");
        check(P.parseChef(" W ") == WaitTask.INSTANCE, "chef w");
    }

    private static void expectWaiterError(String line, String contains) {
        try {
            P.parseWaiter(line);
            check(false, "waiter should reject '" + line + "'");
        } catch (ParseError e) {
            check(e.getMessage().contains(contains), "waiter '" + line + "' message: " + e.getMessage());
        }
    }

    private static void expectChefError(String line, String contains) {
        try {
            P.parseChef(line);
            check(false, "chef should reject '" + line + "'");
        } catch (ParseError e) {
            check(e.getMessage().contains(contains), "chef '" + line + "' message: " + e.getMessage());
        }
    }

    private static void waiterErrors() {
        expectWaiterError("", "Please type a command");
        expectWaiterError("   ", "Please type a command");
        expectWaiterError("serv 1", "Unknown command 'serv'");
        expectWaiterError("cook 1", "'cook' is a Chef task");
        expectWaiterError("seat", "'seat' needs a customer id");
        expectWaiterError("seat abc", "not a valid customer id");
        expectWaiterError("seat 3 x", "not a valid table id");
        expectWaiterError("seat 3 1 2", "takes a customer and an optional table");
        expectWaiterError("seat t1", "not a valid customer id");
        expectWaiterError("seat -1", "not a valid customer id");
        expectWaiterError("seat 3.5", "not a valid customer id");
        expectWaiterError("seat 99999999999999", "not a valid customer id");
        expectWaiterError("order", "'order' needs a customer id");
        expectWaiterError("order 1 2", "exactly one customer id");
        expectWaiterError("serve x", "not a valid customer id");
        expectWaiterError("wait 3", "takes no arguments");
        expectWaiterError("c3", "Unknown command 'c3'");
    }

    private static void chefErrors() {
        expectChefError("", "Please type a command");
        expectChefError("seat 1", "'seat' is a Waiter task");
        expectChefError("order 1", "'order' is a Waiter task");
        expectChefError("serve 1", "'serve' is a Waiter task");
        expectChefError("cook", "'cook' needs a customer id");
        expectChefError("cook 1 2", "exactly one customer id");
        expectChefError("bake 1", "Unknown command 'bake'");
        expectChefError("wait now", "takes no arguments");
    }
}
