package hkust.marketplace.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Salted password hashing, ids and exceptions. */
class PasswordHasherTest {

    @Test
    void sameSaltSameHashDifferentSaltDifferentHash() {
        String s1 = PasswordHasher.newSalt();
        String s2 = PasswordHasher.newSalt();
        assertNotEquals(s1, s2);
        assertEquals(PasswordHasher.hash("Buyer123", s1), PasswordHasher.hash("Buyer123", s1));
        assertNotEquals(PasswordHasher.hash("Buyer123", s1), PasswordHasher.hash("Buyer123", s2));
    }

    @Test
    void matches() {
        String salt = PasswordHasher.newSalt();
        String hash = PasswordHasher.hash("Buyer123", salt);
        assertTrue(PasswordHasher.matches("Buyer123", salt, hash));
        assertFalse(PasswordHasher.matches("buyer123", salt, hash));
        assertFalse(PasswordHasher.matches(null, salt, hash));
        assertFalse(PasswordHasher.matches("x", null, hash));
        assertFalse(PasswordHasher.matches("x", salt, null));
    }

    @Test
    void idsAreUniqueAndPrefixed() {
        String a = Ids.next("D");
        String b = Ids.next("D");
        assertTrue(a.startsWith("D-"));
        assertNotEquals(a, b);
    }

    @Test
    void validationExceptionJoinsErrors() {
        ValidationException e = new ValidationException(List.of("a", "b"));
        assertEquals("a\nb", e.getMessage());
        assertEquals(List.of("a", "b"), e.getErrors());
        assertEquals(List.of("c"), new ValidationException("c").getErrors());
        assertThrows(UnsupportedOperationException.class, () -> e.getErrors().add("x"));
    }
}
