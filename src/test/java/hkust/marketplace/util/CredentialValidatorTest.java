package hkust.marketplace.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Username, full-name and password rules. */
class CredentialValidatorTest {

    @Test
    void usernames() {
        assertTrue(CredentialValidator.isValidUsername("demo_buyer1"));
        assertTrue(CredentialValidator.isValidUsername("  trimmed "));
        assertFalse(CredentialValidator.isValidUsername(null));
        assertFalse(CredentialValidator.isValidUsername("   "));
        assertFalse(CredentialValidator.isValidUsername("has space"));
        assertFalse(CredentialValidator.isValidUsername("dash-name"));
    }

    @Test
    void fullNames() {
        assertTrue(CredentialValidator.isValidFullName("Saumik Vij"));
        assertTrue(CredentialValidator.isValidFullName("x".repeat(50)));
        assertFalse(CredentialValidator.isValidFullName("x".repeat(51)));
        assertFalse(CredentialValidator.isValidFullName(" "));
        assertFalse(CredentialValidator.isValidFullName(null));
    }

    @Test
    void passwords() {
        assertTrue(CredentialValidator.isStrongPassword("Buyer123"));
        assertEquals(List.of("Password is required"), CredentialValidator.passwordProblems(null));
        assertEquals(List.of("Password is required"), CredentialValidator.passwordProblems(""));
        assertEquals(List.of("Password needs at least 8 characters", "Password needs at least one uppercase letter",
                "Password needs at least one digit"), CredentialValidator.passwordProblems("abc"));
        assertEquals(List.of("Password needs at least one lowercase letter"),
                CredentialValidator.passwordProblems("ABCDEFG1"));
        assertFalse(CredentialValidator.isStrongPassword("abcdefgh"));
    }
}
