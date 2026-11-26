package hkust.marketplace.model;

/** Life cycle of a listing (shared by Tasks 1-3). */
public enum ListingStatus {
    /** Submitted by the seller, waiting for admin approval. */
    PENDING_REVIEW,
    /** Rejected by the admin with a reason. */
    REJECTED,
    /** Approved and open for offers, borrow requests or claims. */
    LIVE,
    /** An offer or claim was accepted (or a giveaway claimed); waiting for completion. */
    RESERVED,
    /** Currently borrowed. */
    LENT,
    /** Sale completed. */
    SOLD,
    /** Giveaway completed. */
    GIVEN,
    /** Taken down by the admin or deleted by the seller. */
    REMOVED;

    /** @return true if buyers can see the listing in Browse Marketplace */
    public boolean isBrowsable() {
        return this == LIVE || this == RESERVED || this == LENT;
    }
}
