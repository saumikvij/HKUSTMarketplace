package hkust.marketplace.data;

import hkust.marketplace.model.Admin;
import hkust.marketplace.model.Buyer;
import hkust.marketplace.model.Category;
import hkust.marketplace.model.ChatMessage;
import hkust.marketplace.model.ChatThread;
import hkust.marketplace.model.Condition;
import hkust.marketplace.model.Deal;
import hkust.marketplace.model.DealStatus;
import hkust.marketplace.model.DealType;
import hkust.marketplace.model.Listing;
import hkust.marketplace.model.ListingStatus;
import hkust.marketplace.model.ListingType;
import hkust.marketplace.model.Notification;
import hkust.marketplace.model.NotificationCategory;
import hkust.marketplace.model.Review;
import hkust.marketplace.model.Role;
import hkust.marketplace.model.Seller;
import hkust.marketplace.util.Ids;
import hkust.marketplace.util.PasswordHasher;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Demo data loaded on the first run so every screen has something to show.
 * <ul>
 *   <li>Admin: {@code admin} / {@code Admin1234} (seeded; there is no admin registration)</li>
 *   <li>Sellers: {@code alice_w} / {@code Seller123}, {@code bob_c} / {@code Seller123}</li>
 *   <li>Buyers: {@code demo_buyer} / {@code Buyer123}, {@code chris_l} / {@code Buyer123}</li>
 * </ul>
 */
public final class SeedData {
    /** Prefix for photo paths bundled inside the application. */
    public static final String RESOURCE_PHOTO = "res:/images/";

    private SeedData() { }

    /**
     * Seeds only the administrator account, for a demo that starts from an empty marketplace (no buyers,
     * sellers, listings or deals). The admin account is needed because admins cannot self-register.
     * @param store an empty store
     * @param clock clock used for timestamps
     */
    public static void populateAdminOnly(DataStore store, Clock clock) {
        store.putUser(new Admin("U-admin", "admin", "Marketplace Admin", hash("Admin1234", "c2VlZGFkbWluMDAwMDAw"),
                "c2VlZGFkbWluMDAwMDAw", LocalDateTime.now(clock)));
    }

