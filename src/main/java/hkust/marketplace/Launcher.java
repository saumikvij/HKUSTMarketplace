package hkust.marketplace;

/**
 * Entry point. Starting JavaFX through a class that does not extend {@code Application} lets the app run from
 * IntelliJ or a plain {@code java} command without module settings.
 */
public final class Launcher {
    private Launcher() { }

    /**
     * Starts the application.
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        MainApp.main(args);
    }
}
