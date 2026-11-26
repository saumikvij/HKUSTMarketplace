package hkust.marketplace.ui;

import hkust.marketplace.model.Listing;
import hkust.marketplace.model.Review;
import hkust.marketplace.model.User;
import java.util.List;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** The reviews this buyer has written, with any seller replies (Task 1.10). */
public class MyReviewsView implements Page {
    private final AppContext app;
    private final VBox root = new VBox(12);
    private final VBox items = new VBox(10);

    /** @param app app context */
    public MyReviewsView(AppContext app) {
        this.app = app;
        ScrollPane scroll = new ScrollPane(items);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("plain-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        root.getChildren().addAll(Ui.title("My Reviews"),
                Ui.muted("Reviews you posted after finished deals. Rate unrated deals from Deal History."), scroll);
        root.setPadding(new Insets(20));
    }

    @Override public Parent node() { return root; }

    @Override
    public void refresh() {
        items.getChildren().clear();
        List<Review> reviews = app.marketplace().reviews().reviewsByBuyer(app.currentUser().getId());
        if (reviews.isEmpty()) {
            items.getChildren().add(Ui.muted("You have not written any reviews yet."));
            return;
        }
        for (Review r : reviews) {
            String seller = app.marketplace().store().findUser(r.getSellerId()).map(User::getFullName).orElse("");
            String item = app.marketplace().store().findDeal(r.getDealId())
                    .flatMap(d -> app.marketplace().store().findListing(d.getListingId()))
                    .map(Listing::getTitle).orElse("");
            Label stars = new Label("★".repeat(r.getRating()) + "☆".repeat(5 - r.getRating()));
            stars.getStyleClass().add("stars");
            Label head = new Label(item + " · " + seller);
            head.getStyleClass().add("card-title");
            Label comment = new Label(r.getComment());
            comment.setWrapText(true);
            comment.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
            VBox card = new VBox(4, stars, head, comment, Ui.muted("Posted " + Ui.time(r.getPostedAt())
                    + (r.isHidden() ? " · hidden by admin" : "")));
            if (r.getSellerReply() != null) {
                Label reply = new Label("Seller reply: " + r.getSellerReply());
                reply.setWrapText(true);
                reply.getStyleClass().add("reply");
                card.getChildren().add(reply);
            }
            card.getStyleClass().add("card");
            card.setPadding(new Insets(12));
            items.getChildren().add(card);
        }
    }
}
