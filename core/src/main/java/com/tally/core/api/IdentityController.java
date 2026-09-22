package com.tally.core.api;

import com.tally.core.api.model.Me;
import com.tally.core.api.model.MeActiveTenant;
import com.tally.core.api.model.MeMembershipsInner;
import com.tally.core.api.model.MeUser;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

// ponytail: stub wiring until TLY-004 adds the real identity use case.
@Controller
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
