package rush.ui;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.stream.Collectors;
import rush.control.TurnReport;
import rush.domain.ActionResult;
import rush.domain.Customer;
import rush.domain.CustomerStatus;
import rush.domain.GameConfig;
import rush.domain.GameOutcome;
import rush.domain.Menu;
import rush.domain.MenuItem;
import rush.domain.Money;
import rush.domain.Order;
import rush.domain.Payment;
import rush.domain.Restaurant;
import rush.domain.Scoreboard;
import rush.domain.Staff;
import rush.domain.Table;
import rush.domain.VictoryTargets;

/** All console formatting. ASCII only, because Windows consoles may not render anything else. */
public class ConsoleView {
    private static final String RULE = "-".repeat(62);
    private static final String DOUBLE_RULE = "=".repeat(62);

    private final PrintStream out;

    public ConsoleView(PrintStream out) { this.out = out; }

    public void showWelcome(GameConfig config, List<Staff> staff) {
        VictoryTargets t = config.targets();
        out.println(DOUBLE_RULE);
        out.println(String.format("%-40s setting: %s", "  RESTAURANT RUSH  -  Stage 1", config.name()));
        out.println(DOUBLE_RULE);
        out.println(" " + config.maxTurns() + " turns | " + config.tableCount() + " tables | Staff: "
                + staff.stream().map(Staff::getRole).collect(Collectors.joining(", ")));
        out.println(" Customers arrive on turns "
                + config.arrivals().getArrivalTurns().stream().map(String::valueOf)
                        .collect(Collectors.joining(", ")) + ".");
        out.println(" WIN at end of turn " + config.maxTurns() + ": >= " + t.minPaid()
                + " paid AND revenue >= " + Money.format(t.minRevenueCents()));
        out.println("                        AND avg satisfaction (paid) >= " + t.minAvgSatisfaction());
        out.println(" Menu: " + menuLine(config.menu()));
        out.println(" Type 'help' at any prompt for commands.");
        out.println(DOUBLE_RULE);
    }

    public void showArrivals(List<Customer> arrived) {
        for (Customer c : arrived) {
            out.println(" >> " + c.getLabel() + " (" + c.getTypeLabel() + ") arrives and joins the queue.");
        }
    }

    /** Start-of-turn board (D1). */
    public void showTurnStart(int turn, Restaurant r, List<Staff> staff) {
        Scoreboard s = r.getScoreboard();
        out.println();
        out.println(RULE);
        out.println(" TURN " + turn + " / " + r.getConfig().maxTurns()
                + "     Revenue " + Money.format(s.getRevenueCents())
                + "     Avg satisfaction (paid) " + average(s.averageSatisfaction()));
        out.println(RULE);

        out.println(" WAITING QUEUE");
        if (r.getQueue().isEmpty()) {
            out.println("   (empty)");
        }
        for (Customer c : r.getQueue()) {
            out.println(String.format("   %-3s %-8s sat %3d  waiting since turn %d",
                    c.getLabel(), c.getTypeLabel(), c.getSatisfaction(), c.getArrivalTurn()));
        }

        out.println();
        out.println(" TABLES");
        for (Table t : r.getTables()) {
            if (t.isFree()) {
                out.println("   T" + t.getNumber() + "  -- free --");
            } else {
                Customer c = t.getOccupant().orElseThrow();
                out.println(String.format("   T%d  %-3s %-8s sat %3d  %-12s %s", t.getNumber(), c.getLabel(),
                        c.getTypeLabel(), c.getSatisfaction(), c.getStatus(), orderText(c)));
            }
        }

        out.println();
        out.println(" KITCHEN");
        List<Order> kitchen = new ArrayList<>(r.getCookableOrders());
        kitchen.addAll(r.getReadyOrders());
        kitchen.sort((a, b) -> Integer.compare(a.getCustomer().getId(), b.getCustomer().getId()));
        if (kitchen.isEmpty()) {
            out.println("   (no orders)");
        }
        for (Order o : kitchen) {
            out.println(String.format("   %-3s %-7s %s (%s)", o.getCustomer().getLabel(),
                    o.getItem().getName(), o.getStatus(), o.progressLabel()));
        }

        out.println();
        out.println(" PAID SO FAR");
        List<String> paid = r.getAllCustomers().stream()
                .filter(c -> c.getStatus() == CustomerStatus.PAID)
                .map(c -> c.getLabel() + "  " + Money.format(c.getOrder().orElseThrow().getLockedPriceCents())
                        + "  sat " + c.getRecordedSatisfaction().orElseThrow())
                .toList();
        out.println("   " + (paid.isEmpty() ? "(nobody yet)" : String.join("   |   ", paid)));

        out.println();
        out.println(" STAFF TASKS AVAILABLE");
        for (Staff st : staff) {
            List<String> tasks = st.describeAvailableTasks(r, turn);
            out.println(String.format("   %-8s: %s", st.getRole(),
                    tasks.isEmpty() ? "nothing available" : String.join(" | ", tasks)));
        }
        out.println(RULE);
    }

