package hkust.marketplace.ui;

import hkust.marketplace.model.Category;
import hkust.marketplace.model.Deal;
import hkust.marketplace.model.DealType;
import hkust.marketplace.model.Listing;
import hkust.marketplace.model.Review;
import hkust.marketplace.service.DealService;
import hkust.marketplace.service.HistoryQuery;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Deal History (Task 1.9): every finished purchase, borrow and giveaway, searchable and filterable. */
public class HistoryView implements Page {
    private final AppContext app;
    private final BuyerPortal portal;
    private final VBox root = new VBox(12);
    private final TableView<Deal> table = new TableView<>();
    private final TextField keyword = new TextField();
    private final DatePicker from = new DatePicker();
    private final DatePicker to = new DatePicker();
    private final ComboBox<Category> category = new ComboBox<>();
    private final ComboBox<DealType> type = new ComboBox<>();
    private final Label count = Ui.muted("");
    private final Button view = new Button("View deal");
    private final Button rate = new Button("Rate seller");

    /**
     * @param app app context
     * @param portal portal shell
     */
    public HistoryView(AppContext app, BuyerPortal portal) {
        this.app = app;
        this.portal = portal;
        keyword.setPromptText("Item or seller name");
        HBox.setHgrow(keyword, Priority.ALWAYS);
        from.setPromptText("From date");
        to.setPromptText("To date");
        category.getItems().add(null);
        category.getItems().addAll(Category.values());
        category.setPromptText("Any category");
        type.getItems().add(null);
        type.getItems().addAll(DealType.values());
        type.setPromptText("Any type");
        type.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(DealType t) { return t == null ? "Any type" : t.historyLabel(); }
            @Override public DealType fromString(String s) { return null; }
        });
        Button search = new Button("Search");
        search.getStyleClass().add("primary");
        search.setDefaultButton(true);
        search.setOnAction(e -> refresh());
        Button clear = new Button("Clear");
        clear.setOnAction(e -> {
            keyword.clear();
            from.setValue(null);
            to.setValue(null);
            category.setValue(null);
            type.setValue(null);
            refresh();
        });
        keyword.setPrefWidth(200);
        from.setPrefWidth(140);
        to.setPrefWidth(140);
        javafx.scene.layout.FlowPane filters = new javafx.scene.layout.FlowPane(8, 8, keyword, from, to, category, type,
                search, clear);
        filters.setAlignment(Pos.CENTER_LEFT);

        table.getColumns().add(col("Date", 150, d -> Ui.time(DealService.dealDate(d))));
        table.getColumns().add(col("Item", 200, d -> listing(d).map(Listing::getTitle).orElse("")));
        table.getColumns().add(col("Seller", 120, d -> listing(d)
                .map(l -> app.marketplace().listings().sellerOf(l).getFullName()).orElse("")));
        table.getColumns().add(col("Type", 80, d -> d.getType().historyLabel()));
        table.getColumns().add(col("Amount / period", 220, this::terms));
        table.getColumns().add(col("Outcome", 110, d -> d.getStatus().label()));
        table.getColumns().add(col("Your rating", 100, d -> review(d)
                .map(r -> "★".repeat(r.getRating()) + "☆".repeat(5 - r.getRating())).orElse("Not rated")));
        table.setPlaceholder(new Label("No finished deals found."));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().selectedItemProperty().addListener((o, x, y) -> updateButtons());
        VBox.setVgrow(table, Priority.ALWAYS);

        view.setOnAction(e -> {
            Deal d = table.getSelectionModel().getSelectedItem();
            if (d != null) {
                showDeal(d);
            }
        });
        rate.getStyleClass().add("primary");
        rate.setOnAction(e -> {
            Deal d = table.getSelectionModel().getSelectedItem();
            if (d != null && ReviewDialog.show(app, d)) {
                refresh();
            }
        });
        root.getChildren().addAll(Ui.title("Deal History"),
                Ui.muted("Your finished purchases, borrows and giveaways."), filters, count, table,
                new HBox(8, view, rate));
        root.setPadding(new Insets(20));
    }

    private TableColumn<Deal, String> col(String name, double width, Function<Deal, String> value) {
        TableColumn<Deal, String> c = new TableColumn<>(name);
        c.setPrefWidth(width);
        c.setCellValueFactory(cd -> new SimpleStringProperty(value.apply(cd.getValue())));
        return c;
    }

    private Optional<Listing> listing(Deal d) {
        return app.marketplace().store().findListing(d.getListingId());
    }

    private Optional<Review> review(Deal d) {
        return app.marketplace().reviews().reviewForDeal(d.getId());
    }

    private String terms(Deal d) {
        switch (d.getType()) {
            case OFFER: return Ui.money(d.getCurrentPrice());
            case BORROW: return Ui.time(d.getBorrowStart()) + " to " + Ui.time(d.getBorrowEnd());
            default: return "Free";
        }
    }

    private void showDeal(Deal d) {
        Listing l = listing(d).orElse(null);
        StringBuilder sb = new StringBuilder();
        sb.append("Item: ").append(l == null ? "" : l.getTitle()).append('\n');
        sb.append("Seller: ").append(l == null ? "" : app.marketplace().listings().sellerOf(l).getFullName())
                .append('\n');
        sb.append("Type: ").append(d.getType().historyLabel()).append('\n');
        sb.append("Amount / period: ").append(terms(d)).append('\n');
        sb.append("Requested: ").append(Ui.time(d.getSubmittedAt())).append('\n');
        sb.append("Finished: ").append(Ui.time(d.getCompletedAt())).append('\n');
        sb.append("Outcome: ").append(d.getStatus().label()).append('\n');
        if (l != null) {
            sb.append("Meet-up: ").append(l.getMeetupLocation()).append('\n');
        }
        review(d).ifPresentOrElse(r -> {
            sb.append("\nYour review: ").append("★".repeat(r.getRating())).append('\n').append(r.getComment());
            if (r.getSellerReply() != null) {
                sb.append("\nSeller reply: ").append(r.getSellerReply());
            }
        }, () -> sb.append("\nYou have not rated this deal yet."));
        Ui.info("Deal details", sb.toString());
    }

    private void updateButtons() {
        Deal d = table.getSelectionModel().getSelectedItem();
        view.setDisable(d == null);
        rate.setDisable(d == null || review(d).isPresent());
    }

    @Override public Parent node() { return root; }

    @Override
    public void refresh() {
        HistoryQuery q = new HistoryQuery().keyword(keyword.getText()).from(from.getValue()).to(to.getValue())
                .category(category.getValue()).dealType(type.getValue());
        List<Deal> deals;
        try {
            deals = app.marketplace().deals().history(app.currentUser().getId(), q);
        } catch (RuntimeException e) {
            Ui.error(e);
            return;
        }
        table.getItems().setAll(deals);
        boolean anyHistory = !app.marketplace().deals().history(app.currentUser().getId(), new HistoryQuery())
                .isEmpty();
        table.setPlaceholder(new Label(anyHistory ? "No deals match your search."
                : "You have no finished deals yet."));
        count.setText(deals.size() + (deals.size() == 1 ? " deal" : " deals"));
        updateButtons();
    }
}
