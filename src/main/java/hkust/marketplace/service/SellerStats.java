package hkust.marketplace.service;

import hkust.marketplace.model.ListingStatus;
import hkust.marketplace.model.ListingType;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.OptionalDouble;

/** Counts and chart data for a seller at one point in time (Task 2.8). */
public record SellerStats(long views, long requests, long completed, long reviewCount, OptionalDouble rating,
                          Map<ListingType, Long> requestsByType, Map<ListingStatus, Long> listingsByStatus,
                          LocalDateTime calculatedAt) {
    public SellerStats {
        requestsByType = Map.copyOf(requestsByType);
        listingsByStatus = Map.copyOf(listingsByStatus);
    }
}
