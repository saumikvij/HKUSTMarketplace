package hkust.marketplace.ui;

import hkust.marketplace.model.Listing;
import hkust.marketplace.model.ListingType;
import hkust.marketplace.service.DealService;
import hkust.marketplace.service.SellerListingService;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.Duration;
import java.util.Locale;

/** Seller listing inventory and management actions. */
final class SellerListingsView implements Page {
    private final SellerPortal portal;
    private final VBox root = new VBox(12);
    private final ListView<Listing> list = SellerInputs.list("You have no listings yet.",
            listing -> listing.getTitle() + "\n" + listing.getCategory() + " · " + listing.getType()
                    + " · " + label(listing.getStatus()));

    SellerListingsView(SellerPortal portal) {
        this.portal = portal;
        Button create = SellerInputs.primary("Create Listing", () -> portal.show(new SellerListingForm(portal, null)));
        Button view = action("View details", this::view);
        Button requests = action("View requests",
                listing -> portal.show(new SellerRequestsView(portal, listing.getId())));
        Button edit = action("Edit", listing -> {
            portal.market().sellerListings().checkEditable(portal.me(), listing.getId());
            portal.show(new SellerListingForm(portal, listing));
        });
        Button delete = action("Delete", listing -> {
            var service = portal.market().sellerListings();
            service.checkEditable(portal.me(), listing.getId());
            var revision = SellerListingService.Revision.of(listing);
            if (!Ui.confirm("Delete listing?", "Remove “" + listing.getTitle() + "”? Its chat and deal history will be kept.")) return;
            service.remove(portal.me(), listing.getId(), revision);
            refresh();
            Ui.info("Listing deleted", "The listing has been removed.");
        });
        list.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && !view.isDisabled()) view.fire();
        });
        root.setPadding(new Insets(20));
        root.getChildren().addAll(Ui.title("My Listings"), new HBox(8, create, view, requests, edit, delete), list);
    }

    private Button action(String label, java.util.function.Consumer<Listing> action) {
        Button button = new Button(label);
        button.disableProperty().bind(list.getSelectionModel().selectedItemProperty().isNull());
        button.setOnAction(event -> {
            try {
                var chosen = list.getSelectionModel().getSelectedItem();
                action.accept(portal.market().sellerListings().requireOwned(portal.me(), chosen.getId()));
            } catch (RuntimeException exception) {
                Ui.error(exception);
                refresh();
            }
        });
        return button;
    }

    void select(String id) {
        list.getItems().stream().filter(item -> item.getId().equals(id)).findFirst().ifPresent(item -> {
            list.getSelectionModel().select(item);
            list.scrollTo(item);
        });
    }

    void view(Listing listing) {
        listing = portal.market().sellerListings().requireOwned(portal.me(), listing.getId());
        String terms = listing.getType() == ListingType.SELL ? Ui.money(listing.getPrice())
                : listing.getType() == ListingType.LEND
                ? DealService.formatDuration(Duration.ofSeconds(listing.getLendingDurationSeconds()))
                : "Free";
        Label description = new Label(listing.getDescription());
        description.setWrapText(true);
        VBox detail = new VBox(8, Ui.muted(listing.getCategory() + " · " + listing.getType() + " · " + listing.getCondition()),
                description, new Label(terms + " · " + listing.getMeetupLocation()),
                new Label(label(listing.getStatus()) + " · " + portal.market().listings().availabilityText(listing)),
                Ui.muted("Posted " + Ui.time(listing.getPostedAt()) + " · " + listing.getViewCount() + " views"));
        if (listing.getRejectionReason() != null) detail.getChildren().add(new Label("Rejection reason: " + listing.getRejectionReason()));
        if (listing.getRemovalReason() != null) detail.getChildren().add(new Label("Removal reason: " + listing.getRemovalReason()));
        if (!listing.getPhotos().isEmpty()) {
            ComboBox<Integer> photo = new ComboBox<>();
            for (int i = 1; i <= listing.getPhotos().size(); i++) photo.getItems().add(i);
            var photos = listing.getPhotos();
            var preview = new HBox();
            photo.setOnAction(event -> preview.getChildren().setAll(Ui.photo(photos.get(photo.getValue() - 1), 340, 220)));
            preview.getChildren().setAll(Ui.photo(photos.getFirst(), 340, 220));
            photo.setValue(1);
            detail.getChildren().addAll(new HBox(8, new Label("Photo"), photo), preview);
        }
        var deals = portal.market().deals().dealsForSeller(portal.me(), listing.getId());
        detail.getChildren().add(Ui.muted(deals.size() + " offer(s), borrow request(s) and claim(s)"));
        if (!deals.isEmpty()) {
            var requests = SellerInputs.<hkust.marketplace.model.Deal>list("No requests", deal -> SellerRequestsView.describe(portal, deal));
            requests.getItems().setAll(deals);
            requests.setPrefHeight(135);
            detail.getChildren().add(requests);
        }
        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setHeaderText(listing.getTitle());
        dialog.getDialogPane().setContent(detail);
        Ui.style(dialog);
        dialog.showAndWait();
    }

    static String label(Enum<?> value) {
        String text = value.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    @Override public Parent node() { return root; }

    @Override public void refresh() {
        Listing selected = list.getSelectionModel().getSelectedItem();
        list.getItems().setAll(portal.market().sellerListings().forSeller(portal.me()));
        if (selected != null) select(selected.getId());
    }
}
