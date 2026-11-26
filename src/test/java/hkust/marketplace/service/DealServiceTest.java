package hkust.marketplace.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hkust.marketplace.model.AccountStatus;
import hkust.marketplace.model.Category;
import hkust.marketplace.model.ChatThread;
import hkust.marketplace.model.Deal;
import hkust.marketplace.model.DealStatus;
import hkust.marketplace.model.DealType;
import hkust.marketplace.model.ListingStatus;
import hkust.marketplace.model.NotificationCategory;
import hkust.marketplace.util.MarketplaceException;
import hkust.marketplace.util.ValidationException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** Tasks 1.5, 1.6 and 1.9: offers, borrow requests, claims, My Deals actions, auto-return and history. */
class DealServiceTest extends ServiceTestBase {

    private DealService deals() {
        return app.deals();
    }

    private Deal borrow(String buyer, LocalDateTime start, LocalDateTime end) {
        return deals().requestBorrow(buyer, "L-umbr", start, end, null, false);
    }

    // ----- offers -----

    @Test
    void offerCreatesPendingDealChatCardAndSellerNotification() {
        Deal d = deals().makeOffer(DEMO, "L-calc", " 150.5 ", " Can we meet at LG1? ", false);
        assertEquals(DealStatus.PENDING, d.getStatus());
        assertEquals(new BigDecimal("150.50"), d.getOfferedPrice());
        assertEquals("Can we meet at LG1?", d.getMessage());
        assertEquals(DealType.OFFER, d.getType());
        ChatThread t = app.chat().findThread(DEMO, "L-calc").orElseThrow();
        assertEquals(d.getId(), t.getMessages().get(0).getDealId());
        assertTrue(t.getMessages().get(0).getText().startsWith("Offer: HK$150.50"));
        assertEquals("New offer", app.notifications().forUser(ALICE, NotificationCategory.DEALS, false)
                .get(0).getTitle());
        assertEquals(ListingStatus.LIVE, listing("L-calc").getStatus());
    }

    @Test
    void offerPriceValidation() {
        assertEquals("Please enter a price", assertThrows(ValidationException.class,
                () -> deals().makeOffer(DEMO, "L-calc", " ", null, false)).getMessage());
        assertThrows(ValidationException.class, () -> deals().makeOffer(DEMO, "L-calc", null, null, false));
        assertEquals("Price must be a number", assertThrows(ValidationException.class,
                () -> deals().makeOffer(DEMO, "L-calc", "ten", null, false)).getMessage());
        assertEquals("Price must be greater than 0", assertThrows(ValidationException.class,
                () -> deals().makeOffer(DEMO, "L-calc", "0", null, false)).getMessage());
        assertEquals("Price can have at most 2 decimal places", assertThrows(ValidationException.class,
                () -> deals().makeOffer(DEMO, "L-calc", "1.999", null, false)).getMessage());
        assertEquals(new BigDecimal("12.00"), deals().makeOffer(DEMO, "L-calc", "12.000", "", false)
                .getOfferedPrice());
    }

    @Test
    void offerOnlyOnLiveSellListingOfActiveSeller() {
        assertThrows(MarketplaceException.class, () -> deals().makeOffer(DEMO, "L-umbr", "10", null, false));
        assertThrows(MarketplaceException.class, () -> deals().makeOffer(DEMO, "L-pend", "10", null, false));
        assertThrows(MarketplaceException.class, () -> deals().makeOffer(DEMO, "missing", "10", null, false));
        store.findUser(ALICE).orElseThrow().setStatus(AccountStatus.DEACTIVATED);
        assertThrows(MarketplaceException.class, () -> deals().makeOffer(DEMO, "L-calc", "10", null, false));
        assertThrows(MarketplaceException.class, () -> deals().makeOffer(BOB, "L-rack", "10", null, false));
    }

    @Test
    void secondOfferNeedsReplaceAndReplacingWithdrawsTheFirst() {
        Deal first = deals().makeOffer(DEMO, "L-calc", "100", null, false);
        MarketplaceException e = assertThrows(MarketplaceException.class,
                () -> deals().makeOffer(DEMO, "L-calc", "120", null, false));
        assertEquals("You already have a pending request on this listing.", e.getMessage());
        Deal second = deals().makeOffer(DEMO, "L-calc", "120", null, true);
        assertEquals(DealStatus.WITHDRAWN, first.getStatus());
        assertEquals(second, deals().openRequest(DEMO, "L-calc").orElseThrow());
    }

    // ----- borrow requests -----

