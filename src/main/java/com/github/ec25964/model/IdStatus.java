package com.github.ec25964.model;

import java.util.Set;

public enum IdStatus {
    ACTIVE,
    SUSPENDED,
    REVOKED;

    public boolean canTransitionTo(IdStatus target) {
        return getAllowedTransitions().contains(target);
    }

    private Set<IdStatus> getAllowedTransitions() {
        return switch (this) {
            case ACTIVE -> Set.of(SUSPENDED, REVOKED);
            case SUSPENDED -> Set.of(ACTIVE, REVOKED);
            case REVOKED -> Set.of();
        };
    }
}
