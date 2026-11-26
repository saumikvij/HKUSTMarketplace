package hkust.marketplace.ui;

import hkust.marketplace.model.ListingType;
import hkust.marketplace.service.ListingService;
import hkust.marketplace.util.ValidationException;
import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

/** Shared look and helpers so every screen (and the Seller/Admin portals) looks the same. */
public final class Ui {
    private Ui() { }

    /** @param scene scene to style with the shared stylesheet */
    public static void applyStyle(Scene scene) {
        scene.getStylesheets().add(Ui.class.getResource("style.css").toExternalForm());
    }

    /**
     * @param text title text
     * @return a page title label
     */
    public static Label title(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("page-title");
        return l;
    }

    /**
     * @param text subtitle text
     * @return a muted subtitle label
     */
    public static Label muted(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("muted");
        l.setWrapText(true);
        return l;
    }

    /**
     * @param text badge text
     * @param style extra style class, e.g. "badge-sell"
     * @return a small coloured badge
     */
    public static Label badge(String text, String style) {
        Label l = new Label(text);
        l.getStyleClass().addAll("badge", style);
        return l;
    }

    /**
     * @param type listing type
     * @return badge for the type
     */
    public static Label typeBadge(ListingType type) {
        return badge(type.label(), "badge-" + type.name().toLowerCase());
    }

    /**
     * Shows an error. A validation error lists every broken rule.
     * @param e the error
     */
    public static void error(Throwable e) {
        String text = e instanceof ValidationException
                ? "• " + String.join("\n• ", ((ValidationException) e).getErrors())
                : e.getMessage();
        Alert a = new Alert(Alert.AlertType.ERROR, text, ButtonType.OK);
        a.setHeaderText(e instanceof ValidationException ? "Please fix the following" : "Cannot do that");
        style(a);
        a.showAndWait();
    }

    /**
     * Shows a success or information message.
     * @param header header
     * @param text message
     */
    public static void info(String header, String text) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, text, ButtonType.OK);
        a.setHeaderText(header);
        style(a);
        a.showAndWait();
    }

    /**
     * Asks a yes/no question.
     * @param header question
     * @param text details
     * @return true if the user confirmed
     */
    public static boolean confirm(String header, String text) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, text, ButtonType.OK, ButtonType.CANCEL);
        a.setHeaderText(header);
        style(a);
        Optional<ButtonType> r = a.showAndWait();
        return r.isPresent() && r.get() == ButtonType.OK;
    }

    /** @param a alert to style like the rest of the app */
    public static void style(Alert a) {
        a.getDialogPane().getStylesheets().add(Ui.class.getResource("style.css").toExternalForm());
        a.getDialogPane().setMinWidth(460);
        a.getDialogPane().setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        String text = a.getContentText();
        if (text != null && !text.isEmpty()) {
            Label content = new Label(text);
            content.setWrapText(true);
            content.setMaxWidth(520);
            content.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
            a.getDialogPane().setContent(content);
        }
        a.setResizable(true);
    }

    /**
     * Loads a listing photo. "res:/..." paths come from inside the app; anything else is a file path or URL.
     * @param path photo path, may be null
     * @param width display width
     * @param height display height
     * @return the image, or a grey "No photo" placeholder if it cannot be loaded
     */
    public static Node photo(String path, double width, double height) {
        Image image = loadImage(path);
        if (image == null || image.isError()) {
            Label l = new Label("No photo");
            l.getStyleClass().add("muted");
            StackPane p = new StackPane(l);
            p.getStyleClass().add("photo-placeholder");
            p.setMinSize(width, height);
            p.setPrefSize(width, height);
            p.setMaxSize(width, height);
            return p;
        }
        ImageView iv = new ImageView(image);
        iv.setFitWidth(width);
        iv.setFitHeight(height);
        iv.setPreserveRatio(true);
        StackPane p = new StackPane(iv);
        p.setAlignment(Pos.CENTER);
        p.getStyleClass().add("photo-frame");
        p.setMinSize(width, height);
        p.setPrefSize(width, height);
        p.setMaxSize(width, height);
        return p;
    }

    private static Image loadImage(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        try {
            if (path.startsWith("photo:")) {
                byte[] bytes = java.util.Base64.getDecoder().decode(path.substring(6));
                return new Image(new java.io.ByteArrayInputStream(bytes), 1200, 1200, true, true);
            }
            if (path.startsWith("res:")) {
                InputStream in = Ui.class.getResourceAsStream(path.substring(4));
                return in == null ? null : new Image(in);
            }
            if (path.startsWith("http") || path.startsWith("file:")) {
                return new Image(path, true);
            }
            File f = new File(path);
            return f.exists() ? new Image(f.toURI().toString()) : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * @param price price or null
     * @return "HK$120.00", or an empty string
     */
    public static String money(BigDecimal price) {
        return price == null ? "" : "HK$" + price.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    /**
     * @param t time or null
     * @return formatted time, or an empty string
     */
    public static String time(LocalDateTime t) {
        return t == null ? "" : t.format(ListingService.DISPLAY_TIME);
    }
}
