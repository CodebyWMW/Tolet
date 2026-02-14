package com.tolet;

import java.time.LocalDate;

public class BookingRequest {
    private final String tenantName;
    private final String property;
    private final LocalDate requestDate;
    private final LocalDate moveInDate;
    private final double monthlyRent;
    private final String status;

    public BookingRequest(
            String tenantName,
            String property,
            LocalDate requestDate,
            LocalDate moveInDate,
            double monthlyRent,
            String status) {
        this.tenantName = tenantName;
        this.property = property;
        this.requestDate = requestDate;
        this.moveInDate = moveInDate;
        this.monthlyRent = monthlyRent;
        this.status = status;
    }

    public String getTenantName() {
        return tenantName;
    }

    public String getProperty() {
        return property;
    }

    public LocalDate getRequestDate() {
        return requestDate;
    }

    public LocalDate getMoveInDate() {
        return moveInDate;
    }

    public double getMonthlyRent() {
        return monthlyRent;
    }

    public String getStatus() {
        return status;
    }
}
