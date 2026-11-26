package hkust.marketplace.model;

import java.time.LocalDateTime;

/** A student or staff member using the buyer portal (Task 1). */
public class Buyer extends User {
    private static final long serialVersionUID = 1L;

    private String residenceHall;

    /**
     * Creates a buyer.
     * @param id unique id
     * @param username unique username
     * @param fullName display name
     * @param passwordHash salted password hash
     * @param salt salt
     * @param registeredAt registration time
     * @param residenceHall residence hall or "Off-campus"; may be null
     */
    public Buyer(String id, String username, String fullName, String passwordHash, String salt,
                 LocalDateTime registeredAt, String residenceHall) {
        super(id, username, fullName, passwordHash, salt, registeredAt);
        this.residenceHall = residenceHall;
    }

    @Override public Role getRole() { return Role.BUYER; }

    /** @return residence hall, "Off-campus", or null if not given (used for meet-up suggestions) */
    public String getResidenceHall() { return residenceHall; }
    /** @param residenceHall new residence hall, or null */
    public void setResidenceHall(String residenceHall) { this.residenceHall = residenceHall; }
}
