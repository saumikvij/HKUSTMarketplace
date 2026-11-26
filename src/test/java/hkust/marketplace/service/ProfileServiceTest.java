package hkust.marketplace.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hkust.marketplace.model.Buyer;
import hkust.marketplace.model.Role;
import hkust.marketplace.util.MarketplaceException;
import hkust.marketplace.util.ValidationException;
import org.junit.jupiter.api.Test;

/** Task 1.7: manage profile. */
class ProfileServiceTest extends ServiceTestBase {

    @Test
    void updatesNameAndHall() {
        Buyer b = app.profile().updateDetails(DEMO, "  Demo Two ", " Hall 9 ");
        assertEquals("Demo Two", b.getFullName());
        assertEquals("Hall 9", b.getResidenceHall());
        assertNull(app.profile().updateDetails(DEMO, "Demo Two", "").getResidenceHall());
    }

    @Test
    void rejectsEmptyOrLongNameAndNoChanges() {
        assertThrows(ValidationException.class, () -> app.profile().updateDetails(DEMO, " ", "Hall 1"));
        assertThrows(ValidationException.class, () -> app.profile().updateDetails(DEMO, null, "Hall 1"));
        assertThrows(ValidationException.class, () -> app.profile().updateDetails(DEMO, "y".repeat(60), "Hall 1"));
        MarketplaceException e = assertThrows(MarketplaceException.class,
                () -> app.profile().updateDetails(DEMO, "Demo Buyer", "Hall 1"));
        assertEquals("There are no changes to save.", e.getMessage());
    }

    @Test
    void onlyBuyersCanUpdateDetails() {
        assertThrows(MarketplaceException.class, () -> app.profile().updateDetails(ALICE, "A", null));
    }

    @Test
    void changesPasswordAndOldOneStopsWorking() {
        app.profile().changePassword(DEMO, "Buyer123", "NewPass99", "NewPass99");
        assertDoesNotThrow(() -> app.auth().login("demo_buyer", "NewPass99", Role.BUYER));
        assertThrows(ValidationException.class, () -> app.auth().login("demo_buyer", "Buyer123", Role.BUYER));
    }

    @Test
    void changePasswordChecksEveryRule() {
        assertEquals("Current password is incorrect", assertThrows(ValidationException.class,
                () -> app.profile().changePassword(DEMO, "nope", "NewPass99", "NewPass99")).getMessage());
        assertThrows(ValidationException.class, () -> app.profile().changePassword(DEMO, null, "NewPass99", "x"));
        ValidationException e = assertThrows(ValidationException.class,
                () -> app.profile().changePassword(DEMO, "Buyer123", "short", "other"));
        assertTrue(e.getErrors().contains("New passwords do not match"));
        assertTrue(e.getErrors().contains("Password needs at least 8 characters"));
        ValidationException same = assertThrows(ValidationException.class,
                () -> app.profile().changePassword(DEMO, "Buyer123", "Buyer123", "Buyer123"));
        assertTrue(same.getErrors().contains("New password must be different from the current password"));
        assertThrows(ValidationException.class, () -> app.profile().changePassword(DEMO, "Buyer123", null, null));
        assertThrows(MarketplaceException.class, () -> app.profile().changePassword("U-x", "a", "b", "c"));
    }
}
