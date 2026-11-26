package hkust.marketplace.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hkust.marketplace.MutableClock;
import hkust.marketplace.model.Buyer;
import hkust.marketplace.model.Listing;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Saving and loading the data file, and look-ups. */
class DataStoreTest {
    @TempDir Path dir;

    private final MutableClock clock = new MutableClock(LocalDateTime.of(2026, 11, 20, 12, 0));

    @Test
    void missingFileGivesEmptyStoreBoundToFile() throws IOException {
        Path f = dir.resolve("data.ser");
        DataStore s = DataStore.load(f);
        assertTrue(s.isEmpty());
        assertEquals(f, s.getFile());
        assertFalse(Files.exists(f));
    }

    @Test
    void savedDataSurvivesARestart() throws IOException {
        Path f = dir.resolve("sub").resolve("data.ser");
        DataStore s = DataStore.load(f);
        SeedData.populate(s, clock);
        s.commit();
        assertTrue(Files.exists(f));
        DataStore again = DataStore.load(f);
        assertFalse(again.isEmpty());
        assertEquals(s.users().size(), again.users().size());
        assertEquals(s.listings().size(), again.listings().size());
        assertEquals(s.deals().size(), again.deals().size());
        assertEquals(s.threads().size(), again.threads().size());
        assertEquals(s.reviews().size(), again.reviews().size());
        assertEquals(s.notifications().size(), again.notifications().size());
        assertEquals("Calculus textbook (Stewart 8e)", again.findListing("L-calc").map(Listing::getTitle)
                .orElseThrow());
    }

    @Test
    void inMemoryStoreSaveDoesNothing() throws IOException {
        DataStore s = new DataStore();
        s.save();
        s.commit();
        assertNull(s.getFile());
    }

    @Test
    void corruptFileIsRejected() throws IOException {
        Path f = dir.resolve("bad.ser");
        Files.writeString(f, "not a store");
        assertThrows(IOException.class, () -> DataStore.load(f));
    }

    @Test
    void wrongObjectTypeIsRejected() throws IOException {
        Path f = dir.resolve("other.ser");
        try (var out = new java.io.ObjectOutputStream(Files.newOutputStream(f))) {
            out.writeObject("a string");
        }
        assertThrows(IOException.class, () -> DataStore.load(f));
    }

    @Test
    void commitFailureIsUnchecked() throws IOException {
        Path blocker = dir.resolve("file");
        Files.writeString(blocker, "x");
        DataStore s = DataStore.load(blocker.resolve("data.ser"));
        assertThrows(UncheckedIOException.class, s::commit);
    }

    @Test
    void usernameLookupIgnoresCaseAndSpaces() {
        DataStore s = new DataStore();
        SeedData.populate(s, clock);
        assertEquals("U-demo", s.findUserByUsername(" DEMO_buyer ").orElseThrow().getId());
        assertTrue(s.findUserByUsername(null).isEmpty());
        assertTrue(s.findUserByUsername("nobody").isEmpty());
        assertTrue(s.findUser("U-demo").orElseThrow() instanceof Buyer);
        assertTrue(s.findThread("C-head").isPresent());
        assertTrue(s.findDeal("D-mug").isPresent());
        assertTrue(s.reports().isEmpty());
    }
}
