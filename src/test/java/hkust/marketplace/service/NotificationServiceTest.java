package hkust.marketplace.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hkust.marketplace.model.Notification;
import hkust.marketplace.model.NotificationCategory;
import hkust.marketplace.util.MarketplaceException;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Task 1.8: notifications. */
class NotificationServiceTest extends ServiceTestBase {

    @Test
    void seededNotificationsNewestFirst() {
        List<Notification> list = app.notifications().forUser(DEMO, null, false);
        assertEquals(4, list.size());
        assertEquals("Borrow request accepted", list.get(0).getTitle());
        assertEquals("Welcome to HKUST Marketplace", list.get(3).getTitle());
        assertEquals(4, app.notifications().unreadCount(DEMO));
    }

    @Test
    void filterByCategoryAndUnread() {
        assertEquals(2, app.notifications().forUser(DEMO, NotificationCategory.DEALS, false).size());
        Notification n = app.notifications().forUser(DEMO, NotificationCategory.DEALS, false).get(0);
        app.notifications().markRead(DEMO, n.getId());
        assertTrue(n.isRead());
        assertEquals(1, app.notifications().forUser(DEMO, NotificationCategory.DEALS, true).size());
        assertEquals(3, app.notifications().unreadCount(DEMO));
    }

    @Test
    void markAllReadAndOwnershipCheck() {
        assertEquals(4, app.notifications().markAllRead(DEMO));
        assertEquals(0, app.notifications().unreadCount(DEMO));
        assertEquals(0, app.notifications().markAllRead(DEMO));
        String id = app.notifications().forUser(DEMO, null, false).get(0).getId();
        assertThrows(MarketplaceException.class, () -> app.notifications().markRead(CHRIS, id));
    }

    @Test
    void notifyAndNotifyAdmins() {
        clock.advance(Duration.ofSeconds(5));
        Notification n = app.notifications().notify(CHRIS, NotificationCategory.ACCOUNT, "t", "c", "x");
        assertEquals(T0.plusSeconds(5), n.getCreatedAt());
        assertEquals("x", n.getRelatedId());
        app.notifications().notifyAdmins(NotificationCategory.MODERATION, "a", "b", null);
        assertEquals(1, app.notifications().forUser(ADMIN, null, false).size());
    }
}
