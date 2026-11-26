package hkust.marketplace.ui;

import hkust.marketplace.service.MeetupLocations;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Function;

/** Form controls shared by the seller screens. */
final class SellerInputs {
    private SellerInputs() { }

    static ListView<String> locations(List<String> selected) {
        ListView<String> list = new ListView<>();
        list.getItems().setAll(MeetupLocations.ALL);
        list.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        selected.forEach(value -> list.getSelectionModel().select(value));
        list.setPrefHeight(106);
        return list;
    }

    static GridPane form() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        return grid;
    }

    static Label row(GridPane grid, int index, String name, Node field) {
        Label label = new Label(name);
        label.setLabelFor(field);
        grid.addRow(index, label, field);
        GridPane.setHgrow(field, Priority.ALWAYS);
        return label;
    }

    static Button primary(String title, Runnable action) {
        Button button = new Button(title);
        button.getStyleClass().add("primary");
        button.setOnAction(event -> action.run());
        return button;
    }

    static TextArea textArea(String value) {
        TextArea area = new TextArea(value == null ? "" : value);
        area.setWrapText(true);
        area.setPrefRowCount(3);
        return area;
    }

    static <T> ListView<T> list(String emptyMessage, Function<T, String> describe) {
        ListView<T> list = new ListView<>();
        list.setPlaceholder(new Label(emptyMessage));
        list.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : describe.apply(item));
                setWrapText(true);
            }
        });
        VBox.setVgrow(list, Priority.ALWAYS);
        return list;
    }
}
