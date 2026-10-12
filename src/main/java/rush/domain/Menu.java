package rush.domain;

import java.util.List;

/** An ordered list of dishes. Order matters because customers pick by position (Rule F1). */
public class Menu {
    private final List<MenuItem> items;

    public Menu(List<MenuItem> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("menu needs at least one item");
        }
        this.items = List.copyOf(items);
    }

    /**
     * The base-setting menu: Salad $9.00/1, Burger $15.00/1, Pasta $18.00/2, then two combos.
     * The single dishes stay first so rule F1 gives the same dishes to the first customers.
     */
    public static Menu base() {
        MenuItem salad = new MenuItem("Salad", 900, 1);
        MenuItem burger = new MenuItem("Burger", 1500, 1);
        MenuItem pasta = new MenuItem("Pasta", 1800, 2);
        return new Menu(List.of(salad, burger, pasta,
                new ComboMeal("Combo A", salad, burger),
                new ComboMeal("Combo B", salad, pasta)));
    }

    public MenuItem itemAt(int index) { return items.get(index); }
    public int size() { return items.size(); }
    public List<MenuItem> getItems() { return items; }
}
