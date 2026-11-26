package hkust.marketplace.util;

import java.util.UUID;

/** Generates short unique ids for stored records. */
public final class Ids {
    private Ids() { }

    /**
     * @param prefix short type prefix, e.g. "L" for listing
     * @return a new unique id such as {@code L-1a2b3c4d5e}
     */
    public static String next(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }
}
