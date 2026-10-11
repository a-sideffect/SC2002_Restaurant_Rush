package rush.ui;

/** A command the manager typed that could not be understood; the message explains why. */
public class ParseError extends Exception {
    public ParseError(String message) { super(message); }
}
