package com.framework.domains.booking.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

/**
 * Movie - Movie entity for booking system
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Movie {

    private String id;
    private String title;
    private String description;
    private String genre;
    private Duration duration;
    private String language;
    private String format;           // 2D, 3D, IMAX, 4DX
    private String rating;           // U, UA, A, S
    private LocalDate releaseDate;
    private String posterUrl;
    private String trailerUrl;
    private List<String> cast;
    private String director;
    private double userRating;
    private int totalReviews;
    private boolean nowShowing;
    private boolean comingSoon;

    public enum Format {
        FORMAT_2D("2D"),
        FORMAT_3D("3D"),
        IMAX("IMAX"),
        IMAX_3D("IMAX 3D"),
        FOURK_DX("4DX");

        private final String displayName;

        Format(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum Rating {
        U("Universal"),
        UA("Parental Guidance"),
        A("Adults Only"),
        S("Restricted");

        private final String description;

        Rating(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public String getDurationFormatted() {
        if (duration == null) return "";
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        return String.format("%dh %dm", hours, minutes);
    }
}
