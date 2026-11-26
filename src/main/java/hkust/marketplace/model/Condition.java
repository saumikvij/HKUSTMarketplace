package hkust.marketplace.model;

/** Physical condition of an item. */
public enum Condition {
    NEW("New"),
    LIKE_NEW("Like New"),
    USED("Used"),
    FOR_PARTS("For Parts");

    private final String label;

    Condition(String label) { this.label = label; }

    /** @return the name shown in the user interface */
    public String label() { return label; }

    @Override public String toString() { return label; }
}
