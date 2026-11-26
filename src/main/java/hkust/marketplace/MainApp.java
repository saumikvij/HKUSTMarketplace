package hkust.marketplace;

import hkust.marketplace.data.DataStore;
import hkust.marketplace.data.SeedData;
import hkust.marketplace.service.Marketplace;
import hkust.marketplace.ui.AppContext;
import java.nio.file.Path;
import java.time.Clock;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * JavaFX application. Loads (or seeds) the data file, starts the one-second timer for auto-return and
 * due-date reminders, and shows the welcome screen.
 */
public class MainApp extends Application {
    /** Data file, in the folder the app is started from. Delete it to reset to the demo data. */
    public static final Path DATA_FILE = Path.of("marketplace-data.ser");

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
        DataStore store = DataStore.load(DATA_FILE);
        if (store.isEmpty()) {
            SeedData.populate(store, Clock.systemDefaultZone());
            store.save();
        }
        Marketplace marketplace = new Marketplace(store, Clock.systemDefaultZone());
        AppContext app = new AppContext(marketplace, stage);
        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> app.tick()));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
        app.showWelcome();
        stage.show();
    }

    @Override
    public void stop() {
        if (timer != null) {
            timer.stop();
        }
    }
}
