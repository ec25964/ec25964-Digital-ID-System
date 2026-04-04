package com.github.ec25964.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IdStatusTest {

    @Test
    void activeCanTransitionToSuspended() {
        assertTrue(IdStatus.ACTIVE.canTransitionTo(IdStatus.SUSPENDED));
    }

    @Test
    void activeCanTransitionToRevoked() {
        assertTrue(IdStatus.ACTIVE.canTransitionTo(IdStatus.REVOKED));
    }

    @Test
    void suspendedCanTransitionToActive() {
        assertTrue(IdStatus.SUSPENDED.canTransitionTo(IdStatus.ACTIVE));
    }

    @Test
    void suspendedCanTransitionToRevoked() {
        assertTrue(IdStatus.SUSPENDED.canTransitionTo(IdStatus.REVOKED));
    }

    @Test
    void revokedCannotTransitionToAny() {
        assertFalse(IdStatus.REVOKED.canTransitionTo(IdStatus.ACTIVE));
        assertFalse(IdStatus.REVOKED.canTransitionTo(IdStatus.SUSPENDED));
    }

    @Test
    void cannotTransitionToSameStatus() {
        assertFalse(IdStatus.ACTIVE.canTransitionTo(IdStatus.ACTIVE));
        assertFalse(IdStatus.SUSPENDED.canTransitionTo(IdStatus.SUSPENDED));
        assertFalse(IdStatus.REVOKED.canTransitionTo(IdStatus.REVOKED));
    }
}
