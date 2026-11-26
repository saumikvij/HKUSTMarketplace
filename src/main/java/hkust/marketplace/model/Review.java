package hkust.marketplace.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/** A buyer's 1-5 star rating and comment on a finished deal. Hidden reviews are kept but not shown or counted. */
public class Review implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String dealId;
    private final String buyerId;
    private final String sellerId;
    private final int rating;
    private final String comment;
    private final LocalDateTime postedAt;
    private String sellerReply;
    private boolean flagged;
    private boolean hidden;

    /**
     * Creates a review.
     * @param id unique id
     * @param dealId the finished deal it rates
     * @param buyerId reviewer
     * @param sellerId reviewed seller
     * @param rating 1 to 5 stars
     * @param comment written review
     * @param postedAt time posted
     */
    public Review(String id, String dealId, String buyerId, String sellerId, int rating, String comment,
                  LocalDateTime postedAt) {
        this.id = id;
        this.dealId = dealId;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.rating = rating;
        this.comment = comment;
        this.postedAt = postedAt;
    }

    /** @return unique id */
    public String getId() { return id; }
    /** @return deal id */
    public String getDealId() { return dealId; }
    /** @return reviewer id */
    public String getBuyerId() { return buyerId; }
    /** @return reviewed seller id */
    public String getSellerId() { return sellerId; }
    /** @return 1 to 5 */
    public int getRating() { return rating; }
    /** @return written review */
    public String getComment() { return comment; }
    /** @return time posted */
    public LocalDateTime getPostedAt() { return postedAt; }
    /** @return the seller's reply, or null */
    public String getSellerReply() { return sellerReply; }
    /** @param reply seller's reply */
    public void setSellerReply(String reply) { this.sellerReply = reply; }
    /** @return true if the seller flagged it as inappropriate */
    public boolean isFlagged() { return flagged; }
    /** @param flagged flag state */
    public void setFlagged(boolean flagged) { this.flagged = flagged; }
    /** @return true if the admin hid it */
    public boolean isHidden() { return hidden; }
    /** @param hidden hidden state */
    public void setHidden(boolean hidden) { this.hidden = hidden; }
}
