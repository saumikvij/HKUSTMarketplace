package hkust.marketplace.ui;

import javafx.scene.Parent;

/** A screen inside the buyer portal. */
public interface Page {
    /** @return the page content */
    Parent node();

    /** Reloads the page from the services. */
    void refresh();

    /** Called every second while the page is open (e.g. to update countdowns). */
    default void tick() { }
}
