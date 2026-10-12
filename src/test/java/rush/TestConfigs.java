package rush;

import java.util.ArrayList;
import java.util.List;
import rush.domain.ArrivalSchedule;
import rush.domain.ArrivalSchedule.ScheduledArrival;
import rush.domain.GameConfig;
import rush.domain.Menu;
import rush.domain.RegularCustomer;
import rush.domain.VictoryTargets;

/** Game settings built only for tests. */
public final class TestConfigs {
    private TestConfigs() { }

    /**
     * The original Stage 1 game: base menu, base arrival turns, but every customer Regular.
     * The Stage 1 scripts and hand-traced numbers still hold under this setting.
     */
    public static GameConfig regularOnly() {
        List<ScheduledArrival> arrivals = new ArrayList<>();
        for (int turn : new int[] {1, 4, 7, 10, 13, 16}) {
            arrivals.add(new ScheduledArrival(turn, RegularCustomer::new));
        }
        return new GameConfig("Regular only", 18, 2, Menu.base(), new ArrivalSchedule(arrivals),
                VictoryTargets.base());
    }
}
