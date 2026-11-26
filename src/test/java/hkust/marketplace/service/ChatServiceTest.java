package hkust.marketplace.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hkust.marketplace.model.AccountStatus;
import hkust.marketplace.model.ChatMessage;
import hkust.marketplace.model.ChatThread;
import hkust.marketplace.model.ListingStatus;
import hkust.marketplace.model.NotificationCategory;
import hkust.marketplace.model.Role;
import hkust.marketplace.util.MarketplaceException;
import hkust.marketplace.util.ValidationException;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Task 1.5: chat with the seller, and message search. */
class ChatServiceTest extends ServiceTestBase {

    @Test
    void openThreadIsReusedPerBuyerAndListing() {
        ChatThread a = app.chat().openThread(DEMO, "L-calc");
        assertSame(a, app.chat().openThread(DEMO, "L-calc"));
        assertEquals(ALICE, a.getSellerId());
        assertTrue(a.hasParticipant(DEMO) && a.hasParticipant(ALICE) && !a.hasParticipant(BOB));
        assertThrows(MarketplaceException.class, () -> app.chat().openThread(DEMO, "missing"));
        assertThrows(MarketplaceException.class, () -> app.chat().openThread(ALICE, "L-calc"));
    }

    @Test
    void sendStoresMessageAndNotifiesOtherSide() {
        ChatThread t = app.chat().openThread(DEMO, "L-calc");
        ChatMessage m = app.chat().send(t.getId(), DEMO, "Is it still available?");
        assertEquals(Role.BUYER, m.getSenderRole());
        assertFalse(m.isOfferCard());
        assertEquals(T0, m.getSentAt());
        assertEquals("New message from Demo Buyer",
                app.notifications().forUser(ALICE, NotificationCategory.MESSAGES, false).get(0).getTitle());
        ChatMessage reply = app.chat().send(t.getId(), ALICE, "x".repeat(100));
        assertEquals(Role.SELLER, reply.getSenderRole());
        assertTrue(app.notifications().forUser(DEMO, NotificationCategory.MESSAGES, false).get(0).getContent()
                .endsWith("..."));
        assertEquals(2, t.getMessages().size());
    }

    @Test
    void sendValidation() {
        ChatThread t = app.chat().openThread(DEMO, "L-calc");
        assertEquals("Message cannot be empty", assertThrows(ValidationException.class,
                () -> app.chat().send(t.getId(), DEMO, "  ")).getMessage());
        assertThrows(ValidationException.class, () -> app.chat().send(t.getId(), DEMO, null));
        assertThrows(ValidationException.class, () -> app.chat().send(t.getId(), DEMO, "a".repeat(1001)));
        app.chat().send(t.getId(), DEMO, "a".repeat(1000));
        assertThrows(MarketplaceException.class, () -> app.chat().send(t.getId(), BOB, "hi"));
        assertThrows(MarketplaceException.class, () -> app.chat().send("missing", DEMO, "hi"));
    }

    @Test
    void chatBecomesReadOnly() {
        ChatThread t = app.chat().openThread(DEMO, "L-calc");
        assertTrue(app.chat().readOnlyReason(t).isEmpty());
        store.findUser(ALICE).orElseThrow().setStatus(AccountStatus.DEACTIVATED);
        assertTrue(app.chat().readOnlyReason(t).orElseThrow().contains("no longer active"));
        assertThrows(MarketplaceException.class, () -> app.chat().send(t.getId(), DEMO, "hi"));
        store.findUser(ALICE).orElseThrow().setStatus(AccountStatus.ACTIVE);
        listing("L-calc").setStatus(ListingStatus.REMOVED);
        assertTrue(app.chat().readOnlyReason(t).orElseThrow().contains("removed"));
        ChatThread orphan = new ChatThread("C-x", DEMO, ALICE, "L-none");
        assertTrue(app.chat().readOnlyReason(orphan).isPresent());
        store.findUser(ALICE).orElseThrow().setStatus(AccountStatus.ACTIVE);
        listing("L-calc").setStatus(ListingStatus.LIVE);
        store.findUser(DEMO).orElseThrow().setStatus(AccountStatus.DEACTIVATED);
        assertTrue(app.chat().readOnlyReason(t).isPresent());
    }

    @Test
    void searchInOneChatOrAllChats() {
        ChatThread calc = app.chat().openThread(DEMO, "L-calc");
        app.chat().send(calc.getId(), DEMO, "Can we meet at the LIBRARY?");
        clock.advance(Duration.ofSeconds(1));
        ChatThread head = store.findThread("C-head").orElseThrow();
        app.chat().send(head.getId(), DEMO, "library is fine for me");
        List<ChatService.SearchHit> all = app.chat().search(DEMO, " Library ", null);
        assertEquals(2, all.size());
        assertSame(calc, all.get(0).getThread());
        assertEquals(1, app.chat().search(DEMO, "library", calc.getId()).size());
        assertEquals(1, app.chat().search(DEMO, "headphones", null).size());
        assertTrue(app.chat().search(CHRIS, "library", null).isEmpty());
        assertEquals("Please enter a keyword to search", assertThrows(ValidationException.class,
                () -> app.chat().search(DEMO, " ", null)).getMessage());
        assertThrows(ValidationException.class, () -> app.chat().search(DEMO, null, null));
    }

    @Test
    void threadsForEitherParticipant() {
        assertEquals(1, app.chat().threadsFor(DEMO).size());
        assertEquals(1, app.chat().threadsFor(BOB).size());
        assertTrue(app.chat().threadsFor(ALICE).isEmpty());
        assertTrue(app.chat().findThread(DEMO, "L-head").isPresent());
    }
}
