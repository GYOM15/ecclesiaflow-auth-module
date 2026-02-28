package com.ecclesiaflow.springsecurity.io.grpc.server;

import com.ecclesiaflow.grpc.auth.TemporaryTokenRequest;
import com.ecclesiaflow.grpc.auth.TemporaryTokenResponse;
import com.ecclesiaflow.springsecurity.business.services.SetupTokenService;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthGrpcServiceImpl - Unit tests")
class AuthGrpcServiceImplTest {

    @Mock
    private SetupTokenService setupTokenService;

    @Mock
    private StreamObserver<TemporaryTokenResponse> responseObserver;

    private AuthGrpcServiceImpl authGrpcService;

    private static final String TEST_EMAIL = "test@example.com";
    private static final UUID TEST_MEMBER_ID = UUID.randomUUID();
    private static final String TEST_TOKEN = "test-token-123";
    private static final int TTL_HOURS = 24;
    private static final String PASSWORD_ENDPOINT = "/ecclesiaflow/auth/password/setup";

    @BeforeEach
    void setUp() {
        authGrpcService = new AuthGrpcServiceImpl(setupTokenService);
        ReflectionTestUtils.setField(authGrpcService, "setupTokenTtlHours", TTL_HOURS);
        ReflectionTestUtils.setField(authGrpcService, "passwordSetupEndpoint", PASSWORD_ENDPOINT);
    }

    @Nested
    @DisplayName("generateTemporaryToken - Tests for success")
    class GenerateTemporaryTokenSuccessTests {

        @Test
        @DisplayName("Should generate a temporary token avec success")
        void shouldGenerateTemporaryTokenSuccessfully() {
            TemporaryTokenRequest request = TemporaryTokenRequest.newBuilder()
                    .setEmail(TEST_EMAIL)
                    .setMemberId(TEST_MEMBER_ID.toString())
                    .build();

            when(setupTokenService.generateSetupToken(TEST_EMAIL, TEST_MEMBER_ID))
                    .thenReturn(TEST_TOKEN);

            authGrpcService.generateTemporaryToken(request, responseObserver);

            ArgumentCaptor<TemporaryTokenResponse> responseCaptor = 
                    ArgumentCaptor.forClass(TemporaryTokenResponse.class);
            verify(responseObserver).onNext(responseCaptor.capture());
            verify(responseObserver).onCompleted();
            verify(responseObserver, never()).onError(any());

            TemporaryTokenResponse response = responseCaptor.getValue();
            assertThat(response.getTemporaryToken()).isEqualTo(TEST_TOKEN);
            assertThat(response.getExpiresInSeconds()).isEqualTo(TTL_HOURS * 3600);
            assertThat(response.getPasswordEndpoint()).isEqualTo(PASSWORD_ENDPOINT);
        }

        @Test
        @DisplayName("Should call the service avec the correct parameters")
        void shouldCallServiceWithCorrectParameters() {
            TemporaryTokenRequest request = TemporaryTokenRequest.newBuilder()
                    .setEmail(TEST_EMAIL)
                    .setMemberId(TEST_MEMBER_ID.toString())
                    .build();

            when(setupTokenService.generateSetupToken(TEST_EMAIL, TEST_MEMBER_ID))
                    .thenReturn(TEST_TOKEN);

            authGrpcService.generateTemporaryToken(request, responseObserver);

            verify(setupTokenService).generateSetupToken(TEST_EMAIL, TEST_MEMBER_ID);
        }

        @Test
        @DisplayName("Devrait calculer correctement l'expiration en secondes")
        void shouldCalculateExpirationInSecondsCorrectly() {
            TemporaryTokenRequest request = TemporaryTokenRequest.newBuilder()
                    .setEmail(TEST_EMAIL)
                    .setMemberId(TEST_MEMBER_ID.toString())
                    .build();

            when(setupTokenService.generateSetupToken(any(), any())).thenReturn(TEST_TOKEN);

            authGrpcService.generateTemporaryToken(request, responseObserver);

            ArgumentCaptor<TemporaryTokenResponse> responseCaptor = 
                    ArgumentCaptor.forClass(TemporaryTokenResponse.class);
            verify(responseObserver).onNext(responseCaptor.capture());

            TemporaryTokenResponse response = responseCaptor.getValue();
            assertThat(response.getExpiresInSeconds()).isEqualTo(24 * 3600);
        }
    }

    @Nested
    @DisplayName("generateTemporaryToken - Validation input")
    class GenerateTemporaryTokenValidationTests {

