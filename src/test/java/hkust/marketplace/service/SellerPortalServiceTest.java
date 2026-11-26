package hkust.marketplace.service;

import hkust.marketplace.data.DataStore;
import hkust.marketplace.data.SeedData;
import hkust.marketplace.model.*;
import hkust.marketplace.util.MarketplaceException;
import hkust.marketplace.util.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Regression checks for seller ownership, state transitions and Buyer Portal integration. */
class SellerPortalServiceTest extends ServiceTestBase {
    @Test
    void registersSellerAndUsesCaseInsensitiveRoleCheck() {
        Seller seller = app.auth().registerSeller("test_seller", "Test Seller", "TestPass123",
                "TestPass123", "  Bio  ", List.of("Library", "Library", "Atrium"));
        assertEquals("Bio", seller.getBio());
        assertEquals(List.of("Library", "Atrium"), seller.getMeetupLocations());
        assertNotEquals("TestPass123", seller.getPasswordHash());
        assertSame(seller, app.auth().login("TEST_SELLER", "TestPass123", Role.SELLER));
        assertThrows(MarketplaceException.class, () -> app.auth().login("test_seller", "TestPass123", Role.BUYER));
        assertThrows(MarketplaceException.class, () -> app.auth().login("demo_buyer", "Buyer123", Role.SELLER));
        seller.setStatus(AccountStatus.DEACTIVATED);
        assertThrows(MarketplaceException.class, () -> app.auth().login("test_seller", "TestPass123", Role.SELLER));
    }

    @Test
    void rejectsDuplicateUsernameAndInvalidSellerRegistrationFields() {
        assertThrows(ValidationException.class, () -> app.auth().registerSeller("DEMO_BUYER", "Seller",
                "TestPass123", "TestPass123", null, List.of()));
        var failure = assertThrows(ValidationException.class, () -> app.auth().registerSeller("bad name", "",
                "short", "other", null, List.of("Unknown location")));
        assertTrue(failure.getErrors().stream().anyMatch(text -> text.contains("HKUST")));
        assertTrue(failure.getErrors().stream().anyMatch(text -> text.contains("match")));
        assertTrue(store.findUserByUsername("bad name").isEmpty());
    }

    @Test
    void submitsEachListingTypeAndDoesNotDuplicateFormSubmission() {
        for (ListingType type : ListingType.values()) {
            var service = app.sellerListings();
            String id = service.newSubmissionId();
            var listing = service.create(ALICE, id, details(type, "Camera", "15.25", "5"));
            assertSame(listing, service.create(ALICE, id, details(type, "Camera", "15.25", "5")));
            assertEquals(ListingStatus.PENDING_REVIEW, listing.getStatus());
            assertEquals(0, listing.getViewCount());
            assertFalse(app.listings().isVisibleToBuyers(listing));
            assertEquals(type == ListingType.LEND ? 5 : 0, listing.getLendingDurationSeconds());
            assertEquals(type == ListingType.SELL, listing.getPrice() != null);
            assertEquals(1, app.notifications().forUser(ADMIN, NotificationCategory.LISTING_UPDATES, false)
                    .stream().filter(item -> id.equals(item.getRelatedId())).count());
        }
    }

    @Test
    void invalidListingNeverSavesOrNotifiesAdmin() {
        var service = app.sellerListings();
        String key = service.newSubmissionId();
        assertThrows(ValidationException.class, () -> service.create(ALICE, key,
                details(ListingType.SELL, "", "13.333", "")));
        assertThrows(ValidationException.class, () -> service.create(ALICE, key,
                details(ListingType.LEND, "Book", "", "4")));
        assertTrue(store.findListing(key).isEmpty());
        assertTrue(app.notifications().forUser(ADMIN, null, false).stream()
                .noneMatch(item -> key.equals(item.getRelatedId())));
    }

