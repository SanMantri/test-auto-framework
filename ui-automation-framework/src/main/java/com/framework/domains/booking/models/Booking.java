package com.framework.domains.booking.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Booking - Movie ticket booking entity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Booking {

    private String id;
    private String bookingNumber;
    private String userId;
    private String showTimeId;
    private String movieId;
    private String movieTitle;
    private String theaterId;
    private String theaterName;
    private String screenId;
    private LocalDateTime showDateTime;
    private List<BookedSeat> seats;
    private int ticketCount;
    private BigDecimal ticketAmount;
    private BigDecimal convenienceFee;
    private BigDecimal gst;
    private BigDecimal discount;
    private BigDecimal totalAmount;
    private String couponCode;
    private BookingStatus status;
    private PaymentInfo paymentInfo;
    private LocalDateTime bookingTime;
    private LocalDateTime expiryTime;      // For pending bookings
    private String qrCode;
    private String mTicketUrl;
    private ContactInfo contactInfo;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookedSeat {
        private String seatId;
        private String row;
        private int number;
        private String category;
        private BigDecimal price;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentInfo {
        private String paymentId;
        private String method;
        private String transactionId;
        private BigDecimal amount;
        private LocalDateTime paidAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContactInfo {
        private String email;
        private String phone;
    }

    public enum BookingStatus {
        PENDING,           // Seats selected, awaiting payment
        CONFIRMED,         // Payment successful
        CANCELLED,         // Cancelled by user
        EXPIRED,           // Payment timeout
        REFUNDED,          // Cancelled and refunded
        USED               // Ticket scanned/used
    }

    public boolean isConfirmed() {
        return status == BookingStatus.CONFIRMED;
    }

    public boolean isPending() {
        return status == BookingStatus.PENDING;
    }

    public boolean canCancel() {
        return status == BookingStatus.CONFIRMED &&
               showDateTime.isAfter(LocalDateTime.now().plusHours(4));
    }

    public String getFormattedSeats() {
        if (seats == null || seats.isEmpty()) return "";
        return seats.stream()
            .map(s -> s.getRow() + s.getNumber())
            .reduce((a, b) -> a + ", " + b)
            .orElse("");
    }
}
