package hkust.marketplace.ui;

import hkust.marketplace.model.*;
import hkust.marketplace.service.*;
import hkust.marketplace.util.ValidationException;
import java.io.File;
import java.math.BigDecimal;
import java.util.*;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

/** Labelled JavaFX forms that retain entered values when validation fails. */
final class AdminForms {
    private final AppContext app;
    private final String id;
    private final AdminService service;
    AdminForms(AppContext app) {
        this.app = app; id = app.currentUser().getId(); service = app.marketplace().admin();
    }
    static GridPane grid() {
        GridPane grid = new GridPane(); grid.setHgap(12); grid.setVgap(9); grid.setPadding(new Insets(10)); return grid;
    }
    static void row(GridPane g, String label, Node input) {
        int i = g.getRowCount(); g.add(new Label(label),0,i); g.add(input,1,i);
        GridPane.setHgrow(input, Priority.ALWAYS);
    }
    static <T> ComboBox<T> choice(List<T> values, T selected) {
        ComboBox<T> box = new ComboBox<>(); box.getItems().setAll(values); box.setValue(selected); return box;
    }
    boolean form(String title, GridPane grid, Runnable save) {
        Dialog<ButtonType> dialog = new Dialog<>(); dialog.initOwner(app.stage()); dialog.setTitle(title);
        dialog.getDialogPane().getStylesheets().add(Ui.class.getResource("style.css").toExternalForm());
        Label error = new Label(); error.getStyleClass().add("error-text"); error.setWrapText(true);
        ScrollPane scroll = new ScrollPane(grid); scroll.setFitToWidth(true); scroll.setMaxHeight(490);
        dialog.getDialogPane().setContent(new VBox(6,scroll,error));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK,ButtonType.CANCEL);
        dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            try { save.run(); } catch (RuntimeException ex) { error.setText(ex.getMessage()); e.consume(); }
        });
        return dialog.showAndWait().filter(ButtonType.OK::equals).isPresent();
    }
    boolean user() {
        GridPane g = grid(); var role = choice(List.of(Role.BUYER,Role.SELLER),Role.BUYER);
        TextField username = new TextField(), name = new TextField();
        PasswordField password = new PasswordField(), confirm = new PasswordField();
        row(g,"Role",role); row(g,"Username",username); row(g,"Full name",name);
        row(g,"Password",password); row(g,"Confirm password",confirm);
        return form("Create account",g,() -> service.addUser(id,role.getValue(),username.getText(),name.getText(),
                password.getText(),confirm.getText()));
    }
    boolean text(String title, String label, String initial, java.util.function.Consumer<String> action) {
        GridPane g = grid(); TextField input = new TextField(initial); input.setPrefColumnCount(32);
        row(g,label,input); return form(title,g,() -> action.accept(input.getText()));
    }
    boolean listing(Listing l, AdminRecords records) {
        GridPane g = grid();
        List<User> sellers = records.users("",Role.SELLER).stream().filter(User::isActive).toList();
        User owner = l == null ? null : app.marketplace().store().findUser(l.getSellerId()).orElseThrow();
        ComboBox<User> seller = choice(sellers,owner); seller.setDisable(l != null);
        var type = choice(List.of(ListingType.values()),l == null ? ListingType.SELL : l.getType());
        var category = choice(List.of(Category.values()),l == null ? null : l.getCategory());
        var condition = choice(List.of(Condition.values()),l == null ? null : l.getCondition());
        TextField title = new TextField(l == null ? "" : l.getTitle());
        TextArea description = new TextArea(l == null ? "" : l.getDescription());
        description.setPrefRowCount(2); description.setWrapText(true);
        List<String> locations = new ArrayList<>(ListingForm.LOCATIONS);
        if (l != null && !locations.contains(l.getMeetupLocation())) locations.add(l.getMeetupLocation());
        var location = choice(locations,l == null ? null : l.getMeetupLocation());
        TextField price = new TextField(l == null || l.getPrice() == null ? "" : l.getPrice().toPlainString());
        TextField seconds = new TextField(l == null || l.getLendingDurationSeconds() == 0 ? "" : Long.toString(l.getLendingDurationSeconds()));
        price.disableProperty().bind(type.valueProperty().isNotEqualTo(ListingType.SELL));
        seconds.disableProperty().bind(type.valueProperty().isNotEqualTo(ListingType.LEND));
        ListView<String> photos = new ListView<>(); photos.setPrefHeight(75);
        if (l != null) photos.getItems().setAll(l.getPhotos());
        Button add = new Button("Choose photos"), remove = new Button("Remove selected");
        add.setOnAction(e -> {
            FileChooser fc = new FileChooser(); fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Pictures","*.png","*.jpg","*.jpeg","*.gif"));
            List<File> files = fc.showOpenMultipleDialog(app.stage());
            if (files == null) return;
            if (photos.getItems().size() + files.size() > Listing.MAX_PHOTOS) { Ui.info("Photos", "Choose up to six photos per listing."); return; }
            files.forEach(f -> photos.getItems().add(f.getAbsolutePath()));
        });
        remove.setOnAction(e -> { int pos = photos.getSelectionModel().getSelectedIndex(); if (pos >= 0) photos.getItems().remove(pos); });
        row(g,"Seller",seller); row(g,"Listing type",type); row(g,"Item title",title); row(g,"Category",category);
        row(g,"Condition",condition); row(g,"Description",description); row(g,"Meet-up location",location);
        row(g,"Price (HK$)",price); row(g,"Loan duration (seconds)",seconds); row(g,"Photos",new VBox(4,photos,new HBox(6,add,remove)));
        ListingStatus expected = l == null ? null : l.getStatus();
        return form(l == null ? "Add live listing for seller" : "Save listing changes", g, () -> {
            if (seller.getValue() == null) throw new ValidationException("Select an active seller.");
            BigDecimal amount = null; long duration = 0;
            try {
                if (type.getValue() == ListingType.SELL) amount = new BigDecimal(price.getText().trim());
                if (type.getValue() == ListingType.LEND) duration = Long.parseLong(seconds.getText().trim());
            } catch (NumberFormatException ex) { throw new ValidationException("Enter a numerical price or a whole number of seconds."); }
            ListingForm f = new ListingForm(type.getValue(),title.getText(),category.getValue(),condition.getValue(),
                    description.getText(),location.getValue(),amount,duration,photos.getItems());
            if (l == null) service.addListing(id,seller.getValue().getId(),f); else service.editListing(id,l.getId(),expected,f);
        });
    }
}