    @Test
    void revisionsAndOpenOffersProtectListings() {
        var service = app.sellerListings();
        Listing listing = listing("L-calc");
        var original = SellerListingService.Revision.of(listing);
        var replacement = details(ListingType.SELL, "Updated book", "150", "");
        assertThrows(MarketplaceException.class, () -> service.edit(BOB, listing.getId(), original, replacement));
        listing.setReviewedAt(now());
        assertThrows(MarketplaceException.class, () -> service.edit(ALICE, listing.getId(), original, replacement));
        Deal offer = app.deals().makeOffer(DEMO, listing.getId(), "100", null, false);
        var current = SellerListingService.Revision.of(listing);
        assertThrows(MarketplaceException.class, () -> service.remove(ALICE, listing.getId(), current));
        app.deals().declineDeal(ALICE, offer.getId());
        service.edit(ALICE, listing.getId(), current, replacement);
        assertEquals(ListingStatus.PENDING_REVIEW, listing.getStatus());
        assertNull(listing.getReviewedAt());
        assertEquals("Updated book", listing.getTitle());
        service.remove(ALICE, listing.getId(), SellerListingService.Revision.of(listing));
        assertEquals(ListingStatus.REMOVED, listing.getStatus());
        assertTrue(app.chat().findThread(DEMO, listing.getId()).isPresent());
        assertTrue(store.findDeal(offer.getId()).isPresent());
    }

    @Test
    void sellerCounterShowsInBuyerDealsAndBothSidesShareChat() {
        Deal deal = app.deals().makeOffer(DEMO, "L-calc", "100", null, false);
        assertTrue(app.deals().dealsForSeller(BOB, null).stream().noneMatch(item -> item == deal));
        assertThrows(MarketplaceException.class, () -> app.deals().acceptDeal(BOB, deal.getId()));
        app.deals().counterOffer(ALICE, deal.getId(), "125");
        assertEquals(DealStatus.COUNTERED, deal.getStatus());
        ChatThread thread = app.chat().findThread(DEMO, "L-calc").orElseThrow();
        assertThrows(MarketplaceException.class, () -> app.chat().requireThread(BOB, thread.getId()));
        assertEquals(1, app.chat().search(DEMO, "counter-OFFER", thread.getId()).size());
        app.chat().send(thread.getId(), ALICE, "I can meet at the Library.");
        assertEquals(1, app.chat().search(DEMO, "librarY", null).size());
        app.deals().respondToCounter(DEMO, deal.getId(), true);
        assertEquals(DealStatus.ACCEPTED, deal.getStatus());
        assertEquals(ListingStatus.RESERVED, listing("L-calc").getStatus());
    }

    @Test
    void buyerOffersAndLendingCountOnceAndRejectedClaimReleasesListing() {
        long before = app.sellerStats(ALICE).requests();
        Deal borrow = app.deals().requestBorrow(DEMO, "L-umbr", now().plusSeconds(30), now().plusMinutes(1),
                null, false);
        assertThrows(ValidationException.class, () -> app.deals().counterBorrow(ALICE, borrow.getId(),
                now().minusSeconds(1), now().plusMinutes(1)));
        app.deals().counterBorrow(ALICE, borrow.getId(), now().plusMinutes(1), now().plusMinutes(2));
        assertEquals(before + 1, app.sellerStats(ALICE).requests());
        app.deals().respondToCounter(DEMO, borrow.getId(), true);
        assertEquals(ListingStatus.LENT, listing("L-umbr").getStatus());
        Deal claim = app.deals().claim(DEMO, "L-lamp");
        app.deals().declineDeal(BOB, claim.getId());
        assertEquals(ListingStatus.LIVE, listing("L-lamp").getStatus());
    }

    @Test
    void sellerCanUpdateProfileAndPasswordWithoutOverwritingNewerProfile() {
        var service = app.profile();
        var shown = service.sellerDetails(ALICE);
        var updated = service.updateSeller(ALICE, shown, "Updated Name", "", List.of("Library"));
        assertEquals("Updated Name", updated.fullName());
        assertNull(updated.bio());
        assertEquals(List.of("Library"), updated.locations());
        assertThrows(MarketplaceException.class,
                () -> service.updateSeller(ALICE, shown, "Stale Name", "", List.of()));
        assertThrows(ValidationException.class,
                () -> service.changePassword(ALICE, "wrong", "Updated123", "Updated123"));
        service.changePassword(ALICE, "Seller123", "Updated123", "Updated123");
        assertEquals(ALICE, app.auth().login("alice_w", "Updated123", Role.SELLER).getId());
    }

