package hkust.marketplace.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hkust.marketplace.model.AccountStatus;
import hkust.marketplace.model.Category;
import hkust.marketplace.model.Condition;
import hkust.marketplace.model.Listing;
import hkust.marketplace.model.ListingStatus;
import hkust.marketplace.model.ListingType;
import hkust.marketplace.model.NotificationCategory;
import hkust.marketplace.model.Report;
import hkust.marketplace.model.ReportReason;
import hkust.marketplace.model.ReportStatus;
import hkust.marketplace.util.MarketplaceException;
import hkust.marketplace.util.ValidationException;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** Tasks 1.3 and 1.4: browse, search, filter, sort, listing detail and reporting. */
class ListingServiceTest extends ServiceTestBase {

    private List<String> ids(BrowseQuery q) {
        return app.listings().browse(q).stream().map(Listing::getId).collect(Collectors.toList());
    }

    @Test
    void browseShowsOnlyApprovedListingsNewestFirst() {
        assertEquals(List.of("L-rack", "L-hood", "L-calcr", "L-chair", "L-head", "L-umbr", "L-lamp", "L-calc"),
                ids(new BrowseQuery()));
    }

    @Test
    void pendingSoldAndRemovedListingsAreHidden() {
        List<String> all = ids(new BrowseQuery());
        assertFalse(all.contains("L-pend"));
        assertFalse(all.contains("L-mug"));
        listing("L-calc").setStatus(ListingStatus.REMOVED);
        assertFalse(ids(new BrowseQuery()).contains("L-calc"));
    }

    @Test
    void listingsOfDeactivatedSellersAreHidden() {
        store.findUser(ALICE).orElseThrow().setStatus(AccountStatus.DEACTIVATED);
        assertEquals(List.of("L-rack", "L-calcr", "L-head", "L-lamp"), ids(new BrowseQuery()));
    }

    @Test
    void keywordSearchesTitleAndDescriptionIgnoringCase() {
        assertEquals(List.of("L-calc"), ids(new BrowseQuery().keyword("  STEWART ")));
        assertEquals(List.of("L-head"), ids(new BrowseQuery().keyword("30h battery")));
        assertTrue(ids(new BrowseQuery().keyword("zzz-nothing")).isEmpty());
        assertEquals(8, ids(new BrowseQuery().keyword("   ")).size());
    }

    @Test
    void filtersCombine() {
        assertEquals(List.of("L-head"), ids(new BrowseQuery().category(Category.ELECTRONICS)));
        assertEquals(List.of("L-hood", "L-lamp"), ids(new BrowseQuery().type(ListingType.GIVEAWAY)));
        assertEquals(List.of("L-hood", "L-head", "L-umbr"), ids(new BrowseQuery().condition(Condition.LIKE_NEW)));
        assertEquals(List.of("L-umbr"), ids(new BrowseQuery().type(ListingType.LEND).condition(Condition.LIKE_NEW)));
    }

    @Test
    void priceRangeKeepsOnlyPricedSellListings() {
        assertEquals(List.of("L-chair", "L-calc"), ids(new BrowseQuery().minPrice("150").maxPrice("300")));
        assertEquals(List.of("L-rack"), ids(new BrowseQuery().maxPrice("120")));
        assertEquals(List.of("L-head"), ids(new BrowseQuery().minPrice("400")));
    }

    @Test
    void invalidPriceRangeIsRejected() {
        assertEquals("Minimum price cannot be greater than maximum price", assertThrows(ValidationException.class,
                () -> app.listings().browse(new BrowseQuery().minPrice("500").maxPrice("100"))).getMessage());
        assertThrows(ValidationException.class, () -> app.listings().browse(new BrowseQuery().minPrice("abc")));
        assertThrows(ValidationException.class, () -> app.listings().browse(new BrowseQuery().maxPrice("-5")));
    }

