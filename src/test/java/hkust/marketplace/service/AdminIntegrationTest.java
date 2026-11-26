package hkust.marketplace.service;

import hkust.marketplace.data.DataStore;
import hkust.marketplace.data.SeedData;
import hkust.marketplace.model.*;
import hkust.marketplace.util.MarketplaceException;
import hkust.marketplace.util.ValidationException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/** Checks administration against real Buyer services, seeded records and the on-disk format. */
class AdminIntegrationTest extends ServiceTestBase {
    private AdminRecords records() { return new AdminRecords(app, ADMIN); }
    private ListingForm sale(String title) {
        return new ListingForm(ListingType.SELL,title,Category.ELECTRONICS,Condition.USED,"Test description",
                "Library",new BigDecimal("120.00"),0,List.of());
    }

    @ParameterizedTest @ValueSource(strings = {"U-demo","U-alice","missing"})
    void managementRequiresAdmin(String actor) {
        assertThrows(MarketplaceException.class, () -> new AdminRecords(app,actor));
        assertThrows(MarketplaceException.class, () -> app.admin().review(actor,"L-pend",true,null));
        assertThrows(MarketplaceException.class, () -> app.admin().hideReview(actor,"V-book",true));
        assertThrows(MarketplaceException.class, () -> app.admin().deleteUser(actor,ALICE));
        assertThrows(MarketplaceException.class, () -> app.admin().archive(actor,"missing",true));
        assertThrows(MarketplaceException.class, () -> app.admin().listings(actor,false));
        assertEquals(ListingStatus.PENDING_REVIEW,listing("L-pend").getStatus());
    }

    @Test void approvalsAndRejectionsCrossPortals() {
        AdminService a = app.admin();
        Listing l = a.submitForReview(ALICE,sale("Lab lamp"));
        assertFalse(app.listings().browse(new BrowseQuery()).contains(l));
        assertTrue(records().listings(true,"alice",null).contains(l));
        a.syncPendingAlerts(ADMIN); a.syncPendingAlerts(ADMIN);
        assertEquals(1,records().notifications(NotificationCategory.LISTING_UPDATES,new AdminRecords.Dates(null,null),false)
                .stream().filter(n -> l.getId().equals(n.getRelatedId())).count());
        a.review(ADMIN,l.getId(),true,null);
        assertTrue(app.listings().browse(new BrowseQuery()).contains(l));
        assertEquals(now(),l.getReviewedAt());
        assertThrows(MarketplaceException.class, () -> a.review(ADMIN,l.getId(),false,"Late"));
        assertThrows(ValidationException.class, () -> a.review(ADMIN,"L-pend",false,"  "));
        a.review(ADMIN,"L-pend",false,"Photo is missing");
        assertEquals("Photo is missing",listing("L-pend").getRejectionReason());
        assertTrue(app.notifications().forUser(ALICE,NotificationCategory.LISTING_UPDATES,false).stream()
                .anyMatch(n -> n.getContent().contains("Photo is missing")));
    }

    @Test void takeDownClosesOnlyAffectedOffersAndReports() {
        Deal one = app.deals().makeOffer(DEMO,"L-chair","110","",false);
        Deal two = app.deals().makeOffer(CHRIS,"L-chair","105","",false);
        Report report = app.listings().reportListing(CHRIS,"L-chair",ReportReason.OTHER,"Duplicate");
        int notices = app.notifications().forUser(BOB,null,false).size();
        assertThrows(ValidationException.class, () -> app.admin().takeDown(ADMIN,"L-chair"," "));
        app.admin().takeDown(ADMIN,"L-chair","Duplicate");
        assertEquals(DealStatus.CANCELLED,one.getStatus()); assertEquals(DealStatus.CANCELLED,two.getStatus());
        assertEquals(ReportStatus.RESOLVED,report.getStatus());
        assertEquals("Duplicate",listing("L-chair").getRemovalReason());
        assertFalse(app.listings().isVisibleToBuyers(listing("L-chair")));
        assertEquals(notices,app.notifications().forUser(BOB,null,false).size());
        for (Deal d : List.of(one,two)) {
            assertTrue(app.notifications().forUser(d.getBuyerId(),NotificationCategory.DEALS,false).stream()
                    .anyMatch(n -> d.getId().equals(n.getRelatedId()) && n.getContent().contains("Duplicate")));
        }
        assertThrows(MarketplaceException.class, () -> app.admin().takeDown(ADMIN,"L-chair","Again"));
    }