    public void showResult(ActionResult res) {
        out.println(res.success() ? "  OK: " + res.message() : "  X " + res.message());
    }

    public void showError(String message) { out.println("  X " + message); }

    public void showInfo(String message) { out.println("  " + message); }

    /** End-of-turn results (D2). */
    public void showEndOfTurn(int turn, TurnReport rep, Scoreboard s, GameConfig config) {
        out.println();
        out.println(" END OF TURN " + turn);
        if (rep.payment().isPresent()) {
            Payment p = rep.payment().get();
            out.println("   $ Cashier collects " + Money.format(p.amountCents()) + " from "
                    + p.customer().getLabel() + " (satisfaction " + p.recordedSatisfaction()
                    + "), table T" + p.tableNumber() + " is free.");
        }
        for (Customer c : rep.finishedEating()) {
            out.println("   * " + c.getLabel() + " finishes eating and is ready to pay.");
        }
        for (Map.Entry<Customer, Integer> e : rep.satisfactionBefore().entrySet()) {
            int after = e.getKey().getSatisfaction();
            if (after < e.getValue()) {
                out.println("   * " + e.getKey().getLabel() + " loses " + (e.getValue() - after)
                        + " satisfaction (" + e.getValue() + " -> " + after + ")");
            }
        }
        for (Customer c : rep.departed()) {
            out.println("   ! " + c.getLabel() + " runs out of patience and leaves unhappy.");
        }
        out.println(" Served " + s.getServedCount() + " | Unhappy departures " + s.getDepartures()
                + " | Revenue " + Money.format(s.getRevenueCents())
                + " | Avg sat (paid) " + average(s.averageSatisfaction())
                + " | Turns used " + turn + "/" + config.maxTurns());
    }

    public void showFinal(GameOutcome outcome, Scoreboard s, Restaurant r) {
        VictoryTargets t = r.getConfig().targets();
        out.println();
        out.println(DOUBLE_RULE);
        out.println(" FINAL RESULT");
        out.println(targetRow("Paid customers", String.valueOf(s.getPaidCount()),
                String.valueOf(t.minPaid()), t.paidMet(s)));
        out.println(targetRow("Revenue", Money.format(s.getRevenueCents()),
                Money.format(t.minRevenueCents()), t.revenueMet(s)));
        out.println(targetRow("Avg satisfaction", average(s.averageSatisfaction()),
                String.valueOf(t.minAvgSatisfaction()), t.satisfactionMet(s)));
        out.println("   Unhappy departures: " + s.getDepartures());
        List<Customer> unpaid = r.getAllCustomers().stream()
                .filter(c -> c.getStatus() != CustomerStatus.PAID).toList();
        if (!unpaid.isEmpty()) {
            out.println("   Not paid by the end:");
            for (Customer c : unpaid) {
                out.println("     " + c.getLabel() + "  " + c.getStatus() + "  sat " + c.getSatisfaction());
            }
        }
        out.println(" *** " + outcome + " ***");
        out.println(DOUBLE_RULE);
    }

    public void showHelp(String role) {
        out.println("  Commands for the " + role + ":");
        if (role.equals("Chef")) {
            out.println("    cook <c>      advance customer c's order by one unit, e.g. 'cook 3'");
        } else {
            out.println("    seat <c> [t]  seat customer c at table t (default: lowest free), e.g. 'seat 4 2'");
            out.println("    order <c>     take c's order (the customer chooses the dish)");
            out.println("    serve <c>     serve c's READY order");
        }
        out.println("    wait (or w)   do nothing this turn");
        out.println("    status        reprint the board      menu   show the menu");
        out.println("    help          this list              quit   leave the game");
        out.println("  Ids can be written '3' or 'C3'; tables '2' or 'T2'. Invalid commands do not use up a turn.");
    }

    public void showMenu(Menu menu) { out.println("  Menu: " + menuLine(menu)); }

    private static String targetRow(String label, String value, String need, boolean met) {
        return String.format("   %-18s %-9s %-18s %s", label, value, "(need >= " + need + ")",
                met ? "PASS" : "FAIL");
    }

    private static String menuLine(Menu menu) {
        return menu.getItems().stream()
                .map(i -> i.getName() + " " + Money.format(i.getPriceCents()) + " ("
                        + i.getPrepUnits() + (i.getPrepUnits() == 1 ? " unit)" : " units)"))
                .collect(Collectors.joining(" | "));
    }

    private static String orderText(Customer c) {
        if (c.getOrder().isEmpty()) {
            return "(no order yet)";
        }
        Order o = c.getOrder().get();
        MenuItem item = o.getItem();
        return String.format("%-7s %s (%s)  %s", item.getName(), o.getStatus(), o.progressLabel(),
                Money.format(o.getLockedPriceCents()));
    }

    private static String average(OptionalDouble avg) {
        return avg.isPresent() ? String.format(Locale.ROOT, "%.1f", avg.getAsDouble()) : "N/A";
    }
}
