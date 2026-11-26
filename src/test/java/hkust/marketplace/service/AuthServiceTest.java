package hkust.marketplace.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hkust.marketplace.model.AccountStatus;
import hkust.marketplace.model.Buyer;
import hkust.marketplace.model.Role;
import hkust.marketplace.model.User;
import hkust.marketplace.util.MarketplaceException;
import hkust.marketplace.util.ValidationException;
import org.junit.jupiter.api.Test;

/** Tasks 1.1 and 1.2: buyer registration and login. */
class AuthServiceTest extends ServiceTestBase {

    @Test
    void registerCreatesActiveBuyerWithHashedPassword() {
        Buyer b = app.auth().registerBuyer("  new_user1 ", " Nina Ho ", "Secret123", "Secret123", " Hall 2 ");
        assertEquals("new_user1", b.getUsername());
        assertEquals("Nina Ho", b.getFullName());
        assertEquals("Hall 2", b.getResidenceHall());
        assertEquals(AccountStatus.ACTIVE, b.getStatus());
        assertTrue(!b.getPasswordHash().contains("Secret123"));
        assertEquals(T0, b.getRegisteredAt());
        assertEquals(b, app.auth().login("NEW_USER1", "Secret123", Role.BUYER));
    }

    @Test
    void registerWithoutHallStoresNull() {
        assertNull(app.auth().registerBuyer("nohall", "No Hall", "Secret123", "Secret123", "  ").getResidenceHall());
        assertNull(app.auth().registerBuyer("nohall2", "No Hall", "Secret123", "Secret123", null).getResidenceHall());
    }

    @Test
    void registerReportsEveryProblemAtOnce() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> app.auth().registerBuyer("", "", "abc", "", null));
        assertTrue(e.getErrors().contains("Username is required"));
        assertTrue(e.getErrors().contains("Full name is required"));
        assertTrue(e.getErrors().contains("Password needs at least 8 characters"));
        assertTrue(e.getErrors().contains("Please confirm the password"));
    }

    @Test
    void registerRejectsBadUsernameLongNameAndMismatch() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> app.auth().registerBuyer("bad name!", "x".repeat(51), "Secret123", "Secret124", null));
        assertTrue(e.getErrors().contains("Username can only contain letters, digits and underscores"));
        assertTrue(e.getErrors().contains("Full name must be at most 50 characters"));
        assertTrue(e.getErrors().contains("Passwords do not match"));
    }

    @Test
    void registerRejectsTakenUsernameIgnoringCase() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> app.auth().registerBuyer("Demo_Buyer", "Someone", "Secret123", "Secret123", null));
        assertEquals("Username is already taken", e.getMessage());
    }

    @Test
    void registerWithNullPasswordAndConfirm() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> app.auth().registerBuyer("u1", "U", null, null, null));
        assertTrue(e.getErrors().contains("Password is required"));
    }

    @Test
    void loginSucceedsForSeedBuyer() {
        User u = app.auth().login("demo_buyer", "Buyer123", Role.BUYER);
        assertEquals(DEMO, u.getId());
    }

    @Test
    void loginRequiresBothFields() {
        assertEquals("Username and password are both required",
                assertThrows(ValidationException.class, () -> app.auth().login(" ", "x", Role.BUYER)).getMessage());
        assertThrows(ValidationException.class, () -> app.auth().login("demo_buyer", "", Role.BUYER));
        assertThrows(ValidationException.class, () -> app.auth().login(null, null, Role.BUYER));
    }

    @Test
    void loginWithWrongPasswordOrUnknownUserGivesSameMessage() {
        String a = assertThrows(ValidationException.class,
                () -> app.auth().login("demo_buyer", "Wrong123", Role.BUYER)).getMessage();
        String b = assertThrows(ValidationException.class,
                () -> app.auth().login("ghost", "Buyer123", Role.BUYER)).getMessage();
        assertEquals("Invalid username or password", a);
        assertEquals(a, b);
    }

    @Test
    void sellerCannotUseBuyerLogin() {
        MarketplaceException e = assertThrows(MarketplaceException.class,
                () -> app.auth().login("alice_w", "Seller123", Role.BUYER));
        assertEquals("This is a seller account. Please use the seller login screen.", e.getMessage());
    }

    @Test
    void deactivatedBuyerCannotLogIn() {
        store.findUser(DEMO).orElseThrow().setStatus(AccountStatus.DEACTIVATED);
        MarketplaceException e = assertThrows(MarketplaceException.class,
                () -> app.auth().login("demo_buyer", "Buyer123", Role.BUYER));
        assertTrue(e.getMessage().contains("deactivated"));
    }
}
