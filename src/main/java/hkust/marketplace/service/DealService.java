package hkust.marketplace.service;

import hkust.marketplace.data.DataStore;
import hkust.marketplace.model.Buyer;
import hkust.marketplace.model.ChatThread;
import hkust.marketplace.model.Deal;
import hkust.marketplace.model.DealStatus;
import hkust.marketplace.model.DealType;
import hkust.marketplace.model.Listing;
import hkust.marketplace.model.ListingStatus;
import hkust.marketplace.model.ListingType;
import hkust.marketplace.model.NotificationCategory;
import hkust.marketplace.model.Role;
import hkust.marketplace.model.User;
import hkust.marketplace.util.Ids;
import hkust.marketplace.util.MarketplaceException;
import hkust.marketplace.util.ValidationException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Offers, borrow requests and claims (Task 1.5), My Deals actions and auto-return (Task 1.6) and Deal History
 * (Task 1.9). Every state change first re-checks the current status while holding the store lock, so two
 * actions on the same listing at the same time cannot both succeed (e.g. only the first giveaway claim wins).
 * <p>
 * The seller-side methods ({@link #acceptDeal}, {@link #declineDeal}, {@link #counterOffer},
 * {@link #counterBorrow}) hold the shared deal rules so the Seller Portal (Task 2) uses the same logic.
 */
public class DealService {
    /** Shortest borrowing period allowed (5 seconds, for testing). */
    public static final Duration MIN_BORROW = Duration.ofSeconds(5);
    /** How far in the past a borrow start may be (covers the time spent filling in the form). */
    public static final Duration START_TOLERANCE = Duration.ofMinutes(1);

    private final DataStore store;
    private final Clock clock;
    private final NotificationService notifications;
    private final ChatService chat;

    /**
     * @param store data store
     * @param clock clock (a fixed or test clock makes due dates testable)
     * @param notifications notification service
     * @param chat chat service (offers appear as cards in the listing's chat)
     */
    public DealService(DataStore store, Clock clock, NotificationService notifications, ChatService chat) {
        this.store = store;
        this.clock = clock;
        this.notifications = notifications;
        this.chat = chat;
    }

    // =====================================================================
    // Task 1.5: submit an offer, borrow request or claim
    // =====================================================================

    /**
     * Makes a price offer on a Sell listing.
     * @param buyerId buyer
     * @param listingId a Live Sell listing
     * @param priceText offer in HK$, greater than 0 with at most 2 decimal places
     * @param message optional message to the seller
     * @param replaceExisting true to replace the buyer's existing Pending/Countered request on this listing
     * @return the new Pending deal
     * @throws ValidationException if the price is invalid
     * @throws MarketplaceException if the listing is no longer available or a request already exists
     */
    public Deal makeOffer(String buyerId, String listingId, String priceText, String message,
                          boolean replaceExisting) {
        Buyer buyer = Access.requireBuyer(store, buyerId);
        BigDecimal price = parseOfferPrice(priceText);
        synchronized (store) {
            Listing listing = requireOpenListing(listingId, ListingType.SELL);
            handleExisting(buyerId, listingId, replaceExisting);
            Deal deal = newDeal(buyerId, listing, DealType.OFFER, message);
            deal.setOfferedPrice(price);
            store.putDeal(deal);
            postCard(buyerId, listing, "Offer: HK$" + price.toPlainString() + optional(message), deal.getId());
            notifications.notify(listing.getSellerId(), NotificationCategory.DEALS, "New offer",
                    buyer.getFullName() + " offered HK$" + price.toPlainString() + " for \"" + listing.getTitle()
                            + "\".", deal.getId());
            store.commit();
            return deal;
        }
    }

    /**
     * Sends a borrow request with a proposed period on a Lend listing.
     * @param buyerId buyer
     * @param listingId a Live Lend listing
     * @param start proposed start (not in the past)
     * @param end proposed end, after the start
     * @param message optional message to the seller
     * @param replaceExisting true to replace the buyer's existing Pending/Countered request on this listing
     * @return the new Pending deal
     * @throws ValidationException if the period breaks a rule
     * @throws MarketplaceException if the listing is no longer available or a request already exists
     */
    public Deal requestBorrow(String buyerId, String listingId, LocalDateTime start, LocalDateTime end,
                              String message, boolean replaceExisting) {
        Buyer buyer = Access.requireBuyer(store, buyerId);
        synchronized (store) {
            Listing listing = requireOpenListing(listingId, ListingType.LEND);
            validatePeriod(start, end, listing);
            handleExisting(buyerId, listingId, replaceExisting);
            Deal deal = newDeal(buyerId, listing, DealType.BORROW, message);
            deal.setBorrowPeriod(start, end);
            store.putDeal(deal);
            String period = start.format(ListingService.DISPLAY_TIME) + " to " + end.format(ListingService.DISPLAY_TIME);
            postCard(buyerId, listing, "Borrow request: " + period + optional(message), deal.getId());
            notifications.notify(listing.getSellerId(), NotificationCategory.DEALS, "New borrow request",
                    buyer.getFullName() + " wants to borrow \"" + listing.getTitle() + "\" from " + period + ".",
                    deal.getId());
            store.commit();
            return deal;
        }
    }

    /**
     * Claims a Giveaway listing with one action. The check, the claim and the reservation happen in one atomic
     * step, so when two buyers claim at the same time only the first succeeds.
     * @param buyerId buyer
     * @param listingId a Live Giveaway listing
     * @return the new Pending claim (the listing becomes Reserved)
     * @throws MarketplaceException if the listing was already claimed or is otherwise unavailable
     */
    public Deal claim(String buyerId, String listingId) {
        Buyer buyer = Access.requireBuyer(store, buyerId);
        synchronized (store) {
            Listing listing = requireOpenListing(listingId, ListingType.GIVEAWAY);
            Deal deal = newDeal(buyerId, listing, DealType.CLAIM, null);
            store.putDeal(deal);
            listing.setStatus(ListingStatus.RESERVED);
            postCard(buyerId, listing, "Claimed this giveaway", deal.getId());
            notifications.notify(listing.getSellerId(), NotificationCategory.DEALS, "New claim",
                    buyer.getFullName() + " claimed \"" + listing.getTitle() + "\".", deal.getId());
            store.commit();
            return deal;
        }
    }

    /**
     * @param buyerId buyer
     * @param listingId listing
     * @return the buyer's Pending or Countered offer/request on the listing, if any
     */
    public Optional<Deal> openRequest(String buyerId, String listingId) {
        return store.deals().stream().filter(d -> d.getBuyerId().equals(buyerId)
                && d.getListingId().equals(listingId) && d.getStatus().isOpenRequest()).findFirst();
    }

    // =====================================================================
    // Seller-side rules (used by the Seller Portal, Task 2.5)
    // =====================================================================

    /**
     * Seller accepts a Pending deal. Sell: deal Accepted, listing Reserved. Lend: deal Borrowed, listing Lent for
     * the period. Giveaway: claim Accepted, listing stays Reserved. All other open requests on the listing are
     * declined and their buyers notified.
     * @param sellerId seller who owns the listing
     * @param dealId Pending deal
     * @return the accepted deal
     * @throws MarketplaceException if the deal or listing changed meanwhile
     */
    public Deal acceptDeal(String sellerId, String dealId) {
        synchronized (store) {
            Access.requireSeller(store, sellerId);
            Deal deal = requireDeal(dealId);
            Listing listing = requireSellerListing(sellerId, deal);
            if (deal.getStatus() != DealStatus.PENDING) {
                throw new MarketplaceException("This request is no longer pending (" + deal.getStatus().label() + ").");
            }
            boolean available = deal.getType() == DealType.CLAIM
                    ? listing.getStatus() == ListingStatus.RESERVED
                    : listing.getStatus() == ListingStatus.LIVE;
            if (!available) {
                throw new MarketplaceException("This listing is no longer available.");
            }
            return store.transact(() -> {
                allocate(deal, listing);
                notifications.notify(deal.getBuyerId(), NotificationCategory.DEALS, "Offer accepted",
                        "Your " + deal.getType().label().toLowerCase(Locale.ROOT) + " for \"" + listing.getTitle()
                                + "\" was accepted.", deal.getId());
                return deal;
            });
        }
    }

    /**
     * Seller declines a Pending deal. A declined giveaway claim makes the listing Live again.
     * @param sellerId seller who owns the listing
     * @param dealId Pending deal
     * @return the declined deal
     * @throws MarketplaceException if the deal is no longer pending
     */
    public Deal declineDeal(String sellerId, String dealId) {
        synchronized (store) {
            Access.requireSeller(store, sellerId);
            Deal deal = requireDeal(dealId);
            Listing listing = requireSellerListing(sellerId, deal);
            if (deal.getStatus() != DealStatus.PENDING) {
                throw new MarketplaceException("This request is no longer pending (" + deal.getStatus().label() + ").");
            }
            return store.transact(() -> {
                deal.setStatus(DealStatus.DECLINED, now());
                releaseClaimHold(deal, listing);
                notifications.notify(deal.getBuyerId(), NotificationCategory.DEALS, "Offer declined",
                        "Your " + deal.getType().label().toLowerCase(Locale.ROOT) + " for \"" + listing.getTitle()
                                + "\" was declined.", deal.getId());
                return deal;
            });
        }
    }

    /**
     * Seller counters a Pending offer with a different price. The listing stays Live.
     * @param sellerId seller who owns the listing
     * @param dealId Pending offer
     * @param priceText counter price in HK$
     * @return the countered deal
     */
    public Deal counterOffer(String sellerId, String dealId, String priceText) {
        BigDecimal price = parseOfferPrice(priceText);
        synchronized (store) {
            Access.requireSeller(store, sellerId);
            Deal deal = requireDeal(dealId);
            Listing listing = requireCounterable(sellerId, deal, DealType.OFFER);
            return store.transact(() -> {
                deal.setCounterPrice(price);
                deal.setStatus(DealStatus.COUNTERED, now());
                finishCounter(sellerId, deal, listing, "Counter-offer: HK$" + price.toPlainString());
                return deal;
            });
        }
    }

    /**
     * Seller counters a Pending borrow request with a different period. The listing stays Live.
     * @param sellerId seller who owns the listing
     * @param dealId Pending borrow request
     * @param start counter start
     * @param end counter end
     * @return the countered deal
     */
    public Deal counterBorrow(String sellerId, String dealId, LocalDateTime start, LocalDateTime end) {
        synchronized (store) {
            Access.requireSeller(store, sellerId);
            Deal deal = requireDeal(dealId);
            Listing listing = requireCounterable(sellerId, deal, DealType.BORROW);
            if (start != null && start.isBefore(now())) {
                throw new ValidationException("The counter start time cannot be in the past");
            }
            validatePeriod(start, end, listing);
            return store.transact(() -> {
                deal.setCounterPeriod(start, end);
                deal.setStatus(DealStatus.COUNTERED, now());
                finishCounter(sellerId, deal, listing, "Counter-offer: borrow " + start.format(ListingService.DISPLAY_TIME)
                        + " to " + end.format(ListingService.DISPLAY_TIME));
                return deal;
            });
        }
    }

    /** Returns a seller's requests, pending first, optionally scoped to one owned listing. */
    public List<Deal> dealsForSeller(String sellerId, String listingId) {
        synchronized (store) {
            Access.requireSeller(store, sellerId);
            return store.deals().stream()
                    .filter(deal -> listingId == null || listingId.equals(deal.getListingId()))
                    .filter(deal -> store.findListing(deal.getListingId())
                            .filter(listing -> sellerId.equals(listing.getSellerId())).isPresent())
                    .sorted(Comparator.comparing((Deal deal) -> deal.getStatus() != DealStatus.PENDING)
                            .thenComparing(Deal::getSubmittedAt, Comparator.reverseOrder()))
                    .toList();
        }
    }

    // =====================================================================
    // Task 1.6: My Deals actions
    // =====================================================================

    /**
     * Buyer accepts or declines the seller's counter-offer.
     * @param buyerId buyer who owns the deal
     * @param dealId a Countered deal
     * @param accept true to accept (deal Accepted/Borrowed at the counter terms), false to decline
     * @return the updated deal
     * @throws MarketplaceException if the counter is no longer valid or the listing is no longer Live
     */
    public Deal respondToCounter(String buyerId, String dealId, boolean accept) {
        Access.requireBuyer(store, buyerId);
        synchronized (store) {
            Deal deal = requireBuyerDeal(buyerId, dealId);
            Listing listing = requireListing(deal.getListingId());
            if (deal.getStatus() != DealStatus.COUNTERED || listing.getStatus() != ListingStatus.LIVE) {
                throw new MarketplaceException("This counter-offer is no longer valid.");
            }
            String verb;
            if (accept) {
                if (deal.getType() == DealType.BORROW) {
                    validatePeriod(deal.getCounterStart(), deal.getCounterEnd(), listing);
                    deal.setBorrowPeriod(deal.getCounterStart(), deal.getCounterEnd());
                }
                allocate(deal, listing);
                verb = "accepted";
            } else {
                deal.setStatus(DealStatus.DECLINED, now());
                verb = "declined";
            }
            notifications.notify(listing.getSellerId(), NotificationCategory.DEALS, "Counter-offer " + verb,
                    buyerName(buyerId) + " " + verb + " your counter-offer for \"" + listing.getTitle() + "\".",
                    deal.getId());
            store.commit();
            return deal;
        }
    }

    /**
     * Buyer withdraws a Pending or Countered request. Withdrawing a giveaway claim makes the listing Live again.
     * @param buyerId buyer who owns the deal
     * @param dealId deal
     * @return the withdrawn deal
     * @throws MarketplaceException if the seller already accepted or declined it
     */
    public Deal withdraw(String buyerId, String dealId) {
        Access.requireBuyer(store, buyerId);
        synchronized (store) {
            Deal deal = requireBuyerDeal(buyerId, dealId);
            if (!deal.getStatus().isOpenRequest()) {
                throw new MarketplaceException("This request can no longer be withdrawn (it is now "
                        + deal.getStatus().label() + ").");
            }
            Listing listing = requireListing(deal.getListingId());
            deal.setStatus(DealStatus.WITHDRAWN, now());
            releaseClaimHold(deal, listing);
            notifications.notify(listing.getSellerId(), NotificationCategory.DEALS, "Request withdrawn",
                    buyerName(buyerId) + " withdrew their " + deal.getType().label().toLowerCase(Locale.ROOT)
                            + " for \"" + listing.getTitle() + "\".", deal.getId());
            store.commit();
            return deal;
        }
    }

    /**
     * Buyer returns a borrowed item before the due time. The listing becomes Live again.
     * @param buyerId buyer who owns the deal
     * @param dealId a Borrowed deal
     * @return the returned deal
     * @throws MarketplaceException if it is not borrowed, or was already returned automatically
     */
    public Deal returnItem(String buyerId, String dealId) {
        Access.requireBuyer(store, buyerId);
        synchronized (store) {
            Deal deal = requireBuyerDeal(buyerId, dealId);
            if (deal.getStatus() == DealStatus.BORROWED && !now().isBefore(deal.getBorrowEnd())) {
                processDueDates();
            }
            if (deal.getStatus() == DealStatus.AUTO_RETURNED) {
                throw new MarketplaceException("This item was already returned automatically at the due time.");
            }
            if (deal.getStatus() != DealStatus.BORROWED) {
                throw new MarketplaceException("Only borrowed items can be returned.");
            }
            Listing listing = requireListing(deal.getListingId());
            deal.setStatus(DealStatus.RETURNED, now());
            deal.setCompletedAt(now());
            releaseLoan(listing);
            notifications.notify(listing.getSellerId(), NotificationCategory.BORROWING, "Item returned",
                    buyerName(buyerId) + " returned \"" + listing.getTitle() + "\".", deal.getId());
            store.commit();
            return deal;
        }
    }

    /**
     * Buyer marks an accepted purchase or giveaway as received. The listing becomes Sold or Given and any other
     * open requests are declined. The UI then asks the buyer to rate the seller.
     * @param buyerId buyer who owns the deal
     * @param dealId an Accepted offer or claim
     * @return the completed deal
     * @throws MarketplaceException if the deal is not Accepted
     */
    public Deal markComplete(String buyerId, String dealId) {
        Access.requireBuyer(store, buyerId);
        synchronized (store) {
            Deal deal = requireBuyerDeal(buyerId, dealId);
            if (deal.getStatus() != DealStatus.ACCEPTED || deal.getType() == DealType.BORROW) {
                throw new MarketplaceException("Only accepted purchases and giveaways can be marked complete.");
            }
            Listing listing = requireListing(deal.getListingId());
            deal.setStatus(DealStatus.COMPLETED, now());
            deal.setCompletedAt(now());
            if (listing.getStatus() != ListingStatus.REMOVED) {
                listing.setStatus(deal.getType() == DealType.OFFER ? ListingStatus.SOLD : ListingStatus.GIVEN);
            }
            declineOthers(deal, listing);
            notifications.notify(listing.getSellerId(), NotificationCategory.DEALS, "Deal completed",
                    buyerName(buyerId) + " marked \"" + listing.getTitle() + "\" as received.", deal.getId());
            store.commit();
            return deal;
        }
    }

    /**
     * Auto-return and due-date reminders, run every second by a timer (Tasks 1.6 and 1.8).
     * A Borrowed deal past its due time becomes Auto-returned and the listing Live again (unless removed).
     * One reminder is sent 24 hours before the due time, or halfway through periods shorter than 48 hours.
     * @return number of deals changed (reminders or returns)
     */
    public int processDueDates() {
        int changed = 0;
        synchronized (store) {
            LocalDateTime now = now();
            for (Deal deal : store.deals()) {
                if (deal.getStatus() != DealStatus.BORROWED || deal.getBorrowEnd() == null) {
                    continue;
                }
                Listing listing = store.findListing(deal.getListingId()).orElse(null);
                String title = listing == null ? "the item" : "\"" + listing.getTitle() + "\"";
                if (!now.isBefore(deal.getBorrowEnd())) {
                    deal.setStatus(DealStatus.AUTO_RETURNED, now);
                    deal.setCompletedAt(deal.getBorrowEnd());
                    if (listing != null) {
                        releaseLoan(listing);
                        notifications.notify(listing.getSellerId(), NotificationCategory.BORROWING, "Item auto-returned",
                                title + " was returned automatically at the due time.", deal.getId());
                    }
                    notifications.notify(deal.getBuyerId(), NotificationCategory.BORROWING, "Item auto-returned",
                            "The borrowing period for " + title + " ended, so it was returned automatically.",
                            deal.getId());
                    changed++;
                } else if (!deal.isReminderSent() && reminderDue(deal, now)) {
                    deal.setReminderSent(true);
                    notifications.notify(deal.getBuyerId(), NotificationCategory.BORROWING, "Due soon",
                            title + " is due back at " + deal.getBorrowEnd().format(ListingService.DISPLAY_TIME) + ".",
                            deal.getId());
                    changed++;
                }
            }
            if (changed > 0) {
                store.commit();
            }
        }
        return changed;
    }

    // =====================================================================
    // Queries for My Deals (1.6) and Deal History (1.9)
    // =====================================================================

    /**
     * @param buyerId buyer
     * @return all of the buyer's deals, most recently updated first
     */
    public List<Deal> dealsForBuyer(String buyerId) {
        return store.deals().stream().filter(d -> d.getBuyerId().equals(buyerId))
                .sorted(Comparator.comparing(Deal::getUpdatedAt).reversed()).collect(Collectors.toList());
    }

    /**
     * @param buyerId buyer
     * @return the buyer's active deals (Pending, Countered, Accepted, Borrowed)
     */
    public List<Deal> activeDeals(String buyerId) {
        return dealsForBuyer(buyerId).stream().filter(d -> d.getStatus().isActive()).collect(Collectors.toList());
    }

    /**
     * @param buyerId buyer
     * @return the buyer's deals that are no longer active (finished, declined, withdrawn, cancelled)
     */
    public List<Deal> closedDeals(String buyerId) {
        return dealsForBuyer(buyerId).stream().filter(d -> !d.getStatus().isActive()).collect(Collectors.toList());
    }

    /**
     * Deal History: the buyer's finished deals, newest first, searched and filtered (Task 1.9).
     * @param buyerId buyer
     * @param query filters (keyword on item title or seller name, date range, category, deal type)
     * @return matching finished deals
     * @throws ValidationException if the date range is invalid
     */
    public List<Deal> history(String buyerId, HistoryQuery query) {
        LocalDate today = LocalDate.now(clock);
        if (query.getFrom() != null && query.getTo() != null && query.getFrom().isAfter(query.getTo())) {
            throw new ValidationException("The start date must not be after the end date");
        }
        if ((query.getFrom() != null && query.getFrom().isAfter(today))
                || (query.getTo() != null && query.getTo().isAfter(today))) {
            throw new ValidationException("Dates cannot be in the future");
        }
        String key = query.getKeyword() == null ? "" : query.getKeyword().trim().toLowerCase(Locale.ROOT);
        List<Deal> result = new ArrayList<>();
        for (Deal deal : store.deals()) {
            if (!deal.getBuyerId().equals(buyerId) || !deal.getStatus().isFinished()) {
                continue;
            }
            Listing listing = store.findListing(deal.getListingId()).orElse(null);
            if (listing == null) {
                continue;
            }
            LocalDate date = dealDate(deal).toLocalDate();
            String seller = store.findUser(listing.getSellerId()).map(User::getFullName).orElse("");
            boolean keyOk = key.isEmpty() || listing.getTitle().toLowerCase(Locale.ROOT).contains(key)
                    || seller.toLowerCase(Locale.ROOT).contains(key);
            boolean ok = keyOk
                    && (query.getFrom() == null || !date.isBefore(query.getFrom()))
                    && (query.getTo() == null || !date.isAfter(query.getTo()))
                    && (query.getCategory() == null || listing.getCategory() == query.getCategory())
                    && (query.getDealType() == null || deal.getType() == query.getDealType());
            if (ok) {
                result.add(deal);
            }
        }
        result.sort(Comparator.comparing(DealService::dealDate).reversed());
        return result;
    }

    /**
     * @param deal a deal
     * @return the date shown in Deal History: when it finished, or when it was submitted if not finished
     */
    public static LocalDateTime dealDate(Deal deal) {
        return deal.getCompletedAt() != null ? deal.getCompletedAt() : deal.getSubmittedAt();
    }

    /**
     * @param deal a borrowed deal
     * @return time left before the due time (zero if overdue), or null if it has no due time
     */
    public Duration remaining(Deal deal) {
        if (deal.getBorrowEnd() == null) {
            return null;
        }
        Duration left = Duration.between(now(), deal.getBorrowEnd());
        return left.isNegative() ? Duration.ZERO : left;
    }

    // =====================================================================
    // helpers
    // =====================================================================

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    static BigDecimal parseOfferPrice(String text) {
        if (text == null || text.isBlank()) {
            throw new ValidationException("Please enter a price");
        }
        BigDecimal price;
        try {
            price = new BigDecimal(text.trim());
        } catch (NumberFormatException e) {
            throw new ValidationException("Price must be a number");
        }
        if (price.signum() <= 0) {
            throw new ValidationException("Price must be greater than 0");
        }
        if (price.stripTrailingZeros().scale() > 2) {
            throw new ValidationException("Price can have at most 2 decimal places");
        }
        return price.setScale(2, java.math.RoundingMode.UNNECESSARY);
    }

    private void validatePeriod(LocalDateTime start, LocalDateTime end, Listing listing) {
        if (start == null || end == null) {
            throw new ValidationException("Please enter both a start and an end time");
        }
        List<String> errors = new ArrayList<>();
        if (start.isBefore(now().minus(START_TOLERANCE))) {
            errors.add("The start time cannot be in the past");
        }
        if (!end.isAfter(start)) {
            errors.add("The end time must be after the start time");
        } else {
            Duration length = Duration.between(start, end);
            if (length.compareTo(MIN_BORROW) < 0) {
                errors.add("The borrowing period must be at least " + MIN_BORROW.getSeconds() + " seconds");
            }
            if (listing.getLendingDurationSeconds() > 0
                    && length.compareTo(Duration.ofSeconds(listing.getLendingDurationSeconds())) > 0) {
                errors.add("The borrowing period is longer than the seller allows ("
                        + formatDuration(Duration.ofSeconds(listing.getLendingDurationSeconds())) + ")");
            }
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    private Listing requireOpenListing(String listingId, ListingType type) {
        Listing listing = store.findListing(listingId).orElse(null);
        boolean sellerActive = listing != null
                && store.findUser(listing.getSellerId()).map(User::isActive).orElse(false);
        if (listing == null || listing.getStatus() != ListingStatus.LIVE || !sellerActive) {
            throw new MarketplaceException("This listing is no longer available.");
        }
        if (listing.getType() != type) {
            throw new MarketplaceException("This action does not match the listing type (" + listing.getType() + ").");
        }
        return listing;
    }

    private void handleExisting(String buyerId, String listingId, boolean replaceExisting) {
        Optional<Deal> existing = openRequest(buyerId, listingId);
        if (existing.isPresent()) {
            if (!replaceExisting) {
                throw new MarketplaceException("You already have a " + existing.get().getStatus().label().toLowerCase(Locale.ROOT)
                        + " request on this listing.");
            }
            existing.get().setStatus(DealStatus.WITHDRAWN, now());
        }
    }

    private Deal newDeal(String buyerId, Listing listing, DealType type, String message) {
        Deal deal = new Deal(Ids.next("D"), buyerId, listing.getId(), type, now());
        deal.setMessage(message == null || message.isBlank() ? null : message.trim());
        return deal;
    }

    private void postCard(String buyerId, Listing listing, String text, String dealId) {
        ChatThread thread = chat.openThread(buyerId, listing.getId());
        chat.postOfferCard(thread, buyerId, Role.BUYER, text, dealId);
    }

    private Deal requireDeal(String dealId) {
        return store.findDeal(dealId).orElseThrow(() -> new MarketplaceException("Deal not found."));
    }

    private Deal requireBuyerDeal(String buyerId, String dealId) {
        Deal deal = requireDeal(dealId);
        if (!deal.getBuyerId().equals(buyerId)) {
            throw new MarketplaceException("This deal belongs to another buyer.");
        }
        return deal;
    }

    private Listing requireListing(String listingId) {
        return store.findListing(listingId).orElseThrow(() -> new MarketplaceException("Listing not found."));
    }

    private Listing requireSellerListing(String sellerId, Deal deal) {
        Listing listing = requireListing(deal.getListingId());
        if (!listing.getSellerId().equals(sellerId)) {
            throw new MarketplaceException("This listing belongs to another seller.");
        }
        return listing;
    }

    private Listing requireCounterable(String sellerId, Deal deal, DealType type) {
        Listing listing = requireSellerListing(sellerId, deal);
        if (deal.getType() != type) {
            throw new MarketplaceException("Counter-offers are only for Sell and Lend requests of the matching type.");
        }
        if (deal.getStatus() != DealStatus.PENDING || listing.getStatus() != ListingStatus.LIVE) {
            throw new MarketplaceException("This request can no longer be countered.");
        }
        return listing;
    }

    private void finishCounter(String sellerId, Deal deal, Listing listing, String text) {
        ChatThread thread = chat.getOrCreateThread(deal.getBuyerId(), listing);
        chat.postOfferCard(thread, sellerId, Role.SELLER, text, deal.getId());
        notifications.notify(deal.getBuyerId(), NotificationCategory.DEALS, "Counter-offer received",
                "The seller of \"" + listing.getTitle() + "\" sent a counter-offer. " + text, deal.getId());
    }

    /** Gives the item to this deal's buyer and declines every other open request on the listing. */
    private void allocate(Deal deal, Listing listing) {
        if (deal.getType() == DealType.BORROW) {
            deal.setStatus(DealStatus.BORROWED, now());
            listing.setStatus(ListingStatus.LENT);
            listing.setLentUntil(deal.getBorrowEnd());
        } else {
            deal.setStatus(DealStatus.ACCEPTED, now());
            listing.setStatus(ListingStatus.RESERVED);
        }
        declineOthers(deal, listing);
    }

    private void declineOthers(Deal winner, Listing listing) {
        for (Deal other : store.deals()) {
            if (!other.getId().equals(winner.getId()) && other.getListingId().equals(listing.getId())
                    && other.getStatus().isOpenRequest()) {
                other.setStatus(DealStatus.DECLINED, now());
                notifications.notify(other.getBuyerId(), NotificationCategory.DEALS, "Offer declined",
                        "\"" + listing.getTitle() + "\" went to another buyer, so your request was declined.",
                        other.getId());
            }
        }
    }

    private void releaseClaimHold(Deal deal, Listing listing) {
        if (deal.getType() == DealType.CLAIM && listing.getStatus() == ListingStatus.RESERVED) {
            listing.setStatus(ListingStatus.LIVE);
        }
    }

    private void releaseLoan(Listing listing) {
        listing.setLentUntil(null);
        if (listing.getStatus() == ListingStatus.LENT) {
            listing.setStatus(ListingStatus.LIVE);
        }
    }

    private static boolean reminderDue(Deal deal, LocalDateTime now) {
        Duration period = Duration.between(deal.getBorrowStart(), deal.getBorrowEnd());
        Duration left = Duration.between(now, deal.getBorrowEnd());
        if (period.compareTo(Duration.ofHours(48)) >= 0) {
            return left.compareTo(Duration.ofHours(24)) <= 0;
        }
        return left.compareTo(period.dividedBy(2)) <= 0;
    }

    private String buyerName(String buyerId) {
        return store.findUser(buyerId).map(User::getFullName).orElse("The buyer");
    }

    private static String optional(String message) {
        return message == null || message.isBlank() ? "" : " - \"" + message.trim() + "\"";
    }

    /**
     * Formats a duration for display, e.g. "2d 3h", "45m 10s" or "8s".
     * @param d duration
     * @return short text
     */
    public static String formatDuration(Duration d) {
        long s = Math.max(0, d.getSeconds());
        long days = s / 86_400;
        long hours = (s % 86_400) / 3_600;
        long minutes = (s % 3_600) / 60;
        long seconds = s % 60;
        if (days > 0) {
            return days + "d " + hours + "h";
        }
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        }
        return seconds + "s";
    }
}
