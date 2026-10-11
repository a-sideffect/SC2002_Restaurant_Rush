package rush.ui;

/** Thrown when the manager confirms they want to quit; caught in Main for a clean exit. */
public class QuitRequested extends RuntimeException {
    public QuitRequested() { super("Quit requested"); }
}