    /**
     * Fills an empty store with demo accounts, listings, a finished deal and a review.
     * @param store an empty store
     * @param clock clock used for timestamps
     */
    public static void populate(DataStore store, Clock clock) {
        LocalDateTime now = LocalDateTime.now(clock);
        store.putUser(new Admin("U-admin", "admin", "Marketplace Admin", hash("Admin1234", "c2VlZGFkbWluMDAwMDAw"),
                "c2VlZGFkbWluMDAwMDAw", now.minusDays(60)));
        Seller alice = new Seller("U-alice", "alice_w", "Alice Wong", hash("Seller123", "c2VlZGFsaWNlMDAwMDAw"),
                "c2VlZGFsaWNlMDAwMDAw", now.minusDays(40), "Year 3 CompSci, selling my old stuff");
        alice.getMeetupLocations().add("LG1 Canteen");
        Seller bob = new Seller("U-bob", "bob_c", "Bob Chan", hash("Seller123", "c2VlZGJvYjAwMDAwMDAw"),
                "c2VlZGJvYjAwMDAwMDAw", now.minusDays(35), "Hall 5 resident");
        bob.getMeetupLocations().add("Hall 5 Lobby");
        Buyer demo = new Buyer("U-demo", "demo_buyer", "Demo Buyer", hash("Buyer123", "c2VlZGRlbW8wMDAwMDAw"),
                "c2VlZGRlbW8wMDAwMDAw", now.minusDays(30), "Hall 1");
        Buyer chris = new Buyer("U-chris", "chris_l", "Chris Lee", hash("Buyer123", "c2VlZGNocmlzMDAwMDAw"),
                "c2VlZGNocmlzMDAwMDAw", now.minusDays(25), "Off-campus");
        store.putUser(alice);
        store.putUser(bob);
        store.putUser(demo);
        store.putUser(chris);

        Listing calc = sell("L-calc", alice, "Calculus textbook (Stewart 8e)", Category.TEXTBOOKS, Condition.USED,
                "Used for MATH1013. Some highlighting in chapters 2-4, otherwise clean.", "LG1 Canteen", "180",
                now.minusDays(3), "calculus.png");
        Listing lamp = giveaway("L-lamp", bob, "Desk lamp", Category.HOME_AND_KITCHEN, Condition.USED,
                "Warm white LED desk lamp. Works fine, free to a good home.", "Hall 5 Lobby", now.minusDays(2),
                "lamp.png");
        Listing umbrella = lend("L-umbr", alice, "Large umbrella", Category.OTHER, Condition.LIKE_NEW,
                "Big golf umbrella for rainy days. Borrow it for up to 3 days.", "Atrium", 3 * 86_400,
                now.minusDays(1), "umbrella.png");
        Listing headphones = sell("L-head", bob, "Noise-cancelling headphones", Category.ELECTRONICS,
                Condition.LIKE_NEW, "Over-ear, 30h battery, with case and cable.", "Hall 5 Lobby", "450",
                now.minusHours(20), "headphones.png");
        Listing chair = sell("L-chair", alice, "Ergonomic study chair", Category.FURNITURE, Condition.USED,
                "Adjustable height and lumbar support. Must pick up from LG1.", "LG1 Canteen", "300",
                now.minusHours(12), "chair.png");
        Listing calcr = lend("L-calcr", bob, "Scientific calculator (Casio fx-991)", Category.STATIONERY,
                Condition.USED, "Exam-approved calculator. Lend for a day, short loans welcome.", "Library",
                86_400, now.minusHours(6), "calculator.png");
        Listing hoodie = giveaway("L-hood", alice, "HKUST hoodie (size M)", Category.CLOTHING, Condition.LIKE_NEW,
                "Navy HKUST hoodie, worn twice. Free!", "Atrium", now.minusHours(3), "hoodie.png");
        Listing racket = sell("L-rack", bob, "Badminton racket", Category.SPORTS, Condition.USED,
                "Yonex racket with new strings and a cover.", "Sports Hall", "120", now.minusHours(1), "racket.png");
        Listing pending = sell("L-pend", alice, "Dell monitor 24\"", Category.ELECTRONICS,
                Condition.USED, "Waiting for admin approval, so buyers cannot see it yet.", "LG1 Canteen", "600",
                now.minusMinutes(30), "monitor.png");
        pending.setStatus(ListingStatus.PENDING_REVIEW);
        for (Listing l : new Listing[] {calc, lamp, umbrella, headphones, chair, calcr, hoodie, racket, pending}) {
            store.putListing(l);
        }
        calc.incrementViewCount();
        headphones.incrementViewCount();

        // A finished purchase for the demo buyer (rate it from Deal History) and a review from another buyer.
        Listing mug = sell("L-mug", bob, "HKUST mug", Category.HOME_AND_KITCHEN, Condition.NEW,
                "Unused HKUST souvenir mug.", "Hall 5 Lobby", "40", now.minusDays(10), "mug.png");
        mug.setStatus(ListingStatus.SOLD);
        store.putListing(mug);
        Deal bought = new Deal("D-mug", demo.getId(), mug.getId(), DealType.OFFER, now.minusDays(9));
        bought.setOfferedPrice(new BigDecimal("35.00"));
        bought.setStatus(DealStatus.COMPLETED, now.minusDays(8));
        bought.setCompletedAt(now.minusDays(8));
        store.putDeal(bought);

        Listing book = sell("L-book", bob, "Intro to Economics", Category.TEXTBOOKS, Condition.USED,
                "ECON1210 textbook.", "Hall 5 Lobby", "90", now.minusDays(20), "economics.png");
        book.setStatus(ListingStatus.SOLD);
        store.putListing(book);
        Deal chrisDeal = new Deal("D-book", chris.getId(), book.getId(), DealType.OFFER, now.minusDays(19));
        chrisDeal.setOfferedPrice(new BigDecimal("90.00"));
        chrisDeal.setStatus(DealStatus.COMPLETED, now.minusDays(18));
        chrisDeal.setCompletedAt(now.minusDays(18));
        store.putDeal(chrisDeal);
        store.putReview(new Review("V-book", chrisDeal.getId(), chris.getId(), bob.getId(), 5,
                "Book was exactly as described and Bob was on time.", now.minusDays(17)));

        store.putNotification(new Notification(Ids.next("N"), demo.getId(), NotificationCategory.ANNOUNCEMENTS,
                "Welcome to HKUST Marketplace", "Browse listings, make offers and chat with sellers on campus.",
                now.minusDays(30), null));
        seedActiveDeals(store, now, demo, alice, bob, headphones, hoodie, calcr);
    }

