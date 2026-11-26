package hkust.marketplace.model;

/** Reasons a buyer can give when reporting a listing. */
public enum ReportReason {
    PROHIBITED_ITEM("Prohibited item"),
    MISLEADING_INFORMATION("Misleading information"),
    SUSPECTED_SCAM("Suspected scam"),
    OFFENSIVE_CONTENT("Offensive content"),
    OTHER("Other");

    private final String label;

    ReportReason(String label) { this.label = label; }

    /** @return the name shown in the user interface */
    public String label() { return label; }

    @Override public String toString() { return label; }
}
