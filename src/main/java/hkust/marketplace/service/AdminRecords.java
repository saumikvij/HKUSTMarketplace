package hkust.marketplace.service;

import hkust.marketplace.data.DataStore;
import hkust.marketplace.model.*;
import hkust.marketplace.util.ValidationException;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

/** Read models and filters for admin oversight. Dates are inclusive and use the application's local clock. */
public final class AdminRecords {
    private final Marketplace market;
    private final String admin;
    public AdminRecords(Marketplace market, String admin) {
        this.market = market;
        this.admin = admin;
        market.admin().requireAdmin(admin);
    }
    private DataStore data() { market.admin().requireAdmin(admin); return market.store(); }
    public String username(String id) { return data().findUser(id).map(User::getUsername).orElse("Unknown"); }
    public Listing listing(String id) { return data().findListing(id).orElseThrow(); }
    public String reviewItem(Review r) {
        return data().findDeal(r.getDealId()).map(d -> listing(d.getListingId()).getTitle()).orElse("Unknown listing");
    }
    private static boolean matches(String query, String value) {
        return query == null || value.toLowerCase(Locale.ROOT).contains(query.strip().toLowerCase(Locale.ROOT));
    }
    /** Inclusive date interval with optional boundaries. */
    public record Dates(LocalDate from, LocalDate until) {
        public Dates {
            if (from != null && until != null && from.isAfter(until)) throw new ValidationException("Start date must be on or before the end date.");
        }
        boolean contains(LocalDateTime value) {
            LocalDate day = value.toLocalDate();
            return (from == null || !day.isBefore(from)) && (until == null || !day.isAfter(until));
        }
    }
    public List<User> users(String query, Role role) {
        return data().users().stream().filter(u -> u.getRole() != Role.ADMIN && (role == null || u.getRole() == role))
                .filter(u -> matches(query, u.getUsername() + " " + u.getFullName())).toList();
    }
    public List<Listing> listings(boolean pending, String query, ListingStatus status) {
        return market.admin().listings(admin, pending).stream().filter(l -> status == null || status == l.getStatus())
                .filter(l -> matches(query, l.getTitle() + " " + username(l.getSellerId()))).toList();
    }
    public List<Report> reports(String id) {
        return data().reports().stream().filter(r -> r.getStatus() == ReportStatus.OPEN && (id == null || r.getListingId().equals(id)))
                .sorted(Comparator.comparing(Report::getReportedAt).reversed()).toList();
    }
    public enum Stage { ACTIVE, FINISHED, CLOSED }
    /** Returns the transactions currently displayed or exported. */
    public List<Deal> deals(String query, DealType type, Stage stage) {
        return data().deals().stream().filter(d -> type == null || type == d.getType())
                .filter(d -> stage == null || stage == (d.getStatus().isActive() ? Stage.ACTIVE :
                        d.getStatus().isFinished() ? Stage.FINISHED : Stage.CLOSED))
                .filter(d -> matches(query, listing(d.getListingId()).getTitle() + " " + username(d.getBuyerId())
                        + " " + username(listing(d.getListingId()).getSellerId())))
                .sorted(Comparator.comparing(Deal::getSubmittedAt).reversed()).toList();
    }
    public LocalDateTime start(Deal d) { return d.getBorrowStart() == null ? d.getSubmittedAt() : d.getBorrowStart(); }
    public LocalDateTime end(Deal d) { return d.getCompletedAt() == null ? d.getBorrowEnd() : d.getCompletedAt(); }
    /** Quotes fields and guards spreadsheet formula prefixes in user-entered values. */
    public String csv(List<Deal> shown) {
        data();
        StringBuilder out = new StringBuilder("Item title,Buyer,Seller,Type,Start,Return or completion,Status\r\n");
        for (Deal d : shown) {
            Listing l = listing(d.getListingId());
            List<String> row = List.of(l.getTitle(),username(d.getBuyerId()),username(l.getSellerId()),d.getType().label(),
                    start(d).toString(), Objects.toString(end(d), ""), d.getStatus().label());
            out.append(row.stream().map(AdminRecords::csvCell).collect(Collectors.joining(","))).append("\r\n");
        }
        return out.toString();
    }
    private static String csvCell(String s) {
        if (!s.isEmpty() && ("=+-@".indexOf(s.stripLeading().isEmpty() ? ' ' : s.stripLeading().charAt(0)) >= 0
                || "\t\r\n".indexOf(s.charAt(0)) >= 0)) s = "'" + s;
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
    public List<Review> ratings(String query, Integer score, Dates dates, boolean flaggedOnly) {
        return data().reviews().stream().filter(r -> !flaggedOnly || r.isFlagged())
                .filter(r -> score == null || r.getRating() == score).filter(r -> dates.contains(r.getPostedAt()))
                .filter(r -> matches(query, username(r.getBuyerId()) + " " + username(r.getSellerId())))
                .sorted(Comparator.comparing(Review::isFlagged).thenComparing(Review::getPostedAt).reversed()).toList();
    }
    public List<Notification> notifications(NotificationCategory category, Dates dates, boolean archive) {
        data();
        return market.notifications().forUser(admin, category, false).stream().filter(n -> n.isArchived() == archive)
                .filter(n -> dates.contains(n.getCreatedAt())).toList();
    }
}
