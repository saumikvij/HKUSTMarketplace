package hkust.marketplace;

import hkust.marketplace.data.DataStore;
import hkust.marketplace.data.SeedData;
import hkust.marketplace.service.Marketplace;
import hkust.marketplace.ui.AppContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import javafx.application.Platform;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * JavaFX application. Loads (or seeds) the data file, starts the one-second timer for auto-return and
 * due-date reminders, and shows the welcome screen.
 */
public class MainApp extends Application {
    /** Data file, in the folder the app is started from. Delete it to reset the marketplace. */
    public static final Path DATA_FILE = Path.of("marketplace-data.ser");

    /**
     * Start-up data for a new data file: {@code demo} (default) seeds demo accounts and listings, {@code clean}
     * seeds only the admin account. Set with {@code -Dmarketplace.seed=clean}.
     */
    public static final String SEED_PROPERTY = "marketplace.seed";

    private Timeline timer;

    /**
     * Launches JavaFX.
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> reportUnexpected(error));
        DataStore store = loadOrReset();
        if (store.isEmpty()) {
            if ("clean".equalsIgnoreCase(System.getProperty(SEED_PROPERTY, "demo").trim())) {
                SeedData.populateAdminOnly(store, Clock.systemDefaultZone());
            } else {
                SeedData.populate(store, Clock.systemDefaultZone());
            }
            store.save();
        }
        Marketplace marketplace = new Marketplace(store, Clock.systemDefaultZone());
        AppContext app = new AppContext(marketplace, stage);
        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            try {
                app.tick();
            } catch (RuntimeException ex) {
                ex.printStackTrace();
            }
        }));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
        app.showWelcome();
        stage.show();
    }

    /**
     * Loads the data file. A file that cannot be read (for example one saved by an older build) is kept as a
     * backup and the app starts from fresh demo data instead of failing to open.
     */
    private static DataStore loadOrReset() throws IOException {
        try {
            return DataStore.load(DATA_FILE);
        } catch (IOException | RuntimeException e) {
            Path backup = DATA_FILE.resolveSibling(DATA_FILE.getFileName() + ".unreadable");
            Files.move(DATA_FILE, backup, StandardCopyOption.REPLACE_EXISTING);
            System.err.println("Could not read " + DATA_FILE + " (" + e + "); moved it to " + backup
                    + " and started with fresh demo data.");
            return DataStore.load(DATA_FILE);
        }
    }

    /** Shows an unexpected error instead of letting it go unnoticed; the app keeps running. */
    private static void reportUnexpected(Throwable error) {
        error.printStackTrace();
        Runnable show = () -> {
            Alert alert = new Alert(Alert.AlertType.ERROR, String.valueOf(error.getMessage()), ButtonType.OK);
            alert.setHeaderText("Something went wrong, but your data is safe");
            alert.show();
        };
        if (Platform.isFxApplicationThread()) {
            show.run();
        } else {
            Platform.runLater(show);
        }
    }

    @Override
    public void stop() {
        if (timer != null) {
            timer.stop();
        }
    }
}
