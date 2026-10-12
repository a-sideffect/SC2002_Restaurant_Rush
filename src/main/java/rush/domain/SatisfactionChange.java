package rush.domain;

/** A customer's satisfaction before and after a promotion changed it. */
public record SatisfactionChange(Customer customer, int before, int after) { }
