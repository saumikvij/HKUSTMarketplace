package hkust.marketplace.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Enum rules and entity behaviour. */
class ModelTest {
    private static final LocalDateTime T = LocalDateTime.of(2026, 11, 20, 12, 0);

    @Test
    void browsableStatuses() {
        Set<ListingStatus> browsable = EnumSet.of(ListingStatus.LIVE, ListingStatus.RESERVED, ListingStatus.LENT);
        for (ListingStatus s : ListingStatus.values()) {
            assertEquals(browsable.contains(s), s.isBrowsable(), s.name());
        }
    }

    @Test
    void dealStatusGroups() {
        for (DealStatus s : DealStatus.values()) {
            boolean active = s == DealStatus.PENDING || s == DealStatus.COUNTERED || s == DealStatus.ACCEPTED
                    || s == DealStatus.BORROWED;
            boolean finished = s == DealStatus.COMPLETED || s == DealStatus.RETURNED || s == DealStatus.AUTO_RETURNED;
            assertEquals(active, s.isActive(), s.name());
            assertEquals(finished, s.isFinished(), s.name());
            assertEquals(s == DealStatus.PENDING || s == DealStatus.COUNTERED, s.isOpenRequest(), s.name());
            assertEquals(s.label(), s.toString());
        }
        assertEquals("Auto-returned", DealStatus.AUTO_RETURNED.label());
    }

    @Test
    void dealTypesMatchListingTypes() {
        assertEquals(DealType.OFFER, DealType.forListingType(ListingType.SELL));
        assertEquals(DealType.BORROW, DealType.forListingType(ListingType.LEND));
        assertEquals(DealType.CLAIM, DealType.forListingType(ListingType.GIVEAWAY));
        assertEquals("Bought", DealType.OFFER.historyLabel());
        assertEquals("Received", DealType.CLAIM.historyLabel());
        assertEquals("Borrow", DealType.BORROW.toString());
    }

    @Test
    void labelsAreReadable() {
        for (Category c : Category.values()) {
            assertEquals(c.label(), c.toString());
        }
        for (NotificationCategory c : NotificationCategory.values()) {
            assertEquals(c.label(), c.toString());
        }
        for (ReportReason r : ReportReason.values()) {
            assertEquals(r.label(), r.toString());
        }
        for (ListingType t : ListingType.values()) {
            assertFalse(t.label().isEmpty());
        }
        for (Condition c : Condition.values()) {
            assertFalse(c.label().isEmpty());
        }
    }

    @Test
    void dealCurrentPriceAndTimestamps() {
        Deal d = new Deal("D1", "U1", "L1", DealType.OFFER, T);
        assertEquals(DealStatus.PENDING, d.getStatus());
        assertEquals(T, d.getUpdatedAt());
        d.setOfferedPrice(new BigDecimal("10.00"));
        assertEquals(new BigDecimal("10.00"), d.getCurrentPrice());
        d.setCounterPrice(new BigDecimal("12.00"));
        assertEquals(new BigDecimal("12.00"), d.getCurrentPrice());
        d.setStatus(DealStatus.ACCEPTED, T.plusMinutes(1));
        assertEquals(T.plusMinutes(1), d.getUpdatedAt());
        d.setCounterPeriod(T, T.plusHours(1));
        assertEquals(T.plusHours(1), d.getCounterEnd());
        d.setReminderSent(true);
        assertTrue(d.isReminderSent());
    }

    @Test
    void listingPhotosAndCounters() {
        Listing l = new Listing("L1", "S1", ListingType.SELL, "Lamp", Category.OTHER, Condition.USED, "d", "Atrium", T);
        assertNull(l.getThumbnail());
        l.getPhotos().add("a.png");
        l.getPhotos().add("b.png");
        assertEquals("a.png", l.getThumbnail());
        l.incrementViewCount();
        assertEquals(1, l.getViewCount());
        assertEquals(ListingStatus.PENDING_REVIEW, l.getStatus());
        l.setRejectionReason("blurry");
        l.setRemovalReason("spam");
        assertEquals("blurry", l.getRejectionReason());
        assertEquals("spam", l.getRemovalReason());
    }

    @Test
    void usersKnowTheirRole() {
        Buyer b = new Buyer("U1", "b", "B", "h", "s", T, "Hall 1");
        Seller s = new Seller("U2", "s", "S", "h", "s", T, "bio");
        Admin a = new Admin("U3", "a", "A", "h", "s", T);
        assertEquals(Role.BUYER, b.getRole());
        assertEquals(Role.SELLER, s.getRole());
        assertEquals(Role.ADMIN, a.getRole());
        assertTrue(b.isActive());
        b.setStatus(AccountStatus.DEACTIVATED);
        assertFalse(b.isActive());
        assertEquals("B (@b)", b.toString());
        b.setPassword("h2", "s2");
        assertEquals("h2", b.getPasswordHash());
        assertEquals("s2", b.getSalt());
        assertEquals("bio", s.getBio());
    }

    @Test
    void chatAndNotificationBasics() {
        ChatMessage card = new ChatMessage("U1", Role.BUYER, "Offer", T, "D1");
        assertTrue(card.isOfferCard());
        Notification n = new Notification("N1", "U1", NotificationCategory.DEALS, "t", "c", T, null);
        assertFalse(n.isRead());
        n.setArchived(true);
        assertTrue(n.isArchived());
        Review r = new Review("V1", "D1", "U1", "U2", 5, "ok", T);
        r.setSellerReply("thanks");
        r.setFlagged(true);
        assertEquals("thanks", r.getSellerReply());
        assertTrue(r.isFlagged());
    }
}
