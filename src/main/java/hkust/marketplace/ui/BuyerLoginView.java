package hkust.marketplace.ui;

import hkust.marketplace.model.Role;
import hkust.marketplace.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Buyer login (Task 1.2). Seller and admin accounts are refused here. */
public class BuyerLoginView {
    private final AppContext app;
    private final Role role;

    /** @param app app context */
    public BuyerLoginView(AppContext app) {
        this(app, Role.BUYER);
    }

    BuyerLoginView(AppContext app, Role role) {
        this.app = app;
        this.role = role;
    }

    /** @return the screen */
    public Parent build() {
        TextField username = new TextField();
        username.setPromptText("Username");
        username.setId("login-username");
        PasswordField password = new PasswordField();
        password.setPromptText("Password");
        password.setId("login-password");
        Label error = new Label();
        error.getStyleClass().add("error-text");
        error.setWrapText(true);

        Button login = new Button("Log in");
        login.getStyleClass().add("primary");
        login.setDefaultButton(true);
        login.setMaxWidth(Double.MAX_VALUE);
        login.setOnAction(e -> {
            try {
                User user = app.marketplace().auth().login(username.getText(), password.getText(), role);
                if (role == Role.ADMIN) app.enterAdminPortal(user);
                else app.enterBuyerPortal(user);
            } catch (RuntimeException ex) {
                error.setText(ex.getMessage());
                password.clear();
            }
        });
        Hyperlink register = new Hyperlink("New here? Register as a buyer");
        register.setOnAction(e -> app.showBuyerRegister());
        Button back = new Button("Back");
        back.setOnAction(e -> app.showWelcome());

        String heading = role == Role.ADMIN ? "Admin login" : "Buyer login";
        VBox card = new VBox(12, Ui.title(heading), Ui.muted(role == Role.ADMIN ? "Sign in with your administrator account." : "Log in with your buyer account."),
                new Label("Username"), username, new Label("Password"), password, error, login,
                role == Role.ADMIN ? back : new HBox(10, back, register));
        card.getStyleClass().add("card");
        card.setMaxWidth(380);
        card.setPadding(new Insets(32));
        VBox root = new VBox(card);
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("app-root");
        return root;
    }
}
