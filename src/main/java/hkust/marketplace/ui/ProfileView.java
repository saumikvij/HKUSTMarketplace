package hkust.marketplace.ui;

import hkust.marketplace.model.Buyer;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/** Manage Profile (Task 1.7): view the username, edit full name and hall, and change the password. */
public class ProfileView implements Page {
    private final AppContext app;
    private final VBox root = new VBox(16);
    private final TextField username = new TextField();
    private final TextField fullName = new TextField();
    private final ComboBox<String> hall = new ComboBox<>();
    private final Label registered = new Label();

    /** @param app app context */
    public ProfileView(AppContext app) {
        this.app = app;
        username.setEditable(false);
        username.setDisable(true);
        hall.getItems().addAll(BuyerRegisterView.HALLS);
        hall.setEditable(true);
        Button save = new Button("Save changes");
        save.getStyleClass().add("primary");
        save.setOnAction(e -> {
            try {
                app.marketplace().profile().updateDetails(app.currentUser().getId(), fullName.getText(),
                        hall.getEditor().getText());
                Ui.info("Profile updated", "Your details have been saved.");
                refresh();
            } catch (RuntimeException ex) {
                Ui.error(ex);
            }
        });
        GridPane details = form();
        details.addRow(0, new Label("Username"), username);
        details.addRow(1, new Label("Full name"), fullName);
        details.addRow(2, new Label("Residence hall"), hall);
        details.addRow(3, new Label("Member since"), registered);
        details.add(save, 1, 4);

        PasswordField current = new PasswordField();
        PasswordField next = new PasswordField();
        PasswordField confirm = new PasswordField();
        next.setPromptText("8+ chars with upper, lower and a digit");
        Button change = new Button("Change password");
        change.setOnAction(e -> {
            try {
                app.marketplace().profile().changePassword(app.currentUser().getId(), current.getText(),
                        next.getText(), confirm.getText());
                current.clear();
                next.clear();
                confirm.clear();
                Ui.info("Password changed", "Use your new password next time you log in.");
            } catch (RuntimeException ex) {
                Ui.error(ex);
            }
        });
        GridPane pw = form();
        pw.addRow(0, new Label("Current password"), current);
        pw.addRow(1, new Label("New password"), next);
        pw.addRow(2, new Label("Confirm new password"), confirm);
        pw.add(change, 1, 3);

        VBox d = new VBox(10, section("Account details"), Ui.muted("Your username cannot be changed."), details);
        VBox p = new VBox(10, section("Change password"), pw);
        d.getStyleClass().add("card");
        p.getStyleClass().add("card");
        d.setPadding(new Insets(16));
        p.setPadding(new Insets(16));
        d.setMaxWidth(560);
        p.setMaxWidth(560);
        root.getChildren().addAll(Ui.title("Manage Profile"), d, p);
        root.setPadding(new Insets(20));
    }

    private static GridPane form() {
        GridPane g = new GridPane();
        g.setHgap(12);
        g.setVgap(10);
        return g;
    }

    private static Label section(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("section-title");
        return l;
    }

    @Override public Parent node() { return root; }

    @Override
    public void refresh() {
        Buyer b = (Buyer) app.currentUser();
        username.setText(b.getUsername());
        fullName.setText(b.getFullName());
        hall.getEditor().setText(b.getResidenceHall() == null ? "" : b.getResidenceHall());
        hall.setValue(b.getResidenceHall() == null ? "" : b.getResidenceHall());
        registered.setText(Ui.time(b.getRegisteredAt()));
    }
}
