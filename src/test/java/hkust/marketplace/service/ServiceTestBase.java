package hkust.marketplace.service;

import hkust.marketplace.MutableClock;
import hkust.marketplace.data.DataStore;
import hkust.marketplace.data.SeedData;
import hkust.marketplace.model.Listing;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;

/** Builds a fresh in-memory marketplace with the demo seed data before each test. */
abstract class ServiceTestBase {
    static final LocalDateTime T0 = LocalDateTime.of(2026, 11, 20, 12, 0, 0);
    static final String DEMO = "U-demo";
    static final String CHRIS = "U-chris";
    static final String ALICE = "U-alice";
    static final String BOB = "U-bob";
    static final String ADMIN = "U-admin";

    MutableClock clock;
    DataStore store;
    Marketplace app;

    @BeforeEach
    void setUpMarketplace() {
        clock = new MutableClock(T0);
        store = new DataStore();
        SeedData.populate(store, clock);
        app = new Marketplace(store, clock);
    }

    Listing listing(String id) {
        return store.findListing(id).orElseThrow();
    }

    LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
