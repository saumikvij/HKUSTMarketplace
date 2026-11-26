package hkust.marketplace.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** First screen: pick a portal. Seller and Admin portals are delivered by Tasks 2 and 3. */
public class WelcomeView {
    private final AppContext app;

    /** @param app app context */
    public WelcomeView(AppContext app) {
        this.app = app;
    }

    /** @return the screen */
    public Parent build() {
        Label logo = new Label("HKUST Marketplace");
        logo.getStyleClass().add("hero-title");
        Label tag = Ui.muted("Buy, borrow and give away on campus.");

        Button buyerLogin = new Button("Buyer login");
        buyerLogin.getStyleClass().add("primary");
        buyerLogin.setOnAction(e -> app.showBuyerLogin());
        Button buyerRegister = new Button("Register as buyer");
        buyerRegister.setOnAction(e -> app.showBuyerRegister());
        HBox buyerRow = new HBox(10, buyerLogin, buyerRegister);
        buyerRow.setAlignment(Pos.CENTER);

        Button seller = new Button("Seller portal");
        seller.setOnAction(e -> Ui.info("Seller portal", "The Seller Portal is Task 2 of the team project."));
        Button admin = new Button("Admin portal");
        admin.setOnAction(e -> Ui.info("Admin portal", "The Admin Portal is Task 3 of the team project."));
        HBox otherRow = new HBox(10, seller, admin);
        otherRow.setAlignment(Pos.CENTER);

        VBox card = new VBox(16, logo, tag, new Label(" "), Ui.muted("I want to buy, borrow or claim items"),
                buyerRow, new Label(" "), Ui.muted("Other portals"), otherRow);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("card");
        card.setMaxWidth(460);
        card.setPadding(new Insets(40));

        VBox root = new VBox(card);
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("app-root");
        return root;
    }
}
