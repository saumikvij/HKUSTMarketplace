package hkust.marketplace.model;

/** Status of a deal (offer, borrow request or claim) from the buyer's side. */
public enum DealStatus {
    /** Submitted, waiting for the seller. */
    PENDING("Pending"),
    /** The seller proposed different terms; waiting for the buyer. */
    COUNTERED("Countered"),
    /** Sell or Giveaway deal agreed; waiting for the buyer to mark it complete. */
    ACCEPTED("Accepted"),
    /** Lend deal agreed; the borrowing period is running. */
    BORROWED("Borrowed"),
    /** Rejected by the seller or buyer, or auto-declined because another deal won. */
    DECLINED("Declined"),
    /** Cancelled by the buyer before the seller answered. */
    WITHDRAWN("Withdrawn"),
    /** The listing was removed while the deal was open. */
    CANCELLED("Cancelled"),
    /** The buyer marked a Sell/Giveaway deal complete. */
    COMPLETED("Completed"),
    /** The buyer returned a borrowed item before the due time. */
    RETURNED("Returned"),
    /** The borrowed item was returned automatically at the due time. */
    AUTO_RETURNED("Auto-returned");

    private final String label;

    DealStatus(String label) { this.label = label; }

    /** @return the name shown in the user interface */
    public String label() { return label; }

    /** @return true while the deal still needs action (shown under Active in My Deals) */
    public boolean isActive() {
        return this == PENDING || this == COUNTERED || this == ACCEPTED || this == BORROWED;
    }

    /** @return true if the deal finished successfully (shown in Deal History and can be reviewed) */
    public boolean isFinished() {
        return this == COMPLETED || this == RETURNED || this == AUTO_RETURNED;
    }

    /** @return true if the buyer may still withdraw the request */
    public boolean isOpenRequest() {
        return this == PENDING || this == COUNTERED;
    }

    @Override public String toString() { return label; }
}
