package rush.domain;

/** A command for the Chef, built by the UI and executed by the domain. */
public interface ChefTask {
    ActionResult execute(Chef c, Restaurant r, int turn);

    String describe();
}
