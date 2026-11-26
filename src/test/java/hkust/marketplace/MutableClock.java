package hkust.marketplace;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** A clock that tests can move forward, so due dates and reminders are tested without waiting. */
public class MutableClock extends Clock {
    private Instant now;

    /** @param start starting time (UTC) */
    public MutableClock(LocalDateTime start) {
        this.now = start.toInstant(ZoneOffset.UTC);
    }

    /** @param d amount to move the clock forward */
    public void advance(Duration d) {
        now = now.plus(d);
    }

    @Override public ZoneId getZone() { return ZoneOffset.UTC; }
    @Override public Clock withZone(ZoneId zone) { return this; }
    @Override public Instant instant() { return now; }
}
