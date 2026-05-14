package com.github.ec25964.cli;

import com.github.ec25964.model.Organisation;
import com.github.ec25964.model.OrganisationType;

import java.util.List;

public class MenuRenderer {

    public String renderOrganisationMenu(List<Organisation> organisations) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=== Digital ID System ===\n");
        sb.append("Select your organisation:\n");
        for (int i = 0; i < organisations.size(); i++) {
            Organisation org = organisations.get(i);
            String roleLabel = org.getType() == OrganisationType.CENTRAL_AUTHORITY
                    ? " [Central Authority]"
                    : "";
            sb.append("  ").append(i + 1).append(") ")
                    .append(org.getName()).append(roleLabel).append("\n");
        }
        sb.append("Enter choice: ");
        return sb.toString();
    }

    public String renderCommandMenu(Organisation org) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n--- Operating as: ").append(org.getName()).append(" ---\n");

        if (org.isCentralAuthority()) {
            sb.append("  1) Create Digital ID\n");
            sb.append("  2) Update attribute\n");
            sb.append("  3) Change status\n");
            sb.append("  4) Verify Digital ID\n");
            sb.append("  5) View audit logs\n");
            sb.append("  6) View audit logs by Digital ID\n");
            sb.append("  7) Query status history\n");
            sb.append("  0) Exit\n");
        } else {
            sb.append("  1) Verify Digital ID\n");
            sb.append("  0) Exit\n");
        }

        sb.append("Enter choice: ");
        return sb.toString();
    }
}
