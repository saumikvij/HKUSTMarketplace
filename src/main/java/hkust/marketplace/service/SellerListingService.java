package hkust.marketplace.service;

import hkust.marketplace.data.DataStore;
import hkust.marketplace.model.*;
import hkust.marketplace.util.Ids;
import hkust.marketplace.util.MarketplaceException;
import hkust.marketplace.util.ValidationException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Seller-owned listing submission and management (Tasks 2.3 and 2.4). */
public final class SellerListingService {
    private final DataStore store;
    private final Clock clock;
    private final NotificationService notifications;

    /** Form values. The photo order is also the buyer gallery order. */
    public record Details(ListingType type, String title, Category category, Condition condition,
                          String description, String location, String price, String duration, List<String> photos) {
        public Details {
            photos = List.copyOf(photos);
        }

        /** Copies the current persisted listing values for display or editing. */
        public static Details of(Listing listing) {
            return new Details(listing.getType(), listing.getTitle(), listing.getCategory(), listing.getCondition(),
                    listing.getDescription(), listing.getMeetupLocation(),
                    listing.getPrice() == null ? "" : listing.getPrice().toPlainString(),
                    listing.getType() == ListingType.LEND ? Long.toString(listing.getLendingDurationSeconds()) : "",
                    listing.getPhotos());
        }
    }

    /** Listing data that must still match when an edit or removal is confirmed. */
    public record Revision(Details details, ListingStatus status, LocalDateTime reviewedAt, String reason) {
        /** Captures the revision shown to the seller. */
        public static Revision of(Listing listing) {
            return new Revision(Details.of(listing), listing.getStatus(), listing.getReviewedAt(),
                    listing.getRejectionReason());
        }
    }

    SellerListingService(DataStore store, Clock clock, NotificationService notifications) {
        this.store = store;
        this.clock = clock;
        this.notifications = notifications;
    }

    /** Returns the active seller's own listings, including removed records. */
    public List<Listing> forSeller(String sellerId) {
        Access.requireSeller(store, sellerId);
        return store.listings().stream().filter(listing -> listing.getSellerId().equals(sellerId))
                .sorted(Comparator.comparing(Listing::getPostedAt).reversed()).toList();
    }

    /** Retrieves one listing after checking seller ownership. */
    public Listing requireOwned(String sellerId, String listingId) {
        Access.requireSeller(store, sellerId);
        return store.findListing(listingId).filter(listing -> listing.getSellerId().equals(sellerId))
                .orElseThrow(() -> new MarketplaceException("This listing is unavailable."));
    }

    /** Allocates a stable key to reuse if the same creation form is submitted again. */
    public String newSubmissionId() {
        return Ids.next("L");
    }

    /** Submits a listing once per form key and alerts the admin. */
    public Listing create(String sellerId, String submissionId, Details details) {
        Objects.requireNonNull(submissionId);
        synchronized (store) {
            Access.requireSeller(store, sellerId);
            Listing existing = store.findListing(submissionId).orElse(null);
            if (existing != null) {
                return requireOwned(sellerId, submissionId);
            }
            Terms terms = validate(details);
            Listing listing = new Listing(submissionId, sellerId, details.type(), details.title().trim(),
                    details.category(), details.condition(), details.description().trim(), details.location(),
                    LocalDateTime.now(clock));
            applyDetails(listing, details, terms);
            return store.transact(() -> {
                store.putListing(listing);
                notifyAdmin(listing, "Listing submitted");
                return listing;
            });
        }
    }

    /** Resubmits a valid, eligible edit for admin approval. */
    public Listing edit(String sellerId, String listingId, Revision expected, Details details) {
        synchronized (store) {
            Listing listing = requireUnchanged(sellerId, listingId, expected);
            Terms terms = validate(details);
            return store.transact(() -> {
                applyDetails(listing, details, terms);
                listing.setStatus(ListingStatus.PENDING_REVIEW);
                listing.setReviewedAt(null);
                listing.setRejectionReason(null);
                notifyAdmin(listing, "Listing resubmitted");
                return listing;
            });
        }
    }

