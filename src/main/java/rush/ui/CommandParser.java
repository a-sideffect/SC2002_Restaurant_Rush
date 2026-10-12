package rush.ui;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import rush.domain.ChefTask;
import rush.domain.CookTask;
import rush.domain.HostTask;
import rush.domain.SeatTask;
import rush.domain.ServeTask;
import rush.domain.TakeOrderTask;
import rush.domain.WaitTask;
import rush.domain.WaiterTask;

/**
 * Turns a typed line into a task. Pure text handling, no game rules. Case-insensitive;
 * the "C" and "T" prefixes on ids are optional.
 */
public class CommandParser {
    private static final String WAITER_CMDS = "seat, order, serve, wait";
    private static final String CHEF_CMDS = "cook, wait";
    private static final String HOST_CMDS = "seat, wait";
    private static final Pattern CUSTOMER_ID = Pattern.compile("c?(\\d{1,6})");
    private static final Pattern TABLE_ID = Pattern.compile("t?(\\d{1,6})");

    public WaiterTask parseWaiter(String line) throws ParseError {
        String[] tok = tokenize(line, "Waiter", WAITER_CMDS);
        String cmd = tok[0];
        switch (cmd) {
            case "seat":
                return parseSeat(tok);
            case "order":
                return new TakeOrderTask(singleCustomer(tok, "order"));
            case "serve":
                return new ServeTask(singleCustomer(tok, "serve"));
            case "wait":
            case "w":
                noArgs(tok, "wait");
                return WaitTask.INSTANCE;
            case "cook":
                throw new ParseError("'cook' is a Chef task. Waiter can: " + WAITER_CMDS + ".");
            default:
                throw unknown(tok[0], "Waiter", WAITER_CMDS);
        }
    }

    public HostTask parseHost(String line) throws ParseError {
        String[] tok = tokenize(line, "Host", HOST_CMDS);
        switch (tok[0]) {
            case "seat":
                return parseSeat(tok);
            case "wait":
            case "w":
                noArgs(tok, "wait");
                return WaitTask.INSTANCE;
            case "order":
            case "serve":
                throw new ParseError("'" + tok[0] + "' is a Waiter task. Host can: " + HOST_CMDS + ".");
            case "cook":
                throw new ParseError("'cook' is a Chef task. Host can: " + HOST_CMDS + ".");
            default:
                throw unknown(tok[0], "Host", HOST_CMDS);
        }
    }

    /** Answer to the Happy Hour question: yes or no. */
    public boolean parseYesNo(String line) throws ParseError {
        String answer = line == null ? "" : line.trim().toLowerCase(Locale.ROOT);
        if (answer.equals("yes") || answer.equals("y")) {
            return true;
        }
        if (answer.equals("no") || answer.equals("n")) {
            return false;
        }
        throw new ParseError("Please answer yes or no. Happy Hour can only be started once per game.");
    }

    public ChefTask parseChef(String line) throws ParseError {
        String[] tok = tokenize(line, "Chef", CHEF_CMDS);
        switch (tok[0]) {
            case "cook":
                return new CookTask(singleCustomer(tok, "cook"));
            case "wait":
            case "w":
                noArgs(tok, "wait");
                return WaitTask.INSTANCE;
            case "seat":
            case "order":
            case "serve":
                throw new ParseError("'" + tok[0] + "' is a Waiter task. Chef can: " + CHEF_CMDS + ".");
            default:
                throw unknown(tok[0], "Chef", CHEF_CMDS);
        }
    }

    private static SeatTask parseSeat(String[] tok) throws ParseError {
        if (tok.length == 1) {
            throw new ParseError("'seat' needs a customer id, e.g. 'seat 3' or 'seat 3 1'.");
        }
        if (tok.length > 3) {
            throw new ParseError("'seat' takes a customer and an optional table, e.g. 'seat 3 1'.");
        }
        int customer = parseId(CUSTOMER_ID, tok[1], "customer", "'seat 3' or 'seat 3 1'");
        Integer table = tok.length == 3
                ? Integer.valueOf(parseId(TABLE_ID, tok[2], "table", "'seat 3 1'")) : null;
        return new SeatTask(customer, table);
    }

    private static String[] tokenize(String line, String role, String cmds) throws ParseError {
        String trimmed = line == null ? "" : line.trim().toLowerCase(Locale.ROOT);
        if (trimmed.isEmpty()) {
            throw new ParseError("Please type a command. " + role + " can: " + cmds + ". Type 'help'.");
        }
        return trimmed.split("\\s+");
    }

    private static ParseError unknown(String cmd, String role, String cmds) {
        return new ParseError("Unknown command '" + cmd + "'. " + role + " can: " + cmds + ". Type 'help'.");
    }

    private static int singleCustomer(String[] tok, String cmd) throws ParseError {
        if (tok.length == 1) {
            throw new ParseError("'" + cmd + "' needs a customer id, e.g. '" + cmd + " 3'.");
        }
        if (tok.length > 2) {
            throw new ParseError("'" + cmd + "' takes exactly one customer id, e.g. '" + cmd + " 3'.");
        }
        return parseId(CUSTOMER_ID, tok[1], "customer", "'" + cmd + " 3'");
    }

    private static void noArgs(String[] tok, String cmd) throws ParseError {
        if (tok.length > 1) {
            throw new ParseError("'" + cmd + "' takes no arguments.");
        }
    }

    private static int parseId(Pattern p, String text, String what, String example) throws ParseError {
        Matcher m = p.matcher(text);
        if (!m.matches()) {
            throw new ParseError("'" + text + "' is not a valid " + what + " id. Use e.g. " + example + ".");
        }
        return Integer.parseInt(m.group(1));
    }
}
