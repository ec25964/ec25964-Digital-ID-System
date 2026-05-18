package com.github.ec25964.model;

import java.util.Set;

public class Organisation {

    private final String name;
    private final OrganisationType type;
    private final Set<String> accessibleAttributes;
    private final boolean canVerifyAcrossPeriod;

    public Organisation(String name, OrganisationType type, Set<String> accessibleAttributes, boolean canVerifyAcrossPeriod) {
        this.name = name;
        this.type = type;
        this.accessibleAttributes = accessibleAttributes;
        this.canVerifyAcrossPeriod = canVerifyAcrossPeriod;
    }

    public String getName() {
        return name;
    }

    public OrganisationType getType() {
        return type;
    }

    public Set<String> getAccessibleAttributes() {
        return accessibleAttributes;
    }

    public boolean isCentralAuthority() {
        return type == OrganisationType.CENTRAL_AUTHORITY;
    }

    public boolean canVerifyAcrossPeriod() {
        return canVerifyAcrossPeriod;
    }
}
