package hkust.marketplace.ui;

import hkust.marketplace.model.Notification;
import hkust.marketplace.model.NotificationCategory;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Notifications (Task 1.8): deal, chat, borrowing and review updates with filters and read state. */
public class NotificationsView implements Page {
    private final AppContext app;
    private final BuyerPortal portal;
    private final VBox root = new VBox(12);
    private final ListView<Notification> list = new ListView<>();
    private final ComboBox<NotificationCategory> category = new ComboBox<>();
    private final CheckBox unreadOnly = new CheckBox("Unread only");
    private final Label summary = Ui.muted("");

    /**
     * @param app app context
     * @param portal portal shell
     */
    public NotificationsView(AppContext app, BuyerPortal portal) {
        this.app = app;
        this.portal = portal;
        category.getItems().add(null);
        category.getItems().addAll(NotificationCategory.DEALS, NotificationCategory.MESSAGES,
                NotificationCategory.BORROWING, NotificationCategory.LISTING_UPDATES, NotificationCategory.REVIEWS,
                NotificationCategory.ACCOUNT, NotificationCategory.ANNOUNCEMENTS);
        category.setPromptText("All categories");
        category.setOnAction(e -> refresh());
        unreadOnly.setOnAction(e -> refresh());
        Button markAll = new Button("Mark all as read");
        markAll.setOnAction(e -> {
            app.marketplace().notifications().markAllRead(app.currentUser().getId());
            portal.refreshCurrent();
        });
        Button open = new Button("Open");
        open.getStyleClass().add("primary");
        open.setOnAction(e -> openSelected());
        HBox bar = new HBox(10, category, unreadOnly, markAll, open);
        bar.setAlignment(Pos.CENTER_LEFT);

        list.setCellFactory(v -> new NotificationCell());
        list.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                openSelected();
            }
        });
        VBox.setVgrow(list, Priority.ALWAYS);
        root.getChildren().addAll(Ui.title("Notifications"), summary, bar, list,
                Ui.muted("Double-click a notification to open the related deal, chat or listing."));
        root.setPadding(new Insets(20));
    }

    private void openSelected() {
        Notification n = list.getSelectionModel().getSelectedItem();
        if (n == null) {
            return;
        }
        app.marketplace().notifications().markRead(app.currentUser().getId(), n.getId());
        String id = n.getRelatedId();
        var store = app.marketplace().store();
        if (id != null && store.findDeal(id).isPresent()) {
            Page p = portal.show("My Deals");
            ((MyDealsView) p).select(id);
        } else if (id != null && store.findThread(id).isPresent()) {
            ChatWindow.openThread(app, portal, store.findThread(id).get(), null);
            portal.refreshCurrent();
        } else if (id != null && store.findListing(id).isPresent()) {
            new ListingDetailDialog(app, portal, id).show();
            portal.refreshCurrent();
        } else {
            Ui.info(n.getTitle(), n.getContent());
            portal.refreshCurrent();
        }
    }

    @Override public Parent node() { return root; }

    @Override
    public void refresh() {
        String me = app.currentUser().getId();
        List<Notification> items = app.marketplace().notifications().forUser(me, category.getValue(),
                unreadOnly.isSelected());
        Notification keep = list.getSelectionModel().getSelectedItem();
        list.getItems().setAll(items);
        if (keep != null) {
            items.stream().filter(x -> x.getId().equals(keep.getId())).findFirst()
                    .ifPresent(x -> list.getSelectionModel().select(x));
        }
        long unread = app.marketplace().notifications().unreadCount(me);
        summary.setText(unread == 0 ? "You're all caught up." : unread + " unread notification"
                + (unread == 1 ? "" : "s"));
        list.setPlaceholder(new Label("No notifications."));
    }

    @Override
    public void tick() {
        if (app.marketplace().notifications().forUser(app.currentUser().getId(), category.getValue(),
                unreadOnly.isSelected()).size() != list.getItems().size()) {
            refresh();
        }
    }

    private static final class NotificationCell extends ListCell<Notification> {
        @Override
        protected void updateItem(Notification n, boolean empty) {
            super.updateItem(n, empty);
            setText(null);
            if (empty || n == null) {
                setGraphic(null);
                return;
            }
            Label title = new Label((n.isRead() ? "" : "● ") + n.getTitle());
            title.getStyleClass().add(n.isRead() ? "notif-title" : "notif-title-unread");
            Label content = new Label(n.getContent());
            content.setWrapText(true);
            content.setMaxWidth(780);
            HBox meta = new HBox(8, Ui.badge(n.getCategory().label(), "badge-info"), Ui.muted(Ui.time(n.getCreatedAt())));
            meta.setAlignment(Pos.CENTER_LEFT);
            VBox box = new VBox(3, title, content, meta);
            box.setPadding(new Insets(4, 2, 4, 2));
            setGraphic(box);
        }
    }
}