    @Test
    void borrowRequestStoresThePeriod() {
        Deal d = borrow(DEMO, T0.plusMinutes(1), T0.plusHours(5));
        assertEquals(DealStatus.PENDING, d.getStatus());
        assertEquals(T0.plusMinutes(1), d.getBorrowStart());
        assertEquals(T0.plusHours(5), d.getBorrowEnd());
        assertTrue(app.chat().findThread(DEMO, "L-umbr").orElseThrow().getMessages().get(0).getText()
                .startsWith("Borrow request: "));
    }

    @Test
    void borrowPeriodRules() {
        assertEquals("Please enter both a start and an end time", assertThrows(ValidationException.class,
                () -> borrow(DEMO, null, T0)).getMessage());
        assertThrows(ValidationException.class, () -> borrow(DEMO, T0, null));
        assertTrue(assertThrows(ValidationException.class, () -> borrow(DEMO, T0.minusMinutes(2), T0.plusHours(1)))
                .getErrors().contains("The start time cannot be in the past"));
        assertTrue(assertThrows(ValidationException.class, () -> borrow(DEMO, T0.plusHours(1), T0.plusHours(1)))
                .getErrors().contains("The end time must be after the start time"));
        assertTrue(assertThrows(ValidationException.class, () -> borrow(DEMO, T0, T0.plusSeconds(4)))
                .getErrors().contains("The borrowing period must be at least 5 seconds"));
        assertTrue(assertThrows(ValidationException.class, () -> borrow(DEMO, T0, T0.plusDays(4)))
                .getErrors().get(0).startsWith("The borrowing period is longer than the seller allows"));
        // within the 1-minute tolerance and exactly the minimum length are fine
        assertNotNull(borrow(DEMO, T0.minusSeconds(30), T0.minusSeconds(25)));
    }

    @Test
    void lendingLimitZeroMeansNoMaximum() {
        listing("L-umbr").setLendingDurationSeconds(0);
        assertNotNull(borrow(DEMO, T0, T0.plusDays(30)));
    }

    @Test
    void borrowOnlyForLendListings() {
        assertThrows(MarketplaceException.class,
                () -> deals().requestBorrow(DEMO, "L-calc", T0, T0.plusHours(1), null, false));
    }

    // ----- giveaway claims -----

    @Test
    void claimReservesImmediatelyAndSecondClaimFails() {
        Deal d = deals().claim(DEMO, "L-lamp");
        assertEquals(DealStatus.PENDING, d.getStatus());
        assertEquals(ListingStatus.RESERVED, listing("L-lamp").getStatus());
        MarketplaceException e = assertThrows(MarketplaceException.class, () -> deals().claim(CHRIS, "L-lamp"));
        assertEquals("This listing is no longer available.", e.getMessage());
    }

    @Test
    void concurrentClaimsOnlyOneWins() throws Exception {
        int[] wins = new int[1];
        Runnable a = () -> { try { deals().claim(DEMO, "L-lamp"); synchronized (wins) { wins[0]++; } }
                             catch (MarketplaceException ignored) { } };
        Runnable b = () -> { try { deals().claim(CHRIS, "L-lamp"); synchronized (wins) { wins[0]++; } }
                             catch (MarketplaceException ignored) { } };
        Thread t1 = new Thread(a);
        Thread t2 = new Thread(b);
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        assertEquals(1, wins[0]);
    }

    // ----- seller decisions (used by Task 2) -----

    @Test
    void acceptOfferReservesListingAndDeclinesOthers() {
        Deal mine = deals().makeOffer(DEMO, "L-calc", "150", null, false);
        Deal other = deals().makeOffer(CHRIS, "L-calc", "160", null, false);
        deals().acceptDeal(ALICE, other.getId());
        assertEquals(DealStatus.ACCEPTED, other.getStatus());
        assertEquals(DealStatus.DECLINED, mine.getStatus());
        assertEquals(ListingStatus.RESERVED, listing("L-calc").getStatus());
        assertTrue(app.notifications().forUser(DEMO, NotificationCategory.DEALS, true).stream()
                .anyMatch(n -> n.getContent().contains("went to another buyer")));
        assertThrows(MarketplaceException.class, () -> deals().acceptDeal(ALICE, mine.getId()));
    }

    @Test
    void acceptBorrowLendsTheItem() {
        Deal d = borrow(DEMO, T0, T0.plusHours(2));
        deals().acceptDeal(ALICE, d.getId());
        assertEquals(DealStatus.BORROWED, d.getStatus());
        assertEquals(ListingStatus.LENT, listing("L-umbr").getStatus());
        assertEquals(T0.plusHours(2), listing("L-umbr").getLentUntil());
    }

