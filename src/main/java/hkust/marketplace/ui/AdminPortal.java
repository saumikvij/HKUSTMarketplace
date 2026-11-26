package hkust.marketplace.ui;

import hkust.marketplace.model.*;
import hkust.marketplace.service.*;
import java.nio.file.Files;
import java.io.*;
import java.util.*;
import java.util.function.*;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

/** Task 3 dashboard. All actions share the marketplace and session used by the welcome screen. */
public final class AdminPortal {
    private final AppContext app;
    private final String id;
    private final AdminService service;
    private final AdminRecords records;
    private final AdminForms forms;
    private final BorderPane root = new BorderPane();
    private final VBox content = new VBox(12);
    private final Label feedback = Ui.muted("");
    private final Map<String, Button> menu = new LinkedHashMap<>();
    private final Label who;
    private Runnable refresh = () -> {};

    public AdminPortal(AppContext app) {
        this.app = app; id = app.currentUser().getId(); service = app.marketplace().admin();
        records = new AdminRecords(app.marketplace(), id); forms = new AdminForms(app);
        who = new Label(app.currentUser().getFullName()); who.getStyleClass().add("sidebar-user");
    }
    /** Creates the portal and initially displays the approval queue. */
    public Parent build() {
        Label brand = new Label("HKUST Marketplace"); brand.getStyleClass().add("brand");
        VBox side = new VBox(7,brand,who); side.setPrefWidth(210); side.setPadding(new Insets(18));
        side.getStyleClass().add("sidebar");
        for (String page : List.of("Review queue","All listings","Listing reports","Users","All deals","Notifications","Seller ratings","My profile")) {
            Button b = new Button(page); b.setMaxWidth(Double.MAX_VALUE); b.getStyleClass().add("menu-item");
            b.setOnAction(e -> navigate(page)); side.getChildren().add(b); menu.put(page,b);
        }
        Region spacer = new Region(); VBox.setVgrow(spacer,Priority.ALWAYS);
        Button out = new Button("Log out"); out.setOnAction(e -> app.showWelcome()); out.getStyleClass().add("menu-item");
        side.getChildren().addAll(spacer,out);
        content.setPadding(new Insets(20)); root.setLeft(side); root.setCenter(content); root.getStyleClass().add("app-root");
        navigate("Review queue"); feedback.setText("Signed in as administrator."); return root;
    }
    private void navigate(String page) {
        service.requireAdmin(id);
        menu.values().forEach(b -> b.getStyleClass().remove("menu-selected")); menu.get(page).getStyleClass().add("menu-selected");
        feedback.setText(""); content.getChildren().setAll(Ui.title(page)); refresh = () -> {};
        switch (page) {
            case "Review queue" -> listings(true);
            case "All listings" -> listings(false);
            case "Listing reports" -> reports();
            case "Users" -> users();
            case "All deals" -> deals();
            case "Notifications" -> notifications();
            case "Seller ratings" -> ratings();
            case "My profile" -> profile();
            default -> throw new IllegalArgumentException(page);
        }
        content.getChildren().add(feedback); refresh.run();
    }
    private void run(Runnable task) {
        try { task.run(); } catch (RuntimeException e) { Ui.error(e); }
    }
    private Button button(String text, Runnable action) {
        Button b = new Button(text); b.setOnAction(e -> run(action)); return b;
    }
    private FlowPane bar(Node... nodes) {
        FlowPane p = new FlowPane(8,6,nodes); p.setAlignment(Pos.CENTER_LEFT); return p;
    }
    private TextField search(String hint) {
        TextField field = new TextField(); field.setPromptText(hint);
        field.setOnAction(e -> run(refresh)); return field;
    }
    private <T> ComboBox<T> filter(String hint, List<T> options) {
        ComboBox<T> box = new ComboBox<>(); box.setPromptText(hint);
        box.getItems().add(null); box.getItems().addAll(options); return box;
    }
    private <T> TableView<T> table(String empty) {
        TableView<T> table = new TableView<>(); table.setPlaceholder(Ui.muted(empty));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(table,Priority.ALWAYS); return table;
    }
    private <T> void col(TableView<T> table, String label, Function<T,String> value, int width) {
        TableColumn<T,String> column = new TableColumn<>(label);
        column.setCellValueFactory(x -> new ReadOnlyStringWrapper(value.apply(x.getValue())));
        column.setPrefWidth(width); table.getColumns().add(column);
    }
    private <T> Button onRow(String label, TableView<T> t, Consumer<T> action) {
        Button b = button(label,() -> action.accept(t.getSelectionModel().getSelectedItem()));
        b.disableProperty().bind(t.getSelectionModel().selectedItemProperty().isNull()); return b;
    }
    private void saved(String message) { refresh.run(); feedback.setText(message); }
    private void showListing(Listing l) {
        User seller = app.marketplace().store().findUser(l.getSellerId()).orElseThrow();
        VBox detail = new VBox(8,Ui.title(l.getTitle()),new Label(seller + "  |  " + l.getStatus()),
                Ui.muted(l.getCategory() + " · " + l.getCondition() + " · " + l.getType() + " · " + l.getMeetupLocation()),
                Ui.muted("Submitted " + Ui.time(l.getPostedAt())),new Label(l.getType() == ListingType.SELL ? Ui.money(l.getPrice())
                : l.getType() == ListingType.LEND ? l.getLendingDurationSeconds() + " seconds" : "Free"));
        Label description = new Label(l.getDescription()); description.setWrapText(true); detail.getChildren().add(description);
        if (l.getRejectionReason() != null) detail.getChildren().add(Ui.muted("Rejection: " + l.getRejectionReason()));
        if (l.getRemovalReason() != null) detail.getChildren().add(Ui.muted("Removal: " + l.getRemovalReason()));
        FlowPane images = bar(); images.setPrefWrapLength(540);
        for (String path : l.getPhotos()) images.getChildren().add(Ui.photo(path,160,115));
        detail.getChildren().add(l.getPhotos().isEmpty() ? Ui.muted("No photos attached.") : images);
        Dialog<Void> d = new Dialog<>(); d.initOwner(app.stage()); d.setTitle("Listing details");
        ScrollPane sp = new ScrollPane(detail); sp.setFitToWidth(true); sp.setPrefViewportWidth(560); sp.setMaxHeight(530);
        d.getDialogPane().setContent(sp); d.getDialogPane().getButtonTypes().add(ButtonType.CLOSE); d.showAndWait();
    }
    private void listings(boolean pending) {
        TableView<Listing> t = table(pending ? "No listings are awaiting review." : "No matching listings.");
        TextField q = search("Item or seller"); var status = filter("Any status",List.of(ListingStatus.values()));
        FlowPane filters = bar(q); if (!pending) filters.getChildren().add(status); filters.getChildren().add(button("Apply",() -> refresh.run()));
        col(t,"Item",Listing::getTitle,195); col(t,"Seller",l -> records.username(l.getSellerId()),85);
        col(t,"Category",l -> l.getCategory().label(),85); col(t,"Type",l -> l.getType().label(),65);
        col(t,"Meet-up",Listing::getMeetupLocation,85); col(t,"Submitted",l -> Ui.time(l.getPostedAt()),140);
        col(t,"Status",l -> l.getStatus().name(),95);
        FlowPane actions = bar(onRow("View details & photos",t,this::showListing));
        if (pending) {
            actions.getChildren().addAll(onRow("Approve",t,l -> {
                if (Ui.confirm("Approve listing?",l.getTitle() + " will become visible to buyers.")) {
                    service.review(id,l.getId(),true,null); saved("Listing approved.");
                }
            }),onRow("Reject",t,l -> {
                if (forms.text("Reject " + l.getTitle(),"Reason for seller","",s -> service.review(id,l.getId(),false,s))) saved("Listing rejected.");
            }));
        } else {
            actions.getChildren().addAll(button("Add listing",() -> { if (forms.listing(null,records)) saved("Listing published."); }),
                    onRow("Edit",t,l -> { if (forms.listing(l,records)) saved("Listing updated."); }),
                    onRow("Take down",t,this::takeDown));
        }
        content.getChildren().addAll(filters,t,actions);
        refresh = () -> t.getItems().setAll(records.listings(pending,q.getText(),status.getValue()));
    }
    private void takeDown(Listing l) {
        if (l.getStatus() != ListingStatus.LIVE) { Ui.info("Take down", "Select a live listing."); return; }
        if (forms.text("Take down " + l.getTitle(),"Reason sent to affected users","",s -> service.takeDown(id,l.getId(),s)))
            saved("Listing removed and affected users notified.");
    }
    private void reports() {
        TableView<Report> t = table("No open listing reports.");
        col(t,"Listing",r -> records.listing(r.getListingId()).getTitle(),160); col(t,"Reported by",r -> records.username(r.getBuyerId()),95);
        col(t,"Reason",r -> r.getReason().label(),100); col(t,"Comment",Report::getComment,190); col(t,"Reported",r -> Ui.time(r.getReportedAt()),140);
        content.getChildren().addAll(t,bar(onRow("View listing",t,r -> showListing(records.listing(r.getListingId()))),
                onRow("Dismiss reports",t,r -> { if (Ui.confirm("Dismiss open reports?", "All open reports on this item will be dismissed.")) {
                    service.dismissReports(id,r.getListingId()); saved("Reports dismissed."); } }),
                onRow("Take down listing",t,r -> takeDown(records.listing(r.getListingId())))));
        refresh = () -> t.getItems().setAll(records.reports(null));
    }
    private void users() {
        TableView<User> t = table("No matching users."); TextField q = search("Username or full name"); var role = filter("Any role",List.of(Role.BUYER,Role.SELLER));
        col(t,"Username",User::getUsername,110); col(t,"Full name",User::getFullName,150); col(t,"Role",u -> u.getRole().name(),70);
        col(t,"Status",u -> u.getStatus().name(),100); col(t,"Joined",u -> Ui.time(u.getRegisteredAt()),140);
        content.getChildren().addAll(bar(q,role,button("Apply",() -> refresh.run())),t,bar(
                button("Add account",() -> { if (forms.user()) saved("Account created."); }),
                onRow("Edit name",t,u -> { if (forms.text("Edit account","Full name",u.getFullName(),s -> service.rename(id,u.getId(),s))) saved("Name saved."); }),
                onRow("Deactivate / reactivate",t,u -> {
                    boolean enable = !u.isActive();
                    if (Ui.confirm(enable ? "Reactivate account?" : "Deactivate account?", enable ? "Login and eligible listings will be restored." : "Login will be blocked and this seller's listings hidden.")) {
                        service.setActive(id,u.getId(),enable); saved("Account status updated.");
                    }
                }),onRow("Delete account",t,u -> {
                    if (Ui.confirm("Permanently delete " + u.getUsername() + "?","Personal details are erased. Open commitments are cancelled; seller listings are removed. Historical deals, chats and reviews are retained. An outstanding loan blocks deletion.")) {
                        service.deleteUser(id,u.getId()); saved("Account deleted.");
                    }
                })));
        refresh = () -> t.getItems().setAll(records.users(q.getText(),role.getValue()));
    }
    private void deals() {
        TableView<Deal> t = table("No matching deals."); TextField q = search("Item, buyer or seller");
        var type = filter("All deal types",List.of(DealType.values())); var stage = filter("All statuses",List.of(AdminRecords.Stage.values()));
        col(t,"Item",d -> records.listing(d.getListingId()).getTitle(),145); col(t,"Buyer",d -> records.username(d.getBuyerId()),80);
        col(t,"Seller",d -> records.username(records.listing(d.getListingId()).getSellerId()),80); col(t,"Type",d -> d.getType().label(),58);
        col(t,"Start",d -> Ui.time(records.start(d)),145); col(t,"Return / complete",d -> Ui.time(records.end(d)),145);
        col(t,"Status",d -> d.getStatus().label(),85);
        content.getChildren().addAll(bar(q,type,stage,button("Apply",() -> refresh.run()),button("Export shown to CSV",() -> {
            FileChooser fc = new FileChooser(); fc.setInitialFileName("marketplace-deals.csv");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV","*.csv")); File f = fc.showSaveDialog(app.stage());
            if (f != null) { try { Files.writeString(f.toPath(),records.csv(t.getItems())); feedback.setText("Saved " + t.getItems().size() + " deals."); }
                catch (IOException e) { throw new UncheckedIOException(e); } }
        })),t);
        refresh = () -> t.getItems().setAll(records.deals(q.getText(),type.getValue(),stage.getValue()));
    }
    private void notifications() {
        TableView<Notification> t = table("No matching notifications."); Label count = Ui.muted("");
        var cat = filter("All categories",List.of(NotificationCategory.values())); DatePicker from = date("From"), until = date("Until");
        CheckBox archive = new CheckBox("Show archive");
        col(t,"Read",n -> n.isRead() ? "Read" : "Unread",57); col(t,"Category",n -> n.getCategory().label(),95);
        col(t,"Title",Notification::getTitle,135); col(t,"Message",Notification::getContent,210); col(t,"Received",n -> Ui.time(n.getCreatedAt()),135);
        content.getChildren().addAll(count,bar(cat,from,until,archive,button("Apply",() -> refresh.run())),t,bar(
                onRow("Open",t,n -> { app.marketplace().notifications().markRead(id,n.getId()); Ui.info(n.getTitle(),n.getContent()); refresh.run(); }),
                onRow("Archive / restore",t,n -> { service.archive(id,n.getId(),!n.isArchived()); saved("Notification updated."); })));
        refresh = () -> {
            t.getItems().setAll(records.notifications(cat.getValue(),new AdminRecords.Dates(from.getValue(),until.getValue()),archive.isSelected()));
            long unread = records.notifications(null,new AdminRecords.Dates(null,null),false).stream().filter(n -> !n.isRead()).count();
            count.setText(unread + " unread in inbox");
        };
    }
    private DatePicker date(String hint) { DatePicker p = new DatePicker(); p.setPromptText(hint); p.setPrefWidth(135); p.setEditable(false); return p; }
    private void ratings() {
        TableView<Review> t = table("No matching seller ratings."); TextField q = search("Buyer or seller");
        var score = filter("Any score",List.of(1,2,3,4,5)); DatePicker from = date("From"),until = date("Until");
        CheckBox flagged = new CheckBox("Flagged only");
        col(t,"Seller",r -> records.username(r.getSellerId()),75); col(t,"Buyer",r -> records.username(r.getBuyerId()),75);
        col(t,"Listing",records::reviewItem,120); col(t,"Stars",r -> String.valueOf(r.getRating()),45); col(t,"Comment",Review::getComment,135);
        col(t,"Posted",r -> Ui.time(r.getPostedAt()),125); col(t,"Visibility",r -> r.isHidden() ? "Hidden" : "Public",60); col(t,"Flag",r -> r.isFlagged() ? "Flagged" : "",55);
        content.getChildren().addAll(bar(q,score,from,until,flagged,button("Apply",() -> refresh.run())),t,bar(
                onRow("Read review",t,r -> Ui.info(records.reviewItem(r),r.getRating() + "/5 — " + Objects.toString(r.getComment(),""))),
                onRow("Hide / restore",t,r -> { if (Ui.confirm(r.isHidden() ? "Restore rating?" : "Hide rating?","The seller's public rating average will update.")) {
                    service.hideReview(id,r.getId(),!r.isHidden()); saved("Rating visibility updated."); } })));
        refresh = () -> t.getItems().setAll(records.ratings(q.getText(),score.getValue(),new AdminRecords.Dates(from.getValue(),until.getValue()),flagged.isSelected()));
    }
    private void profile() {
        GridPane grid = AdminForms.grid(); grid.setMaxWidth(550);
        TextField name = new TextField(app.currentUser().getFullName()); PasswordField current = new PasswordField(), next = new PasswordField(), confirm = new PasswordField();
        AdminForms.row(grid,"Full name",name); AdminForms.row(grid,"Current password",current); AdminForms.row(grid,"New password (optional)",next);
        AdminForms.row(grid,"Confirm new password",confirm);
        content.getChildren().addAll(grid,bar(button("Save changes",() -> {
            boolean logout = service.profile(id,name.getText(),current.getText(),next.getText(),confirm.getText());
            if (logout) { Ui.info("Password changed","Please log in again."); app.showAdminLogin(); }
            else { current.clear(); who.setText(app.currentUser().getFullName()); saved("Profile updated."); }
        }),button("Discard changes",() -> navigate("My profile"))));
    }
}
