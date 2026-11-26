package hkust.marketplace.ui;

import hkust.marketplace.model.*;
import hkust.marketplace.service.ChatService;
import hkust.marketplace.util.ValidationException;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.function.Consumer;

/** Seller request handling for sales, lending and giveaways (Task 2.5). */
final class SellerRequestsView implements Page {
    private final SellerPortal portal;
    private final String listingId;
    private final VBox root = new VBox(12);
    private final ListView<Deal> list;

    SellerRequestsView(SellerPortal portal, String listingId) {
        this.portal = portal;
        this.listingId = listingId;
        list = SellerInputs.list("No requests yet.", deal -> describe(portal, deal));
        Button accept = button("Accept", deal -> confirm("Accept request?", deal,
                () -> portal.market().deals().acceptDeal(portal.me(), deal.getId())), deal -> deal.getStatus() == DealStatus.PENDING);
        accept.getStyleClass().add("primary");
        Button decline = button("Decline", deal -> confirm("Decline request?", deal,
                () -> portal.market().deals().declineDeal(portal.me(), deal.getId())), deal -> deal.getStatus() == DealStatus.PENDING);
        Button counter = button("Counter", this::counter, deal ->
                deal.getStatus() == DealStatus.PENDING && deal.getType() != DealType.CLAIM);
        Button chat = button("Open chat", this::chat, deal -> true);
        Button chats = new Button("All chats");
        chats.setOnAction(event -> openChats());
        Button refresh = new Button("Refresh");
        refresh.setOnAction(event -> refresh());
        root.setPadding(new Insets(20));
        root.getChildren().add(Ui.title("Offers and Requests"));
        if (listingId != null) {
            root.getChildren().add(Ui.muted(portal.market().sellerListings().requireOwned(portal.me(), listingId).getTitle()));
        }
        root.getChildren().addAll(new HBox(8, accept, decline, counter, chat, chats, refresh), list);
    }

    private Button button(String name, Consumer<Deal> action, java.util.function.Predicate<Deal> available) {
        Button button = new Button(name);
        button.disableProperty().bind(javafx.beans.binding.Bindings.createBooleanBinding(() -> {
            Deal deal = list.getSelectionModel().getSelectedItem();
            return deal == null || !available.test(deal);
        }, list.getSelectionModel().selectedItemProperty()));
        button.setOnAction(event -> {
            try {
                action.accept(list.getSelectionModel().getSelectedItem());
            } catch (RuntimeException exception) {
                Ui.error(exception);
                refresh();
            }
        });
        return button;
    }

    private void confirm(String title, Deal deal, Runnable action) {
        if (!Ui.confirm(title, describe(portal, deal))) return;
        action.run();
        refresh();
        Ui.info("Request updated", "The buyer has been notified.");
    }

