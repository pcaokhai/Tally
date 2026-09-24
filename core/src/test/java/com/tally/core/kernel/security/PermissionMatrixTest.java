package com.tally.core.kernel.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PermissionMatrixTest {

    @Test
    void should_deny_write_when_role_is_viewer__TLY_103_AC4() {
        assertThat(PermissionMatrix.allows(Role.VIEWER, Scope.WRITE)).isFalse();
    }

    @Test
    void should_allow_read_when_role_is_viewer__TLY_103_AC4() {
        assertThat(PermissionMatrix.allows(Role.VIEWER, Scope.READ)).isTrue();
    }

    @Test
    void should_allow_write_when_role_is_owner_admin_developer_or_finance__TLY_103_AC4() {
        assertThat(PermissionMatrix.allows(Role.OWNER, Scope.WRITE)).isTrue();
        assertThat(PermissionMatrix.allows(Role.ADMIN, Scope.WRITE)).isTrue();
        assertThat(PermissionMatrix.allows(Role.DEVELOPER, Scope.WRITE)).isTrue();
        assertThat(PermissionMatrix.allows(Role.FINANCE, Scope.WRITE)).isTrue();
    }
}
