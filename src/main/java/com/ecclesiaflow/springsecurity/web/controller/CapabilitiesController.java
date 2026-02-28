package com.ecclesiaflow.springsecurity.web.controller;

import com.ecclesiaflow.springsecurity.web.api.CapabilitiesApi;
import com.ecclesiaflow.springsecurity.web.delegate.CapabilitiesDelegate;
import com.ecclesiaflow.springsecurity.web.model.AllCapabilitiesResponse;
import com.ecclesiaflow.springsecurity.web.model.CapabilityDto;
import com.ecclesiaflow.springsecurity.web.model.ModuleCapabilitiesResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for platform capabilities.
 * Protected endpoint - requires SUPER_ADMIN or SUPPORT role.
 *
 * @author EcclesiaFlow Team
 * @since 2.0.0
 */
@RestController
@RequiredArgsConstructor
public class CapabilitiesController implements CapabilitiesApi {

    private final CapabilitiesDelegate capabilitiesDelegate;

    @Override
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SUPPORT')")
    public ResponseEntity<AllCapabilitiesResponse> _capabilitiesGetAll() {
        return capabilitiesDelegate.getAllCapabilities();
    }

    @Override
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SUPPORT')")
    public ResponseEntity<ModuleCapabilitiesResponse> _capabilitiesGetAuthCapabilities() {
        ModuleCapabilitiesResponse response = new ModuleCapabilitiesResponse();
        response.setModule("auth");
        response.setCapabilities(getAuthCapabilities());
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
}