    @Test
    void acceptClaimKeepsReservedAndDeclineReopens() {
        Deal d = deals().claim(DEMO, "L-lamp");
        deals().acceptDeal(BOB, d.getId());
        assertEquals(DealStatus.ACCEPTED, d.getStatus());
        assertEquals(ListingStatus.RESERVED, listing("L-lamp").getStatus());

        listing("L-rack");
        Deal c = deals().makeOffer(DEMO, "L-rack", "100", null, false);
        deals().declineDeal(BOB, c.getId());
        assertEquals(DealStatus.DECLINED, c.getStatus());
        assertThrows(MarketplaceException.class, () -> deals().declineDeal(BOB, c.getId()));
    }

    @Test
    void declinedClaimMakesGiveawayLiveAgain() {
        Deal d = deals().claim(DEMO, "L-lamp");
        deals().declineDeal(BOB, d.getId());
        assertEquals(ListingStatus.LIVE, listing("L-lamp").getStatus());
    }

    @Test
    void sellerChecks() {
        Deal d = deals().makeOffer(DEMO, "L-calc", "150", null, false);
        assertThrows(MarketplaceException.class, () -> deals().acceptDeal(BOB, d.getId()));
        assertThrows(MarketplaceException.class, () -> deals().acceptDeal(DEMO, d.getId()));
        assertThrows(MarketplaceException.class, () -> deals().acceptDeal(ALICE, "missing"));
        listing("L-calc").setStatus(ListingStatus.REMOVED);
        assertThrows(MarketplaceException.class, () -> deals().acceptDeal(ALICE, d.getId()));
    }

    // ----- counter-offers -----

    @Test
    void acceptCounterOfferUsesTheCounterPrice() {
        Deal d = deals().makeOffer(DEMO, "L-calc", "150", null, false);
        deals().counterOffer(ALICE, d.getId(), "170");
        assertEquals(DealStatus.COUNTERED, d.getStatus());
        assertEquals(ListingStatus.LIVE, listing("L-calc").getStatus());
        deals().respondToCounter(DEMO, d.getId(), true);
        assertEquals(DealStatus.ACCEPTED, d.getStatus());
        assertEquals(new BigDecimal("170.00"), d.getCurrentPrice());
        assertEquals(ListingStatus.RESERVED, listing("L-calc").getStatus());
    }

    @Test
    void declineSeededCounterOffer() {
        Deal d = deals().respondToCounter(DEMO, "D-head", false);
        assertEquals(DealStatus.DECLINED, d.getStatus());
        assertEquals("Counter-offer declined", app.notifications().forUser(BOB, NotificationCategory.DEALS, false)
                .get(0).getTitle());
    }

    @Test
    void counterBorrowPeriodAndAccept() {
        Deal d = borrow(DEMO, T0, T0.plusHours(2));
        deals().counterBorrow(ALICE, d.getId(), T0.plusMinutes(10), T0.plusHours(1));
        assertEquals(T0.plusMinutes(10), d.getCounterStart());
        deals().respondToCounter(DEMO, d.getId(), true);
        assertEquals(DealStatus.BORROWED, d.getStatus());
        assertEquals(T0.plusHours(1), d.getBorrowEnd());
    }

    @Test
    void counterRules() {
        Deal offer = deals().makeOffer(DEMO, "L-calc", "150", null, false);
        assertThrows(MarketplaceException.class,
                () -> deals().counterBorrow(ALICE, offer.getId(), T0, T0.plusHours(1)));
        assertThrows(ValidationException.class, () -> deals().counterOffer(ALICE, offer.getId(), "-1"));
        deals().counterOffer(ALICE, offer.getId(), "170");
        assertThrows(MarketplaceException.class, () -> deals().counterOffer(ALICE, offer.getId(), "160"));
        assertThrows(MarketplaceException.class, () -> deals().respondToCounter(CHRIS, offer.getId(), true));
        listing("L-calc").setStatus(ListingStatus.REMOVED);
        assertEquals("This counter-offer is no longer valid.", assertThrows(MarketplaceException.class,
                () -> deals().respondToCounter(DEMO, offer.getId(), true)).getMessage());
    }

    @Test
    void expiredCounterPeriodCannotBeAccepted() {
        Deal d = borrow(DEMO, T0, T0.plusHours(2));
        deals().counterBorrow(ALICE, d.getId(), T0.plusMinutes(5), T0.plusHours(1));
        clock.advance(Duration.ofMinutes(10));
        assertThrows(ValidationException.class, () -> deals().respondToCounter(DEMO, d.getId(), true));
    }

