package com.github.ec25964.model;

import java.util.Map;

public class VerificationResult {

    private final String digitalIdId;
    private final IdStatus status;
    private final Map<String, String> attributes;

    public VerificationResult(String digitalIdId, IdStatus status, Map<String, String> attributes) {
        this.digitalIdId = digitalIdId;
        this.status = status;
        this.attributes = attributes;
    }

    public String getDigitalIdId() {
        return digitalIdId;
    }

    public IdStatus getStatus() {
        return status;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }
}