    @Test void reportingAndDismissalRetainListing() {
        Report report = app.listings().reportListing(CHRIS,"L-chair",ReportReason.OTHER,"Check condition");
        assertEquals(report, records().reports(null).getFirst());
        app.admin().dismissReports(ADMIN,"L-chair");
        assertEquals(ReportStatus.DISMISSED,report.getStatus()); assertTrue(records().reports("L-chair").isEmpty());
        assertTrue(app.listings().isVisibleToBuyers(listing("L-chair")));
        assertThrows(MarketplaceException.class, () -> app.admin().dismissReports(ADMIN,"L-chair"));
    }

    @Test void createEditAndPreserveDealTypeAndStatus() {
        Listing l = app.admin().addListing(ADMIN,ALICE,sale("Hot plate"));
        assertEquals(ListingStatus.LIVE,l.getStatus()); assertEquals(ALICE,l.getSellerId());
        assertEquals(l,records().listings(false,"hot plate",ListingStatus.LIVE).getFirst());
        app.admin().editListing(ADMIN,l.getId(),ListingStatus.LIVE,sale("Induction plate"));
        assertEquals("Induction plate",l.getTitle());
        assertThrows(MarketplaceException.class, () -> app.admin().editListing(ADMIN,l.getId(),ListingStatus.SOLD,sale("Stale")));
        ListingForm loan = new ListingForm(ListingType.LEND,"Loan",Category.OTHER,Condition.NEW,"Use it","Atrium",null,20,List.of("res:/images/lamp.png"));
        app.admin().editListing(ADMIN,l.getId(),ListingStatus.LIVE,loan); assertNull(l.getPrice());
        assertEquals(20,l.getLendingDurationSeconds()); assertEquals(1,l.getPhotos().size());
        assertThrows(MarketplaceException.class, () -> app.admin().editListing(ADMIN,"L-head",ListingStatus.LIVE,loan));
        assertThrows(ValidationException.class, () -> new ListingForm(ListingType.LEND,"x",Category.OTHER,Condition.NEW,"x","x",null,1,List.of()));
        assertThrows(ValidationException.class, () -> new ListingForm(ListingType.SELL,"x",Category.OTHER,Condition.NEW,"x","x",null,0,List.of()));
        assertThrows(ValidationException.class, () -> new ListingForm(ListingType.SELL,"x",Category.OTHER,Condition.NEW,"x","x",new BigDecimal("-2"),0,List.of()));
        assertThrows(ValidationException.class, () -> new ListingForm(ListingType.GIVEAWAY,"x",Category.OTHER,Condition.NEW,"x","x",null,0,Collections.nCopies(7,"pic")));
        app.admin().setActive(ADMIN,ALICE,false);
        assertThrows(MarketplaceException.class, () -> app.admin().addListing(ADMIN,ALICE,sale("no")));
    }

