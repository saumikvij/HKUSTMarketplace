package hkust.marketplace.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hkust.marketplace.model.AccountStatus;
import hkust.marketplace.model.NotificationCategory;
import hkust.marketplace.model.Review;
import hkust.marketplace.util.MarketplaceException;
import hkust.marketplace.util.ValidationException;
import org.junit.jupiter.api.Test;

/** Task 1.10: rate and review the seller. */
class ReviewServiceTest extends ServiceTestBase {

    @Test
    void reviewAFinishedDeal() {
        Review r = app.reviews().submitReview(DEMO, "D-mug", 4, "  Nice mug, friendly seller. ");
        assertEquals(BOB, r.getSellerId());
        assertEquals("Nice mug, friendly seller.", r.getComment());
        assertEquals(r, app.reviews().reviewForDeal("D-mug").orElseThrow());
        assertEquals(1, app.reviews().reviewsByBuyer(DEMO).size());
        assertEquals(4.5, app.reviews().averageRating(BOB).getAsDouble());
        assertEquals("4.5 ★ (2 reviews)", app.reviews().ratingSummary(BOB));
        assertEquals("New review", app.notifications().forUser(BOB, NotificationCategory.REVIEWS, false)
                .get(0).getTitle());
    }

    @Test
    void ratingSummaryForNoneAndOne() {
        assertEquals("No ratings yet", app.reviews().ratingSummary(ALICE));
        assertFalse(app.reviews().averageRating(ALICE).isPresent());
        assertEquals("5.0 ★ (1 review)", app.reviews().ratingSummary(BOB));
    }

    @Test
    void hiddenReviewsAreNotCounted() {
        store.reviews().get(0).setHidden(true);
        assertEquals("No ratings yet", app.reviews().ratingSummary(BOB));
    }

    @Test
    void onlyOneReviewPerFinishedDeal() {
        app.reviews().submitReview(DEMO, "D-mug", 5, "Great");
        assertEquals("You have already reviewed this deal.", assertThrows(MarketplaceException.class,
                () -> app.reviews().submitReview(DEMO, "D-mug", 5, "Again")).getMessage());
    }

    @Test
    void cannotReviewUnfinishedOrOthersDeals() {
        assertTrue(assertThrows(MarketplaceException.class,
                () -> app.reviews().submitReview(DEMO, "D-hood", 5, "x")).getMessage().contains("finished"));
        assertThrows(MarketplaceException.class, () -> app.reviews().submitReview(DEMO, "D-book", 5, "x"));
        assertThrows(MarketplaceException.class, () -> app.reviews().submitReview(DEMO, "missing", 5, "x"));
        assertThrows(MarketplaceException.class, () -> app.reviews().submitReview(ALICE, "D-mug", 5, "x"));
    }

    @Test
    void ratingAndCommentRules() {
        assertThrows(ValidationException.class, () -> app.reviews().submitReview(DEMO, "D-mug", 0, "x"));
        assertThrows(ValidationException.class, () -> app.reviews().submitReview(DEMO, "D-mug", 6, "x"));
        assertEquals("Please write a review", assertThrows(ValidationException.class,
                () -> app.reviews().submitReview(DEMO, "D-mug", 3, " ")).getMessage());
        assertThrows(ValidationException.class, () -> app.reviews().submitReview(DEMO, "D-mug", 3, null));
        assertThrows(ValidationException.class,
                () -> app.reviews().submitReview(DEMO, "D-mug", 3, "a".repeat(501)));
        app.reviews().submitReview(DEMO, "D-mug", 3, "a".repeat(500));
    }

    @Test
    void deletedSellerOrMissingListingCannotBeRated() {
        store.findUser(BOB).orElseThrow().setStatus(AccountStatus.DELETED);
        assertThrows(MarketplaceException.class, () -> app.reviews().submitReview(DEMO, "D-mug", 5, "x"));
        store.findUser(BOB).orElseThrow().setStatus(AccountStatus.ACTIVE);
        store.findDeal("D-mug").orElseThrow();
        hkust.marketplace.model.Deal ghost = new hkust.marketplace.model.Deal("D-g", DEMO, "L-none",
                hkust.marketplace.model.DealType.OFFER, T0);
        ghost.setStatus(hkust.marketplace.model.DealStatus.COMPLETED, T0);
        store.putDeal(ghost);
        assertThrows(MarketplaceException.class, () -> app.reviews().submitReview(DEMO, "D-g", 5, "x"));
    }
}
