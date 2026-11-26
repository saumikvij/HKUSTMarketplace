package hkust.marketplace.model;

/** Categories shown on the notification boards. */
public enum NotificationCategory {
    DEALS("Deals"),
    MESSAGES("Messages"),
    BORROWING("Borrowing"),
    LISTING_UPDATES("Listing Updates"),
    REVIEWS("Reviews"),
    MODERATION("Moderation"),
    ACCOUNT("Account"),
    ANNOUNCEMENTS("Announcements");

    private final String label;

    NotificationCategory(String label) { this.label = label; }

    /** @return the name shown in the user interface */
    public String label() { return label; }

    @Override public String toString() { return label; }
}
