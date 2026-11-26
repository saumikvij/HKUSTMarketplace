package hkust.marketplace.ui;

import hkust.marketplace.model.ListingStatus;
import hkust.marketplace.model.Notification;
import hkust.marketplace.util.MarketplaceException;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Seller notification board with links to the affected seller records (Task 2.7). */
final class SellerNotificationsView implements Page {
    private final SellerPortal portal;
    private final VBox root = new VBox(12);
    private final ListView<Notification> list = SellerInputs.list("No notifications yet.", notification ->
            (notification.isRead() ? "" : "● ") + notification.getTitle() + "\n" + category(notification)
                    + " · " + Ui.time(notification.getCreatedAt()) + "\n" + notification.getContent());
    private final ComboBox<String> filter = new ComboBox<>();
    private final CheckBox unread = new CheckBox("Unread only");

    SellerNotificationsView(SellerPortal portal) {
        this.portal = portal;
        filter.getItems().addAll("All categories", "Listings", "Requests", "Messages", "Reviews", "Deals", "Announcements", "Account");
        filter.getSelectionModel().selectFirst();
        filter.setOnAction(event -> refresh());
        unread.setOnAction(event -> refresh());
        Button open = SellerInputs.primary("Open", this::open);
        open.disableProperty().bind(list.getSelectionModel().selectedItemProperty().isNull());
        Button markAll = new Button("Mark all as read");
        markAll.setOnAction(event -> {
            try {
                portal.market().notifications().markAllRead(portal.me());
                refresh();
            } catch (RuntimeException exception) {
                Ui.error(exception);
            }
        });
        list.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && !open.isDisabled()) open.fire();
        });
        root.setPadding(new Insets(20));
        root.getChildren().addAll(Ui.title("Notifications"), new HBox(8, filter, unread, open, markAll), list);
    }

    private static String category(Notification notification) {
        return switch (notification.getCategory()) {
            case LISTING_UPDATES -> "Listings";
            case BORROWING, DEALS -> switch (notification.getTitle()) {
                case "New offer", "New borrow request", "New claim" -> "Requests";
                default -> "Deals";
            };
            case MESSAGES -> "Messages";
            case REVIEWS, MODERATION -> "Reviews";
            case ANNOUNCEMENTS -> "Announcements";
            case ACCOUNT -> "Account";
        };
    }

    private void open() {
        Notification item = list.getSelectionModel().getSelectedItem();
        try {
            portal.market().notifications().markRead(portal.me(), item.getId());
            refresh();
            Ui.info(item.getTitle(), item.getContent());
            if (item.getRelatedId() != null) openRecord(item);
        } catch (RuntimeException exception) {
            Ui.error(exception);
        }
    }

    private void openRecord(Notification item) {
        String id = item.getRelatedId();
        switch (category(item)) {
            case "Listings" -> {
                var listing = portal.market().sellerListings().requireOwned(portal.me(), id);
                if (listing.getStatus() == ListingStatus.REMOVED) throw unavailable();
                var page = new SellerListingsView(portal);
                portal.show(page);
                page.select(id);
                page.view(listing);
            }
            case "Requests", "Deals" -> {
                var page = new SellerRequestsView(portal, null);
                portal.show(page);
                if (!page.select(id)) throw unavailable();
            }
            case "Messages" -> ChatWindow.openThread(portal.app(), null,
                    portal.market().chat().requireThread(portal.me(), id), null);
            case "Reviews" -> {
                var page = new SellerReviewsView(portal);
                portal.show(page);
                if (!page.select(id)) throw unavailable();
            }
            default -> { }
        }
    }

    private MarketplaceException unavailable() {
        return new MarketplaceException("The related record is unavailable.");
    }

    @Override public Parent node() { return root; }

    @Override public void refresh() {
        Notification selected = list.getSelectionModel().getSelectedItem();
        var items = portal.market().notifications().forUser(portal.me(), null, unread.isSelected()).stream()
                .filter(item -> "All categories".equals(filter.getValue()) || category(item).equals(filter.getValue()))
                .toList();
        list.getItems().setAll(items);
        if (selected != null) items.stream().filter(item -> item.getId().equals(selected.getId())).findFirst()
                .ifPresent(item -> list.getSelectionModel().select(item));
    }
}
