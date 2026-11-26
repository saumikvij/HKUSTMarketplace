package hkust.marketplace.service;

import hkust.marketplace.data.DataStore;
import hkust.marketplace.model.Deal;
import hkust.marketplace.model.Listing;
import hkust.marketplace.model.NotificationCategory;
import hkust.marketplace.model.Review;
import hkust.marketplace.model.User;
import hkust.marketplace.util.Ids;
import hkust.marketplace.util.MarketplaceException;
import hkust.marketplace.util.ValidationException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

/** Rating and reviewing sellers after a finished deal (Task 1.10). */
public class ReviewService {
    /** Maximum review length. */
    public static final int MAX_COMMENT_LENGTH = 500;

    private final DataStore store;
    private final Clock clock;
    private final NotificationService notifications;

    /**
     * @param store data store
     * @param clock clock
     * @param notifications notification service
     */
    public ReviewService(DataStore store, Clock clock, NotificationService notifications) {
        this.store = store;
        this.clock = clock;
        this.notifications = notifications;
    }

    /**
     * Submits a 1-5 star rating and written review for a finished deal.
     * @param buyerId reviewer (must own the deal)
     * @param dealId a Completed, Returned or Auto-returned deal
     * @param rating 1 to 5
     * @param comment written review, 1 to 500 characters
     * @return the stored review
     * @throws ValidationException if the rating or comment is invalid
     * @throws MarketplaceException if the deal is not finished, already reviewed, or the seller was deleted
     */
    public Review submitReview(String buyerId, String dealId, int rating, String comment) {
        Access.requireBuyer(store, buyerId);
        Deal deal = store.findDeal(dealId).filter(d -> d.getBuyerId().equals(buyerId))
                .orElseThrow(() -> new MarketplaceException("Deal not found."));
        if (!deal.getStatus().isFinished()) {
            throw new MarketplaceException("You can only review a seller after the deal is finished.");
        }
        if (reviewForDeal(dealId).isPresent()) {
            throw new MarketplaceException("You have already reviewed this deal.");
        }
        Listing listing = store.findListing(deal.getListingId())
                .orElseThrow(() -> new MarketplaceException("Listing not found."));
        User seller = store.findUser(listing.getSellerId()).orElse(null);
        if (seller == null || seller.getStatus() == hkust.marketplace.model.AccountStatus.DELETED) {
            throw new MarketplaceException("This seller can no longer be rated.");
        }
        if (rating < 1 || rating > 5) {
            throw new ValidationException("Please choose a rating from 1 to 5 stars");
        }
        if (comment == null || comment.isBlank()) {
            throw new ValidationException("Please write a review");
        }
        if (comment.trim().length() > MAX_COMMENT_LENGTH) {
            throw new ValidationException("Review must be at most " + MAX_COMMENT_LENGTH + " characters");
        }
        Review review = new Review(Ids.next("V"), dealId, buyerId, seller.getId(), rating, comment.trim(),
                LocalDateTime.now(clock));
        store.putReview(review);
        notifications.notify(seller.getId(), NotificationCategory.REVIEWS, "New review",
                "You received " + rating + " star(s) for \"" + listing.getTitle() + "\".", review.getId());
        store.commit();
        return review;
    }

    /**
     * @param dealId deal
     * @return the review of that deal, if any
     */
    public Optional<Review> reviewForDeal(String dealId) {
        return store.reviews().stream().filter(r -> r.getDealId().equals(dealId)).findFirst();
    }

    /**
     * @param buyerId reviewer
     * @return every review the buyer submitted, newest first (My Reviews)
     */
    public List<Review> reviewsByBuyer(String buyerId) {
        return store.reviews().stream().filter(r -> r.getBuyerId().equals(buyerId))
                .sorted(Comparator.comparing(Review::getPostedAt).reversed()).collect(Collectors.toList());
    }

    /** Posts the one permitted seller reply to a visible review and notifies its buyer. */
    public void reply(String sellerId, String reviewId, String reply) {
        if (reply == null || reply.isBlank()) throw new ValidationException("A reply cannot be empty");
        synchronized (store) {
            Review review = requireVisibleOwned(sellerId, reviewId);
            if (review.getSellerReply() != null) throw new MarketplaceException("This review already has a reply.");
            store.transact(() -> {
                review.setSellerReply(reply.trim());
                notifications.notify(review.getBuyerId(), NotificationCategory.REVIEWS, "Seller replied",
                        reply.trim(), review.getId());
                return null;
            });
        }
    }

    /** Flags a visible review for the admin while leaving it visible to buyers. */
    public void flag(String sellerId, String reviewId) {
        synchronized (store) {
            Review review = requireVisibleOwned(sellerId, reviewId);
            if (review.isFlagged()) throw new MarketplaceException("This review is already waiting for the admin.");
            store.transact(() -> {
                review.setFlagged(true);
                notifications.notifyAdmins(NotificationCategory.MODERATION, "Review flagged",
                        "A seller flagged a review for moderation.", reviewId);
                return null;
            });
        }
    }

    /** Retrieves a visible review belonging to the active seller. */
    public Review requireVisibleOwned(String sellerId, String reviewId) {
        Access.requireSeller(store, sellerId);
        return store.reviews().stream().filter(review -> review.getId().equals(reviewId)
                        && review.getSellerId().equals(sellerId) && !review.isHidden()).findFirst()
                .orElseThrow(() -> new MarketplaceException("This review is unavailable."));
    }

    /**
     * @param sellerId seller
     * @return the seller's reviews that are not hidden, newest first (shown to all users)
     */
    public List<Review> visibleReviewsForSeller(String sellerId) {
        return store.reviews().stream().filter(r -> r.getSellerId().equals(sellerId) && !r.isHidden())
                .sorted(Comparator.comparing(Review::getPostedAt).reversed()).collect(Collectors.toList());
    }

    /**
     * @param sellerId seller
     * @return average of visible ratings rounded to one decimal place, or empty if there are none
     */
    public OptionalDouble averageRating(String sellerId) {
        OptionalDouble avg = visibleReviewsForSeller(sellerId).stream().mapToInt(Review::getRating).average();
        return avg.isPresent() ? OptionalDouble.of(Math.round(avg.getAsDouble() * 10) / 10.0) : avg;
    }

    /**
     * @param sellerId seller
     * @return text such as "4.5 ★ (2 reviews)" or "No ratings yet"
     */
    public String ratingSummary(String sellerId) {
        OptionalDouble avg = averageRating(sellerId);
        if (avg.isEmpty()) {
            return "No ratings yet";
        }
        int count = visibleReviewsForSeller(sellerId).size();
        return String.format("%.1f ★ (%d review%s)", avg.getAsDouble(), count, count == 1 ? "" : "s");
    }
}
