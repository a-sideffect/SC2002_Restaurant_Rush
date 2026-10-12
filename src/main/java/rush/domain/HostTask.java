package rush.domain;

/** A command for the Host, built by the UI and executed by the domain. */
public interface HostTask {
    ActionResult execute(Host h, Restaurant r, int turn);

    String describe();
}