    @Test
    void sellerCanReplyAndFlagWithRecipientAndRatingChecks() {
        var review = new Review("REV-test", "D-test", DEMO, ALICE, 2, "Arrived late", now());
        store.putReview(review);
        assertThrows(MarketplaceException.class, () -> app.reviews().flag(BOB, review.getId()));
        assertThrows(ValidationException.class, () -> app.reviews().reply(ALICE, review.getId(), " "));
        app.reviews().reply(ALICE, review.getId(), "Thanks for the feedback.");
        assertThrows(MarketplaceException.class, () -> app.reviews().reply(ALICE, review.getId(), "Again"));
        assertTrue(app.notifications().forUser(DEMO, NotificationCategory.REVIEWS, true).stream()
                .anyMatch(item -> review.getId().equals(item.getRelatedId())));
        assertFalse(app.notifications().forUser(CHRIS, NotificationCategory.REVIEWS, true).stream()
                .anyMatch(item -> review.getId().equals(item.getRelatedId())));
        app.reviews().flag(ALICE, review.getId());
        assertTrue(review.isFlagged());
        assertFalse(review.isHidden());
        assertTrue(app.reviews().visibleReviewsForSeller(ALICE).contains(review));
        assertTrue(app.notifications().forUser(ADMIN, NotificationCategory.MODERATION, true).stream()
                .anyMatch(item -> review.getId().equals(item.getRelatedId())));
        long count = app.sellerStats(ALICE).reviewCount();
        review.setHidden(true);
        assertEquals(count - 1, app.sellerStats(ALICE).reviewCount());
        assertThrows(MarketplaceException.class, () -> app.reviews().flag(ALICE, review.getId()));
    }

    @Test
    void notificationsAreOwnedAndStatsDoNotCountAsViews() {
        var listing = listing("L-calc");
        int viewsBefore = listing.getViewCount();
        app.sellerStats(ALICE);
        assertEquals(viewsBefore, listing.getViewCount());
        Deal deal = app.deals().makeOffer(DEMO, "L-calc", "100", null, false);
        Notification notification = app.notifications().forUser(ALICE, null, true).stream()
                .filter(item -> deal.getId().equals(item.getRelatedId())).findFirst().orElseThrow();
        assertThrows(MarketplaceException.class,
                () -> app.notifications().markRead(BOB, notification.getId()));
        assertTrue(app.notifications().markRead(ALICE, notification.getId()).isRead());
    }

    @Test
    void failedSellerSubmissionRollsBackListingAndAdminNotification(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("data.ser");
        DataStore disk = DataStore.load(file);
        SeedData.populate(disk, clock);
        disk.save();
        Files.createDirectory(directory.resolve("data.ser.tmp"));
        Marketplace bound = new Marketplace(disk, clock);
        String id = bound.sellerListings().newSubmissionId();
        assertThrows(UncheckedIOException.class, () -> bound.sellerListings().create(ALICE, id,
                details(ListingType.GIVEAWAY, "Chair", "", "")));
        assertTrue(disk.findListing(id).isEmpty());
        assertTrue(disk.notifications().stream().noneMatch(item -> id.equals(item.getRelatedId())));
        assertTrue(DataStore.load(file).findListing(id).isEmpty());
        Files.delete(directory.resolve("data.ser.tmp"));
        bound.sellerListings().create(ALICE, id, details(ListingType.GIVEAWAY, "Chair", "", ""));
        assertTrue(DataStore.load(file).findListing(id).isPresent());
    }

    private static SellerListingService.Details details(ListingType type, String title, String price, String duration) {
        return new SellerListingService.Details(type, title, Category.ELECTRONICS, Condition.LIKE_NEW,
                "Description", "Library", price, duration, List.of());
    }
}
