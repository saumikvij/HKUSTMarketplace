package hkust.marketplace.ui;

import hkust.marketplace.model.Listing;
import hkust.marketplace.model.ListingType;
import hkust.marketplace.model.Review;
import hkust.marketplace.model.Seller;
import hkust.marketplace.model.User;
import hkust.marketplace.service.DealService;
import hkust.marketplace.service.ListingService;
import java.time.Duration;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/** Listing Detail (Task 1.4), opened from Browse Marketplace or My Deals. */
public class ListingDetailDialog {
    private final AppContext app;
    private final BuyerPortal portal;
    private final String listingId;
    private Stage stage;
    private int photoIndex;

    /**
     * @param app app context
     * @param portal portal shell (refreshed after actions)
     * @param listingId listing to show
     */
    public ListingDetailDialog(AppContext app, BuyerPortal portal, String listingId) {
        this.app = app;
        this.portal = portal;
        this.listingId = listingId;
    }

    /** Opens the detail window (adds one view) and waits until it is closed. */
    public void show() {
        Listing listing;
        try {
            listing = app.marketplace().listings().openListing(app.currentUser().getId(), listingId);
        } catch (RuntimeException e) {
            Ui.error(e);
            return;
        }
        stage = new Stage();
        stage.initOwner(app.stage());
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle(listing.getTitle());
        Scene scene = new Scene(build(listing), 1000, 640);
        Ui.applyStyle(scene);
        stage.setScene(scene);
        stage.showAndWait();
    }

    private Parent build(Listing l) {
        ListingService ls = app.marketplace().listings();
        Seller seller = ls.sellerOf(l);

        // ---- photos ----
        StackPane main = new StackPane();
        Label photoCount = Ui.muted("");
        HBox thumbs = new HBox(6);
        List<String> photos = l.getPhotos();
        Runnable showPhoto = () -> {
            main.getChildren().setAll(Ui.photo(photos.isEmpty() ? null : photos.get(photoIndex), 400, 300));
            photoCount.setText(photos.isEmpty() ? "No photos" : "Photo " + (photoIndex + 1) + " of " + photos.size());
        };
        for (int i = 0; i < photos.size(); i++) {
            int idx = i;
            Node t = Ui.photo(photos.get(i), 60, 45);
            t.setCursor(Cursor.HAND);
            t.setOnMouseClicked(e -> {
                photoIndex = idx;
                showPhoto.run();
            });
            thumbs.getChildren().add(t);
        }
        Button prev = new Button("◀");
        prev.setOnAction(e -> {
            if (!photos.isEmpty()) {
                photoIndex = (photoIndex + photos.size() - 1) % photos.size();
                showPhoto.run();
            }
        });
        Button next = new Button("▶");
        next.setOnAction(e -> {
            if (!photos.isEmpty()) {
                photoIndex = (photoIndex + 1) % photos.size();
                showPhoto.run();
            }
        });
        showPhoto.run();
        HBox nav = new HBox(8, prev, photoCount, next);
        nav.setAlignment(Pos.CENTER);
        VBox photoBox = new VBox(10, main, nav, thumbs);

        // ---- details ----
        Label title = Ui.title(l.getTitle());
        title.setWrapText(true);
        String avail = ls.availabilityText(l);
        HBox badges = new HBox(8, Ui.typeBadge(l.getType()),
                Ui.badge(avail, "Available".equals(avail) ? "badge-ok" : "badge-warn"));
        Label price = new Label(l.getType() == ListingType.SELL ? Ui.money(l.getPrice())
                : l.getType() == ListingType.LEND ? "Lend for up to "
                + DealService.formatDuration(Duration.ofSeconds(l.getLendingDurationSeconds())) : "Free giveaway");
        price.getStyleClass().add("price-large");

        GridPane fields = new GridPane();
        fields.setHgap(14);
        fields.setVgap(6);
        int row = 0;
        fields.addRow(row++, bold("Category"), new Label(l.getCategory().label()));
        fields.addRow(row++, bold("Condition"), new Label(l.getCondition().label()));
        fields.addRow(row++, bold("Meet-up"), new Label(l.getMeetupLocation()));
        fields.addRow(row++, bold("Posted"), new Label(Ui.time(l.getPostedAt())));
        fields.addRow(row++, bold("Views"), new Label(String.valueOf(l.getViewCount())));
        Hyperlink reviews = new Hyperlink(app.marketplace().reviews().ratingSummary(seller.getId()));
        reviews.setOnAction(e -> showSellerReviews(seller));
        fields.addRow(row, bold("Seller"), new HBox(6, new Label(seller.getFullName()), reviews));

        Label desc = new Label(l.getDescription());
        desc.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        desc.setWrapText(true);

        // ---- actions ----
        boolean open = ls.isOpenForDeals(l);
        Button action = new Button(l.getType() == ListingType.SELL ? "Make Offer"
                : l.getType() == ListingType.LEND ? "Request to Borrow" : "Claim Item");
        action.getStyleClass().add("primary");
        action.setDisable(!open);
        action.setOnAction(e -> {
            boolean done;
            switch (l.getType()) {
                case SELL: done = DealDialogs.makeOffer(app, l); break;
                case LEND: done = DealDialogs.requestBorrow(app, l); break;
                default: done = DealDialogs.claim(app, l);
            }
            if (done || !ls.isOpenForDeals(l)) {
                portal.refreshCurrent();
                stage.close();
            }
        });
        Button chatButton = new Button("Chat with Seller");
        chatButton.setOnAction(e -> ChatWindow.open(app, portal, l.getId()));
        Button report = new Button("Report Listing");
        report.getStyleClass().add("danger-link");
        report.setOnAction(e -> DealDialogs.report(app, l));
        Button close = new Button("Close");
        close.setOnAction(e -> stage.close());
        javafx.scene.layout.FlowPane actions = new javafx.scene.layout.FlowPane(10, 10, action, chatButton, report, close);
        Label notOpen = Ui.muted(open ? "" : "New requests are not possible right now: " + avail + ".");

        VBox details = new VBox(10, title, badges, price, fields, bold("Description"), desc, notOpen, actions);
        details.setPrefWidth(470);
        details.setMinWidth(470);
        HBox content = new HBox(24, photoBox, details);
        content.setPadding(new Insets(24));
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().addAll("plain-scroll", "detail-root");
        return scroll;
    }

    private void showSellerReviews(Seller seller) {
        List<Review> reviews = app.marketplace().reviews().visibleReviewsForSeller(seller.getId());
        StringBuilder sb = new StringBuilder();
        if (reviews.isEmpty()) {
            sb.append("No ratings yet.");
        }
        for (Review r : reviews) {
            String reviewer = app.marketplace().store().findUser(r.getBuyerId()).map(User::getFullName).orElse("Buyer");
            sb.append("★".repeat(r.getRating())).append("☆".repeat(5 - r.getRating()))
                    .append("  ").append(reviewer).append(", ").append(Ui.time(r.getPostedAt())).append('\n')
                    .append(r.getComment()).append('\n');
            if (r.getSellerReply() != null) {
                sb.append("Seller reply: ").append(r.getSellerReply()).append('\n');
            }
            sb.append('\n');
        }
        Ui.info("Reviews for " + seller.getFullName() + " — "
                + app.marketplace().reviews().ratingSummary(seller.getId()), sb.toString().trim());
    }

    private static Label bold(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("field-label");
        return l;
    }
}
