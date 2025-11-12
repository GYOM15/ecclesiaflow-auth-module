package com.ecclesiaflow.springsecurity.web.client;

import com.ecclesiaflow.springsecurity.web.model.MemberConfirmationStatusResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MembersClientImplTest {

    @Mock
    private WebClient membersWebClient;
    private MembersClientImpl membersClient;

    @BeforeEach
    void setUp() {
        membersClient = new MembersClientImpl(membersWebClient);
    }

    @SuppressWarnings("unchecked")
    private void mockWebClientCall(String email, Mono<MemberConfirmationStatusResponse> responseMono) {
        WebClient.RequestHeadersUriSpec uriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(membersWebClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString(), anyString())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(MemberConfirmationStatusResponse.class)).thenReturn(responseMono);
    }

    @Test
    void shouldReturnTrueWhenEmailIsNotConfirmed() {
        MemberConfirmationStatusResponse response = new MemberConfirmationStatusResponse();
        response.setConfirmed(false);
        mockWebClientCall("test@example.com", Mono.just(response));

        boolean result = membersClient.isEmailNotConfirmed("test@example.com");

        assertTrue(result);
    }

    @Test
    void shouldReturnFalseWhenEmailIsConfirmed() {
        MemberConfirmationStatusResponse response = new MemberConfirmationStatusResponse();
        response.setConfirmed(true);
        mockWebClientCall("confirmed@example.com", Mono.just(response));

        boolean result = membersClient.isEmailNotConfirmed("confirmed@example.com");

        assertFalse(result);
    }

    @Test
    void shouldReturnTrueWhenResponseIsNull() {
        mockWebClientCall("null@example.com", Mono.justOrEmpty(null));

        boolean result = membersClient.isEmailNotConfirmed("null@example.com");

        assertTrue(result);
    }

    @Test
    void shouldReturnTrueWhenKeyConfirmedIsMissing() {
        MemberConfirmationStatusResponse response = new MemberConfirmationStatusResponse();
        // confirmed est null par défaut
        mockWebClientCall("missing@example.com", Mono.just(response));

        boolean result = membersClient.isEmailNotConfirmed("missing@example.com");

        assertTrue(result);
    }

    @Test
    void shouldReturnTrueWhenConfirmedValueIsNotBoolean() {
        // Ce test n'est plus pertinent avec un type strongly-typed
        // Si vous avez besoin de tester ce cas, il faudrait simuler une erreur de parsing
        MemberConfirmationStatusResponse response = new MemberConfirmationStatusResponse();
        response.setConfirmed(null); // null est le cas le plus proche
        mockWebClientCall("invalid@example.com", Mono.just(response));

        boolean result = membersClient.isEmailNotConfirmed("invalid@example.com");

        assertTrue(result);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnTrueWhen404NotFoundExceptionIsThrown() {
        WebClient.RequestHeadersUriSpec uriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(membersWebClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString(), anyString())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);

        when(responseSpec.bodyToMono(MemberConfirmationStatusResponse.class)).thenReturn(
                Mono.error(WebClientResponseException.NotFound.create(404, "Not Found", null, null, null))
        );

        boolean result = membersClient.isEmailNotConfirmed("notfound@example.com");

        assertTrue(result);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldThrowForOtherHttpErrors() {
        WebClient.RequestHeadersUriSpec uriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(membersWebClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString(), anyString())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);

        when(responseSpec.bodyToMono(MemberConfirmationStatusResponse.class)).thenReturn(
                Mono.error(WebClientResponseException.create(500, "Internal Server Error", null, null, null))
        );

        assertThrows(WebClientResponseException.class, () -> {
            membersClient.isEmailNotConfirmed("servererror@example.com");
        });
    }
}