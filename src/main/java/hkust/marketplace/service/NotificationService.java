package hkust.marketplace.service;

import hkust.marketplace.data.DataStore;
import hkust.marketplace.model.Admin;
import hkust.marketplace.model.Notification;
import hkust.marketplace.model.NotificationCategory;
import hkust.marketplace.util.Ids;
import hkust.marketplace.util.MarketplaceException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/** Sends and reads notifications (Task 1.8 Notification Board). */
public class NotificationService {
    private final DataStore store;
    private final Clock clock;

    /**
     * @param store data store
     * @param clock clock used for timestamps
     */
    public NotificationService(DataStore store, Clock clock) {
        this.store = store;
        this.clock = clock;
    }

    /**
     * Sends a notification to one user.
     * @param userId recipient
     * @param category category
     * @param title short title
     * @param content full message
     * @param relatedId related listing, deal, thread or review id, or null
     * @return the stored notification
     */
    public Notification notify(String userId, NotificationCategory category, String title, String content,
                               String relatedId) {
        Notification n = new Notification(Ids.next("N"), userId, category, title, content,
                LocalDateTime.now(clock), relatedId);
        store.putNotification(n);
        return n;
    }

    /**
     * Sends the same notification to every admin account.
     * @param category category
     * @param title title
     * @param content message
     * @param relatedId related record id, or null
     */
    public void notifyAdmins(NotificationCategory category, String title, String content, String relatedId) {
        store.users().stream().filter(u -> u instanceof Admin)
                .forEach(a -> notify(a.getId(), category, title, content, relatedId));
    }

    /**
     * Lists a user's notifications, newest first.
     * @param userId recipient
     * @param category only this category, or null for all
     * @param unreadOnly true to show only unread ones
     * @return matching notifications
     */
    public List<Notification> forUser(String userId, NotificationCategory category, boolean unreadOnly) {
        return store.notifications().stream()
                .filter(n -> n.getUserId().equals(userId))
                .filter(n -> category == null || n.getCategory() == category)
                .filter(n -> !unreadOnly || !n.isRead())
                .sorted(Comparator.comparing(Notification::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    /**
     * @param userId recipient
     * @return number of unread notifications
     */
    public long unreadCount(String userId) {
        return store.notifications().stream().filter(n -> n.getUserId().equals(userId) && !n.isRead()).count();
    }

    /**
     * Marks one notification as read.
     * @param userId the user opening it (must be the recipient)
     * @param notificationId notification id
     * @return the notification
     * @throws MarketplaceException if it does not exist or belongs to someone else
     */
    public Notification markRead(String userId, String notificationId) {
        synchronized (store) {
            Notification n = store.notifications().stream()
                    .filter(x -> x.getId().equals(notificationId) && x.getUserId().equals(userId))
                    .findFirst()
                    .orElseThrow(() -> new MarketplaceException("Notification not found."));
            if (!n.isRead()) {
                store.transact(() -> {
                    n.setRead(true);
                    return null;
                });
            }
            return n;
        }
    }

    /**
     * Marks all of a user's notifications as read.
     * @param userId recipient
     * @return how many were changed
     */
    public int markAllRead(String userId) {
        synchronized (store) {
            List<Notification> unread = forUser(userId, null, true);
            if (unread.isEmpty()) return 0;
            return store.transact(() -> {
                unread.forEach(notification -> notification.setRead(true));
                return unread.size();
            });
        }
    }
}
