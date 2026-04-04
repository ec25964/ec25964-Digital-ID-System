package com.github.ec25964.model;

import java.util.Set;

public class Organisation {

    private final String name;
    private final OrganisationType type;
    private final Set<String> accessibleAttributes;

    public Organisation(String name, OrganisationType type, Set<String> accessibleAttributes) {
        this.name = name;
        this.type = type;
        this.accessibleAttributes = accessibleAttributes;
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
}
