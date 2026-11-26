package hkust.marketplace.service;

import hkust.marketplace.data.DataStore;
import hkust.marketplace.model.Buyer;
import hkust.marketplace.model.Seller;
import hkust.marketplace.model.User;
import hkust.marketplace.util.CredentialValidator;
import hkust.marketplace.util.MarketplaceException;
import hkust.marketplace.util.PasswordHasher;
import hkust.marketplace.util.ValidationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Buyer profile editing (Task 1.7). The username cannot be changed. */
public class ProfileService {
    private final DataStore store;

    /** @param store data store */
    public ProfileService(DataStore store) {
        this.store = store;
    }

    /**
     * Updates the buyer's full name and residence hall.
     * @param buyerId buyer
     * @param fullName new full name
     * @param residenceHall new residence hall; blank means not given
     * @return the updated buyer
     * @throws ValidationException if the full name is blank or too long
     * @throws MarketplaceException if nothing changed
     */
    public Buyer updateDetails(String buyerId, String fullName, String residenceHall) {
        Buyer buyer = Access.requireBuyer(store, buyerId);
        if (fullName == null || fullName.isBlank()) {
            throw new ValidationException("Full name cannot be empty");
        }
        if (!CredentialValidator.isValidFullName(fullName)) {
            throw new ValidationException("Full name must be at most "
                    + CredentialValidator.MAX_FULL_NAME_LENGTH + " characters");
        }
        String name = fullName.trim();
        String hall = residenceHall == null || residenceHall.isBlank() ? null : residenceHall.trim();
        if (name.equals(buyer.getFullName()) && Objects.equals(hall, buyer.getResidenceHall())) {
            throw new MarketplaceException("There are no changes to save.");
        }
        buyer.setFullName(name);
        buyer.setResidenceHall(hall);
        store.commit();
        return buyer;
    }

    /** Snapshot of the editable seller profile fields for stale form detection. */
    public record SellerDetails(String fullName, String bio, List<String> locations) {
        public SellerDetails {
            locations = List.copyOf(locations);
        }
    }

    /** Reads the current profile of an active seller. */
    public SellerDetails sellerDetails(String sellerId) {
        synchronized (store) {
            return detailsOf(Access.requireSeller(store, sellerId));
        }
    }

    /** Saves seller details if the profile still matches the displayed snapshot. */
    public SellerDetails updateSeller(String sellerId, SellerDetails expected, String fullName,
                                      String bio, List<String> locations) {
        List<String> errors = new ArrayList<>();
        if (fullName == null || fullName.isBlank() || !CredentialValidator.isValidFullName(fullName)) {
            errors.add("Full name is required and must be at most 50 characters");
        }
        if (!MeetupLocations.valid(locations)) {
            errors.add("Select meet-up locations from the HKUST list");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        synchronized (store) {
            Seller seller = Access.requireSeller(store, sellerId);
            if (!detailsOf(seller).equals(expected)) {
                throw new MarketplaceException("Your profile changed while you were editing. Reload it and try again.");
            }
            return store.transact(() -> {
                seller.setFullName(fullName.trim());
                seller.setBio(bio == null || bio.isBlank() ? null : bio.trim());
                seller.getMeetupLocations().clear();
                if (locations != null) {
                    seller.getMeetupLocations().addAll(locations.stream().distinct().toList());
                }
                return detailsOf(seller);
            });
        }
    }

    private static SellerDetails detailsOf(Seller seller) {
        return new SellerDetails(seller.getFullName(), seller.getBio(), seller.getMeetupLocations());
    }

    /**
     * Changes a user's password after checking the current one.
     * @param userId user
     * @param currentPassword current password
     * @param newPassword new password meeting the team rules
     * @param confirmPassword must equal the new password
     * @throws ValidationException if the current password is wrong or the new one breaks a rule
     */
    public void changePassword(String userId, String currentPassword, String newPassword, String confirmPassword) {
        synchronized (store) {
            changeCurrentPassword(userId, currentPassword, newPassword, confirmPassword);
        }
    }

    private void changeCurrentPassword(String userId, String currentPassword, String newPassword,
                                       String confirmPassword) {
        User user = store.findUser(userId).filter(User::isActive)
                .orElseThrow(() -> new MarketplaceException("Active account not found."));
        if (!PasswordHasher.matches(currentPassword == null ? "" : currentPassword, user.getSalt(),
                user.getPasswordHash())) {
            throw new ValidationException("Current password is incorrect");
        }
        List<String> errors = new ArrayList<>(CredentialValidator.passwordProblems(newPassword));
        if (newPassword != null && !newPassword.equals(confirmPassword)) {
            errors.add("New passwords do not match");
        }
        if (newPassword != null && newPassword.equals(currentPassword)) {
            errors.add("New password must be different from the current password");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        String salt = PasswordHasher.newSalt();
        String hash = PasswordHasher.hash(newPassword, salt);
        store.transact(() -> {
            user.setPassword(hash, salt);
            return null;
        });
    }
}
