package hkust.marketplace.model;

import java.time.LocalDateTime;

/** The seeded moderator account (Task 3). There is no admin registration. */
public class Admin extends User {
    private static final long serialVersionUID = 1L;

    /**
     * Creates the admin account.
     * @param id unique id
     * @param username username
     * @param fullName display name
     * @param passwordHash salted password hash
     * @param salt salt
     * @param registeredAt creation time
     */
    public Admin(String id, String username, String fullName, String passwordHash, String salt,
                 LocalDateTime registeredAt) {
        super(id, username, fullName, passwordHash, salt, registeredAt);
    }

    @Override public Role getRole() { return Role.ADMIN; }
}
