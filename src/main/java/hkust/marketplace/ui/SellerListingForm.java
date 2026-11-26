package hkust.marketplace.ui;

import hkust.marketplace.model.*;
import hkust.marketplace.service.MeetupLocations;
import hkust.marketplace.service.SellerListingService;
import hkust.marketplace.util.ValidationException;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/** Type-specific listing entry form with an ordered photo gallery. */
final class SellerListingForm implements Page {
    private static final int PHOTO_SIZE = 1200;

    private final SellerPortal portal;
    private final String listingId;
    private final String submissionId;
    private final SellerListingService.Revision revision;
    private final ScrollPane root;
    private final ComboBox<ListingType> type = new ComboBox<>();
    private final TextField name = new TextField();
    private final ComboBox<Category> category = new ComboBox<>();
    private final ComboBox<Condition> condition = new ComboBox<>();
    private final TextArea description = SellerInputs.textArea(null);
    private final ComboBox<String> location = new ComboBox<>();
    private final TextField price = new TextField();
    private final TextField duration = new TextField();
    private final List<String> photos = new ArrayList<>();
    private final ComboBox<Integer> photoSelection = new ComboBox<>();
    private final HBox previews = new HBox(8);
    private final Label feedback = new Label();

    SellerListingForm(SellerPortal portal, Listing listing) {
        this.portal = portal;
        listingId = listing == null ? null : listing.getId();
        submissionId = listing == null ? portal.market().sellerListings().newSubmissionId() : null;
        revision = listing == null ? null : SellerListingService.Revision.of(listing);
        type.getItems().setAll(ListingType.values());
        category.getItems().setAll(Category.values());
        condition.getItems().setAll(Condition.values());
        location.getItems().setAll(MeetupLocations.ALL);
        type.setPromptText("Select listing type");
        location.setPromptText("Select a campus location");
        price.setPromptText("e.g. 120.00");
        duration.setPromptText("Minimum 5 seconds");

        var grid = SellerInputs.form();
        SellerInputs.row(grid, 0, "Item name", name);
        SellerInputs.row(grid, 1, "Category", category);
        SellerInputs.row(grid, 2, "Condition", condition);
        SellerInputs.row(grid, 3, "Description", description);
        SellerInputs.row(grid, 4, "Meet-up location", location);
        Label priceLabel = SellerInputs.row(grid, 5, "Price (HK$)", price);
        Label durationLabel = SellerInputs.row(grid, 6, "Lending duration (seconds)", duration);
        for (var control : List.of(priceLabel, price)) {
            control.visibleProperty().bind(type.valueProperty().isEqualTo(ListingType.SELL));
            control.managedProperty().bind(control.visibleProperty());
        }
        for (var control : List.of(durationLabel, duration)) {
            control.visibleProperty().bind(type.valueProperty().isEqualTo(ListingType.LEND));
            control.managedProperty().bind(control.visibleProperty());
        }

        if (listing != null) load(revision.details());

        type.valueProperty().addListener((observable, previous, next) -> {
            if (next != ListingType.SELL) price.clear();
            if (next != ListingType.LEND) duration.clear();
        });
        var form = new VBox(10, grid, Ui.muted("Optional photos, up to 6. Photo 1 is the thumbnail."),
                photoControls(), previews);
        form.visibleProperty().bind(type.valueProperty().isNotNull());
        form.managedProperty().bind(form.visibleProperty());
        Button save = SellerInputs.primary("Submit for review", this::save);
        save.disableProperty().bind(type.valueProperty().isNull());
        Button cancel = new Button("Cancel");
        cancel.setOnAction(event -> portal.show(new SellerListingsView(portal)));
        feedback.getStyleClass().add("error-text");
        feedback.setWrapText(true);
        HBox actions = new HBox(8, save, cancel);
        VBox content = new VBox(12, Ui.title(listing == null ? "Create Listing" : "Edit Listing"),
                type, form, feedback, actions);
        if (listingId != null) {
            Button reload = new Button("Reload listing");
            reload.setOnAction(event -> {
                try {
                    var services = portal.market().sellerListings();
                    services.checkEditable(portal.me(), listingId);
                    portal.show(new SellerListingForm(portal, services.requireOwned(portal.me(), listingId)));
                } catch (RuntimeException exception) {
                    Ui.error(exception);
                }
            });
            actions.getChildren().add(reload);
        }
        content.setPadding(new Insets(20));
        root = new ScrollPane(content);
        root.setFitToWidth(true);
        refreshPhotos(0);
    }

