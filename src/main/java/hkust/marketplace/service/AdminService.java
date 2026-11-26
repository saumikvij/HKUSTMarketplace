package hkust.marketplace.service;

import hkust.marketplace.data.DataStore;
import hkust.marketplace.model.*;
import hkust.marketplace.util.*;
import java.time.LocalDateTime;
import java.util.*;
import static hkust.marketplace.model.NotificationCategory.*;

/** Administration of the same persistent records used by the Buyer and Seller services (tasks 3.2–3.8). */
public final class AdminService {
    private final Marketplace market;
    private final DataStore store;
    AdminService(Marketplace market) { this.market = market; this.store = market.store(); }

    /** Checks the role and current account state at the service boundary. */
    public Admin requireAdmin(String id) {
        User u = store.findUser(id).orElse(null);
        if (!(u instanceof Admin admin) || !admin.isActive()) throw new MarketplaceException("An active admin account is required.");
        return admin;
    }

    private LocalDateTime now() { return LocalDateTime.now(market.clock()); }
    private Listing listing(String id) {
        return store.findListing(id).orElseThrow(() -> new MarketplaceException("Listing no longer exists."));
    }
    private User account(String id) {
        User user = store.findUser(id).orElseThrow(() -> new MarketplaceException("Account not found."));
        if (user instanceof Admin || user.getStatus() == AccountStatus.DELETED)
            throw new MarketplaceException("Choose an existing buyer or seller.");
        return user;
    }
    private void audit(String admin, NotificationCategory category, String title, String detail, String related) {
        market.notifications().notifyAdmins(category, title, requireAdmin(admin).getUsername() + " — " + detail, related);
    }
    private void tell(String id, NotificationCategory category, String title, String detail, String related) {
        market.notifications().notify(id, category, title, detail, related);
    }

    /** Adds missing queue alerts for pending listings in an existing Buyer-only data file. */
    public void syncPendingAlerts(String admin) {
        synchronized (store) {
            requireAdmin(admin);
            Set<String> existing = new HashSet<>();
            for (Notification n : market.notifications().forUser(admin, LISTING_UPDATES, false)) {
                if (n.getRelatedId() != null) existing.add(n.getRelatedId());
            }
            boolean changed = false;
            for (Listing l : listings(admin, true)) {
                if (existing.add(l.getId())) {
                    tell(admin, LISTING_UPDATES, "Listing awaiting review", l.getTitle(), l.getId());
                    changed = true;
                }
            }
            if (changed) store.commit();
        }
    }

    /** Returns pending submissions only when requested, otherwise all listings, newest first. */
    public List<Listing> listings(String admin, boolean pending) {
        requireAdmin(admin);
        return store.listings().stream().filter(l -> !pending || l.getStatus() == ListingStatus.PENDING_REVIEW)
                .sorted(Comparator.comparing(Listing::getPostedAt).reversed()).toList();
    }

    /** Approves a pending submission or rejects it with a recorded reason and a seller notification. */
    public void review(String admin, String id, boolean approve, String reason) {
        synchronized (store) {
            requireAdmin(admin);
            Listing l = listing(id);
            if (l.getStatus() != ListingStatus.PENDING_REVIEW) throw new MarketplaceException("This submission has already changed.");
            if (approve) Access.requireSeller(store, l.getSellerId());
            String explanation = approve ? null : ListingForm.required(reason, "Rejection reason");
            l.setStatus(approve ? ListingStatus.LIVE : ListingStatus.REJECTED);
            l.setRejectionReason(explanation);
            l.setReviewedAt(now());
            tell(l.getSellerId(), LISTING_UPDATES, approve ? "Listing approved" : "Listing rejected",
                    l.getTitle() + (approve ? " is now live." : ": " + explanation), id);
            store.commit();
        }
    }

    /** Adds an already approved listing directly to the selected active seller's inventory. */
    public Listing addListing(String admin, String sellerId, ListingForm form) {
        synchronized (store) {
            requireAdmin(admin);
            Access.requireSeller(store, sellerId);
            Listing l = create(sellerId, form);
            l.setReviewedAt(now());
            l.setStatus(ListingStatus.LIVE);
            tell(sellerId, LISTING_UPDATES, "Listing added by admin", l.getTitle(), l.getId());
            store.commit();
            return l;
        }
    }

