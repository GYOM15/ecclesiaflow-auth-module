package com.ecclesiaflow.springsecurity.web.controller;

import com.ecclesiaflow.springsecurity.web.delegate.CapabilitiesDelegate;
import com.ecclesiaflow.springsecurity.web.model.AllCapabilitiesResponse;
import com.ecclesiaflow.springsecurity.web.model.CapabilityDto;
import com.ecclesiaflow.springsecurity.web.model.ModuleCapabilitiesResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CapabilitiesController - Unit tests")
class CapabilitiesControllerTest {

    @Mock
    private CapabilitiesDelegate capabilitiesDelegate;

    @InjectMocks
    private CapabilitiesController capabilitiesController;

    @Nested
    @DisplayName("_capabilitiesGetAll - Tests")
    class GetAllCapabilitiesTests {

        @Test
        @DisplayName("Should return toutes les capabilities via le delegate")
        void shouldReturnAllCapabilitiesViaDelegate() {
            AllCapabilitiesResponse expectedResponse = new AllCapabilitiesResponse();
            when(capabilitiesDelegate.getAllCapabilities())
                    .thenReturn(ResponseEntity.ok(expectedResponse));

            ResponseEntity<AllCapabilitiesResponse> response = capabilitiesController._capabilitiesGetAll();

            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo(expectedResponse);
            verify(capabilitiesDelegate).getAllCapabilities();
        }

        @Test
        @DisplayName("Should delegate the call to the delegate")
        void shouldDelegateCallToDelegate() {
            AllCapabilitiesResponse response = new AllCapabilitiesResponse();
            when(capabilitiesDelegate.getAllCapabilities())
                    .thenReturn(ResponseEntity.ok(response));

            capabilitiesController._capabilitiesGetAll();

            verify(capabilitiesDelegate, times(1)).getAllCapabilities();
        }
    }

    @Nested
    @DisplayName("_capabilitiesGetAuthCapabilities - Tests")
    class GetAuthCapabilitiesTests {

        @Test
        @DisplayName("Should return les capabilities du module Auth")
        void shouldReturnAuthCapabilities() {
            ResponseEntity<ModuleCapabilitiesResponse> response = 
                    capabilitiesController._capabilitiesGetAuthCapabilities();

            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getModule()).isEqualTo("auth");
        }

        @Test
        @DisplayName("Should return the 5 defined Auth capabilities")
        void shouldReturnFiveAuthCapabilities() {
            ResponseEntity<ModuleCapabilitiesResponse> response = 
                    capabilitiesController._capabilitiesGetAuthCapabilities();

            List<CapabilityDto> capabilities = response.getBody().getCapabilities();
            assertThat(capabilities).hasSize(5);
        }

        @Test
        @DisplayName("Should include la capability users:read")
        void shouldIncludeUsersReadCapability() {
            ResponseEntity<ModuleCapabilitiesResponse> response = 
                    capabilitiesController._capabilitiesGetAuthCapabilities();

            List<CapabilityDto> capabilities = response.getBody().getCapabilities();
            assertThat(capabilities)
                    .extracting(CapabilityDto::getCode)
                    .contains("ef:auth:users:read");
        }

        @Test
        @DisplayName("Should include la capability users:write")
        void shouldIncludeUsersWriteCapability() {
            ResponseEntity<ModuleCapabilitiesResponse> response = 
                    capabilitiesController._capabilitiesGetAuthCapabilities();

            List<CapabilityDto> capabilities = response.getBody().getCapabilities();
            assertThat(capabilities)
                    .extracting(CapabilityDto::getCode)
                    .contains("ef:auth:users:write");
        }

        @Test
        @DisplayName("Should include la capability users:delete")
        void shouldIncludeUsersDeleteCapability() {
            ResponseEntity<ModuleCapabilitiesResponse> response = 
                    capabilitiesController._capabilitiesGetAuthCapabilities();

            List<CapabilityDto> capabilities = response.getBody().getCapabilities();
            assertThat(capabilities)
                    .extracting(CapabilityDto::getCode)
                    .contains("ef:auth:users:delete");
        }

        @Test
        @DisplayName("Should include la capability roles:read")
        void shouldIncludeRolesReadCapability() {
            ResponseEntity<ModuleCapabilitiesResponse> response = 
                    capabilitiesController._capabilitiesGetAuthCapabilities();

            List<CapabilityDto> capabilities = response.getBody().getCapabilities();
            assertThat(capabilities)
                    .extracting(CapabilityDto::getCode)
                    .contains("ef:auth:roles:read");
        }

        @Test
        @DisplayName("Should include la capability roles:write")
        void shouldIncludeRolesWriteCapability() {
            ResponseEntity<ModuleCapabilitiesResponse> response = 
                    capabilitiesController._capabilitiesGetAuthCapabilities();

            List<CapabilityDto> capabilities = response.getBody().getCapabilities();
            assertThat(capabilities)
                    .extracting(CapabilityDto::getCode)
                    .contains("ef:auth:roles:write");
        }

        @Test
        @DisplayName("Chaque capability should have un nom et une description")
        void eachCapabilityShouldHaveNameAndDescription() {
            ResponseEntity<ModuleCapabilitiesResponse> response = 
                    capabilitiesController._capabilitiesGetAuthCapabilities();

            List<CapabilityDto> capabilities = response.getBody().getCapabilities();
            for (CapabilityDto capability : capabilities) {
                assertThat(capability.getCode()).isNotBlank();
                assertThat(capability.getName()).isNotBlank();
                assertThat(capability.getDescription()).isNotBlank();
            }
        }

        @Test
        @DisplayName("Les codes de capability devraient suivre le format ef:auth:*")
        void capabilityCodesShouldFollowFormat() {
            ResponseEntity<ModuleCapabilitiesResponse> response = 
                    capabilitiesController._capabilitiesGetAuthCapabilities();

            List<CapabilityDto> capabilities = response.getBody().getCapabilities();
            for (CapabilityDto capability : capabilities) {
                assertThat(capability.getCode()).startsWith("ef:auth:");
            }
        }
    }
}
