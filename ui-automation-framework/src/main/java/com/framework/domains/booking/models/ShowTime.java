package com.framework.domains.booking.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * ShowTime - A specific movie screening
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowTime {

    private String id;
    private String movieId;
    private String theaterId;
    private String screenId;
    private LocalDate date;
    private LocalTime time;
    private String format;               // 2D, 3D, IMAX
    private String language;
    private int availableSeats;
    private int totalSeats;
    private Map<String, CategoryAvailability> categoryAvailability;
    private boolean fastFilling;
    private boolean almostFull;
    private boolean soldOut;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryAvailability {
        private String category;
        private int available;
        private int total;
        private BigDecimal price;
        private List<String> availableSeats;  // Seat IDs
    }

    public String getDisplayTime() {
        if (time == null) return "";
        return time.toString();
    }

    public boolean hasAvailability() {
        return availableSeats > 0 && !soldOut;
    }

    public BigDecimal getLowestPrice() {
        if (categoryAvailability == null || categoryAvailability.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return categoryAvailability.values().stream()
            .map(CategoryAvailability::getPrice)
            .min(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO);
    }
}
