package com.tally.core.tenancy.adapter.in.web;

import com.tally.core.api.IdentityApi;
import com.tally.core.api.model.Me;
import com.tally.core.api.model.MeActiveTenant;
import com.tally.core.api.model.MeMembershipsInner;
import com.tally.core.api.model.MeUser;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

// ponytail: stub wiring until TLY-103 adds the real identity use case.
@RestController
public class IdentityController implements IdentityApi {

    @Override
    public ResponseEntity<Me> getMe() {
        Me me = new Me(
                        new MeUser("stub-user-id", "stub@example.invalid"),
                        new MeActiveTenant("stub-tenant-id", "Stub Tenant", MeActiveTenant.RoleEnum.OWNER),
                        List.of())
                .memberships(List.of(new MeMembershipsInner("stub-tenant-id", "Stub Tenant", "OWNER")));
        return ResponseEntity.ok(me);
    }
}