    // ----- withdraw / return / complete -----

    @Test
    void withdrawPendingAndCounteredRequests() {
        Deal d = deals().makeOffer(DEMO, "L-calc", "150", null, false);
        deals().withdraw(DEMO, d.getId());
        assertEquals(DealStatus.WITHDRAWN, d.getStatus());
        deals().withdraw(DEMO, "D-head");
        assertEquals(DealStatus.WITHDRAWN, store.findDeal("D-head").orElseThrow().getStatus());
        assertTrue(assertThrows(MarketplaceException.class, () -> deals().withdraw(DEMO, "D-hood")).getMessage()
                .contains("can no longer be withdrawn"));
        assertThrows(MarketplaceException.class, () -> deals().withdraw(CHRIS, d.getId()));
    }

    @Test
    void withdrawingAClaimReopensTheGiveaway() {
        Deal d = deals().claim(DEMO, "L-lamp");
        deals().withdraw(DEMO, d.getId());
        assertEquals(ListingStatus.LIVE, listing("L-lamp").getStatus());
    }

    @Test
    void returnBorrowedItemEarly() {
        clock.advance(Duration.ofMinutes(1));
        Deal d = deals().returnItem(DEMO, "D-calcr");
        assertEquals(DealStatus.RETURNED, d.getStatus());
        assertEquals(now(), d.getCompletedAt());
        assertEquals(ListingStatus.LIVE, listing("L-calcr").getStatus());
        assertNull(listing("L-calcr").getLentUntil());
        assertThrows(MarketplaceException.class, () -> deals().returnItem(DEMO, "D-calcr"));
    }

    @Test
    void returnAfterDueTimeReportsAutoReturn() {
        clock.advance(Duration.ofMinutes(5));
        MarketplaceException e = assertThrows(MarketplaceException.class, () -> deals().returnItem(DEMO, "D-calcr"));
        assertEquals("This item was already returned automatically at the due time.", e.getMessage());
        assertEquals(DealStatus.AUTO_RETURNED, store.findDeal("D-calcr").orElseThrow().getStatus());
    }

    @Test
    void returnOnlyForBorrowedDeals() {
        assertEquals("Only borrowed items can be returned.", assertThrows(MarketplaceException.class,
                () -> deals().returnItem(DEMO, "D-hood")).getMessage());
    }

    @Test
    void markCompleteGivesAwayAndDeclinesOthers() {
        Deal d = deals().markComplete(DEMO, "D-hood");
        assertEquals(DealStatus.COMPLETED, d.getStatus());
        assertEquals(ListingStatus.GIVEN, listing("L-hood").getStatus());
        assertEquals(now(), d.getCompletedAt());
    }

    @Test
    void markCompleteOnPurchaseMarksSold() {
        Deal mine = deals().makeOffer(DEMO, "L-calc", "150", null, false);
        deals().acceptDeal(ALICE, mine.getId());
        deals().markComplete(DEMO, mine.getId());
        assertEquals(ListingStatus.SOLD, listing("L-calc").getStatus());
    }

    @Test
    void markCompleteRules() {
        assertThrows(MarketplaceException.class, () -> deals().markComplete(DEMO, "D-head"));
        assertThrows(MarketplaceException.class, () -> deals().markComplete(DEMO, "D-calcr"));
        listing("L-hood").setStatus(ListingStatus.REMOVED);
        deals().markComplete(DEMO, "D-hood");
        assertEquals(ListingStatus.REMOVED, listing("L-hood").getStatus());
    }

    // ----- due dates -----

    @Test
    void shortLoanGetsReminderAtHalfTimeThenAutoReturn() {
        // seeded loan: T0-1min to T0+3min, so the reminder is due at T0+1min
        assertEquals(0, deals().processDueDates());
        clock.advance(Duration.ofMinutes(1));
        assertEquals(1, deals().processDueDates());
        assertEquals(1, app.notifications().forUser(DEMO, NotificationCategory.BORROWING, false).stream()
                .filter(n -> n.getTitle().equals("Due soon")).count());
        assertEquals(0, deals().processDueDates());
        clock.advance(Duration.ofMinutes(2));
        assertEquals(1, deals().processDueDates());
        Deal d = store.findDeal("D-calcr").orElseThrow();
        assertEquals(DealStatus.AUTO_RETURNED, d.getStatus());
        assertEquals(d.getBorrowEnd(), d.getCompletedAt());
        assertEquals(ListingStatus.LIVE, listing("L-calcr").getStatus());
        assertTrue(app.notifications().forUser(BOB, NotificationCategory.BORROWING, false).stream()
                .anyMatch(n -> n.getTitle().equals("Item auto-returned")));
    }