        @Test
        @DisplayName("Devrait rejeter un email vide")
        void shouldRejectEmptyEmail() {
            TemporaryTokenRequest request = TemporaryTokenRequest.newBuilder()
                    .setEmail("")
                    .setMemberId(TEST_MEMBER_ID.toString())
                    .build();

            authGrpcService.generateTemporaryToken(request, responseObserver);

            ArgumentCaptor<StatusRuntimeException> errorCaptor = 
                    ArgumentCaptor.forClass(StatusRuntimeException.class);
            verify(responseObserver).onError(errorCaptor.capture());
            verify(responseObserver, never()).onNext(any());
            verify(responseObserver, never()).onCompleted();

            StatusRuntimeException error = errorCaptor.getValue();
            assertThat(error.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
            assertThat(error.getStatus().getDescription()).contains("Email cannot be empty");
        }

        @Test
        @DisplayName("Devrait rejeter un email avec format invalide")
        void shouldRejectInvalidEmailFormat() {
            TemporaryTokenRequest request = TemporaryTokenRequest.newBuilder()
                    .setEmail("invalid-email")
                    .setMemberId(TEST_MEMBER_ID.toString())
                    .build();

            authGrpcService.generateTemporaryToken(request, responseObserver);

            ArgumentCaptor<StatusRuntimeException> errorCaptor = 
                    ArgumentCaptor.forClass(StatusRuntimeException.class);
            verify(responseObserver).onError(errorCaptor.capture());

            StatusRuntimeException error = errorCaptor.getValue();
            assertThat(error.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
            assertThat(error.getStatus().getDescription()).contains("Invalid email format");
        }

        @Test
        @DisplayName("Devrait rejeter un memberId vide")
        void shouldRejectEmptyMemberId() {
            TemporaryTokenRequest request = TemporaryTokenRequest.newBuilder()
                    .setEmail(TEST_EMAIL)
                    .setMemberId("")
                    .build();

            authGrpcService.generateTemporaryToken(request, responseObserver);

            ArgumentCaptor<StatusRuntimeException> errorCaptor = 
                    ArgumentCaptor.forClass(StatusRuntimeException.class);
            verify(responseObserver).onError(errorCaptor.capture());

            StatusRuntimeException error = errorCaptor.getValue();
            assertThat(error.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
            assertThat(error.getStatus().getDescription()).contains("cannot be empty");
        }

        @Test
        @DisplayName("Devrait rejeter un memberId avec format UUID invalide")
        void shouldRejectInvalidUuidFormat() {
            TemporaryTokenRequest request = TemporaryTokenRequest.newBuilder()
                    .setEmail(TEST_EMAIL)
                    .setMemberId("not-a-uuid")
                    .build();

            authGrpcService.generateTemporaryToken(request, responseObserver);

            ArgumentCaptor<StatusRuntimeException> errorCaptor = 
                    ArgumentCaptor.forClass(StatusRuntimeException.class);
            verify(responseObserver).onError(errorCaptor.capture());

            StatusRuntimeException error = errorCaptor.getValue();
            assertThat(error.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
            assertThat(error.getStatus().getDescription()).contains("must be a valid UUID format");
        }

        @Test
        @DisplayName("Should accept a valid email avec different formats")
        void shouldAcceptValidEmailFormats() {
            String[] validEmails = {
                "test@example.com",
                "user.name@example.com",
                "user+tag@example.co.uk",
                "test_user@sub.example.com"
            };

            for (String email : validEmails) {
                StreamObserver<TemporaryTokenResponse> observer = mock(StreamObserver.class);
                TemporaryTokenRequest request = TemporaryTokenRequest.newBuilder()
                        .setEmail(email)
                        .setMemberId(TEST_MEMBER_ID.toString())
                        .build();

                when(setupTokenService.generateSetupToken(any(), any())).thenReturn(TEST_TOKEN);

                authGrpcService.generateTemporaryToken(request, observer);

                verify(observer).onNext(any());
                verify(observer).onCompleted();
                verify(observer, never()).onError(any());
            }
        }
    }

    @Nested
    @DisplayName("generateTemporaryToken - Gestion des erreurs")
    class GenerateTemporaryTokenErrorHandlingTests {

        @Test
        @DisplayName("Should return INTERNAL pour exception inattendue")
        void shouldReturnInternalForUnexpectedException() {
            TemporaryTokenRequest request = TemporaryTokenRequest.newBuilder()
                    .setEmail(TEST_EMAIL)
                    .setMemberId(TEST_MEMBER_ID.toString())
                    .build();

            when(setupTokenService.generateSetupToken(any(), any()))
                    .thenThrow(new RuntimeException("Unexpected error"));

            authGrpcService.generateTemporaryToken(request, responseObserver);

            ArgumentCaptor<StatusRuntimeException> errorCaptor = 
                    ArgumentCaptor.forClass(StatusRuntimeException.class);
            verify(responseObserver).onError(errorCaptor.capture());
            verify(responseObserver, never()).onNext(any());
            verify(responseObserver, never()).onCompleted();

            StatusRuntimeException error = errorCaptor.getValue();
            assertThat(error.getStatus().getCode()).isEqualTo(Status.Code.INTERNAL);
            assertThat(error.getStatus().getDescription()).contains("Failed to generate temporary token");
        }

        @Test
        @DisplayName("Should not propagate the exception beyond the responseObserver")
        void shouldNotPropagateExceptionBeyondResponseObserver() {
            TemporaryTokenRequest request = TemporaryTokenRequest.newBuilder()
                    .setEmail(TEST_EMAIL)
                    .setMemberId(TEST_MEMBER_ID.toString())
                    .build();

            when(setupTokenService.generateSetupToken(any(), any()))
                    .thenThrow(new RuntimeException("Test exception"));

            assertThatCode(() -> authGrpcService.generateTemporaryToken(request, responseObserver))
                    .doesNotThrowAnyException();

            verify(responseObserver).onError(any(StatusRuntimeException.class));
        }
    }
}
