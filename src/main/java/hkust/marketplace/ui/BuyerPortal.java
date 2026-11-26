package hkust.marketplace.ui;

import hkust.marketplace.model.User;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** The buyer portal shell: a menu on the left and the selected page on the right. */
public class BuyerPortal {
    private final AppContext app;
    private final BorderPane root = new BorderPane();
    private final Map<String, Button> menu = new LinkedHashMap<>();
    private Page current;
    private Button notificationsButton;
    private Label who;

    /** @param app app context */
    public BuyerPortal(AppContext app) {
        this.app = app;
    }

    /** @return the portal */
    public Parent build() {
        User user = app.currentUser();
        Label brand = new Label("HKUST Marketplace");
        brand.getStyleClass().add("brand");
        brand.setWrapText(true);
        who = new Label();
        who.getStyleClass().add("sidebar-user");

        VBox side = new VBox(6, brand, who, new Region());
        side.getStyleClass().add("sidebar");
        side.setPadding(new Insets(18, 12, 18, 12));
        side.setPrefWidth(210);
        side.setMinWidth(210);
        addMenu(side, "Browse Marketplace", () -> new BrowseView(app, this));
        addMenu(side, "My Deals", () -> new MyDealsView(app, this));
        addMenu(side, "Deal History", () -> new HistoryView(app, this));
        notificationsButton = addMenu(side, "Notifications", () -> new NotificationsView(app, this));
        addMenu(side, "My Reviews", () -> new MyReviewsView(app));
        addMenu(side, "Manage Profile", () -> new ProfileView(app));
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        Button logout = new Button("Log out");
        logout.getStyleClass().add("menu-item");
        logout.setMaxWidth(Double.MAX_VALUE);
        logout.setOnAction(e -> app.showWelcome());
        side.getChildren().addAll(spacer, logout);

        root.setLeft(side);
        root.getStyleClass().add("app-root");
        show("Browse Marketplace");
        updateBadge();
        return root;
    }

    private Button addMenu(VBox side, String name, Supplier<Page> factory) {
        Button b = new Button(name);
        b.getStyleClass().add("menu-item");
        b.setMaxWidth(Double.MAX_VALUE);
        b.setUserData(factory);
        b.setOnAction(e -> show(name));
        menu.put(name, b);
        side.getChildren().add(b);
        return b;
    }

    /**
     * Opens a page from the menu.
     * @param name menu name, e.g. "My Deals"
     * @return the opened page
     */
    @SuppressWarnings("unchecked")
    public Page show(String name) {
        Button b = menu.get(name);
        menu.values().forEach(x -> x.getStyleClass().remove("menu-selected"));
        b.getStyleClass().add("menu-selected");
        current = ((Supplier<Page>) b.getUserData()).get();
        current.refresh();
        root.setCenter(current.node());
        updateBadge();
        return current;
    }

    /** Reloads the open page (after an action in a dialog or chat window). */
    public void refreshCurrent() {
        if (current != null) {
            current.refresh();
        }
        updateBadge();
    }

    /**
     * Timer tick from {@link AppContext#tick()}.
     * @param dataChanged true if auto-return or reminders changed data
     */
    public void tick(boolean dataChanged) {
        if (current != null) {
            if (dataChanged) {
                current.refresh();
            } else {
                current.tick();
            }
        }
        updateBadge();
    }

    private void updateBadge() {
        if (notificationsButton == null || app.currentUser() == null) {
            return;
        }
        User user = app.currentUser();
        who.setText(user.getFullName() + "\n@" + user.getUsername() + " \u00b7 Buyer");
        long unread = app.marketplace().notifications().unreadCount(app.currentUser().getId());
        notificationsButton.setText(unread > 0 ? "Notifications (" + unread + ")" : "Notifications");
    }

    /** @return app context */
    AppContext app() { return app; }
}
