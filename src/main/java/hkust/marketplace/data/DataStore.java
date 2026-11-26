package hkust.marketplace.data;

import hkust.marketplace.model.ChatThread;
import hkust.marketplace.model.Deal;
import hkust.marketplace.model.Listing;
import hkust.marketplace.model.Notification;
import hkust.marketplace.model.Report;
import hkust.marketplace.model.Review;
import hkust.marketplace.model.User;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Supplier;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory store for every record, saved to one file so data survives restarts.
 * Services lock on the store ({@code synchronized (store)}) for actions that must be atomic,
 * such as two buyers claiming the same giveaway.
 */
public class DataStore implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Map<String, User> users = new LinkedHashMap<>();
    private final Map<String, Listing> listings = new LinkedHashMap<>();
    private final Map<String, Deal> deals = new LinkedHashMap<>();
    private final Map<String, Review> reviews = new LinkedHashMap<>();
    private final Map<String, ChatThread> threads = new LinkedHashMap<>();
    private final Map<String, Notification> notifications = new LinkedHashMap<>();
    private final Map<String, Report> reports = new LinkedHashMap<>();

    private transient Path file;

    /** Creates an empty in-memory store that is never saved (used by tests). */
    public DataStore() { }

    /**
     * Loads the store from a file, or returns an empty store bound to that file if it does not exist.
     * @param file data file
     * @return the loaded store
     * @throws IOException if the file exists but cannot be read
     */
    public static DataStore load(Path file) throws IOException {
        DataStore store;
        if (Files.exists(file)) {
            try (InputStream in = Files.newInputStream(file); ObjectInputStream ois = new ObjectInputStream(in)) {
                store = (DataStore) ois.readObject();
            } catch (ClassNotFoundException | ClassCastException e) {
                throw new IOException("Data file is not a marketplace data file: " + file, e);
            }
        } else {
            store = new DataStore();
        }
        store.file = file;
        return store;
    }

    /**
     * Saves every record to the bound file (written to a temporary file first, then moved into place).
     * Does nothing for a store without a file.
     * @throws IOException if writing fails
     */
    public synchronized void save() throws IOException {
        if (file == null) {
            return;
        }
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        try (OutputStream out = Files.newOutputStream(tmp); ObjectOutputStream oos = new ObjectOutputStream(out)) {
            oos.writeObject(this);
        }
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Saves the store, turning an I/O failure into an unchecked exception (used after every change).
     * @throws java.io.UncheckedIOException if the data cannot be written
     */
    public void commit() {
        try {
            save();
        } catch (IOException e) {
            throw new java.io.UncheckedIOException("Could not save data: " + e.getMessage(), e);
        }
    }

    /**
     * Saves a group of changes together. A failed save restores the previous in-memory records.
     * The callback must not call commit itself.
     */
    public synchronized <T> T transact(Supplier<T> change) {
        if (file == null) {
            return change.get();
        }
        DataStore before = snapshot();
        try {
            T result = change.get();
            commit();
            return result;
        } catch (UncheckedIOException exception) {
            restore(users, before.users);
            restore(listings, before.listings);
            restore(deals, before.deals);
            restore(reviews, before.reviews);
            restore(threads, before.threads);
            restore(notifications, before.notifications);
            restore(reports, before.reports);
            throw exception;
        }
    }

    private DataStore snapshot() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try {
            try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
                out.writeObject(this);
            }
            try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
                return (DataStore) in.readObject();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not prepare the data update.", e);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Could not prepare the data update.", e);
        }
    }

    private static <T> void restore(Map<String, T> target, Map<String, T> previous) {
        target.clear();
        target.putAll(previous);
    }

    /** @return the bound data file, or null for an in-memory store */
    public Path getFile() { return file; }

    /** @return true if nothing has been stored yet */
    public synchronized boolean isEmpty() { return users.isEmpty(); }

    // ---------- users ----------

    /** @param user user to add or replace */
    public synchronized void putUser(User user) { users.put(user.getId(), user); }

    /**
     * @param id user id
     * @return the user, if any
     */
    public synchronized Optional<User> findUser(String id) { return Optional.ofNullable(users.get(id)); }

    /**
     * Finds a user by username, ignoring case and outer spaces (usernames are unique across all roles).
     * @param username username
     * @return the user, if any
     */
    public synchronized Optional<User> findUserByUsername(String username) {
        if (username == null) {
            return Optional.empty();
        }
        String key = username.trim().toLowerCase(Locale.ROOT);
        return users.values().stream()
                .filter(u -> u.getUsername().toLowerCase(Locale.ROOT).equals(key))
                .findFirst();
    }

    /** @return a snapshot of all users */
    public synchronized List<User> users() { return new ArrayList<>(users.values()); }

    // ---------- listings ----------

    /** @param listing listing to add or replace */
    public synchronized void putListing(Listing listing) { listings.put(listing.getId(), listing); }

    /**
     * @param id listing id
     * @return the listing, if any
     */
    public synchronized Optional<Listing> findListing(String id) { return Optional.ofNullable(listings.get(id)); }

    /** @return a snapshot of all listings */
    public synchronized List<Listing> listings() { return new ArrayList<>(listings.values()); }

    // ---------- deals ----------

    /** @param deal deal to add or replace */
    public synchronized void putDeal(Deal deal) { deals.put(deal.getId(), deal); }

    /**
     * @param id deal id
     * @return the deal, if any
     */
    public synchronized Optional<Deal> findDeal(String id) { return Optional.ofNullable(deals.get(id)); }

    /** @return a snapshot of all deals */
    public synchronized List<Deal> deals() { return new ArrayList<>(deals.values()); }

    // ---------- reviews ----------

    /** @param review review to add or replace */
    public synchronized void putReview(Review review) { reviews.put(review.getId(), review); }

    /** @return a snapshot of all reviews */
    public synchronized List<Review> reviews() { return new ArrayList<>(reviews.values()); }

    // ---------- chat ----------

    /** @param thread thread to add or replace */
    public synchronized void putThread(ChatThread thread) { threads.put(thread.getId(), thread); }

    /**
     * @param id thread id
     * @return the thread, if any
     */
    public synchronized Optional<ChatThread> findThread(String id) { return Optional.ofNullable(threads.get(id)); }

    /** @return a snapshot of all chat threads */
    public synchronized List<ChatThread> threads() { return new ArrayList<>(threads.values()); }

    // ---------- notifications ----------

    /** @param notification notification to add or replace */
    public synchronized void putNotification(Notification notification) {
        notifications.put(notification.getId(), notification);
    }

    /** @return a snapshot of all notifications */
    public synchronized List<Notification> notifications() { return new ArrayList<>(notifications.values()); }

    // ---------- reports ----------

    /** @param report report to add or replace */
    public synchronized void putReport(Report report) { reports.put(report.getId(), report); }

    /** @return a snapshot of all reports */
    public synchronized List<Report> reports() { return new ArrayList<>(reports.values()); }
}
