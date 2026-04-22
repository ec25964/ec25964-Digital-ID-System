package com.github.ec25964.service;

import com.github.ec25964.model.Organisation;
import com.github.ec25964.model.OrganisationType;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class OrganisationRegistry {

    private final List<Organisation> organisations;

    public OrganisationRegistry() {
        this.organisations = List.of(
            new Organisation("CentralAuthority", OrganisationType.CENTRAL_AUTHORITY,
                    Set.of("id", "firstName", "lastName", "dateOfBirth",
                            "address", "nationality", "email", "status")),

            new Organisation("TaxAuthority", OrganisationType.CONSUMING_ORGANISATION,
                    Set.of("id", "firstName", "lastName", "address", "nationality")),

            new Organisation("DrivingLicenceAuthority", OrganisationType.CONSUMING_ORGANISATION,
                    Set.of("id", "firstName", "lastName", "dateOfBirth", "address")),

            new Organisation("WelfareService", OrganisationType.CONSUMING_ORGANISATION,
                    Set.of("id", "firstName", "lastName", "dateOfBirth", "address", "nationality")),

            new Organisation("ImmigrationService", OrganisationType.CONSUMING_ORGANISATION,
                    Set.of("id", "firstName", "lastName", "dateOfBirth", "nationality", "email")),

            new Organisation("LocalAuthority", OrganisationType.CONSUMING_ORGANISATION,
                    Set.of("id", "firstName", "lastName", "address")),

            new Organisation("Bank", OrganisationType.CONSUMING_ORGANISATION,
                    Set.of("id", "firstName", "lastName", "dateOfBirth", "address", "email")),

            new Organisation("Employer", OrganisationType.CONSUMING_ORGANISATION,
                    Set.of("id", "firstName", "lastName")),
                    
            new Organisation("CommunityFoodBank", OrganisationType.CONSUMING_ORGANISATION,
                    Set.of("id"))

        );

    }

    public List<Organisation> getAll() {
        return organisations;
    }

    public Optional<Organisation> findByName(String name) {
        return organisations.stream()
                .filter(o -> o.getName().equals(name))
                .findFirst();
    }
}
