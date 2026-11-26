package hkust.marketplace.service;

import hkust.marketplace.model.*;
import hkust.marketplace.util.ValidationException;
import java.math.BigDecimal;
import java.util.List;

/** Validated listing details shared by admin creation, moderation edits and seller submissions. */
public record ListingForm(ListingType type, String title, Category category, Condition condition,
                          String description, String location, BigDecimal price, long seconds, List<String> photos) {
    /** HKUST meet-up locations used in the editor. Existing listings retain their recorded location. */
    public static final List<String> LOCATIONS = List.of("Atrium", "Library", "LG1 Canteen", "Sports Hall",
            "Hall 5 Lobby", "North Bus Stop", "South Bus Stop", "Academic Concourse");

    public ListingForm {
        if (type == null || category == null || condition == null) throw new ValidationException("Select a type, category and condition.");
        title = required(title, "Item title");
        description = required(description, "Description");
        location = required(location, "Meet-up location");
        if (type == ListingType.SELL && (price == null || price.signum() < 0 || price.stripTrailingZeros().scale() > 2))
            throw new ValidationException("Enter a non-negative sale price with at most two decimal places.");
        if (type == ListingType.LEND && seconds < DealService.MIN_BORROW.toSeconds())
            throw new ValidationException("Lending duration must be at least five seconds.");
        if (type != ListingType.SELL) price = null;
        if (type != ListingType.LEND) seconds = 0;
        if (photos == null || photos.size() > Listing.MAX_PHOTOS || photos.stream().anyMatch(s -> s == null || s.isBlank()))
            throw new ValidationException("Choose up to six photos.");
        photos = List.copyOf(photos);
    }

    static String required(String value, String field) {
        if (value == null || value.isBlank()) throw new ValidationException(field + " is required.");
        return value.trim();
    }

    void apply(Listing listing) {
        listing.setType(type);
        listing.setTitle(title);
        listing.setCategory(category);
        listing.setCondition(condition);
        listing.setDescription(description);
        listing.setMeetupLocation(location);
        listing.setPrice(price);
        listing.setLendingDurationSeconds(seconds);
        listing.getPhotos().clear();
        listing.getPhotos().addAll(photos);
    }
}
