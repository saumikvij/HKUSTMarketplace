package hkust.marketplace.ui;

import hkust.marketplace.service.ProfileService;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

/** Seller profile and password settings (Task 2.6). */
final class SellerProfileView implements Page {
    private final SellerPortal portal;
    private final ScrollPane root;
    private final TextField name = new TextField();
    private final TextArea bio = SellerInputs.textArea(null);
    private final ListView<String> locations = SellerInputs.locations(List.of());
    private final PasswordField current = new PasswordField();
    private final PasswordField password = new PasswordField();
    private final PasswordField confirmation = new PasswordField();
    private final Label profileStatus = new Label();
    private final Label passwordStatus = new Label();
    private ProfileService.SellerDetails displayed;

    SellerProfileView(SellerPortal portal) {
        this.portal = portal;
        var profile = SellerInputs.form();
        SellerInputs.row(profile, 0, "Username", new Label("@" + portal.app().currentUser().getUsername()));
        SellerInputs.row(profile, 1, "Full name", name);
        SellerInputs.row(profile, 2, "Bio (optional)", bio);
        SellerInputs.row(profile, 3, "Meet-up locations", locations);
        var credentials = SellerInputs.form();
        SellerInputs.row(credentials, 0, "Current password", current);
        SellerInputs.row(credentials, 1, "New password", password);
        SellerInputs.row(credentials, 2, "Confirm new password", confirmation);
        Button save = SellerInputs.primary("Save profile", this::save);
        Button reset = new Button("Discard changes");
        reset.setOnAction(event -> {
            load();
            profileStatus.setText("");
        });
        Button update = SellerInputs.primary("Update password", this::changePassword);
        Button clear = new Button("Clear");
        clear.setOnAction(event -> clearPasswords());
        profileStatus.setWrapText(true);
        passwordStatus.setWrapText(true);
        VBox content = new VBox(12, Ui.title("Manage Profile"), profile,
                Ui.muted("Use Command or Ctrl to select multiple locations."),
                new HBox(8, save, reset), profileStatus, new Separator(),
                Ui.title("Change Password"), credentials, new HBox(8, update, clear), passwordStatus);
        content.setPadding(new Insets(20));
        root = new ScrollPane(content);
        root.setFitToWidth(true);
    }

    private void save() {
        try {
            displayed = portal.market().profile().updateSeller(portal.me(), displayed, name.getText(), bio.getText(),
                    locations.getSelectionModel().getSelectedItems());
            display();
            status(profileStatus, "Profile saved.", false);
        } catch (RuntimeException exception) {

            status(profileStatus, exception.getMessage(), true);
        }
    }

    private void changePassword() {
        try {
            portal.market().profile().changePassword(portal.me(), current.getText(), password.getText(),
                    confirmation.getText());
            status(passwordStatus, "Password updated.", false);
        } catch (RuntimeException exception) {
            status(passwordStatus, exception.getMessage(), true);
        }
        clearPasswords();
    }

    private void clearPasswords() {
        current.clear();
        password.clear();
        confirmation.clear();
    }

    private void load() {
        displayed = portal.market().profile().sellerDetails(portal.me());
        display();
    }

    private void display() {
        name.setText(displayed.fullName());
        bio.setText(displayed.bio());
        locations.getSelectionModel().clearSelection();
        displayed.locations().forEach(location -> locations.getSelectionModel().select(location));
    }

    private static void status(Label label, String text, boolean error) {
        label.getStyleClass().remove("error-text");
        if (error) label.getStyleClass().add("error-text");
        label.setText(text);
    }

    @Override public Parent node() { return root; }

    @Override public void refresh() {
        if (displayed == null) load();
    }
}
