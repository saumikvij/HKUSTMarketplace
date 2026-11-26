package hkust.marketplace.ui;

import hkust.marketplace.model.Deal;
import hkust.marketplace.model.Listing;
import hkust.marketplace.model.ReportReason;
import hkust.marketplace.service.DealService;
import hkust.marketplace.util.ValidationException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

/** Dialogs for making an offer, a borrow request, a claim and a report (Tasks 1.4 and 1.5). */
public final class DealDialogs {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private DealDialogs() { }

    /**
     * Asks whether to replace an existing Pending/Countered request on the listing.
     * @param app app context
     * @param listing listing
     * @return null to cancel, otherwise true if there was a request to replace
     */
    private static Boolean confirmReplace(AppContext app, Listing listing) {
        Optional<Deal> existing = app.marketplace().deals().openRequest(app.currentUser().getId(), listing.getId());
        if (existing.isEmpty()) {
            return Boolean.FALSE;
        }
        boolean ok = Ui.confirm("You already have a " + existing.get().getStatus().label().toLowerCase()
                        + " request on this listing",
                "Replace it with a new one? The earlier request will be withdrawn.");
        return ok ? Boolean.TRUE : null;
    }

    /**
     * Shows the offer form for a Sell listing and submits it.
     * @param app app context
     * @param listing a Sell listing
     * @return true if an offer was submitted
     */
    public static boolean makeOffer(AppContext app, Listing listing) {
        Boolean replace = confirmReplace(app, listing);
        if (replace == null) {
            return false;
        }
        Dialog<ButtonType> d = dialog("Make an offer", "Asking price: " + Ui.money(listing.getPrice()));
        TextField price = new TextField();
        price.setPromptText("e.g. 150 or 149.50");
        TextArea message = new TextArea();
        message.setPrefRowCount(3);
        message.setPromptText("Optional message to the seller");
        GridPane g = grid();
        g.addRow(0, new Label("Your offer (HK$)"), price);
        g.addRow(1, new Label("Message"), message);
        d.getDialogPane().setContent(g);
        while (true) {
            Optional<ButtonType> r = d.showAndWait();
            if (r.isEmpty() || r.get().getButtonData() != ButtonBar.ButtonData.OK_DONE) {
                return false;
            }
            try {
                app.marketplace().deals().makeOffer(app.currentUser().getId(), listing.getId(), price.getText(),
                        message.getText(), replace);
                Ui.info("Offer submitted", "Your offer was sent to the seller. Track it in My Deals.");
                return true;
            } catch (ValidationException e) {
                Ui.error(e);
            } catch (RuntimeException e) {
                Ui.error(e);
                return false;
            }
        }
    }

    /**
     * Shows the borrow request form for a Lend listing and submits it.
     * @param app app context
     * @param listing a Lend listing
     * @return true if a request was submitted
     */
    public static boolean requestBorrow(AppContext app, Listing listing) {
        Boolean replace = confirmReplace(app, listing);
        if (replace == null) {
            return false;
        }
        Dialog<ButtonType> d = dialog("Request to borrow", "The seller lends this for up to "
                + DealService.formatDuration(java.time.Duration.ofSeconds(listing.getLendingDurationSeconds()))
                + ". Minimum borrowing time is 5 seconds.");
        LocalDateTime now = LocalDateTime.now(app.marketplace().clock());
        DatePicker startDate = new DatePicker(now.toLocalDate());
        TextField startTime = new TextField(now.plusSeconds(10).toLocalTime().format(TIME));
        DatePicker endDate = new DatePicker(now.toLocalDate());
        TextField endTime = new TextField(now.plusMinutes(2).toLocalTime().format(TIME));
        TextArea message = new TextArea();
        message.setPrefRowCount(2);
        message.setPromptText("Optional message to the seller");
        GridPane g = grid();
        g.addRow(0, new Label("Start date"), startDate, new Label("Time (HH:mm:ss)"), startTime);
        g.addRow(1, new Label("End date"), endDate, new Label("Time (HH:mm:ss)"), endTime);
        g.add(new Label("Message"), 0, 2);
        g.add(message, 1, 2, 3, 1);
        d.getDialogPane().setContent(g);
        while (true) {
            Optional<ButtonType> r = d.showAndWait();
            if (r.isEmpty() || r.get().getButtonData() != ButtonBar.ButtonData.OK_DONE) {
                return false;
            }
            try {
                LocalDateTime start = at(startDate.getValue(), startTime.getText(), "start");
                LocalDateTime end = at(endDate.getValue(), endTime.getText(), "end");
                app.marketplace().deals().requestBorrow(app.currentUser().getId(), listing.getId(), start, end,
                        message.getText(), replace);
                Ui.info("Borrow request submitted", "Your request was sent to the seller. Track it in My Deals.");
                return true;
            } catch (ValidationException e) {
                Ui.error(e);
            } catch (RuntimeException e) {
                Ui.error(e);
                return false;
            }
        }
    }

