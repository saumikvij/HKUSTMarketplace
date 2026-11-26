package hkust.marketplace.model;

/** The three kinds of account in HKUST Marketplace. Each portal accepts only its own role. */
public enum Role {
    /** A student or staff member who browses, requests and reviews items. */
    BUYER,
    /** A student or staff member who posts listings. */
    SELLER,
    /** The moderator account (seeded, never registered). */
    ADMIN
}