    @Test
    void sortByPricePutsUnpricedItemsLast() {
        assertEquals(List.of("L-rack", "L-calc", "L-chair", "L-head", "L-hood", "L-calcr", "L-umbr", "L-lamp"),
                ids(new BrowseQuery().sort(SortOrder.PRICE_LOW_HIGH)));
        assertEquals(List.of("L-head", "L-chair", "L-calc", "L-rack", "L-hood", "L-calcr", "L-umbr", "L-lamp"),
                ids(new BrowseQuery().sort(SortOrder.PRICE_HIGH_LOW)));
        assertEquals(SortOrder.NEWEST, new BrowseQuery().sort(null).getSort());
        assertEquals("Newest first", SortOrder.NEWEST.toString());
    }

    @Test
    void openListingCountsAViewEachTime() {
        int before = listing("L-chair").getViewCount();
        app.listings().openListing(DEMO, "L-chair");
        app.listings().openListing(DEMO, "L-chair");
        assertEquals(before + 2, listing("L-chair").getViewCount());
    }

    @Test
    void openListingFailsWhenNotVisibleOrNotBuyer() {
        assertThrows(MarketplaceException.class, () -> app.listings().openListing(DEMO, "L-pend"));
        assertThrows(MarketplaceException.class, () -> app.listings().openListing(DEMO, "missing"));
        assertThrows(MarketplaceException.class, () -> app.listings().openListing(ALICE, "L-calc"));
    }

    @Test
    void availabilityTextForEveryStatus() {
        Listing l = listing("L-calc");
        assertEquals("Available", app.listings().availabilityText(l));
        assertTrue(app.listings().isOpenForDeals(l));
        l.setStatus(ListingStatus.RESERVED);
        assertEquals("Reserved", app.listings().availabilityText(l));
        assertFalse(app.listings().isOpenForDeals(l));
        l.setStatus(ListingStatus.LENT);
        assertEquals("Lent", app.listings().availabilityText(l));
        l.setLentUntil(T0);
        assertEquals("Lent until 20 Nov 2026, 12:00:00", app.listings().availabilityText(l));
        l.setStatus(ListingStatus.SOLD);
        assertEquals("Sold", app.listings().availabilityText(l));
        l.setStatus(ListingStatus.GIVEN);
        assertEquals("Given away", app.listings().availabilityText(l));
        l.setStatus(ListingStatus.REMOVED);
        assertEquals("Not available", app.listings().availabilityText(l));
    }

    @Test
    void sellerOfReturnsTheSeller() {
        assertEquals("Alice Wong", app.listings().sellerOf(listing("L-calc")).getFullName());
        Listing orphan = new Listing("L-x", "U-gone", ListingType.SELL, "x", Category.OTHER, Condition.USED, "",
                "", T0);
        assertThrows(MarketplaceException.class, () -> app.listings().sellerOf(orphan));
    }

    @Test
    void reportNotifiesAdminAndBlocksDuplicates() {
        Report r = app.listings().reportListing(DEMO, "L-calc", ReportReason.values()[0], "  looks fake ");
        assertEquals(ReportStatus.OPEN, r.getStatus());
        assertEquals("looks fake", r.getComment());
        assertEquals(1, app.notifications().forUser(ADMIN, NotificationCategory.MODERATION, false).size());
        assertThrows(MarketplaceException.class,
                () -> app.listings().reportListing(DEMO, "L-calc", ReportReason.values()[0], null));
        r.setStatus(ReportStatus.RESOLVED);
        assertNull(app.listings().reportListing(DEMO, "L-calc", ReportReason.values()[0], " ").getComment());
    }

    @Test
    void reportValidation() {
        assertEquals("Please choose a reason", assertThrows(ValidationException.class,
                () -> app.listings().reportListing(DEMO, "L-calc", null, "x")).getMessage());
        assertThrows(ValidationException.class,
                () -> app.listings().reportListing(DEMO, "L-calc", ReportReason.OTHER, " "));
        assertThrows(MarketplaceException.class,
                () -> app.listings().reportListing(DEMO, "missing", ReportReason.OTHER, "x"));
    }
}
