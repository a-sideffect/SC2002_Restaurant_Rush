package rush.scenario;

import static rush.TestRunner.check;
import static rush.TestRunner.checkEquals;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Random;
import rush.control.GameController;
import rush.control.ManagerInput;
import rush.domain.Cashier;
import rush.domain.Chef;
import rush.domain.ChefTask;
import rush.domain.CookTask;
import rush.domain.Customer;
import rush.domain.CustomerStatus;
import rush.domain.GameConfig;
import rush.domain.GameOutcome;
import rush.domain.Order;
import rush.domain.OrderStatus;
import rush.domain.PriceCalculator;
import rush.domain.Restaurant;
import rush.domain.Scoreboard;
import rush.domain.SeatTask;
import rush.domain.ServeTask;
import rush.domain.Table;
import rush.domain.TakeOrderTask;
import rush.domain.WaitTask;
import rush.domain.Waiter;
import rush.domain.WaiterTask;
import rush.ui.ConsoleView;

/**
 * Plays many games with random (often invalid) commands and checks structural invariants at
 * every prompt. Any exception or broken invariant points at a logic error.
 */
public final class FuzzTest {
    private static final int GAMES = 400;

    private FuzzTest() { }

    public static void run() {
        int victories = 0;
        int totalViolations = 0;
        for (int seed = 0; seed < GAMES; seed++) {
            Game game = new Game(seed);
            try {
                GameOutcome outcome = game.play();
                if (outcome == GameOutcome.VICTORY) {
                    victories++;
                }
            } catch (RuntimeException e) {
                check(false, "seed " + seed + " threw " + e);
            }
            totalViolations += game.violations;
        }
        checkEquals(0, totalViolations, "fuzz: invariant violations across " + GAMES + " games");
        check(victories >= 0, "fuzz ran");
        System.out.println("fuzz: " + GAMES + " games played, " + victories + " random victories");
    }

    private static final class Game implements ManagerInput {
        final Random rnd;
        final Restaurant r = new Restaurant(GameConfig.base());
        final Waiter w = new Waiter("Waiter", new PriceCalculator());
        final Chef c = new Chef("Chef");
        final Cashier k = new Cashier("Cashier");
        final long seed;
        int violations;
        int lastTurn;
        int lastPaid;

        Game(long seed) {
            this.seed = seed;
            this.rnd = new Random(seed);
        }

        GameOutcome play() {
            ConsoleView view = new ConsoleView(new PrintStream(OutputStream.nullOutputStream()));
            GameOutcome o = new GameController(r, w, c, k, this, view).run();
            audit(19);
            checkEquals(r.getConfig().targets().evaluate(r.getScoreboard()), o, "seed " + seed + " outcome matches");
            return o;
        }

        private int id() { return rnd.nextInt(8); } // 0 and 7+ are mostly invalid

        @Override
        public WaiterTask nextWaiterTask(int turn) {
            audit(turn);
            switch (rnd.nextInt(5)) {
                case 0: return new SeatTask(id(), rnd.nextBoolean() ? null : rnd.nextInt(4));
                case 1: return new TakeOrderTask(id());
                case 2: return new ServeTask(id());
                case 3: return smartWaiter();
                default: return WaitTask.INSTANCE;
            }
        }

        @Override
        public ChefTask nextChefTask(int turn) {
            audit(turn);
            if (rnd.nextInt(3) == 0) {
                return WaitTask.INSTANCE;
            }
            if (rnd.nextInt(2) == 0 && !r.getCookableOrders().isEmpty()) {
                return new CookTask(r.getCookableOrders().get(0).getCustomer().getId());
            }
            return new CookTask(id());
        }

        private WaiterTask smartWaiter() {
            if (!r.getReadyOrders().isEmpty()) {
                return new ServeTask(r.getReadyOrders().get(0).getCustomer().getId());
            }
            for (Customer cu : r.getAllCustomers()) {
                if (cu.getStatus() == CustomerStatus.SEATED) {
                    return new TakeOrderTask(cu.getId());
                }
            }
            if (!r.getQueue().isEmpty()) {
                return new SeatTask(r.getQueue().get(0).getId(), null);
            }
            return WaitTask.INSTANCE;
        }

