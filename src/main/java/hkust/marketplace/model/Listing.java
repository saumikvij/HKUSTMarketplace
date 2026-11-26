package hkust.marketplace.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * An item posted by a seller for sale, lending or giveaway.
 * Buyers only see listings whose status is browsable (Live, Reserved or Lent).
 */
public class Listing implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Maximum number of photos per listing. */
    public static final int MAX_PHOTOS = 6;

    private final String id;
    private final String sellerId;
    private ListingType type;
    private String title;
    private Category category;
    private Condition condition;
    private String description;
    private BigDecimal price;
    private long lendingDurationSeconds;
    private String meetupLocation;
    private final List<String> photos = new ArrayList<>();
    private final LocalDateTime postedAt;
    private LocalDateTime reviewedAt;
    private ListingStatus status = ListingStatus.PENDING_REVIEW;
    private int viewCount;
    private String rejectionReason;
    private String removalReason;
    private LocalDateTime lentUntil;

    /**
     * Creates a listing.
     * @param id unique id
     * @param sellerId id of the seller who posted it
     * @param type sell, lend or giveaway
     * @param title item name
     * @param category category
     * @param condition condition
     * @param description full description
     * @param meetupLocation HKUST meet-up location
     * @param postedAt submission time
     */
    public Listing(String id, String sellerId, ListingType type, String title, Category category,
                   Condition condition, String description, String meetupLocation, LocalDateTime postedAt) {
        this.id = id;
        this.sellerId = sellerId;
        this.type = type;
        this.title = title;
        this.category = category;
        this.condition = condition;
        this.description = description;
        this.meetupLocation = meetupLocation;
        this.postedAt = postedAt;
    }

    /** @return unique id */
    public String getId() { return id; }
    /** @return id of the seller */
    public String getSellerId() { return sellerId; }
    /** @return listing type */
    public ListingType getType() { return type; }
    /** @param type new listing type */
    public void setType(ListingType type) { this.type = type; }
    /** @return item name */
    public String getTitle() { return title; }
    /** @param title new item name */
    public void setTitle(String title) { this.title = title; }
    /** @return category */
    public Category getCategory() { return category; }
    /** @param category new category */
    public void setCategory(Category category) { this.category = category; }
    /** @return condition */
    public Condition getCondition() { return condition; }
    /** @param condition new condition */
    public void setCondition(Condition condition) { this.condition = condition; }
    /** @return full description */
    public String getDescription() { return description; }
    /** @param description new description */
    public void setDescription(String description) { this.description = description; }
    /** @return asking price in HK$ (Sell listings only), otherwise null */
    public BigDecimal getPrice() { return price; }
    /** @param price asking price in HK$ */
    public void setPrice(BigDecimal price) { this.price = price; }
    /** @return maximum lending duration in seconds (Lend listings only) */
    public long getLendingDurationSeconds() { return lendingDurationSeconds; }
    /** @param seconds maximum lending duration in seconds */
    public void setLendingDurationSeconds(long seconds) { this.lendingDurationSeconds = seconds; }
    /** @return meet-up location */
    public String getMeetupLocation() { return meetupLocation; }
    /** @param meetupLocation new meet-up location */
    public void setMeetupLocation(String meetupLocation) { this.meetupLocation = meetupLocation; }
    /** @return photo file paths or URLs, first one is the thumbnail (modifiable, at most 6) */
    public List<String> getPhotos() { return photos; }
    /** @return first photo or null */
    public String getThumbnail() { return photos.isEmpty() ? null : photos.get(0); }
    /** @return submission time */
    public LocalDateTime getPostedAt() { return postedAt; }
    /** @return time the admin approved or rejected it, or null */
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    /** @param reviewedAt approval or rejection time */
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    /** @return current status */
    public ListingStatus getStatus() { return status; }
    /** @param status new status */
    public void setStatus(ListingStatus status) { this.status = status; }
    /** @return number of times the detail screen was opened */
    public int getViewCount() { return viewCount; }
    /** Adds one view (called every time the detail screen opens). */
    public void incrementViewCount() { viewCount++; }
    /** @return admin's rejection reason, or null */
    public String getRejectionReason() { return rejectionReason; }
    /** @param reason rejection reason */
    public void setRejectionReason(String reason) { this.rejectionReason = reason; }
    /** @return admin's take-down reason, or null */
    public String getRemovalReason() { return removalReason; }
    /** @param reason take-down reason */
    public void setRemovalReason(String reason) { this.removalReason = reason; }
    /** @return end of the current borrowing period when Lent, otherwise null */
    public LocalDateTime getLentUntil() { return lentUntil; }
    /** @param lentUntil end of the current borrowing period, or null */
    public void setLentUntil(LocalDateTime lentUntil) { this.lentUntil = lentUntil; }

    @Override public String toString() { return title; }
}
