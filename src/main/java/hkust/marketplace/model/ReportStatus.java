package hkust.marketplace.model;

/** Progress of a listing report through admin moderation. */
public enum ReportStatus {
    /** Waiting for the admin. */
    OPEN,
    /** The admin acted on it (e.g. took the listing down). */
    RESOLVED,
    /** The admin decided no action was needed. */
    DISMISSED
}
