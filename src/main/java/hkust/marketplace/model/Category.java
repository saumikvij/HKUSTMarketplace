package hkust.marketplace.model;

/** The team's predefined listing categories. */
public enum Category {
    TEXTBOOKS("Textbooks"),
    ELECTRONICS("Electronics"),
    FURNITURE("Furniture"),
    HOME_AND_KITCHEN("Home & Kitchen"),
    CLOTHING("Clothing"),
    SPORTS("Sports"),
    STATIONERY("Stationery"),
    OTHER("Other");

    private final String label;

    Category(String label) { this.label = label; }

    /** @return the name shown in the user interface */
    public String label() { return label; }

    @Override public String toString() { return label; }
}
