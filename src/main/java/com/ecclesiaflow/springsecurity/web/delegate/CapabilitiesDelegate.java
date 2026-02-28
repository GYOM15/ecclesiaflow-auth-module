package com.ecclesiaflow.springsecurity.web.delegate;

import com.ecclesiaflow.springsecurity.web.model.AllCapabilitiesResponse;
import com.ecclesiaflow.springsecurity.web.model.CapabilityDto;
import com.ecclesiaflow.springsecurity.web.model.CapabilitySourceStatus;
import com.ecclesiaflow.springsecurity.web.model.CapabilitySourceStatus.ModuleEnum;
import com.ecclesiaflow.springsecurity.web.model.CapabilitySourceStatus.StatusEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Delegate for aggregating capabilities from all modules.
 * Uses gRPC to communicate with external modules.
 */
@Service
@RequiredArgsConstructor
public class CapabilitiesDelegate {

    // TODO: Inject gRPC clients when capabilities endpoints are available in other modules
    // private final MembersGrpcClient membersGrpcClient;
    // private final CommGrpcClient commGrpcClient;

    public ResponseEntity<AllCapabilitiesResponse> getAllCapabilities() {
        AllCapabilitiesResponse response = new AllCapabilitiesResponse();
        List<CapabilitySourceStatus> sources = new ArrayList<>();
        boolean isPartial = false;

        // Auth module (always available - local)
        response.setAuth(getAuthCapabilities());
        sources.add(createSourceStatus(ModuleEnum.AUTH, StatusEnum.AVAILABLE, null));

        // TODO: Replace with actual gRPC call when Members module implements capabilities endpoint
        response.setChurch(new ArrayList<>());
        sources.add(createSourceStatus(ModuleEnum.MEMBERS, StatusEnum.UNAVAILABLE, "Service not yet implemented"));
        isPartial = true;

        // TODO: Replace with actual gRPC call when Comm module implements capabilities endpoint
        response.setComm(new ArrayList<>());
        sources.add(createSourceStatus(ModuleEnum.COMM, StatusEnum.UNAVAILABLE, "Service not yet implemented"));
        isPartial = true;

        // Initialize empty lists for other modules
        response.setEvents(new ArrayList<>());
        response.setFinance(new ArrayList<>());

        // Set resilience fields
        response.setPartial(isPartial);
        response.setSources(sources);

        return ResponseEntity.ok(response);
    }

    private List<CapabilityDto> getAuthCapabilities() {
        return List.of(
            createCapability("ef:auth:users:read", "Read users", "View user account information"),
            createCapability("ef:auth:users:write", "Manage users", "Create and update user accounts"),
            createCapability("ef:auth:users:delete", "Delete users", "Delete user accounts"),
            createCapability("ef:auth:roles:read", "Read roles", "View role definitions"),
            createCapability("ef:auth:roles:write", "Manage roles", "Create and update roles")
        );
    }

    private CapabilityDto createCapability(String code, String name, String description) {
        CapabilityDto dto = new CapabilityDto();
        dto.setCode(code);
        dto.setName(name);
        dto.setDescription(description);
        return dto;
    }

    private CapabilitySourceStatus createSourceStatus(ModuleEnum module, StatusEnum status, String error) {
        CapabilitySourceStatus sourceStatus = new CapabilitySourceStatus();
        sourceStatus.setModule(module);
        sourceStatus.setStatus(status);
        sourceStatus.setError(error);
        return sourceStatus;
    }
}
