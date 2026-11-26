package hkust.marketplace.ui;

import hkust.marketplace.model.Deal;
import hkust.marketplace.model.DealStatus;
import hkust.marketplace.model.DealType;
import hkust.marketplace.model.Listing;
import hkust.marketplace.service.DealService;
import java.util.List;
import java.util.function.Function;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * My Deals (Task 1.6): every offer, borrow request and claim, with actions to reopen the listing or chat,
 * answer a counter-offer, withdraw, return a borrowed item early or mark a deal complete.
 */
public class MyDealsView implements Page {
    private final AppContext app;
    private final BuyerPortal portal;
    private final VBox root = new VBox(12);
    private final TableView<Deal> active = new TableView<>();
    private final TableView<Deal> closed = new TableView<>();
    private final TabPane tabs = new TabPane();
    private final Button openListing = new Button("Open listing");
    private final Button openChat = new Button("Open chat");
    private final Button acceptCounter = new Button("Accept counter-offer");
    private final Button declineCounter = new Button("Decline counter-offer");
    private final Button withdraw = new Button("Withdraw");
    private final Button returnItem = new Button("Return item");
    private final Button complete = new Button("Mark complete");
    private final Button rate = new Button("Rate seller");

    /**
     * @param app app context
     * @param portal portal shell
     */
    public MyDealsView(AppContext app, BuyerPortal portal) {
        this.app = app;
        this.portal = portal;
        setUp(active);
        setUp(closed);
        Tab a = new Tab("Active", active);
        Tab c = new Tab("Completed / closed", closed);
        tabs.getTabs().addAll(a, c);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getSelectionModel().selectedItemProperty().addListener((o, x, y) -> updateButtons());
        VBox.setVgrow(tabs, Priority.ALWAYS);

        acceptCounter.getStyleClass().add("primary");
        complete.getStyleClass().add("primary");
        openListing.setOnAction(e -> withSelected(d -> {
            new ListingDetailDialog(app, portal, d.getListingId()).show();
            return null;
        }));
        openChat.setOnAction(e -> withSelected(d -> {
            ChatWindow.open(app, portal, d.getListingId());
            return null;
        }));
        acceptCounter.setOnAction(e -> act(d -> app.marketplace().deals()
                .respondToCounter(app.currentUser().getId(), d.getId(), true), "Counter-offer accepted", null));
        declineCounter.setOnAction(e -> act(d -> app.marketplace().deals()
                .respondToCounter(app.currentUser().getId(), d.getId(), false), "Counter-offer declined",
                "Decline the seller's counter-offer?"));
        withdraw.setOnAction(e -> act(d -> app.marketplace().deals().withdraw(app.currentUser().getId(), d.getId()),
                "Request withdrawn", "Withdraw this request?"));
        returnItem.setOnAction(e -> act(d -> app.marketplace().deals().returnItem(app.currentUser().getId(), d.getId()),
                "Item returned", "Return this item now, before the due time?"));
        complete.setOnAction(e -> {
            Deal done = act(d -> app.marketplace().deals().markComplete(app.currentUser().getId(), d.getId()),
                    null, "Confirm that you have received the item?");
            if (done != null) {
                ReviewDialog.show(app, done);
                refresh();
            }
        });
        rate.setOnAction(e -> withSelected(d -> {
            if (ReviewDialog.show(app, d)) {
                refresh();
            }
            return null;
        }));

        FlowPane buttons = new FlowPane(8, 8, openListing, openChat, acceptCounter, declineCounter, withdraw,
                returnItem, complete, rate);
        root.getChildren().addAll(Ui.title("My Deals"),
                Ui.muted("Everything you have offered on, asked to borrow or claimed. Select a row to act on it."),
                tabs, buttons);
        root.setPadding(new Insets(20));
    }