    /**
     * Active deals for the demo buyer, so the buyer side of seller decisions (Task 2) can be shown on its own:
     * a counter-offer to answer, an accepted giveaway claim to mark complete, and a short borrow that
     * counts down, sends a reminder and is auto-returned a few minutes after the first start.
     */
    private static void seedActiveDeals(DataStore store, LocalDateTime now, Buyer demo, Seller alice, Seller bob,
                                        Listing headphones, Listing hoodie, Listing calcr) {
        Deal offer = new Deal("D-head", demo.getId(), headphones.getId(), DealType.OFFER, now.minusHours(2));
        offer.setOfferedPrice(new BigDecimal("380.00"));
        offer.setMessage("Would you take 380? I can meet today.");
        offer.setCounterPrice(new BigDecimal("420.00"));
        offer.setStatus(DealStatus.COUNTERED, now.minusHours(1));
        store.putDeal(offer);
        ChatThread chat = new ChatThread("C-head", demo.getId(), bob.getId(), headphones.getId());
        chat.getMessages().add(new ChatMessage(demo.getId(), Role.BUYER, "Hi Bob, are the headphones still available?",
                now.minusHours(2).minusMinutes(5), null));
        chat.getMessages().add(new ChatMessage(bob.getId(), Role.SELLER, "Yes! Battery is still great.",
                now.minusHours(2).minusMinutes(3), null));
        chat.getMessages().add(new ChatMessage(demo.getId(), Role.BUYER,
                "Offer: HK$380.00 — Would you take 380? I can meet today.", now.minusHours(2), offer.getId()));
        chat.getMessages().add(new ChatMessage(bob.getId(), Role.SELLER, "Counter-offer: HK$420.00",
                now.minusHours(1), offer.getId()));
        store.putThread(chat);
        store.putNotification(new Notification(Ids.next("N"), demo.getId(), NotificationCategory.DEALS,
                "Counter-offer received", "Bob Chan countered your offer on \"Noise-cancelling headphones\" with "
                + "HK$420.00.", now.minusHours(1), offer.getId()));

        Deal claim = new Deal("D-hood", demo.getId(), hoodie.getId(), DealType.CLAIM, now.minusHours(2));
        claim.setStatus(DealStatus.ACCEPTED, now.minusMinutes(90));
        hoodie.setStatus(ListingStatus.RESERVED);
        store.putDeal(claim);
        store.putNotification(new Notification(Ids.next("N"), demo.getId(), NotificationCategory.DEALS,
                "Claim accepted", alice.getFullName() + " confirmed your claim on \"" + hoodie.getTitle()
                + "\". Pick it up at " + hoodie.getMeetupLocation() + ".", now.minusMinutes(90), claim.getId()));

        Deal borrow = new Deal("D-calcr", demo.getId(), calcr.getId(), DealType.BORROW, now.minusMinutes(5));
        borrow.setBorrowPeriod(now.minusMinutes(1), now.plusMinutes(3));
        borrow.setMessage("Need it for the MATH quiz.");
        borrow.setStatus(DealStatus.BORROWED, now.minusMinutes(1));
        calcr.setStatus(ListingStatus.LENT);
        calcr.setLentUntil(borrow.getBorrowEnd());
        store.putDeal(borrow);
        store.putNotification(new Notification(Ids.next("N"), demo.getId(), NotificationCategory.BORROWING,
                "Borrow request accepted", bob.getFullName() + " accepted your request to borrow \""
                + calcr.getTitle() + "\". Return it by " + borrow.getBorrowEnd().toLocalTime().withNano(0) + ".",
                now.minusMinutes(1), borrow.getId()));
    }

    private static Listing sell(String id, Seller s, String title, Category c, Condition cond, String desc,
                                String where, String price, LocalDateTime at, String photo) {
        Listing l = base(id, s, ListingType.SELL, title, c, cond, desc, where, at, photo);
        l.setPrice(new BigDecimal(price));
        return l;
    }

    private static Listing lend(String id, Seller s, String title, Category c, Condition cond, String desc,
                                String where, long seconds, LocalDateTime at, String photo) {
        Listing l = base(id, s, ListingType.LEND, title, c, cond, desc, where, at, photo);
        l.setLendingDurationSeconds(seconds);
        return l;
    }

    private static Listing giveaway(String id, Seller s, String title, Category c, Condition cond, String desc,
                                    String where, LocalDateTime at, String photo) {
        return base(id, s, ListingType.GIVEAWAY, title, c, cond, desc, where, at, photo);
    }

    private static Listing base(String id, Seller s, ListingType type, String title, Category c, Condition cond,
                                String desc, String where, LocalDateTime at, String photo) {
        Listing l = new Listing(id, s.getId(), type, title, c, cond, desc, where, at);
        l.setStatus(ListingStatus.LIVE);
        l.setReviewedAt(at.plusMinutes(10));
        l.getPhotos().add(RESOURCE_PHOTO + photo);
        return l;
    }

    private static String hash(String password, String salt) {
        return PasswordHasher.hash(password, salt);
    }
}
