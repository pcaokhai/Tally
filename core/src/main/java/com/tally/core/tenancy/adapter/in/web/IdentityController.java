package com.tally.core.tenancy.adapter.in.web;

import com.tally.core.api.IdentityApi;
import com.tally.core.api.model.Me;
import com.tally.core.api.model.MeActiveTenant;
import com.tally.core.api.model.MeMembershipsInner;
import com.tally.core.api.model.MeUser;
import com.tally.core.kernel.security.AuthenticatedUser;
import com.tally.core.kernel.security.AuthenticatedUserContext;
import com.tally.core.kernel.security.Membership;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** TLY-103: identity resolved from the validated bearer token (see AuthenticatedUserContext). */
@RestController
public class IdentityController implements IdentityApi {

    @Override
    public ResponseEntity<Me> getMe() {
        AuthenticatedUser user = AuthenticatedUserContext.require();

        Me me = new Me(
                        new MeUser(user.userId().toString(), user.email()),
                        new MeActiveTenant(
                                user.activeTenantId().toString(),
                                user.activeTenantName(),
                                MeActiveTenant.RoleEnum.valueOf(
                                        user.activeRole().name())),
                        List.of())
                .memberships(
                        user.memberships().stream().map(this::toMembershipDto).toList());
        return ResponseEntity.ok(me);
    }

    private MeMembershipsInner toMembershipDto(Membership membership) {
        return new MeMembershipsInner(
                membership.tenantId().toString(),
                membership.tenantName(),
                membership.role().name());
    }
}
