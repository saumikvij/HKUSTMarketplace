package hkust.marketplace.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/** One message in a chat thread. A formal offer is a message linked to a deal. */
public class ChatMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String senderId;
    private final Role senderRole;
    private final String text;
    private final LocalDateTime sentAt;
    private final String dealId;

    /**
     * Creates a message.
     * @param senderId id of the sender
     * @param senderRole BUYER or SELLER
     * @param text message text
     * @param sentAt time sent
     * @param dealId id of the deal for a formal offer card, or null for a normal message
     */
    public ChatMessage(String senderId, Role senderRole, String text, LocalDateTime sentAt, String dealId) {
        this.senderId = senderId;
        this.senderRole = senderRole;
        this.text = text;
        this.sentAt = sentAt;
        this.dealId = dealId;
    }

    /** @return sender id */
    public String getSenderId() { return senderId; }
    /** @return sender role */
    public Role getSenderRole() { return senderRole; }
    /** @return text */
    public String getText() { return text; }
    /** @return time sent */
    public LocalDateTime getSentAt() { return sentAt; }
    /** @return linked deal id for an offer card, or null */
    public String getDealId() { return dealId; }
    /** @return true if this is a formal offer card */
    public boolean isOfferCard() { return dealId != null; }
}
