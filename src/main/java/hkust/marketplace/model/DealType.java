package hkust.marketplace.model;

/** What a buyer asked for: one deal type per listing type. */
public enum DealType {
    /** Price offer on a Sell listing ("Bought" in history). */
    OFFER("Buy", "Bought"),
    /** Borrow request on a Lend listing ("Borrowed" in history). */
    BORROW("Borrow", "Borrowed"),
    /** Claim on a Giveaway listing ("Received" in history). */
    CLAIM("Claim", "Received");

    private final String label;
    private final String historyLabel;

    DealType(String label, String historyLabel) {
        this.label = label;
        this.historyLabel = historyLabel;
    }

    /** @return the name shown in My Deals */
    public String label() { return label; }

    /** @return the name shown in Deal History (Bought / Borrowed / Received) */
    public String historyLabel() { return historyLabel; }

    /**
     * Returns the deal type that matches a listing type.
     * @param type the listing type
     * @return OFFER for SELL, BORROW for LEND, CLAIM for GIVEAWAY
     */
    public static DealType forListingType(ListingType type) {
        switch (type) {
            case SELL: return OFFER;
            case LEND: return BORROW;
            default: return CLAIM;
        }
    }

    @Override public String toString() { return label; }
}
