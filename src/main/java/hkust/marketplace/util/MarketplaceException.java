package hkust.marketplace.util;

/** Thrown when an action is not allowed in the current state (e.g. the listing is no longer available). */
public class MarketplaceException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /** @param message message shown to the user */
    public MarketplaceException(String message) {
        super(message);
    }
}
