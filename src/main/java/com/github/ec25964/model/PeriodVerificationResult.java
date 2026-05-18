package com.github.ec25964.model;

import java.time.LocalDate;
import java.util.List;

public class PeriodVerificationResult {

    private final String digitalIdId;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final boolean continuouslyActive;
    private final List<AuditEntry> statusEventsInPeriod;

    public PeriodVerificationResult(String digitalIdId, LocalDate startDate, LocalDate endDate,
                                    boolean continuouslyActive,
                                    List<AuditEntry> statusEventsInPeriod) {
        this.digitalIdId = digitalIdId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.continuouslyActive = continuouslyActive;
        this.statusEventsInPeriod = List.copyOf(statusEventsInPeriod);
    }

    public String getDigitalIdId() {
        return digitalIdId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public boolean isContinuouslyActive() {
        return continuouslyActive;
    }

    public List<AuditEntry> getStatusEventsInPeriod() {
        return statusEventsInPeriod;
    }
}
