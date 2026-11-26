package hkust.marketplace.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A buyer's offer, borrow request or claim on one listing, from submission until it is finished.
 * The record of all deals and each buyer's deal history are deals with a finished status.
 */
public class Deal implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String buyerId;
    private final String listingId;
    private final DealType type;
    private DealStatus status = DealStatus.PENDING;
    private BigDecimal offeredPrice;
    private BigDecimal counterPrice;
    private LocalDateTime borrowStart;
    private LocalDateTime borrowEnd;
    private LocalDateTime counterStart;
    private LocalDateTime counterEnd;
    private String message;
    private final LocalDateTime submittedAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
    private boolean reminderSent;

    /**
     * Creates a pending deal.
     * @param id unique id
     * @param buyerId buyer who made it
     * @param listingId listing it is for
     * @param type offer, borrow or claim
     * @param submittedAt submission time
     */
    public Deal(String id, String buyerId, String listingId, DealType type, LocalDateTime submittedAt) {
        this.id = id;
        this.buyerId = buyerId;
        this.listingId = listingId;
        this.type = type;
        this.submittedAt = submittedAt;
        this.updatedAt = submittedAt;
    }

    /** @return unique id */
    public String getId() { return id; }
    /** @return buyer id */
    public String getBuyerId() { return buyerId; }
    /** @return listing id */
    public String getListingId() { return listingId; }
    /** @return deal type */
    public DealType getType() { return type; }
    /** @return current status */
    public DealStatus getStatus() { return status; }
    /**
     * Changes the status and records the time.
     * @param status new status
     * @param at time of the change
     */
    public void setStatus(DealStatus status, LocalDateTime at) {
        this.status = status;
        this.updatedAt = at;
    }
    /** @return offered price for an offer, or null */
    public BigDecimal getOfferedPrice() { return offeredPrice; }
    /** @param price offered price */
    public void setOfferedPrice(BigDecimal price) { this.offeredPrice = price; }
    /** @return seller's counter price, or null */
    public BigDecimal getCounterPrice() { return counterPrice; }
    /** @param price counter price */
    public void setCounterPrice(BigDecimal price) { this.counterPrice = price; }
    /** @return agreed or proposed borrowing start, or null */
    public LocalDateTime getBorrowStart() { return borrowStart; }
    /** @return agreed or proposed borrowing end (the due time), or null */
    public LocalDateTime getBorrowEnd() { return borrowEnd; }
    /**
     * Sets the borrowing period.
     * @param start start time
     * @param end end (due) time
     */
    public void setBorrowPeriod(LocalDateTime start, LocalDateTime end) {
        this.borrowStart = start;
        this.borrowEnd = end;
    }
    /** @return seller's counter start, or null */
    public LocalDateTime getCounterStart() { return counterStart; }
    /** @return seller's counter end, or null */
    public LocalDateTime getCounterEnd() { return counterEnd; }
    /**
     * Sets the seller's counter borrowing period.
     * @param start counter start
     * @param end counter end
     */
    public void setCounterPeriod(LocalDateTime start, LocalDateTime end) {
        this.counterStart = start;
        this.counterEnd = end;
    }
    /** @return optional message from the buyer */
    public String getMessage() { return message; }
    /** @param message optional message */
    public void setMessage(String message) { this.message = message; }
    /** @return submission time */
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    /** @return last update time */
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    /** @return time the deal finished (completed or returned), or null */
    public LocalDateTime getCompletedAt() { return completedAt; }
    /** @param at finish time */
    public void setCompletedAt(LocalDateTime at) { this.completedAt = at; }
    /** @return true once the due-date reminder has been sent */
    public boolean isReminderSent() { return reminderSent; }
    /** @param sent whether the due-date reminder has been sent */
    public void setReminderSent(boolean sent) { this.reminderSent = sent; }

    /** @return the price the deal is at: the counter price if one exists, else the offered price */
    public BigDecimal getCurrentPrice() { return counterPrice != null ? counterPrice : offeredPrice; }
}
