package rush.domain;

/** Everything that defines a game setting. The base setting is the default. */
public record GameConfig(String name, int maxTurns, int tableCount, Menu menu,
                         ArrivalSchedule arrivals, VictoryTargets targets) {

    /** The mandatory base setting: 18 turns, 2 tables, base menu, base arrivals and targets. */
    public static GameConfig base() {
        return new GameConfig("Base", 18, 2, Menu.base(), ArrivalSchedule.base(), VictoryTargets.base());
    }
}
