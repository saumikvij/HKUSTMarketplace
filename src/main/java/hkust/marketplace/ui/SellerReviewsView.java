package hkust.marketplace.ui;

import hkust.marketplace.model.Review;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/** Seller ratings, review replies and moderation flags (Task 2.9). */
final class SellerReviewsView implements Page {
    private final SellerPortal portal;
    private final VBox root = new VBox(12);
    private final Label summary = Ui.muted("");
    private final ListView<Review> list;

    SellerReviewsView(SellerPortal portal) {
        this.portal = portal;
        list = SellerInputs.list("No reviews yet.", this::describe);
        Button reply = action("Reply", this::reply);
        Button flag = action("Flag for review", review -> {
            if (review.isFlagged()) {
                Ui.info("Already flagged", "The review is waiting for the admin.");
                return;
            }
            if (!Ui.confirm("Flag review?", "Send this review to the admin for moderation?")) return;
            portal.market().reviews().flag(portal.me(), review.getId());
            refresh();
            Ui.info("Review flagged", "The admin has been notified.");
        });
        root.setPadding(new Insets(20));
        root.getChildren().addAll(Ui.title("Reviews"), summary, new HBox(8, reply, flag), list);
    }

    private Button action(String title, Consumer<Review> action) {
        Button button = new Button(title);
        button.disableProperty().bind(list.getSelectionModel().selectedItemProperty().isNull());
        button.setOnAction(event -> {
            try {
                String id = list.getSelectionModel().getSelectedItem().getId();
                action.accept(portal.market().reviews().requireVisibleOwned(portal.me(), id));
            } catch (RuntimeException exception) {
                Ui.error(exception);
                refresh();
            }
        });
        return button;
    }

    private void reply(Review review) {
        if (review.getSellerReply() != null) {
            Ui.info("Reply already posted", review.getSellerReply());
            return;
        }
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(portal.app().stage());
        dialog.setTitle("Reply to review");
        dialog.setHeaderText(review.getComment());
        TextArea message = SellerInputs.textArea(null);
        message.setPromptText("Write a reply");
        Label error = new Label();
        error.getStyleClass().add("error-text");
        error.setWrapText(true);
        dialog.getDialogPane().setContent(new VBox(8, message, error));
        ButtonType post = new ButtonType("Post reply", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(post, ButtonType.CANCEL);
        dialog.getDialogPane().lookupButton(post).addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            try {
                portal.market().reviews().reply(portal.me(), review.getId(), message.getText());
            } catch (RuntimeException exception) {
                error.setText(exception.getMessage());
                refresh();
                event.consume();
            }
        });
        dialog.showAndWait().filter(post::equals).ifPresent(ignored -> {
            refresh();
            Ui.info("Reply posted", "The buyer has been notified.");
        });
    }

    private String describe(Review review) {
        String listing = portal.market().store().findDeal(review.getDealId())
                .map(deal -> SellerRequestsView.itemTitle(portal, deal.getListingId())).orElse("Unavailable listing");
        return "★".repeat(review.getRating()) + " · " + listing + " · "
                + SellerRequestsView.buyerName(portal, review.getBuyerId()) + " · " + Ui.time(review.getPostedAt())
                + "\n" + review.getComment()
                + (review.getSellerReply() == null ? "" : "\nYour reply: " + review.getSellerReply())
                + (review.isFlagged() ? "\nFlagged for admin review" : "");
    }

    boolean select(String id) {
        for (Review review : list.getItems()) {
            if (!review.getId().equals(id)) continue;
            list.getSelectionModel().select(review);
            list.scrollTo(review);
            return true;
        }
        return false;
    }

    @Override public Parent node() { return root; }

    @Override public void refresh() {
        Review selected = list.getSelectionModel().getSelectedItem();
        list.getItems().setAll(portal.market().reviews().visibleReviewsForSeller(portal.me()));
        summary.setText(list.getItems().isEmpty() ? "Not yet rated"
                : portal.market().reviews().ratingSummary(portal.me()));
        if (selected != null) select(selected.getId());
    }
}
