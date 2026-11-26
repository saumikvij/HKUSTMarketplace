package hkust.marketplace.ui;

import hkust.marketplace.model.ChatMessage;
import hkust.marketplace.model.ChatThread;
import hkust.marketplace.model.Listing;
import hkust.marketplace.model.Role;
import hkust.marketplace.model.User;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Messages inbox shared by the Buyer and Seller portals: every conversation the signed-in user takes part in,
 * newest first, with the other person, the item and the last message. Opening one shows the usual chat window,
 * so a buyer or seller can reach a chat without first finding the listing or the offer.
 */
public class MessagesView implements Page {
    private final AppContext app;
    private final BuyerPortal buyerPortal;
    private final VBox root = new VBox(12);
    private final ListView<ChatThread> list = new ListView<>();
    private final Label summary = Ui.muted("");
    private long shownMessages = -1;

    /**
     * @param app app context
     * @param buyerPortal the buyer portal shell, or null when opened from the Seller Portal
     */
    public MessagesView(AppContext app, BuyerPortal buyerPortal) {
        this.app = app;
        this.buyerPortal = buyerPortal;
        Button open = new Button("Open chat");
        open.getStyleClass().add("primary");
        open.disableProperty().bind(list.getSelectionModel().selectedItemProperty().isNull());
        open.setOnAction(e -> openSelected());
        Button refresh = new Button("Refresh");
        refresh.setOnAction(e -> refresh());
        HBox bar = new HBox(10, open, refresh);
        bar.setAlignment(Pos.CENTER_LEFT);

        list.setCellFactory(v -> new ThreadCell());
        list.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                openSelected();
            }
        });
        boolean buyer = app.currentUser().getRole() == Role.BUYER;
        list.setPlaceholder(new Label(buyer
                ? "No conversations yet. Use \"Chat with Seller\" on a listing to start one."
                : "No conversations yet. Messages from buyers will appear here."));
        VBox.setVgrow(list, Priority.ALWAYS);
        root.getChildren().addAll(Ui.title("Messages"), summary, bar, list,
                Ui.muted("Double-click a conversation to open the chat."));
        root.setPadding(new Insets(20));
    }

    private void openSelected() {
        ChatThread thread = list.getSelectionModel().getSelectedItem();
        if (thread == null) {
            return;
        }
        try {
            ChatWindow.openThread(app, buyerPortal, thread, null);
        } catch (RuntimeException ex) {
            Ui.error(ex);
        }
    }

    @Override public Parent node() { return root; }

    @Override
    public void refresh() {
        List<ChatThread> threads = threads();
        ChatThread keep = list.getSelectionModel().getSelectedItem();
        list.getItems().setAll(threads);
        if (keep != null) {
            threads.stream().filter(t -> t.getId().equals(keep.getId())).findFirst()
                    .ifPresent(t -> list.getSelectionModel().select(t));
        }
        shownMessages = messageCount(threads);
        summary.setText(threads.isEmpty() ? "Your conversations with "
                + (app.currentUser().getRole() == Role.BUYER ? "sellers" : "buyers") + " appear here."
                : threads.size() + " conversation" + (threads.size() == 1 ? "" : "s") + ", newest first.");
    }

    /** Picks up new chats and new messages while the page is open. */
    @Override
    public void tick() {
        List<ChatThread> threads = threads();
        if (threads.size() != list.getItems().size() || messageCount(threads) != shownMessages) {
            refresh();
        }
    }

    private List<ChatThread> threads() {
        return app.marketplace().chat().threadsFor(app.currentUser().getId()).stream()
                .sorted(Comparator.comparing(MessagesView::lastActivity,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private static long messageCount(List<ChatThread> threads) {
        return threads.stream().mapToLong(t -> t.getMessages().size()).sum();
    }

    private static LocalDateTime lastActivity(ChatThread thread) {
        List<ChatMessage> messages = thread.getMessages();
        return messages.isEmpty() ? null : messages.get(messages.size() - 1).getSentAt();
    }

    private final class ThreadCell extends ListCell<ChatThread> {
        @Override
        protected void updateItem(ChatThread thread, boolean empty) {
            super.updateItem(thread, empty);
            setText(null);
            if (empty || thread == null) {
                setGraphic(null);
                return;
            }
            var store = app.marketplace().store();
            String me = app.currentUser().getId();
            String otherId = me.equals(thread.getBuyerId()) ? thread.getSellerId() : thread.getBuyerId();
            String other = store.findUser(otherId).map(u -> u.getFullName() + " (@" + u.getUsername() + ")")
                    .orElse("Unknown user");
            Listing listing = store.findListing(thread.getListingId()).orElse(null);

            Label title = new Label(listing == null ? "Removed listing" : listing.getTitle());
            title.getStyleClass().add("card-title");
            Label with = Ui.muted("with " + other);

            List<ChatMessage> messages = thread.getMessages();
            String preview;
            if (messages.isEmpty()) {
                preview = "No messages yet.";
            } else {
                ChatMessage last = messages.get(messages.size() - 1);
                String who = last.getSenderId().equals(me) ? "You: "
                        : store.findUser(last.getSenderId()).map(User::getFullName).orElse("Them") + ": ";
                preview = who + (last.isOfferCard() ? "[Offer] " : "") + oneLine(last.getText());
            }
            Label last = new Label(preview);
            last.setMaxWidth(760);

            Region gap = new Region();
            HBox.setHgrow(gap, Priority.ALWAYS);
            HBox top = new HBox(8, title, gap,
                    Ui.muted(messages.isEmpty() ? "" : Ui.time(lastActivity(thread))));
            top.setAlignment(Pos.CENTER_LEFT);
            HBox meta = new HBox(8, Ui.badge(messages.size() + " message" + (messages.size() == 1 ? "" : "s"),
                    "badge-info"), with);
            meta.setAlignment(Pos.CENTER_LEFT);
            VBox box = new VBox(4, top, last, meta);
            box.setPadding(new Insets(6, 4, 6, 4));
            setGraphic(box);
        }
    }

    private static String oneLine(String text) {
        String flat = text == null ? "" : text.replace('\n', ' ').trim();
        return flat.length() > 120 ? flat.substring(0, 117) + "..." : flat;
    }
}
