package com.ecclesiaflow.springsecurity.web.delegate;

import com.ecclesiaflow.springsecurity.web.model.AllCapabilitiesResponse;
import com.ecclesiaflow.springsecurity.web.model.CapabilityDto;
import com.ecclesiaflow.springsecurity.web.model.CapabilitySourceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("CapabilitiesDelegate - Unit tests")
class CapabilitiesDelegateTest {

    private CapabilitiesDelegate capabilitiesDelegate;

    @BeforeEach
    void setUp() {
        capabilitiesDelegate = new CapabilitiesDelegate();
    }

    @Nested
    @DisplayName("getAllCapabilities - Tests")
    class GetAllCapabilitiesTests {

        @Test
        @DisplayName("Should return a response with OK status")
        void shouldReturnResponseWithOkStatus() {
            ResponseEntity<AllCapabilitiesResponse> response = capabilitiesDelegate.getAllCapabilities();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("Should return Auth capabilities")
        void shouldReturnAuthCapabilities() {
            ResponseEntity<AllCapabilitiesResponse> response = capabilitiesDelegate.getAllCapabilities();

            List<CapabilityDto> authCapabilities = response.getBody().getAuth();
            assertThat(authCapabilities).isNotEmpty();
            assertThat(authCapabilities).hasSize(5);
        }

        @Test
        @DisplayName("Should mark the response as partial")
        void shouldMarkResponseAsPartial() {
            ResponseEntity<AllCapabilitiesResponse> response = capabilitiesDelegate.getAllCapabilities();

            assertThat(response.getBody().getPartial()).isTrue();
        }

        @Test
        @DisplayName("Should include the Auth source status as AVAILABLE")
        void shouldIncludeAuthSourceStatusAsAvailable() {
            ResponseEntity<AllCapabilitiesResponse> response = capabilitiesDelegate.getAllCapabilities();

            List<CapabilitySourceStatus> sources = response.getBody().getSources();
            CapabilitySourceStatus authSource = sources.stream()
                    .filter(s -> s.getModule() == CapabilitySourceStatus.ModuleEnum.AUTH)
                    .findFirst()
                    .orElse(null);

            assertThat(authSource).isNotNull();
            assertThat(authSource.getStatus()).isEqualTo(CapabilitySourceStatus.StatusEnum.AVAILABLE);
            assertThat(authSource.getError()).isNull();
        }

        @Test
        @DisplayName("Should include le statut de la source Members comme UNAVAILABLE")
        void shouldIncludeMembersSourceStatusAsUnavailable() {
            ResponseEntity<AllCapabilitiesResponse> response = capabilitiesDelegate.getAllCapabilities();

            List<CapabilitySourceStatus> sources = response.getBody().getSources();
            CapabilitySourceStatus membersSource = sources.stream()
                    .filter(s -> s.getModule() == CapabilitySourceStatus.ModuleEnum.MEMBERS)
                    .findFirst()
                    .orElse(null);

            assertThat(membersSource).isNotNull();
            assertThat(membersSource.getStatus()).isEqualTo(CapabilitySourceStatus.StatusEnum.UNAVAILABLE);
            assertThat(membersSource.getError()).isNotNull();
        }

        @Test
        @DisplayName("Should include le statut de la source Comm comme UNAVAILABLE")
        void shouldIncludeCommSourceStatusAsUnavailable() {
            ResponseEntity<AllCapabilitiesResponse> response = capabilitiesDelegate.getAllCapabilities();

            List<CapabilitySourceStatus> sources = response.getBody().getSources();
            CapabilitySourceStatus commSource = sources.stream()
                    .filter(s -> s.getModule() == CapabilitySourceStatus.ModuleEnum.COMM)
                    .findFirst()
                    .orElse(null);

            assertThat(commSource).isNotNull();
            assertThat(commSource.getStatus()).isEqualTo(CapabilitySourceStatus.StatusEnum.UNAVAILABLE);
            assertThat(commSource.getError()).isNotNull();
        }

        @Test
        @DisplayName("Should return des listes vides pour les modules indisponibles")
        void shouldReturnEmptyListsForUnavailableModules() {
            ResponseEntity<AllCapabilitiesResponse> response = capabilitiesDelegate.getAllCapabilities();

            assertThat(response.getBody().getChurch()).isEmpty();
            assertThat(response.getBody().getComm()).isEmpty();
            assertThat(response.getBody().getEvents()).isEmpty();
            assertThat(response.getBody().getFinance()).isEmpty();
        }

        @Test
        @DisplayName("Should include 3 sources in the response")
        void shouldIncludeThreeSourcesInResponse() {
            ResponseEntity<AllCapabilitiesResponse> response = capabilitiesDelegate.getAllCapabilities();

            List<CapabilitySourceStatus> sources = response.getBody().getSources();
            assertThat(sources).hasSize(3);
        }

        @Test
        @DisplayName("Auth capabilities should have the correct format")
        void authCapabilitiesShouldHaveCorrectFormat() {
            ResponseEntity<AllCapabilitiesResponse> response = capabilitiesDelegate.getAllCapabilities();

            List<CapabilityDto> authCapabilities = response.getBody().getAuth();
            for (CapabilityDto capability : authCapabilities) {
                assertThat(capability.getCode()).startsWith("ef:auth:");
                assertThat(capability.getName()).isNotBlank();
                assertThat(capability.getDescription()).isNotBlank();
            }
        }

        @Test
        @DisplayName("Should include toutes les capabilities Auth attendues")
        void shouldIncludeAllExpectedAuthCapabilities() {
            ResponseEntity<AllCapabilitiesResponse> response = capabilitiesDelegate.getAllCapabilities();

            List<CapabilityDto> authCapabilities = response.getBody().getAuth();
            List<String> codes = authCapabilities.stream()
                    .map(CapabilityDto::getCode)
                    .toList();

            assertThat(codes).containsExactlyInAnyOrder(
                    "ef:auth:users:read",
                    "ef:auth:users:write",
                    "ef:auth:users:delete",
                    "ef:auth:roles:read",
                    "ef:auth:roles:write"
            );
        }
    }
}
