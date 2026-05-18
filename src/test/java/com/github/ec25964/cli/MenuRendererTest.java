package com.github.ec25964.cli;

import com.github.ec25964.model.Organisation;
import com.github.ec25964.model.OrganisationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MenuRendererTest {

    private MenuRenderer renderer;
    private Organisation centralAuthority;
    private Organisation consumingOrg;

    @BeforeEach
    void setUp() {
        renderer = new MenuRenderer();
        centralAuthority = new Organisation("CentralAuthority",
                OrganisationType.CENTRAL_AUTHORITY, Set.of(), true);
        consumingOrg = new Organisation("Bank",
            OrganisationType.CONSUMING_ORGANISATION, Set.of(), false);
    }

    @Test
    void organisationMenuListsAllOrganisations() {
        String menu = renderer.renderOrganisationMenu(
                List.of(centralAuthority, consumingOrg));

        assertTrue(menu.contains("CentralAuthority"));
        assertTrue(menu.contains("Bank"));
        assertTrue(menu.contains("1)"));
        assertTrue(menu.contains("2)"));
    }

    @Test
    void organisationMenuMarksCentralAuthorityWithLabel() {
        String menu = renderer.renderOrganisationMenu(
                List.of(centralAuthority, consumingOrg));

        assertTrue(menu.contains("CentralAuthority [Central Authority]"));
        assertFalse(menu.contains("Bank [Central Authority]"));
    }

    @Test
    void centralAuthorityCommandMenuIncludesAllOptions() {
        String menu = renderer.renderCommandMenu(centralAuthority);

        assertTrue(menu.contains("Create Digital ID"));
        assertTrue(menu.contains("Update attribute"));
        assertTrue(menu.contains("Change status"));
        assertTrue(menu.contains("Verify Digital ID"));
        assertTrue(menu.contains("View audit logs"));
        assertTrue(menu.contains("Query status history"));
        assertTrue(menu.contains("0) Exit"));
    }

    @Test
    void consumingOrganisationCommandMenuOnlyOffersVerifyAndExit() {
        String menu = renderer.renderCommandMenu(consumingOrg);

        assertTrue(menu.contains("Verify Digital ID"));
        assertTrue(menu.contains("0) Exit"));
        assertFalse(menu.contains("Create Digital ID"));
        assertFalse(menu.contains("Change status"));
        assertFalse(menu.contains("audit"));
    }
}