        private void bad(String what) {
            violations++;
            check(false, "seed " + seed + " invariant: " + what);
        }

        /** Structural invariants that must hold whenever the manager is being prompted. */
        private void audit(int turn) {
            Scoreboard s = r.getScoreboard();
            int paid = 0;
            int revenue = 0;
            int sum = 0;
            int departed = 0;
            int served = 0;
            for (Customer cu : r.getAllCustomers()) {
                String who = cu.getLabel() + " " + cu.getStatus();
                if (cu.getSatisfaction() < 0 || cu.getSatisfaction() > 100) {
                    bad(who + " satisfaction out of range " + cu.getSatisfaction());
                }
                boolean inQueue = r.getQueue().contains(cu);
                if (inQueue != (cu.getStatus() == CustomerStatus.WAITING)) {
                    bad(who + " queue membership wrong");
                }
                boolean hasTable = cu.getTable().isPresent();
                boolean shouldHaveTable = cu.getStatus() != CustomerStatus.WAITING
                        && cu.getStatus() != CustomerStatus.PAID && cu.getStatus() != CustomerStatus.LEFT;
                if (hasTable != shouldHaveTable) {
                    bad(who + " table presence wrong");
                }
                if (hasTable && cu.getTable().get().getOccupant().orElse(null) != cu) {
                    bad(who + " table does not point back");
                }
                Order o = cu.getOrder().orElse(null);
                switch (cu.getStatus()) {
                    case WAITING: case SEATED:
                        if (o != null) { bad(who + " has an order too early"); }
                        break;
                    case ORDERED:
                        if (o == null || !(o.isCookable() || o.isReady())) { bad(who + " order state"); }
                        break;
                    case EATING: case READY_TO_PAY:
                        if (o == null || o.getStatus() != OrderStatus.SERVED) { bad(who + " order not SERVED"); }
                        served++;
                        break;
                    case PAID:
                        if (o == null || o.getStatus() != OrderStatus.PAID) { bad(who + " order not PAID"); }
                        paid++;
                        served++;
                        revenue += o.getLockedPriceCents();
                        sum += cu.getRecordedSatisfaction().orElse(-999);
                        break;
                    case LEFT:
                        departed++;
                        if (o != null && o.getStatus() != OrderStatus.CANCELLED) { bad(who + " order not CANCELLED"); }
                        if (cu.getSatisfaction() != 0) { bad(who + " left with satisfaction > 0"); }
                        break;
                    default:
                        break;
                }
                if (o != null && o.getLockedPriceCents() != cu.adjustBill(o.getItem().getPriceCents())) {
                    bad(who + " price changed");
                }
                if (o != null && o.getUnitsDone() > o.getItem().getPrepUnits()) {
                    bad(who + " over-cooked");
                }
            }
            for (Table t : r.getTables()) {
                if (!t.isFree() && t.getOccupant().get().getTable().orElse(null) != t) {
                    bad("table " + t.getNumber() + " occupant mismatch");
                }
            }
            if (s.getPaidCount() != paid) { bad("paidCount " + s.getPaidCount() + " vs " + paid); }
            if (s.getRevenueCents() != revenue) { bad("revenue " + s.getRevenueCents() + " vs " + revenue); }
            if (s.getSatisfactionSum() != sum) { bad("satisfactionSum " + s.getSatisfactionSum() + " vs " + sum); }
            if (s.getDepartures() != departed) { bad("departures " + s.getDepartures() + " vs " + departed); }
            if (s.getServedCount() != served) { bad("servedCount " + s.getServedCount() + " vs " + served); }
            if (turn != lastTurn) {
                if (s.getPaidCount() - lastPaid > 1) { bad("more than one payment in a turn"); }
                lastPaid = s.getPaidCount();
                lastTurn = turn;
            }
            long occupied = r.getTables().stream().filter(t -> !t.isFree()).count();
            if (occupied > r.getConfig().tableCount()) { bad("too many tables"); }
        }
    }
}
