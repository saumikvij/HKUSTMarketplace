package hkust.marketplace.service;

import hkust.marketplace.data.DataStore;
import hkust.marketplace.model.Buyer;
import hkust.marketplace.model.Role;
import hkust.marketplace.model.User;
import hkust.marketplace.util.CredentialValidator;
import hkust.marketplace.util.Ids;
import hkust.marketplace.util.MarketplaceException;
import hkust.marketplace.util.PasswordHasher;
import hkust.marketplace.util.ValidationException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Buyer registration and role-checked login (Tasks 1.1 and 1.2). */
public class AuthService {
    private final DataStore store;
    private final Clock clock;

    /**
     * @param store data store
     * @param clock clock used for registration time
     */
    public AuthService(DataStore store, Clock clock) {
        this.store = store;
        this.clock = clock;
    }

    /**
     * Registers a new buyer (Task 1.1).
     * @param username unique username (letters, digits, underscore)
     * @param fullName full name, not blank, at most 50 characters
     * @param password password meeting the team rules
     * @param confirmPassword must equal the password exactly
     * @param residenceHall optional residence hall or "Off-campus"; blank means not given
     * @return the new buyer
     * @throws ValidationException listing every rule broken, including "Username is already taken"
     */
    public Buyer registerBuyer(String username, String fullName, String password, String confirmPassword,
                               String residenceHall) {
        List<String> errors = new ArrayList<>();
        if (isBlank(username)) {
            errors.add("Username is required");
        } else if (!CredentialValidator.isValidUsername(username)) {
            errors.add("Username can only contain letters, digits and underscores");
        }
        if (isBlank(fullName)) {
            errors.add("Full name is required");
        } else if (!CredentialValidator.isValidFullName(fullName)) {
            errors.add("Full name must be at most " + CredentialValidator.MAX_FULL_NAME_LENGTH + " characters");
        }
        errors.addAll(CredentialValidator.passwordProblems(password));
        if (confirmPassword == null || confirmPassword.isEmpty()) {
            errors.add("Please confirm the password");
        } else if (password != null && !password.equals(confirmPassword)) {
            errors.add("Passwords do not match");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        synchronized (store) {
            if (store.findUserByUsername(username).isPresent()) {
                throw new ValidationException("Username is already taken");
            }
            String salt = PasswordHasher.newSalt();
            Buyer buyer = new Buyer(Ids.next("U"), username.trim(), fullName.trim(),
                    PasswordHasher.hash(password, salt), salt, LocalDateTime.now(clock),
                    isBlank(residenceHall) ? null : residenceHall.trim());
            store.putUser(buyer);
            new NotificationService(store, clock).notifyAdmins(hkust.marketplace.model.NotificationCategory.ACCOUNT,
                    "Buyer registered", buyer.getUsername() + " registered a buyer account.", buyer.getId());
            store.commit();
            return buyer;
        }
    }

    /**
     * Logs in to one portal (Task 1.2). Only accounts of that portal's role are accepted.
     * @param username username, matched ignoring case
     * @param password password
     * @param portal the portal being logged in to
     * @return the logged-in user
     * @throws ValidationException if a field is empty or the credentials are wrong
     * @throws MarketplaceException if the account belongs to another portal or is deactivated
     */
    public User login(String username, String password, Role portal) {
        if (isBlank(username) || password == null || password.isEmpty()) {
            throw new ValidationException("Username and password are both required");
        }
        User user = store.findUserByUsername(username).orElse(null);
        if (user == null || user.getStatus() == hkust.marketplace.model.AccountStatus.DELETED || !PasswordHasher.matches(password, user.getSalt(), user.getPasswordHash())) {
            throw new ValidationException("Invalid username or password");
        }
        if (user.getRole() != portal) {
            String role = user.getRole().name().toLowerCase(Locale.ROOT);
            throw new MarketplaceException("This is a " + role + " account. Please use the " + role
                    + " login screen.");
        }
        if (!user.isActive()) {
            throw new MarketplaceException("This account is deactivated. Please contact the admin.");
        }
        return user;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
