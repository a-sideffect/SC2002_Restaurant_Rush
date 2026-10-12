package rush.domain;

/**
 * The single place where an order's price is decided and locked when it is taken.
 * Later discounts or promotions should change only {@link #quote}.
 */
public class PriceCalculator {
    /** Price in cents that will be locked into the order. */
    public int quote(Customer customer, MenuItem item) {
        return customer.adjustBill(item.getPriceCents());
    }
}
