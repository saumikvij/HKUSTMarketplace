package hkust.marketplace.service;

import hkust.marketplace.data.DataStore;
import hkust.marketplace.model.*;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.OptionalDouble;

/**
 * Creates and wires every service around one data store and clock. Screens get services from here, so the
 * Seller (Task 2) and Admin (Task 3) portals share the same rules and data.
 */
public class Marketplace {
    private final DataStore store;
    private final Clock clock;
    private final NotificationService notifications;
    private final AuthService auth;
    private final ProfileService profile;
    private final ListingService listings;
    private final SellerListingService sellerListings;
    private final ChatService chat;
    private final DealService deals;
    private final ReviewService reviews;
    private final AdminService admin;

    /**
     * @param store data store
     * @param clock clock (system clock in the app, a controllable clock in tests)
     */
    public Marketplace(DataStore store, Clock clock) {
        this.store = store;
        this.clock = clock;
        this.notifications = new NotificationService(store, clock);
        this.auth = new AuthService(store, clock);
        this.profile = new ProfileService(store);
        this.listings = new ListingService(store, clock, notifications);
        this.sellerListings = new SellerListingService(store, clock, notifications);
        this.chat = new ChatService(store, clock, notifications);
        this.deals = new DealService(store, clock, notifications, chat);
        this.reviews = new ReviewService(store, clock, notifications);
        this.admin = new AdminService(this);
    }

    /** @return administrative operations on this marketplace */
    public AdminService admin() { return admin; }

    /** @return the data store */
    public DataStore store() { return store; }
    /** @return the clock */
    public Clock clock() { return clock; }
    /** @return notification service (1.8) */
    public NotificationService notifications() { return notifications; }
    /** @return registration and login (1.1, 1.2) */
    public AuthService auth() { return auth; }
    /** @return profile editing (1.7) */
    public ProfileService profile() { return profile; }
    /** @return browse and listing detail (1.3, 1.4) */
    public ListingService listings() { return listings; }
    /** @return chat (1.5) */
    public ChatService chat() { return chat; }
    /** @return offers, borrows, claims, My Deals and history (1.5, 1.6, 1.9) */
    public DealService deals() { return deals; }
    /** @return ratings and reviews (1.10) */
    public ReviewService reviews() { return reviews; }
    /** @return seller listing management (2.3 and 2.4) */
    public SellerListingService sellerListings() { return sellerListings; }

    /** Calculates seller counts and chart values from one consistent store snapshot. */
    public SellerStats sellerStats(String sellerId) {
        synchronized (store) {
            var ownListings = sellerListings.forSeller(sellerId);
            var ownDeals = deals.dealsForSeller(sellerId, null);
            var visibleReviews = reviews.visibleReviewsForSeller(sellerId);
            var requestCounts = new EnumMap<ListingType, Long>(ListingType.class);
            var statusCounts = new EnumMap<ListingStatus, Long>(ListingStatus.class);
            for (Deal deal : ownDeals) {
                ListingType type = switch (deal.getType()) {
                    case OFFER -> ListingType.SELL;
                    case BORROW -> ListingType.LEND;
                    case CLAIM -> ListingType.GIVEAWAY;
                };
                requestCounts.merge(type, 1L, Long::sum);
            }
            ownListings.stream().map(Listing::getStatus).filter(status -> status != ListingStatus.REMOVED)
                    .forEach(status -> statusCounts.merge(status, 1L, Long::sum));
            long views = ownListings.stream().mapToLong(Listing::getViewCount).sum();
            long completed = ownDeals.stream().filter(deal -> deal.getStatus().isFinished()).count();
            OptionalDouble rating = visibleReviews.stream().mapToInt(Review::getRating).average();
            return new SellerStats(views, ownDeals.size(), completed, visibleReviews.size(), rating,
                    requestCounts, statusCounts, LocalDateTime.now(clock));
        }
    }
}