    private void counter(Deal deal) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(portal.app().stage());
        dialog.setTitle("Counter request");
        dialog.setHeaderText(describe(portal, deal));
        var form = SellerInputs.form();
        TextField first = new TextField();
        TextField second = new TextField();
        if (deal.getType() == DealType.OFFER) {
            first.setText(deal.getOfferedPrice().toPlainString());
            SellerInputs.row(form, 0, "Counter price (HK$)", first);
        } else {
            first.setText(deal.getBorrowStart().toString().replace('T', ' '));
            second.setText(deal.getBorrowEnd().toString().replace('T', ' '));
            SellerInputs.row(form, 0, "Start (YYYY-MM-DD HH:MM:SS)", first);
            SellerInputs.row(form, 1, "End (YYYY-MM-DD HH:MM:SS)", second);
        }
        Label error = new Label();
        error.setWrapText(true);
        error.getStyleClass().add("error-text");
        dialog.getDialogPane().setContent(new VBox(10, form, error));
        ButtonType submit = new ButtonType("Send counter", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(submit, ButtonType.CANCEL);
        dialog.getDialogPane().lookupButton(submit).addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            try {
                if (deal.getType() == DealType.OFFER) {
                    portal.market().deals().counterOffer(portal.me(), deal.getId(), first.getText());
                } else {
                    portal.market().deals().counterBorrow(portal.me(), deal.getId(),
                            parseTime(first.getText()), parseTime(second.getText()));
                }
            } catch (RuntimeException exception) {
                error.setText(exception.getMessage());
                event.consume();
            }
        });
        dialog.showAndWait().filter(submit::equals).ifPresent(ignored ->
                Ui.info("Counter sent", "The buyer can respond from My Deals."));
        refresh();
    }

    private static LocalDateTime parseTime(String value) {
        try {
            return LocalDateTime.parse(value.trim().replace(' ', 'T'));
        } catch (DateTimeParseException exception) {
            throw new ValidationException("Enter a valid campus date and time, e.g. 2026-12-01 13:30:00");
        }
    }

    private void chat(Deal deal) {
        ChatThread thread = portal.market().chat().findThread(deal.getBuyerId(), deal.getListingId())
                .orElseThrow(() -> new ValidationException("There is no linked chat for this request."));
        ChatWindow.openThread(portal.app(), null, thread, null);
    }

    private void openChats() {
        ChatService chat = portal.market().chat();
        var threads = SellerInputs.<ChatThread>list("No chats yet.", item ->
                itemTitle(portal, item.getListingId()) + " · " + buyerName(portal, item.getBuyerId()));
        threads.getItems().setAll(chat.threadsFor(portal.me()));
        threads.setPrefSize(520, 200);
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(portal.app().stage());
        dialog.setTitle("Seller chats");
        dialog.getDialogPane().setContent(threads);
        ButtonType open = new ButtonType("Open chat", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(open, ButtonType.CANCEL);
        dialog.getDialogPane().lookupButton(open).disableProperty()
                .bind(threads.getSelectionModel().selectedItemProperty().isNull());
        dialog.showAndWait().filter(open::equals).ifPresent(ignored ->
                ChatWindow.openThread(portal.app(), null, threads.getSelectionModel().getSelectedItem(), null));
    }

    static String describe(SellerPortal portal, Deal deal) {
        String terms = switch (deal.getType()) {
            case OFFER -> Ui.money(deal.getOfferedPrice()) + (deal.getCounterPrice() == null ? ""
                    : " · Counter " + Ui.money(deal.getCounterPrice()));
            case BORROW -> Ui.time(deal.getBorrowStart()) + " → " + Ui.time(deal.getBorrowEnd())
                    + (deal.getCounterStart() == null ? "" : "\nCounter: " + Ui.time(deal.getCounterStart())
                    + " → " + Ui.time(deal.getCounterEnd()));
            case CLAIM -> "Giveaway claim";
        };
        return itemTitle(portal, deal.getListingId()) + " · " + buyerName(portal, deal.getBuyerId())
                + "\n" + terms + " · " + deal.getStatus().label()
                + (deal.getMessage() == null ? "" : "\n" + deal.getMessage());
    }

    static String itemTitle(SellerPortal portal, String id) {
        return portal.market().store().findListing(id).map(Listing::getTitle).orElse("Unavailable listing");
    }

    static String buyerName(SellerPortal portal, String id) {
        return portal.market().store().findUser(id).map(User::getFullName).orElse("Buyer");
    }

    boolean select(String id) {
        for (Deal deal : list.getItems()) {
            if (!deal.getId().equals(id)) continue;
            list.getSelectionModel().select(deal);
            list.scrollTo(deal);
            return true;
        }
        return false;
    }

    @Override public Parent node() { return root; }

    @Override public void refresh() {
        Deal selected = list.getSelectionModel().getSelectedItem();
        list.getItems().setAll(portal.market().deals().dealsForSeller(portal.me(), listingId));
        if (selected != null) select(selected.getId());
    }
}
