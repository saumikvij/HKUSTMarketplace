package hkust.marketplace.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/** A buyer's report of a listing, for the admin to moderate. */
public class Report implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String buyerId;
    private final String listingId;
    private final ReportReason reason;
    private final String comment;
    private final LocalDateTime reportedAt;
    private ReportStatus status = ReportStatus.OPEN;

    /**
     * Creates an open report.
     * @param id unique id
     * @param buyerId reporting buyer
     * @param listingId reported listing
     * @param reason reason
     * @param comment optional comment (required for OTHER)
     * @param reportedAt time reported
     */
    public Report(String id, String buyerId, String listingId, ReportReason reason, String comment,
                  LocalDateTime reportedAt) {
        this.id = id;
        this.buyerId = buyerId;
        this.listingId = listingId;
        this.reason = reason;
        this.comment = comment;
        this.reportedAt = reportedAt;
    }

    /** @return unique id */
    public String getId() { return id; }
    /** @return reporting buyer */
    public String getBuyerId() { return buyerId; }
    /** @return reported listing */
    public String getListingId() { return listingId; }
    /** @return reason */
    public ReportReason getReason() { return reason; }
    /** @return comment, may be null */
    public String getComment() { return comment; }
    /** @return time reported */
    public LocalDateTime getReportedAt() { return reportedAt; }
    /** @return moderation status */
    public ReportStatus getStatus() { return status; }
    /** @param status new status */
    public void setStatus(ReportStatus status) { this.status = status; }
}
