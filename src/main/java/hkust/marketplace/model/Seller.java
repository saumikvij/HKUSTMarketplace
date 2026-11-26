package hkust.marketplace.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** A student or staff member using the seller portal (Task 2). */
public class Seller extends User {
    private static final long serialVersionUID = 1L;

    private String bio;
    private final List<String> meetupLocations = new ArrayList<>();

    /**
     * Creates a seller.
     * @param id unique id
     * @param username unique username
     * @param fullName display name
     * @param passwordHash salted password hash
     * @param salt salt
     * @param registeredAt registration time
     * @param bio optional bio, may be null
     */
    public Seller(String id, String username, String fullName, String passwordHash, String salt,
                  LocalDateTime registeredAt, String bio) {
        super(id, username, fullName, passwordHash, salt, registeredAt);
        this.bio = bio;
    }

    @Override public Role getRole() { return Role.SELLER; }

    /** @return optional bio */
    public String getBio() { return bio; }
    /** @param bio new bio */
    public void setBio(String bio) { this.bio = bio; }
    /** @return preferred meet-up locations (modifiable) */
    public List<String> getMeetupLocations() { return meetupLocations; }
}
