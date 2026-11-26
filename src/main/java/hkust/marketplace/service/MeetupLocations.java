package hkust.marketplace.service;

import java.util.List;

/** Predefined campus locations from the skeleton, including locations already in demo records. */
public final class MeetupLocations {
    public static final List<String> ALL = List.of(
            "Academic Building (LG1)", "LG1 Canteen", "Atrium", "Library", "Student Centre",
            "Hall 1 Lobby", "Hall 2 Lobby", "Hall 3 Lobby", "Hall 4 Lobby", "Hall 5 Lobby",
            "Hall 6 Lobby", "Hall 7 Lobby", "Hall 8 Lobby", "Hall 9 Lobby", "UG Hall 1", "UG Hall 2",
            "North Gate", "South Gate", "Shaw Auditorium", "Coffee Shop (LG5)", "Sports Hall");

    private MeetupLocations() { }

    /** Checks one required campus location. */
    public static boolean contains(String location) {
        return location != null && ALL.contains(location);
    }

    static boolean valid(List<String> locations) {
        return locations == null || locations.stream().allMatch(MeetupLocations::contains);
    }
}
