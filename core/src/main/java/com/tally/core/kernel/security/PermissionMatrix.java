package com.tally.core.kernel.security;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * The single shared role/scope matrix every controller's write checks go through (docs/02 §7.1,
 * TLY-103 AC4). docs/02 §7.1 names this matrix without publishing one; docs/06 pins down only the
 * coarse rule proven here: VIEWER is read-only, every other role can write. A finer per-resource
 * matrix (e.g. "only OWNER/ADMIN manage the team", TLY-107) is layered on top by whichever story
 * needs it — see the Ruling in docs/plans/TLY-103.md.
 */
public final class PermissionMatrix {

    private static final Map<Role, Set<Scope>> MATRIX = Map.of(
            Role.OWNER, EnumSet.of(Scope.READ, Scope.WRITE),
            Role.ADMIN, EnumSet.of(Scope.READ, Scope.WRITE),
            Role.DEVELOPER, EnumSet.of(Scope.READ, Scope.WRITE),
            Role.FINANCE, EnumSet.of(Scope.READ, Scope.WRITE),
            Role.VIEWER, EnumSet.of(Scope.READ));

    private PermissionMatrix() {}

    public static boolean allows(Role role, Scope scope) {
        return MATRIX.get(role).contains(scope);
    }
}
