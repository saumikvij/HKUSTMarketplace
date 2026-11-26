package hkust.marketplace.ui;

import hkust.marketplace.model.ListingStatus;
import hkust.marketplace.model.ListingType;
import hkust.marketplace.service.SellerStats;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.chart.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Locale;

/** Seller performance totals and charts (Task 2.8). */
final class SellerStatsView implements Page {
    private final SellerPortal portal;
    private final ScrollPane root;
    private final VBox results = new VBox(16);

    SellerStatsView(SellerPortal portal) {
        this.portal = portal;
        Button recalculate = new Button("Refresh");
        recalculate.setOnAction(event -> refresh());
        VBox content = new VBox(12, new HBox(12, Ui.title("Seller Stats"), recalculate), results);
        content.setPadding(new Insets(20));
        root = new ScrollPane(content);
        root.setFitToWidth(true);
    }

    @Override public Parent node() { return root; }

    @Override public void refresh() {
        results.getChildren().clear();
        SellerStats stats;
        try {
            stats = portal.market().sellerStats(portal.me());
        } catch (RuntimeException exception) {
            results.getChildren().add(new Label("Statistics are unavailable. " + exception.getMessage()));
            return;
        }
        String rating = stats.rating().isPresent() ? String.format(Locale.ROOT, "%.2f / 5 (%d)",
                stats.rating().getAsDouble(), stats.reviewCount()) : "Not yet rated";
        HBox cards = new HBox(10, metric("Listing views", Long.toString(stats.views())),
                metric("Requests received", Long.toString(stats.requests())),
                metric("Completed deals", Long.toString(stats.completed())), metric("Average rating", rating));
        results.getChildren().addAll(cards, Ui.muted("Calculated " + Ui.time(stats.calculatedAt())));
        try {
            results.getChildren().add(requestChart(stats));
            results.getChildren().add(stats.listingsByStatus().isEmpty()
                    ? Ui.muted("No current listings to chart.") : statusChart(stats));
        } catch (RuntimeException exception) {
            results.getChildren().add(new Label("A chart could not be displayed."));
        }
    }

    private VBox metric(String name, String value) {
        VBox card = new VBox(6, Ui.muted(name), Ui.title(value));
        card.getStyleClass().add("card");
        card.setPadding(new Insets(12));
        card.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private BarChart<String, Number> requestChart(SellerStats stats) {
        var axis = new NumberAxis();
        axis.setLabel("Requests");
        var bar = new BarChart<String, Number>(new CategoryAxis(), axis);
        bar.setTitle("Requests received by listing type");
        bar.setLegendVisible(false);
        bar.setAnimated(false);
        bar.setPrefHeight(280);
        var series = new XYChart.Series<String, Number>();
        for (ListingType type : ListingType.values()) {
            series.getData().add(new XYChart.Data<>(type.label(), stats.requestsByType().getOrDefault(type, 0L)));
        }
        bar.getData().add(series);
        return bar;
    }

    private PieChart statusChart(SellerStats stats) {
        var pie = new PieChart();
        pie.setTitle("Current listings by status");
        pie.setAnimated(false);
        pie.setPrefHeight(320);
        for (ListingStatus status : ListingStatus.values()) {
            long count = stats.listingsByStatus().getOrDefault(status, 0L);
            if (count > 0) pie.getData().add(new PieChart.Data(SellerListingsView.label(status) + " (" + count + ")", count));
        }
        return pie;
    }
}
