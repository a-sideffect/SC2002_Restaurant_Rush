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

    /** The base-setting menu: Salad $9.00/1, Burger $15.00/1, Pasta $18.00/2. */
    public static Menu base() {
        return new Menu(List.of(
                new MenuItem("Salad", 900, 1),
                new MenuItem("Burger", 1500, 1),
                new MenuItem("Pasta", 1800, 2)));
    }

    public MenuItem itemAt(int index) { return items.get(index); }
    public int size() { return items.size(); }
    public List<MenuItem> getItems() { return items; }
}
