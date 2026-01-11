package com.framework.domains.booking.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Seat - Individual seat in a theater screen
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Seat {

    private String id;              // e.g., "A-1", "H-15"
    private String row;             // e.g., "A", "H"
    private int number;             // e.g., 1, 15
    private String category;        // Silver, Gold, Platinum, Recliner
    private BigDecimal price;
    private SeatStatus status;
    private SeatType type;

    public enum SeatStatus {
        AVAILABLE,
        BOOKED,
        BLOCKED,           // Temporarily held
        SELECTED,          // Selected by current user
        UNAVAILABLE,       // Broken/maintenance
        SOCIAL_DISTANCE    // COVID gap seat
    }

    public enum SeatType {
        REGULAR,
        WHEELCHAIR,
        COMPANION,         // Companion to wheelchair
        RECLINER,
        COUPLE,           // Double seat
        PREMIUM
    }

    public boolean isAvailable() {
        return status == SeatStatus.AVAILABLE;
    }

    public boolean isSelectable() {
        return status == SeatStatus.AVAILABLE || status == SeatStatus.SELECTED;
    }

    public String getDisplayName() {
        return row + "-" + number;
    }

    /**
     * Creates a seat ID from row and number.
     */
    public static String createId(String row, int number) {
        return row + "-" + number;
    }

    /**
     * Parses row from seat ID.
     */
    public static String parseRow(String seatId) {
        return seatId.split("-")[0];
    }

    /**
     * Parses seat number from seat ID.
     */
    public static int parseNumber(String seatId) {
        return Integer.parseInt(seatId.split("-")[1]);
    }
}