    /**
     * Asks for confirmation and claims a Giveaway listing.
     * @param app app context
     * @param listing a Giveaway listing
     * @return true if the claim succeeded
     */
    public static boolean claim(AppContext app, Listing listing) {
        if (!Ui.confirm("Claim \"" + listing.getTitle() + "\"?", "The item will be reserved for you while the seller "
                + "confirms. Pick it up at " + listing.getMeetupLocation() + ".")) {
            return false;
        }
        try {
            app.marketplace().deals().claim(app.currentUser().getId(), listing.getId());
            Ui.info("Claim submitted", "The item is reserved for you. Track it in My Deals.");
            return true;
        } catch (RuntimeException e) {
            Ui.error(e);
            return false;
        }
    }

    /**
     * Shows the report form and reports the listing to the admin.
     * @param app app context
     * @param listing listing
     */
    public static void report(AppContext app, Listing listing) {
        Dialog<ButtonType> d = dialog("Report listing", "Tell the admin what is wrong with \"" + listing.getTitle()
                + "\".");
        ChoiceBox<ReportReason> reason = new ChoiceBox<>();
        reason.getItems().addAll(ReportReason.values());
        TextArea comment = new TextArea();
        comment.setPrefRowCount(3);
        comment.setPromptText("Comment (required for \"Other\")");
        GridPane g = grid();
        g.addRow(0, new Label("Reason"), reason);
        g.addRow(1, new Label("Comment"), comment);
        d.getDialogPane().setContent(g);
        while (true) {
            Optional<ButtonType> r = d.showAndWait();
            if (r.isEmpty() || r.get().getButtonData() != ButtonBar.ButtonData.OK_DONE) {
                return;
            }
            try {
                app.marketplace().listings().reportListing(app.currentUser().getId(), listing.getId(),
                        reason.getValue(), comment.getText());
                Ui.info("Report submitted", "Thank you. The admin will review this listing.");
                return;
            } catch (ValidationException e) {
                Ui.error(e);
            } catch (RuntimeException e) {
                Ui.error(e);
                return;
            }
        }
    }

    private static LocalDateTime at(LocalDate date, String time, String which) {
        if (date == null || time == null || time.isBlank()) {
            throw new ValidationException("Please enter both a start and an end time");
        }
        try {
            return LocalDateTime.of(date, LocalTime.parse(time.trim()));
        } catch (DateTimeParseException e) {
            throw new ValidationException("The " + which + " time must look like 14:30:00");
        }
    }

    private static Dialog<ButtonType> dialog(String title, String header) {
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle(title);
        d.setHeaderText(header);
        d.getDialogPane().getButtonTypes().addAll(new ButtonType("Submit", ButtonBar.ButtonData.OK_DONE),
                ButtonType.CANCEL);
        d.getDialogPane().getStylesheets().add(Ui.class.getResource("style.css").toExternalForm());
        return d;
    }

    private static GridPane grid() {
        GridPane g = new GridPane();
        g.setHgap(10);
        g.setVgap(10);
        return g;
    }
}