    @Test void manageAccountsWithExistingAuthenticationAndBrowse() {
        AdminService a = app.admin();
        User added = a.addUser(ADMIN,Role.SELLER,"new_seller","New Seller","Strong123","Strong123");
        assertEquals(added,app.auth().login("NEW_SELLER","Strong123",Role.SELLER));
        assertThrows(ValidationException.class, () -> a.addUser(ADMIN,Role.BUYER,"NEW_SELLER","Name","Strong123","Strong123"));
        assertThrows(ValidationException.class, () -> a.addUser(ADMIN,Role.ADMIN,"third","Third","Strong123","Strong123"));
        assertThrows(ValidationException.class, () -> a.addUser(ADMIN,Role.BUYER,"bad name","Name","Strong123","Strong123"));
        assertThrows(ValidationException.class, () -> a.addUser(ADMIN,Role.BUYER,"valid","Name","weak","weak"));
        assertThrows(ValidationException.class, () -> a.addUser(ADMIN,Role.BUYER,"valid","Name","Strong123","different"));
        assertEquals(Role.BUYER,a.addUser(ADMIN,Role.BUYER,"new_buyer","Buyer","Strong123","Strong123").getRole());
        a.rename(ADMIN,added.getId(),"Updated"); assertEquals(added,records().users("updated",Role.SELLER).getFirst());
        assertThrows(ValidationException.class, () -> a.rename(ADMIN,added.getId()," "));
        assertThrows(MarketplaceException.class, () -> a.setActive(ADMIN,ADMIN,false));
        a.setActive(ADMIN,ALICE,false);
        assertFalse(app.listings().isVisibleToBuyers(listing("L-chair")));
        assertThrows(MarketplaceException.class, () -> app.auth().login("alice_w","Seller123",Role.SELLER));
        a.setActive(ADMIN,ALICE,true); assertTrue(app.listings().isVisibleToBuyers(listing("L-chair")));
        assertThrows(MarketplaceException.class, () -> a.setActive(ADMIN,ALICE,true));
    }

    @Test void deletionCleansOpenCommitmentsAndPreservesHistory() {
        assertThrows(MarketplaceException.class, () -> app.admin().deleteUser(ADMIN,DEMO));
        assertThrows(MarketplaceException.class, () -> app.admin().deleteUser(ADMIN,BOB));
        Deal d = app.deals().makeOffer(DEMO,"L-chair","75","",false);
        app.admin().deleteUser(ADMIN,ALICE);
        assertEquals(ListingStatus.REMOVED,listing("L-pend").getStatus());
        assertEquals(DealStatus.CANCELLED,d.getStatus());
        assertEquals(DealStatus.CANCELLED,store.findDeal("D-hood").orElseThrow().getStatus());
        assertEquals(ListingStatus.REMOVED,listing("L-hood").getStatus());
        assertTrue(store.findDeal("D-mug").isPresent());
        User old = store.findUser(ALICE).orElseThrow();
        assertEquals(AccountStatus.DELETED,old.getStatus()); assertEquals("Deleted account",old.getFullName());
        assertTrue(store.findUserByUsername("alice_w").isEmpty()); assertTrue(old.getPasswordHash().isEmpty());
        assertThrows(MarketplaceException.class, () -> app.admin().setActive(ADMIN,ALICE,true));
        app.admin().deleteUser(ADMIN,CHRIS);
        assertNotNull(store.reviews().getFirst()); assertFalse(store.threads().isEmpty());
    }

    @Test void deletingBuyerReleasesReservedClaim() {
        Deal claim = app.deals().claim(CHRIS,"L-lamp");
        app.admin().deleteUser(ADMIN,CHRIS);
        assertEquals(DealStatus.CANCELLED,claim.getStatus());
        assertEquals(ListingStatus.LIVE,listing("L-lamp").getStatus());
    }

    @Test void profileIsAtomicOnInvalidPasswordAndRequiresReauthentication() {
        assertThrows(MarketplaceException.class, () -> app.admin().profile(ADMIN,"New","wrong","",""));
        assertThrows(ValidationException.class, () -> app.admin().profile(ADMIN,"New","Admin1234","weak","weak"));
        assertThrows(ValidationException.class, () -> app.admin().profile(ADMIN,"New","Admin1234","Strong123","mismatch"));
        assertThrows(ValidationException.class, () -> app.admin().profile(ADMIN,"New","Admin1234","Admin1234","Admin1234"));
        assertEquals("Marketplace Admin",app.admin().requireAdmin(ADMIN).getFullName());
        assertFalse(app.admin().profile(ADMIN,"New","Admin1234","",""));
        assertThrows(ValidationException.class, () -> app.admin().profile(ADMIN,"New","Admin1234","",""));
        assertTrue(app.admin().profile(ADMIN,"New","Admin1234","NextPass123","NextPass123"));
        assertThrows(ValidationException.class, () -> app.auth().login("admin","Admin1234",Role.ADMIN));
        assertEquals(ADMIN,app.auth().login("admin","NextPass123",Role.ADMIN).getId());
    }

