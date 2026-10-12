package rush.control;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import rush.domain.ActionResult;
import rush.domain.Cashier;
import rush.domain.Chef;
import rush.domain.Customer;
import rush.domain.GameOutcome;
import rush.domain.Host;
import rush.domain.Payment;
import rush.domain.Restaurant;
import rush.domain.SatisfactionChange;
import rush.domain.Staff;
import rush.domain.Waiter;
import rush.ui.ConsoleView;

/** Runs the turn loop. Contains no game rules: it sequences the domain calls (Section 4.2). */
public class GameController {
    private final Restaurant restaurant;
    private final Waiter waiter;
    private final Host host;
    private final Chef chef;
    private final Cashier cashier;
    private final ManagerInput input;
    private final ConsoleView view;
    private final List<Staff> staff;

    public GameController(Restaurant restaurant, Waiter waiter, Host host, Chef chef, Cashier cashier,
                          ManagerInput input, ConsoleView view) {
        this.restaurant = restaurant;
        this.waiter = waiter;
        this.host = host;
        this.chef = chef;
        this.cashier = cashier;
        this.input = input;
        this.view = view;
        this.staff = List.of(waiter, host, chef, cashier);
    }

    /** Plays every turn (never ends early) and returns the outcome. */
    public GameOutcome run() {
        view.showWelcome(restaurant.getConfig(), staff);
        for (int turn = 1; turn <= restaurant.getConfig().maxTurns(); turn++) {
            playTurn(turn);
        }
        GameOutcome outcome = restaurant.getConfig().targets().evaluate(restaurant.getScoreboard());
        view.showFinal(outcome, restaurant.getScoreboard(), restaurant);
        return outcome;
    }

    private void playTurn(int turn) {
        List<Customer> arrived = restaurant.admitArrivals(turn);              // Step 1
        view.showArrivals(arrived);
        happyHourPhase(turn);                                                 // start-of-turn promotion
        view.showTurnStart(turn, restaurant, staff);                          // D1

        runWaiterUntilValid(turn);                                            // Steps 2-3, Waiter first
        runHostUntilValid(turn);                                              // then the Host
        runChefUntilValid(turn);                                              // then the Chef

        Optional<Payment> payment = cashier.collect(restaurant, turn);        // Step 4a
        List<Customer> doneEating = restaurant.finishEating(turn);            // Step 4b

        Map<Customer, Integer> before = new LinkedHashMap<>();               // for the end-of-turn display
        for (Customer c : restaurant.getAllCustomers()) {
            if (c.getStatus().isAwaitingService()) {
                before.put(c, c.getSatisfaction());
            }
        }
        List<Customer> departed = restaurant.applyEndOfTurnLoss(turn);        // Step 5
        restaurant.endHappyHourTurn();

        view.showEndOfTurn(turn, new TurnReport(payment, doneEating, before, new ArrayList<>(departed)),
                restaurant.getScoreboard(), restaurant.getConfig());          // D2
    }

    /** Offer Happy Hour while it is unused, then apply this turn's recovery if it is active. */
    private void happyHourPhase(int turn) {
        if (restaurant.getHappyHour().isAvailable() && input.nextHappyHourDecision(turn)) {
            view.showResult(restaurant.activateHappyHour());
        }
        List<SatisfactionChange> changes = restaurant.applyHappyHourRecovery();
        if (restaurant.getHappyHour().isActive()) {
            view.showHappyHourRecovery(changes, restaurant.getHappyHour().getRemainingTurns());
        }
    }

    private void runWaiterUntilValid(int turn) {
        ActionResult result;
        do {
            result = input.nextWaiterTask(turn).execute(waiter, restaurant, turn);
            view.showResult(result);
        } while (!result.success());
    }

    private void runHostUntilValid(int turn) {
        ActionResult result;
        do {
            result = input.nextHostTask(turn).execute(host, restaurant, turn);
            view.showResult(result);
        } while (!result.success());
    }

    private void runChefUntilValid(int turn) {
        ActionResult result;
        do {
            result = input.nextChefTask(turn).execute(chef, restaurant, turn);
            view.showResult(result);
        } while (!result.success());
    }
}
