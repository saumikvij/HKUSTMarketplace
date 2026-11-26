package hkust.marketplace.model;

/** Whether an account can log in. Deleted accounts are kept for the records (soft delete). */
public enum AccountStatus {
    /** Normal account. */
    ACTIVE,
    /** Disabled by the admin; cannot log in and its listings are hidden. */
    DEACTIVATED,
    /** Soft-deleted by the admin. */
    DELETED
}