    @Test
    void longLoanReminderOneDayBefore() {
        Deal d = borrow(DEMO, T0, T0.plusDays(3));
        deals().acceptDeal(ALICE, d.getId());
        clock.advance(Duration.ofHours(47));
        deals().processDueDates();
        assertFalse(d.isReminderSent());
        clock.advance(Duration.ofHours(1));
        deals().processDueDates();
        assertTrue(d.isReminderSent());
    }

    @Test
    void autoReturnKeepsRemovedListingRemoved() {
        listing("L-calcr").setStatus(ListingStatus.REMOVED);
        clock.advance(Duration.ofMinutes(4));
        deals().processDueDates();
        assertEquals(ListingStatus.REMOVED, listing("L-calcr").getStatus());
    }

    @Test
    void remainingAndFormatDuration() {
        Deal d = store.findDeal("D-calcr").orElseThrow();
        assertEquals(Duration.ofMinutes(3), deals().remaining(d));
        clock.advance(Duration.ofMinutes(10));
        assertEquals(Duration.ZERO, deals().remaining(d));
        assertNull(deals().remaining(store.findDeal("D-hood").orElseThrow()));
        assertEquals("2d 3h", DealService.formatDuration(Duration.ofHours(51)));
        assertEquals("1h 5m", DealService.formatDuration(Duration.ofMinutes(65)));
        assertEquals("2m 5s", DealService.formatDuration(Duration.ofSeconds(125)));
        assertEquals("8s", DealService.formatDuration(Duration.ofSeconds(8)));
        assertEquals("0s", DealService.formatDuration(Duration.ofSeconds(-3)));
    }

    // ----- My Deals lists and history -----

    @Test
    void activeAndClosedLists() {
        List<String> active = deals().activeDeals(DEMO).stream().map(Deal::getId).collect(Collectors.toList());
        assertEquals(List.of("D-calcr", "D-head", "D-hood"), active);
        assertEquals(List.of("D-mug"), deals().closedDeals(DEMO).stream().map(Deal::getId)
                .collect(Collectors.toList()));
        assertEquals(4, deals().dealsForBuyer(DEMO).size());
    }

    @Test
    void historyShowsFinishedDealsWithFilters() {
        deals().markComplete(DEMO, "D-hood");
        clock.advance(Duration.ofMinutes(5));
        deals().processDueDates();
        List<Deal> all = deals().history(DEMO, new HistoryQuery());
        assertEquals(List.of("D-calcr", "D-hood", "D-mug"), all.stream().map(Deal::getId)
                .collect(Collectors.toList()));
        assertEquals(1, deals().history(DEMO, new HistoryQuery().keyword(" mug ")).size());
        assertEquals(2, deals().history(DEMO, new HistoryQuery().keyword("bob")).size());
        assertEquals(1, deals().history(DEMO, new HistoryQuery().dealType(DealType.BORROW)).size());
        assertEquals(1, deals().history(DEMO, new HistoryQuery().category(Category.CLOTHING)).size());
        LocalDate today = T0.toLocalDate();
        assertEquals(2, deals().history(DEMO, new HistoryQuery().from(today).to(today)).size());
        assertEquals(1, deals().history(DEMO, new HistoryQuery().to(today.minusDays(1))).size());
        assertTrue(deals().history(CHRIS, new HistoryQuery()).stream().allMatch(d -> d.getBuyerId().equals(CHRIS)));
    }

    @Test
    void historyDateValidation() {
        LocalDate today = T0.toLocalDate();
        assertEquals("The start date must not be after the end date", assertThrows(ValidationException.class,
                () -> deals().history(DEMO, new HistoryQuery().from(today).to(today.minusDays(1)))).getMessage());
        assertEquals("Dates cannot be in the future", assertThrows(ValidationException.class,
                () -> deals().history(DEMO, new HistoryQuery().from(today.plusDays(1)))).getMessage());
        assertThrows(ValidationException.class, () -> deals().history(DEMO, new HistoryQuery().to(today.plusDays(1))));
    }

    @Test
    void historySkipsDealsWhoseListingIsGone() {
        Deal ghost = new Deal("D-ghost", DEMO, "L-none", DealType.OFFER, T0.minusDays(1));
        ghost.setStatus(DealStatus.COMPLETED, T0.minusDays(1));
        store.putDeal(ghost);
        assertFalse(deals().history(DEMO, new HistoryQuery()).contains(ghost));
        assertEquals(T0.minusDays(1), DealService.dealDate(ghost));
    }
}