    /** Seller integration point: submits an item to the queue and alerts the administrators. */
    public Listing submitForReview(String seller, ListingForm form) {
        synchronized (store) {
            Access.requireSeller(store, seller);
            Listing l = create(seller, form);
            market.notifications().notifyAdmins(LISTING_UPDATES, "Listing awaiting review", l.getTitle(), l.getId());
            store.commit();
            return l;
        }
    }
    private Listing create(String seller, ListingForm f) {
        Listing l = new Listing(Ids.next("L"), seller, f.type(), f.title(), f.category(), f.condition(),
                f.description(), f.location(), now());
        f.apply(l);
        store.putListing(l);
        return l;
    }

    /** Applies an edit only if the displayed status is current. Deal type is fixed once a deal exists. */
    public void editListing(String admin, String id, ListingStatus expected, ListingForm f) {
        synchronized (store) {
            requireAdmin(admin);
            Listing l = listing(id);
            if (l.getStatus() != expected) throw new MarketplaceException("Status changed. Reopen the listing and review the latest details.");
            if (l.getType() != f.type() && store.deals().stream().anyMatch(d -> d.getListingId().equals(id)))
                throw new MarketplaceException("The listing type is referenced by existing deals.");
            f.apply(l);
            tell(l.getSellerId(), LISTING_UPDATES, "Listing edited by admin", l.getTitle(), id);
            store.commit();
        }
    }

    /** Removes a live item, cancels active requests, informs their participants and resolves open reports. */
    public void takeDown(String admin, String id, String reason) {
        synchronized (store) {
            requireAdmin(admin);
            Listing l = listing(id);
            if (l.getStatus() != ListingStatus.LIVE) throw new MarketplaceException("Only a live listing can be taken down.");
            String why = ListingForm.required(reason, "Take-down reason");
            remove(l, why);
            for (Deal d : store.deals()) {
                if (d.getListingId().equals(id) && d.getStatus().isOpenRequest()) cancel(d, l, why, true, false);
            }
            tell(l.getSellerId(), LISTING_UPDATES, "Listing taken down", l.getTitle() + ": " + why, id);
            audit(admin, MODERATION, "Listing taken down", l.getTitle() + ": " + why, id);
            store.commit();
        }
    }
    private void remove(Listing l, String why) {
        l.setStatus(ListingStatus.REMOVED);
        l.setRemovalReason(why);
        closeReports(l.getId(), ReportStatus.RESOLVED);
    }
    private boolean closeReports(String listingId, ReportStatus status) {
        boolean changed = false;
        for (Report r : store.reports()) {
            if (r.getListingId().equals(listingId) && r.getStatus() == ReportStatus.OPEN) {
                r.setStatus(status);
                changed = true;
            }
        }
        return changed;
    }
    /** Dismisses all outstanding reports on a listing without changing its visibility. */
    public void dismissReports(String admin, String id) {
        synchronized (store) {
            requireAdmin(admin);
            if (!closeReports(id, ReportStatus.DISMISSED)) throw new MarketplaceException("No open reports for this listing.");
            audit(admin, MODERATION, "Reports dismissed", listing(id).getTitle(), id);
            store.commit();
        }
    }
    private void cancel(Deal d, Listing l, String why, boolean buyer, boolean seller) {
        boolean reserved = l.getStatus() == ListingStatus.RESERVED &&
                (d.getStatus() == DealStatus.ACCEPTED || d.getType() == DealType.CLAIM);
        d.setStatus(DealStatus.CANCELLED, now());
        if (reserved) l.setStatus(ListingStatus.LIVE);
        String detail = l.getTitle() + ": " + why;
        if (buyer) tell(d.getBuyerId(), DEALS, "Transaction cancelled", detail, d.getId());
        if (seller) tell(l.getSellerId(), DEALS, "Transaction cancelled", detail, d.getId());
    }

