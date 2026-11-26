package hkust.marketplace.ui;

import hkust.marketplace.model.Category;
import hkust.marketplace.model.Condition;
import hkust.marketplace.model.Listing;
import hkust.marketplace.model.ListingType;
import hkust.marketplace.service.BrowseQuery;
import hkust.marketplace.service.ListingService;
import hkust.marketplace.service.SortOrder;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Browse Marketplace (Task 1.3): approved listings with search, filters and sorting. */
public class BrowseView implements Page {
    private final AppContext app;
    private final BuyerPortal portal;
    private final VBox root = new VBox(12);
    private final FlowPane grid = new FlowPane(14, 14);
    private final Label count = new Label();
    private final TextField keyword = new TextField();
    private final ComboBox<Category> category = new ComboBox<>();
    private final ComboBox<ListingType> type = new ComboBox<>();
    private final ComboBox<Condition> condition = new ComboBox<>();
    private final TextField minPrice = new TextField();
    private final TextField maxPrice = new TextField();
    private final ComboBox<SortOrder> sort = new ComboBox<>();

    /**
     * @param app app context
     * @param portal portal shell
     */
    public BrowseView(AppContext app, BuyerPortal portal) {
        this.app = app;
        this.portal = portal;
        build();
    }

    private void build() {
        keyword.setPromptText("Search title or description");
        keyword.setId("browse-keyword");
        HBox.setHgrow(keyword, Priority.ALWAYS);
        Button search = new Button("Search");
        search.getStyleClass().add("primary");
        search.setDefaultButton(true);
        search.setOnAction(e -> refresh());
        Button clear = new Button("Clear");
        clear.setOnAction(e -> clearFilters());
        HBox searchRow = new HBox(8, keyword, search, clear);

        category.getItems().add(null);
        category.getItems().addAll(Category.values());
        category.setPromptText("Any category");
        type.getItems().add(null);
        type.getItems().addAll(ListingType.values());
        type.setPromptText("Any type");
        condition.getItems().add(null);
        condition.getItems().addAll(Condition.values());
        condition.setPromptText("Any condition");
        minPrice.setPromptText("Min HK$");
        minPrice.setPrefWidth(80);
        maxPrice.setPromptText("Max HK$");
        maxPrice.setPrefWidth(80);
        sort.getItems().addAll(SortOrder.values());
        sort.setValue(SortOrder.NEWEST);
        Button apply = new Button("Apply filters");
        apply.setOnAction(e -> refresh());
        sort.setOnAction(e -> refresh());
        HBox filterRow = new HBox(8, category, type, condition, minPrice, new Label("-"), maxPrice,
                new Label("Sort:"), sort, apply);
        filterRow.setAlignment(Pos.CENTER_LEFT);

        grid.setPadding(new Insets(4));
        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("plain-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        count.getStyleClass().add("muted");

        root.getChildren().addAll(Ui.title("Browse Marketplace"), searchRow, filterRow, count, scroll);
        root.setPadding(new Insets(20));
    }

    private void clearFilters() {
        keyword.clear();
        category.setValue(null);
        type.setValue(null);
        condition.setValue(null);
        minPrice.clear();
        maxPrice.clear();
        sort.setValue(SortOrder.NEWEST);
        refresh();
    }

    @Override public Parent node() { return root; }

    @Override
    public void refresh() {
        BrowseQuery q = new BrowseQuery().keyword(keyword.getText()).category(category.getValue())
                .type(type.getValue()).condition(condition.getValue()).minPrice(minPrice.getText())
                .maxPrice(maxPrice.getText()).sort(sort.getValue());
        List<Listing> results;
        try {
            results = app.marketplace().listings().browse(q);
        } catch (RuntimeException e) {
            Ui.error(e);
            return;
        }
        grid.getChildren().clear();
        boolean anyListing = app.marketplace().store().listings().stream()
                .anyMatch(app.marketplace().listings()::isVisibleToBuyers);
        if (results.isEmpty()) {
            Label empty = Ui.muted(anyListing ? "No listings match your search." : "No listings available yet.");
            grid.getChildren().add(empty);
            count.setText("0 results");
            return;
        }
        count.setText(results.size() + (results.size() == 1 ? " result" : " results"));
        for (Listing l : results) {
            grid.getChildren().add(card(l));
        }
    }

    private VBox card(Listing l) {
        ListingService ls = app.marketplace().listings();
        Label title = new Label(l.getTitle());
        title.getStyleClass().add("card-title");
        title.setWrapText(true);
        Label price = new Label(l.getType() == ListingType.SELL ? Ui.money(l.getPrice())
                : l.getType() == ListingType.LEND ? "Free to borrow" : "Free");
        price.getStyleClass().add("price");
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        HBox top = new HBox(6, price, gap, Ui.typeBadge(l.getType()));
        top.setAlignment(Pos.CENTER_LEFT);
        String avail = ls.availabilityText(l);
        Label availability = Ui.badge(avail, "Available".equals(avail) ? "badge-ok" : "badge-warn");
        VBox card = new VBox(6, Ui.photo(l.getThumbnail(), 220, 140), top, title,
                Ui.muted(ls.sellerOf(l).getFullName() + " · " + l.getCategory() + " · " + l.getCondition()),
                Ui.muted("Meet-up: " + l.getMeetupLocation()),
                Ui.muted("Posted " + Ui.time(l.getPostedAt())), availability);
        card.getStyleClass().addAll("card", "listing-card");
        card.setPrefWidth(244);
        card.setPadding(new Insets(12));
        card.setCursor(Cursor.HAND);
        card.setOnMouseClicked(e -> openDetail(l.getId()));
        return card;
    }

    private void openDetail(String listingId) {
        new ListingDetailDialog(app, portal, listingId).show();
        refresh();
    }
}
