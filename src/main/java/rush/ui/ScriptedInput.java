package rush.ui;

import java.io.BufferedReader;
import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import rush.domain.Restaurant;
import rush.domain.Staff;

/**
 * Replays commands from a script (one command per prompt; blank lines and '#' comments are skipped),
 * echoing each so the transcript reads like a live session. When the script runs out it falls back
 * to normal interactive input.
 */
public class ScriptedInput extends ConsoleInput {
    private final Deque<String> lines = new ArrayDeque<>();
    private final PrintStream echo;

    public ScriptedInput(List<String> scriptLines, BufferedReader in, PrintStream out, ConsoleView view,
                         Restaurant restaurant, List<Staff> staff) {
        super(in, out, view, restaurant, staff);
        this.echo = out;
        for (String line : scriptLines) {
            String t = stripBom(line).trim();
            if (!t.isEmpty() && !t.startsWith("#")) {
                lines.add(t);
            }
        }
    }

    @Override
    protected String readLine(String prompt) {
        if (lines.isEmpty()) {
            return super.readLine(prompt);
        }
        String next = lines.poll();
        echo.println(prompt + next);
        return next;
    }
}
