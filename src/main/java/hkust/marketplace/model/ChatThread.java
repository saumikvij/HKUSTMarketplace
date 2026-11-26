package hkust.marketplace.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** The one private chat between a buyer and the seller of a listing. History is kept permanently. */
public class ChatThread implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String buyerId;
    private final String sellerId;
    private final String listingId;
    private final List<ChatMessage> messages = new ArrayList<>();

    /**
     * Creates an empty thread.
     * @param id unique id
     * @param buyerId buyer
     * @param sellerId seller of the listing
     * @param listingId listing the chat is about
     */
    public ChatThread(String id, String buyerId, String sellerId, String listingId) {
        this.id = id;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.listingId = listingId;
    }

    /** @return unique id */
    public String getId() { return id; }
    /** @return buyer id */
    public String getBuyerId() { return buyerId; }
    /** @return seller id */
    public String getSellerId() { return sellerId; }
    /** @return listing id */
    public String getListingId() { return listingId; }
    /** @return messages in time order (modifiable) */
    public List<ChatMessage> getMessages() { return messages; }
    /**
     * @param userId a user id
     * @return true if the user is the buyer or the seller of this thread
     */
    public boolean hasParticipant(String userId) {
        return buyerId.equals(userId) || sellerId.equals(userId);
    }
}