    @Test void archiveAndFilterNotificationsWithoutLosingAudit() {
        app.admin().setActive(ADMIN,ALICE,false);
        var day = new AdminRecords.Dates(T0.toLocalDate(),T0.toLocalDate());
        Notification n = records().notifications(NotificationCategory.ACCOUNT,day,false).getFirst();
        app.notifications().markRead(ADMIN,n.getId());
        app.admin().archive(ADMIN,n.getId(),true);
        assertTrue(records().notifications(null,day,false).isEmpty());
        assertEquals(n,records().notifications(null,day,true).getFirst());
        assertTrue(n.getContent().contains("admin")); assertTrue(n.isRead());
        app.admin().archive(ADMIN,n.getId(),false); assertEquals(n,records().notifications(null,day,false).getFirst());
        assertThrows(MarketplaceException.class, () -> app.admin().archive(ADMIN,app.notifications().forUser(DEMO,null,false).getFirst().getId(),true));
    }

    @Test void hiddenReviewsRecomputePublicAverageAndCanBeRestored() {
        var all = new AdminRecords.Dates(null,null);
        Review r = records().ratings("bob_c",5,all,false).getFirst();
        assertTrue(records().ratings("",5,all,true).isEmpty());
        r.setFlagged(true); assertEquals(r,records().ratings("chris",5,all,true).getFirst());
        app.admin().hideReview(ADMIN,r.getId(),true); assertTrue(app.reviews().averageRating(BOB).isEmpty());
        assertTrue(app.reviews().visibleReviewsForSeller(BOB).isEmpty()); assertTrue(app.reviews().reviewsByBuyer(CHRIS).isEmpty());
        assertEquals(r,records().ratings("",null,all,false).getFirst());
        app.admin().hideReview(ADMIN,r.getId(),false); assertEquals(5,app.reviews().averageRating(BOB).orElseThrow());
        assertTrue(records().ratings("",1,all,false).isEmpty());
        assertTrue(records().ratings("",null,new AdminRecords.Dates(T0.toLocalDate(),null),false).isEmpty());
        assertThrows(ValidationException.class, () -> new AdminRecords.Dates(T0.toLocalDate(),T0.minusDays(1).toLocalDate()));
    }

    @Test void exportedDealsMatchFiltersAndEscapeUserInput() {
        var r=records(); Listing l=app.admin().addListing(ADMIN,ALICE,sale("=kit, \"A\""));
        Deal d=app.deals().makeOffer(DEMO,l.getId(),"110","",false);
        List<Deal> shown=r.deals("=kit",DealType.OFFER,AdminRecords.Stage.ACTIVE);
        assertEquals(List.of(d),shown); assertTrue(r.csv(shown).contains("\"'=kit, \"\"A\"\"\""));
        assertEquals(2,r.csv(shown).lines().count()); assertEquals(d.getSubmittedAt(),r.start(d));
        assertEquals(1,r.csv(List.of()).lines().count()); assertNull(r.end(d));
        assertFalse(r.deals("",null,AdminRecords.Stage.FINISHED).isEmpty());
        assertTrue(r.deals("missing",null,null).isEmpty());
    }

    @Test void buyerOnlyFileOpensAndModerationPersists(@TempDir Path temp) throws Exception {
        Path file=temp.resolve("marketplace-data.ser"); DataStore first=DataStore.load(file);
        SeedData.populate(first,clock); first.save();
        Marketplace reopened=new Marketplace(DataStore.load(file),clock);
        reopened.admin().syncPendingAlerts(ADMIN); reopened.admin().review(ADMIN,"L-pend",true,"");
        Marketplace again=new Marketplace(DataStore.load(file),clock);
        assertTrue(again.listings().browse(new BrowseQuery()).stream().anyMatch(l -> l.getId().equals("L-pend")));
        assertTrue(again.notifications().forUser(ALICE,NotificationCategory.LISTING_UPDATES,false).stream()
                .anyMatch(n -> n.getRelatedId().equals("L-pend")));
    }
}
