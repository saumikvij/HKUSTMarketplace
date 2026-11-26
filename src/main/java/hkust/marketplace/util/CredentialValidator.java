package hkust.marketplace.util;

import java.util.ArrayList;
import java.util.List;

/**
 * The team's shared validation rules for usernames, full names and passwords (Activity 1, Task 1 Appendix A).
 * <ul>
 *   <li>Username: letters, digits and underscore only; unique across all roles, ignoring case.</li>
 *   <li>Full name: not blank, at most 50 characters.</li>
 *   <li>Password: at least 8 characters with an uppercase letter, a lowercase letter and a digit.</li>
 * </ul>
 */
public final class CredentialValidator {
    /** Minimum password length. */
    public static final int MIN_PASSWORD_LENGTH = 8;
    /** Maximum full-name length. */
    public static final int MAX_FULL_NAME_LENGTH = 50;

    private CredentialValidator() { }

    /**
     * @param username the username as typed
     * @return true if non-blank and only letters, digits and underscores (after trimming)
     */
    public static boolean isValidUsername(String username) {
        if (username == null) {
            return false;
        }
        String trimmed = username.trim();
        return !trimmed.isEmpty() && trimmed.matches("[A-Za-z0-9_]+");
    }

    /**
     * @param fullName the full name as typed
     * @return true if not blank and at most 50 characters after trimming
     */
    public static boolean isValidFullName(String fullName) {
        if (fullName == null) {
            return false;
        }
        String trimmed = fullName.trim();
        return !trimmed.isEmpty() && trimmed.length() <= MAX_FULL_NAME_LENGTH;
    }

    /**
     * Lists every password rule that is broken.
     * @param password the password as typed (never trimmed)
     * @return the broken rules; empty if the password is acceptable
     */
    public static List<String> passwordProblems(String password) {
        List<String> problems = new ArrayList<>();
        if (password == null || password.isEmpty()) {
            problems.add("Password is required");
            return problems;
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            problems.add("Password needs at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        if (!password.matches(".*[A-Z].*")) {
            problems.add("Password needs at least one uppercase letter");
        }
        if (!password.matches(".*[a-z].*")) {
            problems.add("Password needs at least one lowercase letter");
        }
        if (!password.matches(".*[0-9].*")) {
            problems.add("Password needs at least one digit");
        }
        return problems;
    }

    /**
     * @param password the password as typed
     * @return true if it meets every rule
     */
    public static boolean isStrongPassword(String password) {
        return passwordProblems(password).isEmpty();
    }
}
