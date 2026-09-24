package com.tally.core.tenancy.adapter.in.web;

import com.tally.core.opsapi.TenantsApi;
import com.tally.core.opsapi.model.OpsTenant;
import com.tally.core.opsapi.model.OpsTenantCreate;
import com.tally.core.tenancy.application.CreateTenantCommand;
import com.tally.core.tenancy.application.CreateTenantResult;
import com.tally.core.tenancy.application.CreateTenantUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code POST /ops/v1/tenants} (docs/06 TLY-101). The operator identity behind {@code actor} is a
 * stub ({@code "operator"}) until TLY-103/an ops auth story resolves the real operator principal.
 */
@RestController
public class TenantProvisioningController implements TenantsApi {

    private final CreateTenantUseCase createTenantUseCase;

    public TenantProvisioningController(CreateTenantUseCase createTenantUseCase) {
        this.createTenantUseCase = createTenantUseCase;
    }

    @Override
    public ResponseEntity<OpsTenant> opsCreateTenant(String idempotencyKey, OpsTenantCreate opsTenantCreate) {
        CreateTenantResult result = createTenantUseCase.create(new CreateTenantCommand(
                opsTenantCreate.getName(),
                opsTenantCreate.getPlanCode(),
                opsTenantCreate.getOwnerEmail(),
                "operator", // ponytail: real operator identity arrives with ops auth (TLY-103-adjacent)
                idempotencyKey));

        OpsTenant response = new OpsTenant(
                result.id().toString(),
                result.name(),
                result.planCode(),
                OpsTenant.TierEnum.fromValue(result.tier().name()),
                OpsTenant.StatusEnum.fromValue(result.status()),
                100);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
