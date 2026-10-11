package rush.domain;

import java.util.Optional;

/** A table holds at most one customer, from seating until they pay or leave. */
public class Table {
    private final int number;
    private Customer occupant;

    Table(int number) { this.number = number; }

    public int getNumber() { return number; }
    public boolean isFree() { return occupant == null; }
    public Optional<Customer> getOccupant() { return Optional.ofNullable(occupant); }

    void occupy(Customer c) {
        if (occupant != null) {
            throw new IllegalStateException("Table " + number + " is already occupied");
        }
        occupant = c;
    }

    void free() { occupant = null; }
}
