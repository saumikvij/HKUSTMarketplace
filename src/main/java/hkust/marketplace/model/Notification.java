package hkust.marketplace.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/** A timestamped, categorised message the system sends to one user. */
public class Notification implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String userId;
    private final NotificationCategory category;
    private final String title;
    private final String content;
    private final LocalDateTime createdAt;
    private final String relatedId;
    private boolean read;
    private boolean archived;

    /**
     * Creates an unread notification.
     * @param id unique id
     * @param userId recipient
     * @param category category
     * @param title short title
     * @param content full message
     * @param createdAt time sent
     * @param relatedId id of the related listing, deal, thread or review, or null
     */
    public Notification(String id, String userId, NotificationCategory category, String title, String content,
                        LocalDateTime createdAt, String relatedId) {
        this.id = id;
        this.userId = userId;
        this.category = category;
        this.title = title;
        this.content = content;
        this.createdAt = createdAt;
        this.relatedId = relatedId;
    }

    /** @return unique id */
    public String getId() { return id; }
    /** @return recipient id */
    public String getUserId() { return userId; }
    /** @return category */
    public NotificationCategory getCategory() { return category; }
    /** @return title */
    public String getTitle() { return title; }
    /** @return full message */
    public String getContent() { return content; }
    /** @return time sent */
    public LocalDateTime getCreatedAt() { return createdAt; }
    /** @return related record id, or null */
    public String getRelatedId() { return relatedId; }
    /** @return true once opened */
    public boolean isRead() { return read; }
    /** @param read read state */
    public void setRead(boolean read) { this.read = read; }
    /** @return true if archived (admin board) */
    public boolean isArchived() { return archived; }
    /** @param archived archived state */
    public void setArchived(boolean archived) { this.archived = archived; }
}
