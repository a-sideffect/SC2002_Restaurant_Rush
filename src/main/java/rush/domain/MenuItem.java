package rush.domain;

/** An immutable dish: name, price in cents and number of Chef preparation units. */
public class MenuItem {
    private final String name;
    private final int priceCents;
    private final int prepUnits;

    public MenuItem(String name, int priceCents, int prepUnits) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (priceCents <= 0 || prepUnits <= 0) {
            throw new IllegalArgumentException("price and prep units must be positive");
        }
        this.name = name;
        this.priceCents = priceCents;
        this.prepUnits = prepUnits;
    }

    public String getName() { return name; }
    public int getPriceCents() { return priceCents; }
    public int getPrepUnits() { return prepUnits; }

    /** Name plus anything worth showing on the menu; a combo lists its parts. */
    public String getDescription() { return name; }

    @Override
    public String toString() {
        return name + " (" + Money.format(priceCents) + ", " + prepUnits
                + (prepUnits == 1 ? " unit)" : " units)");
    }
}
