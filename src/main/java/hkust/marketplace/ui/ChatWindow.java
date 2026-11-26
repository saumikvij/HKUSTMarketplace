package hkust.marketplace.ui;

import hkust.marketplace.model.ChatMessage;
import hkust.marketplace.model.ChatThread;
import hkust.marketplace.model.Deal;
import hkust.marketplace.model.Listing;
import hkust.marketplace.model.ListingType;
import hkust.marketplace.model.Role;
import hkust.marketplace.model.User;
import hkust.marketplace.service.ChatService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Private chat about one listing (Task 1.5): free text, formal offers shown as cards, permanent and searchable
 * history. The window polls every second so the seller's replies appear without reopening it.
 */
public final class ChatWindow {
    private static final Map<String, Stage> OPEN = new HashMap<>();

    private final AppContext app;
    private final BuyerPortal portal;
    private final ChatThread thread;
    private final ListView<ChatMessage> messages = new ListView<>();
    private int shownCount = -1;

    private ChatWindow(AppContext app, BuyerPortal portal, ChatThread thread) {
        this.app = app;
        this.portal = portal;
        this.thread = thread;
    }

    /**
     * Opens (or brings to the front) the buyer's chat for a listing.
     * @param app app context
     * @param portal portal shell
     * @param listingId listing
     */
    public static void open(AppContext app, BuyerPortal portal, String listingId) {
        try {
            ChatThread t = app.marketplace().chat().openThread(app.currentUser().getId(), listingId);
            openThread(app, portal, t, null);
        } catch (RuntimeException e) {
            Ui.error(e);
        }
    }

    /**
     * Opens a thread and optionally scrolls to a message (used by search results and notifications).
     * @param app app context
     * @param portal portal shell
     * @param thread thread
     * @param focus message to select, or null
     */
    public static void openThread(AppContext app, BuyerPortal portal, ChatThread thread, ChatMessage focus) {
        Stage existing = OPEN.get(thread.getId());
        if (existing != null && existing.isShowing()) {
            existing.toFront();
            return;
        }
        ChatWindow w = new ChatWindow(app, portal, thread);
        Stage stage = w.build();
        OPEN.put(thread.getId(), stage);
        stage.show();
        if (focus != null) {
            w.messages.getSelectionModel().select(focus);
            w.messages.scrollTo(focus);
        }
    }

    private Stage build() {
        ChatService chat = app.marketplace().chat();
        Listing listing = app.marketplace().store().findListing(thread.getListingId()).orElse(null);
        String sellerName = app.marketplace().store().findUser(thread.getSellerId()).map(User::getFullName)
                .orElse("Seller");
        Label header = Ui.title("Chat with " + sellerName);
        Label about = Ui.muted("About: " + (listing == null ? "removed listing" : listing.getTitle()));

        messages.setCellFactory(v -> new MessageCell());
        VBox.setVgrow(messages, Priority.ALWAYS);

        TextField searchField = new TextField();
        searchField.setPromptText("Search messages");
        ChoiceBox<String> scope = new ChoiceBox<>();
        scope.getItems().addAll("This chat", "All my chats");
        scope.setValue("This chat");
        Button search = new Button("Search");
        search.setOnAction(e -> search(searchField.getText(), "This chat".equals(scope.getValue())));
        HBox.setHgrow(searchField, Priority.ALWAYS);
        HBox searchRow = new HBox(8, searchField, scope, search);

        TextArea input = new TextArea();
        input.setPromptText("Type a message (max 1,000 characters)");
        input.setPrefRowCount(2);
        input.setWrapText(true);
        HBox.setHgrow(input, Priority.ALWAYS);
        Button send = new Button("Send");
        send.getStyleClass().add("primary");
        send.setOnAction(e -> {
            try {
                chat.send(thread.getId(), app.currentUser().getId(), input.getText());
                input.clear();
                reload();
            } catch (RuntimeException ex) {
                Ui.error(ex);
            }
        });
        Button offer = new Button(listing != null && listing.getType() == ListingType.LEND ? "Request period"
                : "Make Offer");
        offer.setVisible(listing != null && listing.getType() != ListingType.GIVEAWAY);
        offer.setManaged(offer.isVisible());
        offer.setOnAction(e -> {
            Listing current = app.marketplace().store().findListing(thread.getListingId()).orElse(null);
            if (current == null || !app.marketplace().listings().isOpenForDeals(current)) {
                Ui.error(new IllegalStateException("This listing is no longer available for new requests."));
                return;
            }
            boolean done = current.getType() == ListingType.LEND ? DealDialogs.requestBorrow(app, current)
                    : DealDialogs.makeOffer(app, current);
            if (done) {
                reload();
                portal.refreshCurrent();
            }
        });
        send.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        offer.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        send.setMaxWidth(Double.MAX_VALUE);
        offer.setMaxWidth(Double.MAX_VALUE);
        VBox sendButtons = new VBox(6, send, offer);
        sendButtons.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        HBox inputRow = new HBox(8, input, sendButtons);

        Optional<String> readOnly = chat.readOnlyReason(thread);
        Label readOnlyLabel = new Label(readOnly.orElse(""));
        readOnlyLabel.getStyleClass().add("error-text");
        if (readOnly.isPresent()) {
            input.setDisable(true);
            send.setDisable(true);
            offer.setDisable(true);
        }

        VBox top = new VBox(4, header, about, searchRow);
        VBox bottom = new VBox(6, readOnlyLabel, inputRow);
        BorderPane root = new BorderPane(messages, top, null, bottom, null);
        BorderPane.setMargin(messages, new Insets(10, 0, 10, 0));
        root.setPadding(new Insets(16));
        root.getStyleClass().add("app-root");

        Stage stage = new Stage();
        stage.initOwner(app.stage());
        stage.setTitle("Chat — " + (listing == null ? "listing" : listing.getTitle()));
        Scene scene = new Scene(root, 560, 620);
        Ui.applyStyle(scene);
        stage.setScene(scene);
        reload();
        Timeline poll = new Timeline(new KeyFrame(Duration.seconds(1), e -> reload()));
        poll.setCycleCount(Timeline.INDEFINITE);
        poll.play();
        stage.setOnHidden(e -> {
            poll.stop();
            OPEN.remove(thread.getId());
        });
        return stage;
    }

