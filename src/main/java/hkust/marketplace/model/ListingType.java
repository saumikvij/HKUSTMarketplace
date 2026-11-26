package hkust.marketplace.model;

/** How an item is offered. */
public enum ListingType {
    /** For sale at a price in HK$. */
    SELL("Sell"),
    /** Lent for up to a lending duration. */
    LEND("Lend"),
    /** Given away for free to the first claimer. */
    GIVEAWAY("Giveaway");

    private final String label;

    ListingType(String label) { this.label = label; }

    /** @return the name shown in the user interface */
    public String label() { return label; }

    @Override public String toString() { return label; }
}