    private void load(SellerListingService.Details value) {
        type.setValue(value.type());
        name.setText(value.title());
        category.setValue(value.category());
        condition.setValue(value.condition());
        description.setText(value.description());
        location.setValue(value.location());
        price.setText(value.price());
        duration.setText(value.duration());
        photos.addAll(value.photos());
    }

    private HBox photoControls() {
        Button add = new Button("Add photos");
        add.setOnAction(event -> addPhotos());
        photoSelection.setPromptText("Photo number");
        Button earlier = new Button("Move left");
        earlier.setOnAction(event -> movePhoto(-1));
        Button later = new Button("Move right");
        later.setOnAction(event -> movePhoto(1));
        Button remove = new Button("Remove");
        remove.setOnAction(event -> {
            int index = photoSelection.getSelectionModel().getSelectedIndex();
            if (index < 0) return;
            photos.remove(index);
            refreshPhotos(Math.min(index, photos.size() - 1));
        });
        return new HBox(8, add, photoSelection, earlier, later, remove);
    }

    private void movePhoto(int amount) {
        int index = photoSelection.getSelectionModel().getSelectedIndex();
        int target = index + amount;
        if (index < 0 || target < 0 || target >= photos.size()) return;
        java.util.Collections.swap(photos, index, target);
        refreshPhotos(target);
    }

    private void refreshPhotos(int selected) {
        photoSelection.getItems().clear();
        previews.getChildren().clear();
        for (int i = 0; i < photos.size(); i++) {
            photoSelection.getItems().add(i + 1);
            previews.getChildren().add(new VBox(4, Ui.muted("Photo " + (i + 1)), Ui.photo(photos.get(i), 88, 66)));
        }
        if (selected >= 0) photoSelection.getSelectionModel().select(selected);
    }

    private void addPhotos() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose listing photos");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));
        List<File> picked = chooser.showOpenMultipleDialog(portal.app().stage());
        if (picked == null) return;
        try {
            if (photos.size() + picked.size() > Listing.MAX_PHOTOS) {
                throw new ValidationException("Select at most 6 photos in total.");
            }
            List<String> loaded = new ArrayList<>();
            for (File file : picked) {
                try (var stream = Files.newInputStream(file.toPath())) {
                    Image image = new Image(stream, PHOTO_SIZE, PHOTO_SIZE, true, true);
                    if (image.isError() || image.getPixelReader() == null) {
                        throw new ValidationException(file.getName() + " is not a readable image.");
                    }
                    int width = (int) image.getWidth();
                    int height = (int) image.getHeight();
                    int[] pixels = new int[width * height];
                    image.getPixelReader().getPixels(0, 0, width, height,
                            PixelFormat.getIntArgbInstance(), pixels, 0, width);
                    BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                    resized.setRGB(0, 0, width, height, pixels, 0, width);
                    ByteArrayOutputStream encoded = new ByteArrayOutputStream();
                    if (!ImageIO.write(resized, "png", encoded)) {
                        throw new IOException("The selected photo could not be encoded.");
                    }
                    loaded.add("photo:" + Base64.getEncoder().encodeToString(encoded.toByteArray()));
                }
            }
            photos.addAll(loaded);
            refreshPhotos(photos.size() - 1);
            feedback.setText("");
        } catch (IOException | RuntimeException exception) {
            feedback.setText("Could not add photos. " + exception.getMessage());
        }
    }

    private void save() {
        var details = new SellerListingService.Details(type.getValue(), name.getText(), category.getValue(),
                condition.getValue(), description.getText(), location.getValue(), price.getText(), duration.getText(),
                photos);
        try {
            if (listingId == null) {
                portal.market().sellerListings().create(portal.me(), submissionId, details);
            } else {
                String terms = details.type() == ListingType.SELL ? "\nPrice: HK$" + details.price()
                        : details.type() == ListingType.LEND ? "\nLending period: " + details.duration() + " seconds" : "";
                String summary = details.title() + "\n" + details.type() + " · " + details.category() + " · "
                        + details.condition() + "\n" + details.description() + "\n" + details.location()
                        + terms + "\nPhotos: " + details.photos().size();
                if (!Ui.confirm("Resubmit listing?", summary + "\n\nThe listing will require approval again.")) return;
                portal.market().sellerListings().edit(portal.me(), listingId, revision, details);
            }
            portal.show(new SellerListingsView(portal));
            Ui.info("Submitted for review", "The listing is now Pending Review.");
        } catch (RuntimeException exception) {
            feedback.setText(exception.getMessage());
        }
    }

    @Override public Parent node() { return root; }
    @Override public void refresh() { }
}
