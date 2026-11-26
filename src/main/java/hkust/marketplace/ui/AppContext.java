package hkust.marketplace.ui;

import hkust.marketplace.model.User;
import hkust.marketplace.service.Marketplace;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Shared state of the running app: the services, the main window and the logged-in user.
 * Handles screen navigation and the one-second timer tick.
 */
public class AppContext {
    private final Marketplace marketplace;
    private final Stage stage;
    private User currentUser;
    private BuyerPortal portal;
    private SellerPortal sellerPortal;

    /**
     * @param marketplace services
     * @param stage main window
     */
    public AppContext(Marketplace marketplace, Stage stage) {
        this.marketplace = marketplace;
        this.stage = stage;
        stage.setTitle("HKUST Marketplace");
        stage.setMinWidth(1000);
        stage.setMinHeight(680);
    }

    /** @return services */
    public Marketplace marketplace() { return marketplace; }
    /** @return main window */
    public Stage stage() { return stage; }
    /** @return logged-in user, or null */
    public User currentUser() { return currentUser; }

    /** Shows the welcome screen and logs out. */
    public void showWelcome() {
        ChatWindow.closeAll();
        currentUser = null;
        portal = null;
        sellerPortal = null;
        setRoot(new WelcomeView(this).build());
    }

    /** Shows the buyer login screen. */
    public void showBuyerLogin() {
        setRoot(new BuyerLoginView(this).build());
    }

    /** Opens the login-only admin entry point and clears the previous session. */
    public void showAdminLogin() {
        portal = null;
        currentUser = null;
        setRoot(new BuyerLoginView(this, hkust.marketplace.model.Role.ADMIN).build());
    }

    /** Opens the management dashboard after verifying the signed-in administrator. */
    public void enterAdminPortal(User user) {
        marketplace.admin().syncPendingAlerts(user.getId());
        currentUser = user;
        portal = null;
        setRoot(new AdminPortal(this).build());
    }

    /** Shows the buyer registration form. */
    public void showBuyerRegister() {
        setRoot(new BuyerRegisterView(this).build());
    }

    /**
     * Logs a buyer in and opens the buyer portal on Browse Marketplace.
     * @param user the buyer
     */
    public void enterBuyerPortal(User user) {
        currentUser = user;
        portal = new BuyerPortal(this);
        setRoot(portal.build());
    }

    /** Opens the role-checked seller login form. */
    public void showSellerLogin() {
        setRoot(new BuyerLoginView(this, hkust.marketplace.model.Role.SELLER).build());
    }

    /** Opens seller registration. */
    public void showSellerRegister() {
        setRoot(new SellerRegistrationView(this).build());
    }

    /** Opens the seller dashboard after login. */
    public void enterSellerPortal(User seller) {
        currentUser = seller;
        sellerPortal = new SellerPortal(this);
        setRoot(sellerPortal.build());
    }

    /** Called every second: runs auto-return and reminders, then refreshes the open screen if needed. */
    public void tick() {
        int changed = marketplace.deals().processDueDates();
        if (portal != null) {
            portal.tick(changed > 0);
        }
        if (sellerPortal != null) sellerPortal.tick(changed > 0);
    }

    private void setRoot(Parent root) {
        if (stage.getScene() == null) {
            Scene scene = new Scene(root, 1180, 760);
            Ui.applyStyle(scene);
            stage.setScene(scene);
        } else {
            stage.getScene().setRoot(root);
        }
    }
}
