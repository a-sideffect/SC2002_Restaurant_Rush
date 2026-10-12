package rush.domain;

/** Two dishes sold together: preparation units add up and the price is 90% of the two prices combined. */
public class ComboMeal extends MenuItem {
    private final MenuItem first;
    private final MenuItem second;

    public ComboMeal(String name, MenuItem first, MenuItem second) {
        super(name, Money.percentOf(first.getPriceCents() + second.getPriceCents(), 90),
                first.getPrepUnits() + second.getPrepUnits());
        this.first = first;
        this.second = second;
    }

    @Override
    public String getDescription() {
        return getName() + " [" + first.getName() + " + " + second.getName() + "]";
    }
}
