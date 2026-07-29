package com.jnu.ticketinfrastructure.model;


import com.jnu.ticketdomain.domains.user.domain.UserStatus;
import lombok.Getter;

@Getter
public class StockReservationResult {
    private static final String RESERVED = "RESERVED";
    private static final String DUPLICATE = "DUPLICATE";
    private static final String NO_STOCK = "NO_STOCK";
    private static final String CLOSED = "CLOSED";
    private static final String UNAVAILABLE = "UNAVAILABLE";

    private final boolean reserved;
    private final String reason;
    private final Integer position;
    private final UserStatus resultStatus;
    private final Integer sequence;
    private final Integer remainingAmount;

    public StockReservationResult(
            boolean reserved,
            String reason,
            Integer position,
            UserStatus resultStatus,
            Integer sequence,
            Integer remainingAmount) {
        this.reserved = reserved;
        this.reason = reason;
        this.position = position;
        this.resultStatus = resultStatus;
        this.sequence = sequence;
        this.remainingAmount = remainingAmount;
    }

    public static StockReservationResult reserved(
            Integer position, UserStatus resultStatus, Integer sequence, Integer remainingAmount) {
        return new StockReservationResult(
                true, RESERVED, position, resultStatus, sequence, remainingAmount);
    }

    public static StockReservationResult duplicate(Integer remainingAmount) {
        return new StockReservationResult(false, DUPLICATE, null, null, null, remainingAmount);
    }

    public static StockReservationResult noStock(Integer remainingAmount) {
        return new StockReservationResult(false, NO_STOCK, null, null, null, remainingAmount);
    }

    public static StockReservationResult closed(Integer remainingAmount) {
        return new StockReservationResult(false, CLOSED, null, null, null, remainingAmount);
    }

    public static StockReservationResult unavailable(Integer remainingAmount) {
        return new StockReservationResult(false, UNAVAILABLE, null, null, null, remainingAmount);
    }

    public boolean isDuplicate() {
        return DUPLICATE.equals(reason);
    }

    public boolean isNoStock() {
        return NO_STOCK.equals(reason);
    }

    public boolean isClosed() {
        return CLOSED.equals(reason);
    }

    public boolean isUnavailable() {
        return UNAVAILABLE.equals(reason);
    }
}
