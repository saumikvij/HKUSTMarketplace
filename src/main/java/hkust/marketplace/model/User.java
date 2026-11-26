package hkust.marketplace.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * An account in HKUST Marketplace. Every user is exactly one of {@link Buyer}, {@link Seller} or {@link Admin}.
 * Passwords are never stored in plain text: only a salted PBKDF2 hash is kept.
 */
public abstract class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String username;
    private String fullName;
    private String passwordHash;
    private String salt;
    private AccountStatus status = AccountStatus.ACTIVE;
    private final LocalDateTime registeredAt;

    /**
     * Creates a user.
     * @param id unique id
     * @param username unique username (letters, digits, underscore)
     * @param fullName display name
     * @param passwordHash salted password hash
     * @param salt the salt used for the hash
     * @param registeredAt registration time
     */
    protected User(String id, String username, String fullName, String passwordHash, String salt,
                   LocalDateTime registeredAt) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.registeredAt = registeredAt;
    }

    /** @return the account type */
    public abstract Role getRole();

    /** @return unique id */
    public String getId() { return id; }
    /** @return username (cannot be changed) */
    public String getUsername() { return username; }
    /** @return display name */
    public String getFullName() { return fullName; }
    /** @param fullName new display name */
    public void setFullName(String fullName) { this.fullName = fullName; }
    /** @return salted password hash */
    public String getPasswordHash() { return passwordHash; }
    /** @return salt used for the password hash */
    public String getSalt() { return salt; }
    /**
     * Replaces the stored password hash and salt.
     * @param passwordHash new hash
     * @param salt new salt
     */
    public void setPassword(String passwordHash, String salt) {
        this.passwordHash = passwordHash;
        this.salt = salt;
    }
    /** @return account status */
    public AccountStatus getStatus() { return status; }
    /** @param status new account status */
    public void setStatus(AccountStatus status) { this.status = status; }
    /** @return true if the account can log in and act */
    public boolean isActive() { return status == AccountStatus.ACTIVE; }
    /** @return registration time */
    public LocalDateTime getRegisteredAt() { return registeredAt; }

    @Override public String toString() { return fullName + " (@" + username + ")"; }
}
