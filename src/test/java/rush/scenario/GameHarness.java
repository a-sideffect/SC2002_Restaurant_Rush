package rush.scenario;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import rush.control.GameController;
import rush.control.ManagerInput;
import rush.domain.Cashier;
import rush.domain.Chef;
import rush.domain.GameConfig;
import rush.domain.GameOutcome;
import rush.domain.PriceCalculator;
import rush.domain.Restaurant;
import rush.domain.Staff;
import rush.domain.Waiter;
import rush.ui.ConsoleInput;
import rush.ui.ConsoleView;
import rush.ui.ScriptedInput;

/** Runs a full game against scripted or typed input and captures the transcript. */
public final class GameHarness {
    public final Restaurant restaurant;
    public final String output;
    public final GameOutcome outcome;

    private GameHarness(Restaurant restaurant, String output, GameOutcome outcome) {
        this.restaurant = restaurant;
        this.output = output;
        this.outcome = outcome;
    }

    public static List<String> readScript(String path) {
        try {
            return Files.readAllLines(Path.of(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("cannot read " + path, e);
        }
    }

    /** Plays with an optional script first, then falls back to {@code stdinText}. */
    public static GameHarness play(List<String> script, String stdinText) {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(buf, true, StandardCharsets.UTF_8);
        Restaurant r = new Restaurant(GameConfig.base());
        Waiter w = new Waiter("Waiter", new PriceCalculator());
        Chef c = new Chef("Chef");
        Cashier k = new Cashier("Cashier");
        List<Staff> staff = List.of(w, c, k);
        ConsoleView view = new ConsoleView(out);
        BufferedReader in = new BufferedReader(new StringReader(stdinText));
        ManagerInput input = script == null
                ? new ConsoleInput(in, out, view, r, staff)
                : new ScriptedInput(script, in, out, view, r, staff);
        GameOutcome outcome = new GameController(r, w, c, k, input, view).run();
        return new GameHarness(r, buf.toString(StandardCharsets.UTF_8), outcome);
    }

    /** Number of times {@code needle} appears in the transcript. */
    public int count(String needle) {
        int n = 0;
        for (int i = output.indexOf(needle); i >= 0; i = output.indexOf(needle, i + 1)) {
            n++;
        }
        return n;
    }
}
