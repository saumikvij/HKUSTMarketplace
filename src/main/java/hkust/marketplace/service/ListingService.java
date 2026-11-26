package hkust.marketplace.service;

import hkust.marketplace.data.DataStore;
import hkust.marketplace.model.Listing;
import hkust.marketplace.model.ListingStatus;
import hkust.marketplace.model.ListingType;
import hkust.marketplace.model.NotificationCategory;
import hkust.marketplace.model.Report;
import hkust.marketplace.model.ReportReason;
import hkust.marketplace.model.ReportStatus;
import hkust.marketplace.model.Seller;
import hkust.marketplace.model.User;
import hkust.marketplace.util.Ids;
import hkust.marketplace.util.MarketplaceException;
import hkust.marketplace.util.ValidationException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Browse Marketplace and Listing Detail logic (Tasks 1.3 and 1.4). */
public class ListingService {
    /** Format used for dates shown to users. */
    public static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm:ss");

    private final DataStore store;
    private final Clock clock;
    private final NotificationService notifications;

    /**
     * @param store data store
     * @param clock clock
     * @param notifications notification service (for reports)
     */
    public ListingService(DataStore store, Clock clock, NotificationService notifications) {
        this.store = store;
        this.clock = clock;
        this.notifications = notifications;
    }

    /**
     * Returns the listings a buyer can see, searched, filtered and sorted (Task 1.3).
     * Only approved listings that are Live, Reserved or Lent and whose seller is active are shown.
     * A price range keeps only Sell listings inside it. When sorting by price, listings without a price
     * (Lend and Giveaway) come after all Sell listings, newest first.
     * @param query search settings
     * @return matching listings
     * @throws ValidationException if the price range is invalid
     */
    public List<Listing> browse(BrowseQuery query) {
        BigDecimal min = parsePriceBound(query.getMinPrice(), "Minimum price");
        BigDecimal max = parsePriceBound(query.getMaxPrice(), "Maximum price");
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new ValidationException("Minimum price cannot be greater than maximum price");
        }
        String keyword = query.getKeyword() == null ? "" : query.getKeyword().trim().toLowerCase(Locale.ROOT);
        Stream<Listing> stream = store.listings().stream()
                .filter(this::isVisibleToBuyers)
                .filter(l -> keyword.isEmpty() || contains(l.getTitle(), keyword) || contains(l.getDescription(), keyword))
                .filter(l -> query.getCategory() == null || l.getCategory() == query.getCategory())
                .filter(l -> query.getType() == null || l.getType() == query.getType())
                .filter(l -> query.getCondition() == null || l.getCondition() == query.getCondition());
        if (min != null || max != null) {
            stream = stream.filter(l -> l.getType() == ListingType.SELL && l.getPrice() != null
                    && (min == null || l.getPrice().compareTo(min) >= 0)
                    && (max == null || l.getPrice().compareTo(max) <= 0));
        }
        Comparator<Listing> newest = Comparator.comparing(Listing::getPostedAt).reversed();
        Comparator<Listing> order;
        switch (query.getSort()) {
            case PRICE_LOW_HIGH:
                order = Comparator.comparing((Listing l) -> l.getPrice() == null)
                        .thenComparing(l -> l.getPrice() == null ? BigDecimal.ZERO : l.getPrice())
                        .thenComparing(newest);
                break;
            case PRICE_HIGH_LOW:
                order = Comparator.comparing((Listing l) -> l.getPrice() == null)
                        .thenComparing(l -> l.getPrice() == null ? BigDecimal.ZERO : l.getPrice(),
                                Comparator.reverseOrder())
                        .thenComparing(newest);
                break;
            default:
                order = newest;
        }
        return stream.sorted(order).collect(Collectors.toList());
    }

    /**
     * @param listing a listing
     * @return true if its status is browsable and its seller's account is active
     */
    public boolean isVisibleToBuyers(Listing listing) {
        return listing.getStatus().isBrowsable()
                && store.findUser(listing.getSellerId()).map(User::isActive).orElse(false);
    }

    /**
     * Opens a listing's detail screen and adds one to its view count (Task 1.4).
     * @param buyerId buyer opening it
     * @param listingId listing
     * @return the listing
     * @throws MarketplaceException if it was deleted, removed or is no longer approved
     */
    public Listing openListing(String buyerId, String listingId) {
        Access.requireBuyer(store, buyerId);
        Listing listing = store.findListing(listingId).orElse(null);
        if (listing == null || !isVisibleToBuyers(listing)) {
            throw new MarketplaceException("This listing is no longer available.");
        }
        listing.incrementViewCount();
        store.commit();
        return listing;
    }

    /**
     * @param listing a listing
     * @return true if a new offer, borrow request or claim can be made (status Live)
     */
    public boolean isOpenForDeals(Listing listing) {
        return listing.getStatus() == ListingStatus.LIVE;
    }

    /**
     * @param listing a listing
     * @return availability text such as "Available", "Reserved" or "Lent until 30 Oct 2026, 18:00:00"
     */
    public String availabilityText(Listing listing) {
        switch (listing.getStatus()) {
            case LIVE: return "Available";
            case RESERVED: return "Reserved";
            case LENT:
                return listing.getLentUntil() == null ? "Lent"
                        : "Lent until " + listing.getLentUntil().format(DISPLAY_TIME);
            case SOLD: return "Sold";
            case GIVEN: return "Given away";
            default: return "Not available";
        }
    }

    /**
     * @param listing a listing
     * @return its seller
     */
    public Seller sellerOf(Listing listing) {
        return (Seller) store.findUser(listing.getSellerId())
                .orElseThrow(() -> new MarketplaceException("Seller not found."));
    }

    /**
     * Reports a listing to the admin (Task 1.4).
     * @param buyerId reporting buyer
     * @param listingId listing
     * @param reason reason (required)
     * @param comment optional comment; required when the reason is OTHER
     * @return the stored report
     * @throws ValidationException if the reason (or the comment for OTHER) is missing
     * @throws MarketplaceException if the buyer already has an open report on this listing
     */
    public Report reportListing(String buyerId, String listingId, ReportReason reason, String comment) {
        Access.requireBuyer(store, buyerId);
        Listing listing = store.findListing(listingId)
                .orElseThrow(() -> new MarketplaceException("This listing is no longer available."));
        if (reason == null) {
            throw new ValidationException("Please choose a reason");
        }
        if (reason == ReportReason.OTHER && (comment == null || comment.isBlank())) {
            throw new ValidationException("Please describe the problem when choosing \"Other\"");
        }
        boolean alreadyOpen = store.reports().stream().anyMatch(r -> r.getBuyerId().equals(buyerId)
                && r.getListingId().equals(listingId) && r.getStatus() == ReportStatus.OPEN);
        if (alreadyOpen) {
            throw new MarketplaceException("You have already reported this listing. It is waiting for admin review.");
        }
        Report report = new Report(Ids.next("R"), buyerId, listingId, reason,
                comment == null || comment.isBlank() ? null : comment.trim(), LocalDateTime.now(clock));
        store.putReport(report);
        notifications.notifyAdmins(NotificationCategory.MODERATION, "Reported listing",
                "\"" + listing.getTitle() + "\" was reported: " + reason.label(), listingId);
        store.commit();
        return report;
    }

    private static boolean contains(String text, String keyword) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private static BigDecimal parsePriceBound(String text, String label) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            BigDecimal value = new BigDecimal(text.trim());
            if (value.signum() < 0) {
                throw new ValidationException(label + " cannot be negative");
            }
            return value;
        } catch (NumberFormatException e) {
            throw new ValidationException(label + " must be a number");
        }
    }
}
