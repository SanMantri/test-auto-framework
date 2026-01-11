package com.framework.domains.booking.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Theater - Theater/Cinema entity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Theater {

    private String id;
    private String name;
    private String address;
    private String city;
    private String area;
    private double latitude;
    private double longitude;
    private List<String> facilities;     // Parking, F&B, Wheelchair Access
    private List<Screen> screens;
    private Map<String, BigDecimal> pricing;  // Category -> Price
    private boolean cancellationAllowed;
    private boolean mTicketAvailable;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Screen {
        private String id;
        private String name;
        private String format;           // 2D, 3D, IMAX
        private int totalSeats;
        private int rows;
        private int seatsPerRow;
        private List<SeatCategory> categories;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeatCategory {
        private String name;             // Silver, Gold, Platinum, Recliner
        private List<String> rows;       // Rows belonging to this category
        private BigDecimal price;
    }

    public enum Facility {
        PARKING("Parking"),
        FOOD_BEVERAGE("F&B"),
        WHEELCHAIR("Wheelchair Access"),
        DOLBY_ATMOS("Dolby Atmos"),
        M_TICKET("M-Ticket"),
        COVID_SAFE("Covid Safety");

        private final String displayName;

        Facility(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