    /** Creates an active buyer or seller using the project's shared credential rules. */
    public User addUser(String admin, Role role, String username, String name, String password, String confirmation) {
        synchronized (store) {
            requireAdmin(admin);
            if (role != Role.BUYER && role != Role.SELLER) throw new ValidationException("Choose buyer or seller.");
            if (!CredentialValidator.isValidUsername(username)) throw new ValidationException("Use letters, digits and underscores for the username.");
            name = fullName(name);
            List<String> errors = CredentialValidator.passwordProblems(password);
            if (!errors.isEmpty()) throw new ValidationException(errors);
            if (!password.equals(confirmation)) throw new ValidationException("Passwords do not match.");
            if (store.findUserByUsername(username).isPresent()) throw new ValidationException("Username is taken.");
            String id = Ids.next("U"), salt = PasswordHasher.newSalt();
            String hash = PasswordHasher.hash(password, salt);
            User user = role == Role.BUYER ? new Buyer(id,username.trim(),name,hash,salt,now(),null)
                    : new Seller(id,username.trim(),name,hash,salt,now(),null);
            store.putUser(user);
            audit(admin, ACCOUNT, "Account created", user.getUsername(), id);
            store.commit();
            return user;
        }
    }
    private String fullName(String name) {
        if (!CredentialValidator.isValidFullName(name)) throw new ValidationException("Enter a full name of 1–50 characters.");
        return name.trim();
    }
    /** Edits a buyer's or seller's display name. */
    public void rename(String admin, String id, String name) {
        synchronized (store) {
            requireAdmin(admin);
            User user = account(id);
            user.setFullName(fullName(name));
            audit(admin, ACCOUNT, "Account updated", user.getUsername(), id);
            store.commit();
        }
    }
    /** Deactivation hides listings through the existing browse rules. Reactivation restores eligibility. */
    public void setActive(String admin, String id, boolean active) {
        synchronized (store) {
            requireAdmin(admin);
            User user = account(id);
            if (user.isActive() == active) throw new MarketplaceException("Account is already in this state.");
            user.setStatus(active ? AccountStatus.ACTIVE : AccountStatus.DEACTIVATED);
            audit(admin, ACCOUNT, active ? "Account reactivated" : "Account deactivated", user.getUsername(), id);
            store.commit();
        }
    }
    /** Anonymises an account, cancels its commitments and keeps historical ids. An outstanding loan blocks deletion. */
    public void deleteUser(String admin, String id) {
        synchronized (store) {
            requireAdmin(admin);
            User user = account(id);
            List<Deal> involved = store.deals().stream().filter(d -> d.getBuyerId().equals(id)
                    || listing(d.getListingId()).getSellerId().equals(id)).toList();
            if (involved.stream().anyMatch(d -> d.getStatus() == DealStatus.BORROWED))
                throw new MarketplaceException("Return outstanding loans before deleting this account.");
            for (Deal d : involved) {
                if (d.getStatus().isActive()) {
                    Listing l = listing(d.getListingId());
                    cancel(d, l, "Account deleted", !id.equals(d.getBuyerId()), !id.equals(l.getSellerId()));
                }
            }
            for (Listing l : store.listings()) {
                if (id.equals(l.getSellerId()) && (l.getStatus() == ListingStatus.PENDING_REVIEW || l.getStatus() == ListingStatus.LIVE))
                    remove(l, "Seller account deleted");
            }
            user.anonymise("deleted-" + UUID.randomUUID());
            audit(admin, ACCOUNT, "Account deleted", "Historical account ID " + id, id);
            store.commit();
        }
    }

    /** Reauthenticates before applying profile changes; returns true when the UI must sign out. */
    public boolean profile(String admin, String name, String current, String password, String confirm) {
        synchronized (store) {
            Admin user = requireAdmin(admin);
            String nextName = fullName(name);
            market.auth().login(user.getUsername(), current, Role.ADMIN);
            boolean changePassword = !(password == null || password.isEmpty()) || !(confirm == null || confirm.isEmpty());
            if (!changePassword && nextName.equals(user.getFullName())) throw new ValidationException("There are no changes to save.");
            if (changePassword) {
                List<String> errors = CredentialValidator.passwordProblems(password);
                if (!errors.isEmpty()) throw new ValidationException(errors);
                if (!password.equals(confirm)) throw new ValidationException("New passwords do not match.");
                if (password.equals(current)) throw new ValidationException("Choose a different password.");
                String salt = PasswordHasher.newSalt();
                user.setPassword(PasswordHasher.hash(password, salt), salt);
            }
            user.setFullName(nextName);
            audit(admin, ACCOUNT, "Admin profile updated", user.getUsername(), admin);
            store.commit();
            return changePassword;
        }
    }

    /** Changes public visibility. Existing Buyer summaries compute averages from visible reviews. */
    public void hideReview(String admin, String id, boolean hidden) {
        synchronized (store) {
            requireAdmin(admin);
            Review r = store.reviews().stream().filter(x -> x.getId().equals(id)).findFirst()
                    .orElseThrow(() -> new MarketplaceException("Review not found."));
            r.setHidden(hidden);
            audit(admin, MODERATION, hidden ? "Rating hidden" : "Rating restored", "Review " + id, id);
            store.commit();
        }
    }

    /** Archives or restores an owned admin notification, retaining its timestamp and audit record. */
    public void archive(String admin, String id, boolean archived) {
        synchronized (store) {
            requireAdmin(admin);
            Notification n = market.notifications().forUser(admin, null, false).stream()
                    .filter(x -> x.getId().equals(id)).findFirst().orElseThrow(() -> new MarketplaceException("Notification not found."));
            n.setArchived(archived);
            store.commit();
        }
    }
}