    private void reload() {
        List<ChatMessage> all = thread.getMessages();
        if (all.size() != shownCount) {
            shownCount = all.size();
            messages.getItems().setAll(all);
            if (!all.isEmpty()) {
                messages.scrollTo(all.size() - 1);
            }
        } else {
            messages.refresh();
        }
    }

    private void search(String keyword, boolean thisChatOnly) {
        List<ChatService.SearchHit> hits;
        try {
            hits = app.marketplace().chat().search(app.currentUser().getId(), keyword,
                    thisChatOnly ? thread.getId() : null);
        } catch (RuntimeException e) {
            Ui.error(e);
            return;
        }
        if (hits.isEmpty()) {
            Ui.info("Search", "No messages found.");
            return;
        }
        ListView<ChatService.SearchHit> list = new ListView<>();
        list.getItems().setAll(hits);
        list.setCellFactory(v -> new ListCell<>() {
            @Override
            protected void updateItem(ChatService.SearchHit h, boolean empty) {
                super.updateItem(h, empty);
                if (empty || h == null) {
                    setText(null);
                    return;
                }
                String title = app.marketplace().store().findListing(h.getThread().getListingId())
                        .map(Listing::getTitle).orElse("listing");
                setText("[" + title + "] " + Ui.time(h.getMessage().getSentAt()) + "\n" + h.getMessage().getText());
            }
        });
        Stage s = new Stage();
        s.initOwner(app.stage());
        s.setTitle(hits.size() + " message(s) found for \"" + keyword.trim() + "\"");
        list.setOnMouseClicked(e -> {
            ChatService.SearchHit h = list.getSelectionModel().getSelectedItem();
            if (h != null && e.getClickCount() == 2) {
                s.close();
                if (h.getThread().getId().equals(thread.getId())) {
                    messages.getSelectionModel().select(h.getMessage());
                    messages.scrollTo(h.getMessage());
                } else {
                    openThread(app, portal, h.getThread(), h.getMessage());
                }
            }
        });
        VBox box = new VBox(8, Ui.muted("Double-click a result to open it."), list);
        box.setPadding(new Insets(12));
        Scene scene = new Scene(box, 460, 360);
        Ui.applyStyle(scene);
        s.setScene(scene);
        s.show();
    }

    /** Draws a message as a bubble: buyer on the right, seller on the left, offers as highlighted cards. */
    private final class MessageCell extends ListCell<ChatMessage> {
        @Override
        protected void updateItem(ChatMessage m, boolean empty) {
            super.updateItem(m, empty);
            setText(null);
            if (empty || m == null) {
                setGraphic(null);
                return;
            }
            boolean mine = m.getSenderRole() == Role.BUYER;
            String who = mine ? "You" : app.marketplace().store().findUser(m.getSenderId())
                    .map(User::getFullName).orElse("Seller");
            Label meta = Ui.muted(who + " · " + Ui.time(m.getSentAt()));
            Label text = new Label(m.getText());
            text.setWrapText(true);
            text.setMaxWidth(340);
            VBox bubble = new VBox(3, meta, text);
            bubble.getStyleClass().add(m.isOfferCard() ? "offer-card" : mine ? "bubble-mine" : "bubble-theirs");
            if (m.isOfferCard()) {
                String status = app.marketplace().store().findDeal(m.getDealId()).map(Deal::getStatus)
                        .map(s -> "Status: " + s.label()).orElse("");
                bubble.getChildren().add(Ui.badge(status, "badge-warn"));
            }
            HBox row = new HBox(bubble);
            row.setAlignment(mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            row.setPrefWidth(480);
            setGraphic(row);
        }
    }
}
