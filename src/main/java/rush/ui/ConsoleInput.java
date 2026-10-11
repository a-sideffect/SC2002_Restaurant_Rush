package rush.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Locale;
import rush.control.ManagerInput;
import rush.domain.ChefTask;
import rush.domain.Restaurant;
import rush.domain.Staff;
import rush.domain.WaiterTask;

/**
 * Interactive input. Handles help/status/menu/quit itself and re-prompts on parse errors.
 * If the input stream closes, every remaining prompt is treated as "wait" so the game still finishes.
 */
public class ConsoleInput implements ManagerInput {

    @FunctionalInterface
    private interface Parse<T> {
        T parse(String line) throws ParseError;
    }

    private final BufferedReader in;
    private final PrintStream out;
    private final ConsoleView view;
    private final Restaurant restaurant;
    private final List<Staff> staff;
    private final CommandParser parser = new CommandParser();
    private boolean eofReported;

    public ConsoleInput(BufferedReader in, PrintStream out, ConsoleView view,
                        Restaurant restaurant, List<Staff> staff) {
        this.in = in;
        this.out = out;
        this.view = view;
        this.restaurant = restaurant;
        this.staff = staff;
    }

    @Override
    public WaiterTask nextWaiterTask(int turn) {
        return prompt("Waiter", turn, parser::parseWaiter);
    }

    @Override
    public ChefTask nextChefTask(int turn) {
        return prompt("Chef", turn, parser::parseChef);
    }

    /** Reads one line after printing the prompt; null at end of input. */
    protected String readLine(String prompt) {
        out.print(prompt);
        out.flush();
        try {
            return stripBom(in.readLine());
        } catch (IOException e) {
            return null;
        }
    }

    /** Windows editors and shells may prepend an invisible byte-order mark; it is not part of the command. */
    protected static String stripBom(String line) {
        return line != null && !line.isEmpty() && line.charAt(0) == '\uFEFF' ? line.substring(1) : line;
    }

    private <T> T prompt(String role, int turn, Parse<T> parse) {
        while (true) {
            String line = readLine(role + "> ");
            if (line == null) {
                if (!eofReported) {
                    eofReported = true;
                    out.println();
                    view.showInfo("(input closed - remaining turns will wait)");
                }
                return parseWait(parse);
            }
            if (handleMeta(line, role, turn)) {
                continue;
            }
            try {
                return parse.parse(line);
            } catch (ParseError e) {
                view.showError(e.getMessage());
            }
        }
    }

    private static <T> T parseWait(Parse<T> parse) {
        try {
            return parse.parse("wait");
        } catch (ParseError e) {
            throw new IllegalStateException("'wait' must always parse", e);
        }
    }

    private boolean handleMeta(String line, String role, int turn) {
        String cmd = line.trim().toLowerCase(Locale.ROOT);
        switch (cmd) {
            case "help":
            case "?":
                view.showHelp(role);
                return true;
            case "status":
                view.showTurnStart(turn, restaurant, staff);
                return true;
            case "menu":
                view.showMenu(restaurant.getMenu());
                return true;
            case "quit":
            case "exit":
                String answer = readLine("  Really quit? (y/n) ");
                if (answer != null && answer.trim().toLowerCase(Locale.ROOT).startsWith("y")) {
                    throw new QuitRequested();
                }
                return true;
            default:
                return false;
        }
    }
}
