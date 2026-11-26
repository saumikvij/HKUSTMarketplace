package hkust.marketplace.ui;

import hkust.marketplace.service.Marketplace;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Seller navigation and session shell, styled to match the Buyer Portal. */
final class SellerPortal {
    private final AppContext app;
    private final BorderPane root = new BorderPane();
    private final Map<Button, Supplier<Page>> menu = new LinkedHashMap<>();
    private Page page;
    private Button notifications;
    private Label who;

    SellerPortal(AppContext app) {
        this.app = app;
    }

    Parent build() {
        Label brand = new Label("HKUST Marketplace");
        brand.getStyleClass().add("brand");
        brand.setWrapText(true);
        who = new Label();
        who.getStyleClass().add("sidebar-user");
        VBox sidebar = new VBox(6, brand, who, new Region());
        sidebar.setPadding(new Insets(18, 12, 18, 12));
        sidebar.setMinWidth(220);
        sidebar.getStyleClass().add("sidebar");
        Button listings = add(sidebar, "My Listings", () -> new SellerListingsView(this));
        add(sidebar, "Create Listing", () -> new SellerListingForm(this, null));
        add(sidebar, "Offers and Requests", () -> new SellerRequestsView(this, null));
        add(sidebar, "Seller Stats", () -> new SellerStatsView(this));
        add(sidebar, "Reviews", () -> new SellerReviewsView(this));
        notifications = add(sidebar, "Notifications", () -> new SellerNotificationsView(this));
        add(sidebar, "Manage Profile", () -> new SellerProfileView(this));
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        Button logout = new Button("Log out");
        logout.getStyleClass().add("menu-item");
        logout.setMaxWidth(Double.MAX_VALUE);
        logout.setOnAction(event -> app.showWelcome());
        sidebar.getChildren().addAll(spacer, logout);
        root.setLeft(sidebar);
        root.getStyleClass().add("app-root");
        select(listings);
        return root;
    }

    private Button add(VBox sidebar, String label, Supplier<Page> factory) {
        Button button = new Button(label);
        button.getStyleClass().add("menu-item");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(event -> select(button));
        menu.put(button, factory);
        sidebar.getChildren().add(button);
        return button;
    }

    private void select(Button button) {
        show(menu.get(button).get());
        button.getStyleClass().add("menu-selected");
    }

    void show(Page next) {
        menu.keySet().forEach(button -> button.getStyleClass().remove("menu-selected"));
        page = next;
        try {
            page.refresh();
        } catch (RuntimeException exception) {
            Ui.error(exception);
        }
        root.setCenter(page.node());
        updateSidebar();
    }

    void tick(boolean changed) {
        if (page != null) {
            if (changed) page.refresh();
            else page.tick();
        }
        updateSidebar();
    }

    private void updateSidebar() {
        market().store().findUser(me()).ifPresent(user ->
                who.setText(user.getFullName() + "\n@" + user.getUsername() + " · Seller"));
        long unread = market().notifications().unreadCount(me());
        notifications.setText(unread == 0 ? "Notifications" : "Notifications (" + unread + ")");
    }

    String me() { return app.currentUser().getId(); }
    AppContext app() { return app; }
    Marketplace market() { return app.marketplace(); }
}
