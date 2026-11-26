package hkust.marketplace.service;

/** Sort orders on the Browse Marketplace screen. */
public enum SortOrder {
    NEWEST("Newest first"),
    PRICE_LOW_HIGH("Price: low to high"),
    PRICE_HIGH_LOW("Price: high to low");

    private final String label;

    SortOrder(String label) { this.label = label; }

    @Override public String toString() { return label; }
}
