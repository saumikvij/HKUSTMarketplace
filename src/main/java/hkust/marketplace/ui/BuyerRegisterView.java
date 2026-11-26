package hkust.marketplace.ui;

import hkust.marketplace.util.ValidationException;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Buyer registration form (Task 1.1). */
public class BuyerRegisterView {
    /** Residence halls offered for meet-up suggestions. */
    public static final String[] HALLS = {"", "Hall 1", "Hall 2", "Hall 3", "Hall 4", "Hall 5", "Hall 6", "Hall 7",
        "Hall 8", "Hall 9", "Hall 10", "Off-campus"};

    private final AppContext app;

    /** @param app app context */
    public BuyerRegisterView(AppContext app) {
        this.app = app;
    }

    /** @return the screen */
    public Parent build() {
        TextField username = new TextField();
        username.setId("reg-username");
        TextField fullName = new TextField();
        fullName.setId("reg-fullname");
        PasswordField password = new PasswordField();
        password.setId("reg-password");
        PasswordField confirm = new PasswordField();
        confirm.setId("reg-confirm");
        ComboBox<String> hall = new ComboBox<>();
        hall.getItems().addAll(HALLS);
        hall.setEditable(true);
        hall.setPromptText("Optional");
        hall.setMaxWidth(Double.MAX_VALUE);

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);
        form.addRow(0, new Label("Username *"), username);
        form.addRow(1, new Label("Full name *"), fullName);
        form.addRow(2, new Label("Password *"), password);
        form.addRow(3, new Label("Confirm password *"), confirm);
        form.addRow(4, new Label("Residence hall"), hall);

        Label rules = Ui.muted("Username: letters, digits and _ only. Password: at least 8 characters with an "
                + "uppercase letter, a lowercase letter and a digit.");
        Label error = new Label();
        error.getStyleClass().add("error-text");
        error.setWrapText(true);

        Button register = new Button("Register");
        register.getStyleClass().add("primary");
        register.setDefaultButton(true);
        register.setOnAction(e -> {
            try {
                app.marketplace().auth().registerBuyer(username.getText(), fullName.getText(), password.getText(),
                        confirm.getText(), hall.getEditor().getText());
                Ui.info("Registration successful", "Your buyer account was created. Please log in.");
                app.showBuyerLogin();
            } catch (ValidationException ex) {
                error.setText("• " + String.join("\n• ", ex.getErrors()));
                password.clear();
                confirm.clear();
            } catch (RuntimeException ex) {
                error.setText(ex.getMessage());
            }
        });
        Button cancel = new Button("Cancel");
        cancel.setOnAction(e -> app.showWelcome());

        VBox card = new VBox(14, Ui.title("Register as a buyer"), rules, form, error, new HBox(10, cancel, register));
        card.getStyleClass().add("card");
        card.setMaxWidth(520);
        card.setPadding(new Insets(32));
        VBox root = new VBox(card);
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("app-root");
        return root;
    }
}