    private void setUp(TableView<Deal> t) {
        t.getColumns().add(col("Item", 190, d -> listing(d).map(Listing::getTitle).orElse("(removed)")));
        t.getColumns().add(col("Seller", 110, d -> listing(d)
                .map(l -> app.marketplace().listings().sellerOf(l).getFullName()).orElse("")));
        t.getColumns().add(col("Type", 60, d -> d.getType().label()));
        t.getColumns().add(col("Terms", 230, this::terms));
        t.getColumns().add(col("Status", 100, d -> d.getStatus().label()));
        t.getColumns().add(col("Due / time left", 150, this::due));
        t.getColumns().add(col("Last update", 150, d -> Ui.time(d.getUpdatedAt())));
        t.setPlaceholder(new Label("You have no deals here yet. Find something in Browse Marketplace."));
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        t.getSelectionModel().selectedItemProperty().addListener((o, x, y) -> updateButtons());
        t.setRowFactory(v -> {
            javafx.scene.control.TableRow<Deal> row = new javafx.scene.control.TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    new ListingDetailDialog(app, portal, row.getItem().getListingId()).show();
                }
            });
            return row;
        });
    }

    private TableColumn<Deal, String> col(String name, double width, Function<Deal, String> value) {
        TableColumn<Deal, String> c = new TableColumn<>(name);
        c.setPrefWidth(width);
        c.setCellValueFactory(cd -> new SimpleStringProperty(value.apply(cd.getValue())));
        return c;
    }

    private java.util.Optional<Listing> listing(Deal d) {
        return app.marketplace().store().findListing(d.getListingId());
    }

    private String terms(Deal d) {
        if (d.getType() == DealType.OFFER) {
            String s = "Offer " + Ui.money(d.getOfferedPrice());
            return d.getCounterPrice() == null ? s : s + " → counter " + Ui.money(d.getCounterPrice());
        }
        if (d.getType() == DealType.BORROW) {
            if (d.getStatus() == DealStatus.COUNTERED && d.getCounterStart() != null) {
                return "Counter: " + Ui.time(d.getCounterStart()) + " to " + Ui.time(d.getCounterEnd());
            }
            return Ui.time(d.getBorrowStart()) + " to " + Ui.time(d.getBorrowEnd());
        }
        return "Giveaway claim";
    }

    private String due(Deal d) {
        if (d.getStatus() != DealStatus.BORROWED || d.getBorrowEnd() == null) {
            return d.getStatus().isFinished() ? "Finished " + Ui.time(d.getCompletedAt()) : "";
        }
        return "Due " + d.getBorrowEnd().toLocalTime().withNano(0) + " · "
                + DealService.formatDuration(app.marketplace().deals().remaining(d)) + " left";
    }

    private Deal selected() {
        TableView<Deal> t = tabs.getSelectionModel().getSelectedIndex() == 0 ? active : closed;
        return t.getSelectionModel().getSelectedItem();
    }

    private void withSelected(Function<Deal, Void> f) {
        Deal d = selected();
        if (d != null) {
            f.apply(d);
            refresh();
        }
    }

    private Deal act(Function<Deal, Deal> action, String success, String question) {
        Deal d = selected();
        if (d == null) {
            return null;
        }
        if (question != null && !Ui.confirm(question, "")) {
            return null;
        }
        try {
            Deal result = action.apply(d);
            if (success != null) {
                Ui.info(success, "The seller has been notified.");
            }
            return result;
        } catch (RuntimeException e) {
            Ui.error(e);
            return null;
        } finally {
            refresh();
        }
    }

    private void updateButtons() {
        Deal d = selected();
        boolean any = d != null;
        DealStatus s = any ? d.getStatus() : null;
        openListing.setDisable(!any);
        openChat.setDisable(!any);
        acceptCounter.setDisable(s != DealStatus.COUNTERED);
        declineCounter.setDisable(s != DealStatus.COUNTERED);
        withdraw.setDisable(!(any && s.isOpenRequest()));
        returnItem.setDisable(s != DealStatus.BORROWED);
        complete.setDisable(!(s == DealStatus.ACCEPTED && d.getType() != DealType.BORROW));
        rate.setDisable(!(any && s.isFinished()
                && app.marketplace().reviews().reviewForDeal(d.getId()).isEmpty()));
    }

    @Override public Parent node() { return root; }

    @Override
    public void refresh() {
        String buyer = app.currentUser().getId();
        reload(active, app.marketplace().deals().activeDeals(buyer));
        reload(closed, app.marketplace().deals().closedDeals(buyer));
        updateButtons();
    }

    private static void reload(TableView<Deal> t, List<Deal> deals) {
        Deal keep = t.getSelectionModel().getSelectedItem();
        t.getItems().setAll(deals);
        if (keep != null && deals.contains(keep)) {
            t.getSelectionModel().select(keep);
        }
    }

    @Override
    public void tick() {
        active.refresh();
    }

    /**
     * Selects a deal (used when opening a notification).
     * @param dealId deal to select
     */
    public void select(String dealId) {
        for (int i = 0; i < 2; i++) {
            TableView<Deal> t = i == 0 ? active : closed;
            for (Deal d : t.getItems()) {
                if (d.getId().equals(dealId)) {
                    tabs.getSelectionModel().select(i);
                    t.getSelectionModel().select(d);
                    t.scrollTo(d);
                    return;
                }
            }
        }
    }
}