    /** Soft-removes an eligible listing and preserves its chats and deal history. */
    public void remove(String sellerId, String listingId, Revision expected) {
        synchronized (store) {
            Listing listing = requireUnchanged(sellerId, listingId, expected);
            store.transact(() -> {
                listing.setStatus(ListingStatus.REMOVED);
                listing.setRemovalReason("Removed by seller");
                return null;
            });
        }
    }

    /** Checks whether the seller can currently edit or remove their listing. */
    public void checkEditable(String sellerId, String listingId) {
        synchronized (store) {
            checkEditable(requireOwned(sellerId, listingId));
        }
    }

    private Listing requireUnchanged(String sellerId, String listingId, Revision expected) {
        Listing listing = requireOwned(sellerId, listingId);
        if (!Revision.of(listing).equals(expected)) {
            throw new MarketplaceException("The listing changed. Refresh its details and try again.");
        }
        checkEditable(listing);
        return listing;
    }

    private void checkEditable(Listing listing) {
        switch (listing.getStatus()) {
            case PENDING_REVIEW, LIVE, REJECTED -> { }
            default -> throw new MarketplaceException("A listing with status " + listing.getStatus()
                    + " cannot be edited or removed.");
        }
        if (store.deals().stream().anyMatch(deal -> deal.getListingId().equals(listing.getId())
                && deal.getStatus().isOpenRequest())) {
            throw new MarketplaceException("Resolve the open offers or requests before changing this listing.");
        }
    }

    private record Terms(BigDecimal price, long seconds) { }

    private static Terms validate(Details details) {
        List<String> errors = new ArrayList<>();
        if (details.type() == null) errors.add("Listing type is required");
        if (details.title() == null || details.title().isBlank()) errors.add("Item name is required");
        if (details.category() == null) errors.add("Category is required");
        if (details.condition() == null) errors.add("Condition is required");
        if (details.description() == null || details.description().isBlank()) errors.add("Description is required");
        if (!MeetupLocations.contains(details.location())) errors.add("Select a valid HKUST meet-up location");
        if (details.photos().size() > Listing.MAX_PHOTOS) errors.add("Select at most 6 photos");
        if (details.photos().stream().anyMatch(String::isBlank)) errors.add("One or more photos are unavailable");

        BigDecimal price = null;
        long duration = 0;
        if (details.type() == ListingType.SELL) {
            try {
                price = DealService.parseOfferPrice(details.price());
            } catch (ValidationException e) {
                errors.add(e.getMessage());
            }
        }
        if (details.type() == ListingType.LEND) {
            try {
                duration = Long.parseLong(details.duration().trim());
            } catch (NumberFormatException | NullPointerException e) {
                duration = 0;
            }
            if (duration < DealService.MIN_BORROW.getSeconds()) {
                errors.add("Enter a whole-number lending duration of at least 5 seconds");
            }
        }
        if (!errors.isEmpty()) throw new ValidationException(errors);
        return new Terms(price, duration);
    }

    private static void applyDetails(Listing listing, Details details, Terms terms) {
        listing.setType(details.type());
        listing.setTitle(details.title().trim());
        listing.setCategory(details.category());
        listing.setCondition(details.condition());
        listing.setDescription(details.description().trim());
        listing.setMeetupLocation(details.location());
        listing.setPrice(terms.price());
        listing.setLendingDurationSeconds(terms.seconds());
        listing.getPhotos().clear();
        listing.getPhotos().addAll(details.photos());
    }

    private void notifyAdmin(Listing listing, String title) {
        notifications.notifyAdmins(NotificationCategory.LISTING_UPDATES, title,
                listing.getTitle() + " is waiting for review.", listing.getId());
    }
}
