package rush;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import rush.control.GameController;
import rush.control.ManagerInput;
import rush.domain.Cashier;
import rush.domain.Chef;
import rush.domain.GameConfig;
import rush.domain.Host;
import rush.domain.PriceCalculator;
import rush.domain.Restaurant;
import rush.domain.Staff;
import rush.domain.Waiter;
import rush.ui.ConsoleInput;
import rush.ui.ConsoleView;
import rush.ui.QuitRequested;
import rush.ui.ScriptedInput;

/**
 * Wires the game together.
 * Usage: java -cp out rush.Main [script.txt]
 */
public final class Main {
    private Main() { }

    public static void main(String[] args) {
        if (args.length > 1) {
            System.err.println("Usage: java -cp out rush.Main [script.txt]");
            System.exit(2);
        }
        List<String> script = null;
        if (args.length == 1) {
            try {
                script = Files.readAllLines(Path.of(args[0]), StandardCharsets.UTF_8);
            } catch (IOException e) {
                System.err.println("Cannot read script '" + args[0] + "': " + e.getMessage());
                System.exit(2);
            }
        }

        Restaurant restaurant = new Restaurant(GameConfig.base());
        Waiter waiter = new Waiter("Waiter", new PriceCalculator());
        Host host = new Host("Host");
        Chef chef = new Chef("Chef");
        Cashier cashier = new Cashier("Cashier");
        List<Staff> staff = List.of(waiter, host, chef, cashier);
        ConsoleView view = new ConsoleView(System.out);
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

        ManagerInput input = script == null
                ? new ConsoleInput(stdin, System.out, view, restaurant, staff)
                : new ScriptedInput(script, stdin, System.out, view, restaurant, staff);

        try {
            new GameController(restaurant, waiter, host, chef, cashier, input, view).run();
        } catch (QuitRequested q) {
            System.out.println("Game ended by player.");
        }
    }
}
