package hkust.marketplace.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

/** Seller registration form (Task 2.1). */
final class SellerRegistrationView {
    private final AppContext app;

    SellerRegistrationView(AppContext app) {
        this.app = app;
    }

    Parent build() {
        TextField username = new TextField();
        TextField name = new TextField();
        PasswordField password = new PasswordField();
        PasswordField confirmation = new PasswordField();
        ListView<String> locations = SellerInputs.locations(List.of());
        TextArea bio = SellerInputs.textArea(null);
        var form = SellerInputs.form();
        SellerInputs.row(form, 0, "Username", username);
        SellerInputs.row(form, 1, "Full name", name);
        SellerInputs.row(form, 2, "Password", password);
        SellerInputs.row(form, 3, "Confirm password", confirmation);
        SellerInputs.row(form, 4, "Meet-up locations", locations);
        SellerInputs.row(form, 5, "Bio (optional)", bio);
        Label error = new Label();
        error.setWrapText(true);
        error.getStyleClass().add("error-text");
        Button submit = SellerInputs.primary("Register", () -> {
            try {
                app.marketplace().auth().registerSeller(username.getText(), name.getText(), password.getText(),
                        confirmation.getText(), bio.getText(), locations.getSelectionModel().getSelectedItems());
                app.showSellerLogin();
                Ui.info("Seller account created", "You can now log in with your seller account.");
            } catch (RuntimeException exception) {
                error.setText(exception.getMessage());
                password.clear();
                confirmation.clear();
            }
        });
        submit.setDefaultButton(true);
        Button cancel = new Button("Cancel");
        cancel.setOnAction(event -> app.showWelcome());
        VBox card = new VBox(12, Ui.title("Register as a seller"),
                Ui.muted("Use at least 8 characters with uppercase, lowercase and a digit."),
                form, Ui.muted("Use Command or Ctrl to select multiple locations."), error, new HBox(8, submit, cancel));
        card.getStyleClass().add("card");
        card.setPadding(new Insets(26));
        card.setMaxWidth(540);
        VBox root = new VBox(card);
        root.getStyleClass().add("app-root");
        root.setAlignment(Pos.CENTER);
        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        return scroll;
    }
}
