package rush.domain;

/** Result of a staff action. A failed result never consumes the staff member's task. */
public record ActionResult(boolean success, String message) {
    public static ActionResult ok(String msg) { return new ActionResult(true, msg); }
    public static ActionResult fail(String reason) { return new ActionResult(false, reason); }
}
