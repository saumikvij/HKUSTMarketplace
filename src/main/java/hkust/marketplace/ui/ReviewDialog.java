package hkust.marketplace.ui;

import hkust.marketplace.model.Deal;
import hkust.marketplace.model.Listing;
import hkust.marketplace.util.ValidationException;
import java.util.Optional;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Rate and review the seller of a finished deal (Task 1.10). */
public final class ReviewDialog {
    private ReviewDialog() { }

    /**
     * Shows the review form and submits it. "Skip" leaves the deal unrated (it can be rated later from history).
     * @param app app context
     * @param deal a finished deal
     * @return true if a review was posted
     */
    public static boolean show(AppContext app, Deal deal) {
        Listing listing = app.marketplace().store().findListing(deal.getListingId()).orElse(null);
        String sellerName = listing == null ? "the seller" : app.marketplace().listings().sellerOf(listing).getFullName();
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle("Rate the seller");
        d.setHeaderText("How was your deal with " + sellerName + "?\n"
                + (listing == null ? "" : listing.getTitle() + " · " + deal.getType().historyLabel()));
        ToggleGroup stars = new ToggleGroup();
        HBox starRow = new HBox(6);
        for (int i = 1; i <= 5; i++) {
            ToggleButton b = new ToggleButton(i + " ★");
            b.setUserData(i);
            b.setToggleGroup(stars);
            starRow.getChildren().add(b);
        }
        TextArea comment = new TextArea();
        comment.setPrefRowCount(4);
        comment.setWrapText(true);
        comment.setPromptText("Write your review (max 500 characters)");
        d.getDialogPane().setContent(new VBox(10, new Label("Rating"), starRow, new Label("Review"), comment));
        d.getDialogPane().getButtonTypes().addAll(new ButtonType("Post review", ButtonBar.ButtonData.OK_DONE),
                new ButtonType("Skip", ButtonBar.ButtonData.CANCEL_CLOSE));
        d.getDialogPane().getStylesheets().add(Ui.class.getResource("style.css").toExternalForm());
        while (true) {
            Optional<ButtonType> r = d.showAndWait();
            if (r.isEmpty() || r.get().getButtonData() != ButtonBar.ButtonData.OK_DONE) {
                return false;
            }
            int rating = stars.getSelectedToggle() == null ? 0 : (Integer) stars.getSelectedToggle().getUserData();
            try {
                app.marketplace().reviews().submitReview(app.currentUser().getId(), deal.getId(), rating,
                        comment.getText());
                Ui.info("Review posted", "Thank you! Your review is now shown on the seller's listings.");
                return true;
            } catch (ValidationException e) {
                Ui.error(e);
            } catch (RuntimeException e) {
                Ui.error(e);
                return false;
            }
        }
    }
}
