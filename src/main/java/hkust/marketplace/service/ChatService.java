package hkust.marketplace.service;

import hkust.marketplace.data.DataStore;
import hkust.marketplace.model.ChatMessage;
import hkust.marketplace.model.ChatThread;
import hkust.marketplace.model.Listing;
import hkust.marketplace.model.ListingStatus;
import hkust.marketplace.model.NotificationCategory;
import hkust.marketplace.model.Role;
import hkust.marketplace.model.User;
import hkust.marketplace.util.Ids;
import hkust.marketplace.util.MarketplaceException;
import hkust.marketplace.util.ValidationException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Private chat linked to a listing (Task 1.5). There is one thread per buyer and listing, shared by the buyer
 * and the seller. History is kept permanently and is searchable.
 */
public class ChatService {
    /** Maximum message length. */
    public static final int MAX_MESSAGE_LENGTH = 1000;

    private final DataStore store;
    private final Clock clock;
    private final NotificationService notifications;

    /** A message found by a chat search, with the thread it belongs to. */
    public static final class SearchHit {
        private final ChatThread thread;
        private final ChatMessage message;

        SearchHit(ChatThread thread, ChatMessage message) {
            this.thread = thread;
            this.message = message;
        }

        /** @return the thread containing the message */
        public ChatThread getThread() { return thread; }
        /** @return the matching message */
        public ChatMessage getMessage() { return message; }
    }

    /**
     * @param store data store
     * @param clock clock
     * @param notifications notification service
     */
    public ChatService(DataStore store, Clock clock, NotificationService notifications) {
        this.store = store;
        this.clock = clock;
        this.notifications = notifications;
    }

    /**
     * Returns the buyer's thread for a listing, creating it if none exists.
     * @param buyerId buyer
     * @param listingId listing
     * @return the thread
     */
    public ChatThread openThread(String buyerId, String listingId) {
        Access.requireBuyer(store, buyerId);
        Listing listing = store.findListing(listingId)
                .orElseThrow(() -> new MarketplaceException("Listing not found."));
        synchronized (store) {
            Optional<ChatThread> existing = findThread(buyerId, listingId);
            if (existing.isPresent()) {
                return existing.get();
            }
            return store.transact(() -> getOrCreateThread(buyerId, listing));
        }
    }

    ChatThread getOrCreateThread(String buyerId, Listing listing) {
        return findThread(buyerId, listing.getId()).orElseGet(() -> {
            ChatThread thread = new ChatThread(Ids.next("C"), buyerId, listing.getSellerId(), listing.getId());
            store.putThread(thread);
            return thread;
        });
    }

    /** Returns an existing chat only to its signed-in participant. */
    public ChatThread requireThread(String userId, String threadId) {
        return store.findThread(threadId).filter(thread -> thread.hasParticipant(userId))
                .orElseThrow(() -> new MarketplaceException("This chat is unavailable."));
    }

    /**
     * @param buyerId buyer
     * @param listingId listing
     * @return the existing thread, if any
     */
    public Optional<ChatThread> findThread(String buyerId, String listingId) {
        return store.threads().stream()
                .filter(t -> t.getBuyerId().equals(buyerId) && t.getListingId().equals(listingId))
                .findFirst();
    }

    /**
     * @param userId a buyer or seller
     * @return all threads the user takes part in
     */
    public List<ChatThread> threadsFor(String userId) {
        return store.threads().stream().filter(t -> t.hasParticipant(userId)).collect(Collectors.toList());
    }

    /**
     * Explains why a thread is read-only, if it is.
     * @param thread thread
     * @return the reason, or empty if new messages are allowed
     */
    public Optional<String> readOnlyReason(ChatThread thread) {
        Listing listing = store.findListing(thread.getListingId()).orElse(null);
        if (listing == null || listing.getStatus() == ListingStatus.REMOVED) {
            return Optional.of("This listing was removed. The chat is read-only.");
        }
        boolean sellerActive = store.findUser(thread.getSellerId()).map(User::isActive).orElse(false);
        boolean buyerActive = store.findUser(thread.getBuyerId()).map(User::isActive).orElse(false);
        if (!sellerActive || !buyerActive) {
            return Optional.of("The other account is no longer active. The chat is read-only.");
        }
        return Optional.empty();
    }

    /**
     * Sends a free-text message and notifies the other participant.
     * @param threadId thread
     * @param senderId buyer or seller of the thread
     * @param text 1 to 1,000 characters, not only spaces
     * @return the stored message
     * @throws ValidationException if the text is empty or too long
     * @throws MarketplaceException if the sender is not in the thread or the thread is read-only
     */
    public ChatMessage send(String threadId, String senderId, String text) {
        synchronized (store) {
            ChatThread thread = store.findThread(threadId)
                    .orElseThrow(() -> new MarketplaceException("Chat not found."));
            if (!thread.hasParticipant(senderId)) {
                throw new MarketplaceException("You are not part of this chat.");
            }
            Optional<String> readOnly = readOnlyReason(thread);
            if (readOnly.isPresent()) {
                throw new MarketplaceException(readOnly.get());
            }
            if (text == null || text.isBlank()) {
                throw new ValidationException("Message cannot be empty");
            }
            if (text.length() > MAX_MESSAGE_LENGTH) {
                throw new ValidationException("Message must be at most " + MAX_MESSAGE_LENGTH + " characters");
            }
            Role role = thread.getBuyerId().equals(senderId) ? Role.BUYER : Role.SELLER;
            ChatMessage message = new ChatMessage(senderId, role, text, LocalDateTime.now(clock), null);
            String recipient = role == Role.BUYER ? thread.getSellerId() : thread.getBuyerId();
            String sender = store.findUser(senderId).map(User::getFullName).orElse("Someone");
            return store.transact(() -> {
                thread.getMessages().add(message);
                notifications.notify(recipient, NotificationCategory.MESSAGES, "New message from " + sender,
                        preview(text), thread.getId());
                return message;
            });
        }
    }

    /**
     * Adds a formal offer card (an offer, borrow request, claim or counter-offer) to the thread.
     * @param thread thread
     * @param senderId sender
     * @param senderRole sender role
     * @param text card text
     * @param dealId linked deal
     */
    void postOfferCard(ChatThread thread, String senderId, Role senderRole, String text, String dealId) {
        synchronized (store) {
            thread.getMessages().add(new ChatMessage(senderId, senderRole, text, LocalDateTime.now(clock), dealId));
        }
    }

    /**
     * Searches chat history, ignoring case.
     * @param userId the searching user (only their own threads are searched)
     * @param keyword keyword, not blank
     * @param threadId search only this thread, or null for all the user's threads
     * @return matching messages, oldest first
     * @throws ValidationException if the keyword is blank
     */
    public List<SearchHit> search(String userId, String keyword, String threadId) {
        if (keyword == null || keyword.isBlank()) {
            throw new ValidationException("Please enter a keyword to search");
        }
        String key = keyword.trim().toLowerCase(Locale.ROOT);
        List<SearchHit> hits = new ArrayList<>();
        for (ChatThread thread : threadsFor(userId)) {
            if (threadId != null && !thread.getId().equals(threadId)) {
                continue;
            }
            for (ChatMessage m : thread.getMessages()) {
                if (m.getText().toLowerCase(Locale.ROOT).contains(key)) {
                    hits.add(new SearchHit(thread, m));
                }
            }
        }
        hits.sort(Comparator.comparing(h -> h.getMessage().getSentAt()));
        return hits;
    }

    private static String preview(String text) {
        return text.length() <= 80 ? text : text.substring(0, 77) + "...";
    }
}
