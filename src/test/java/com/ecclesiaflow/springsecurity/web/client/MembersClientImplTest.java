package com.ecclesiaflow.springsecurity.web.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MembersClientImplTest {

    private WebClient membersWebClient;
    private MembersClientImpl membersClient;

    @BeforeEach
    void setUp() {
        membersWebClient = mock(WebClient.class);
        membersClient = new MembersClientImpl(membersWebClient);
    }

    private void mockWebClientCall(String email, Mono<Map> responseMono) {
        WebClient.RequestHeadersUriSpec uriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(membersWebClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri("/ecclesiaflow/members/{email}/confirmation-status", email)).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(responseMono);
    }

    @Test
    void shouldReturnTrueWhenEmailIsNotConfirmed() {
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("confirmed", false);
        mockWebClientCall("test@example.com", Mono.just(responseMap));

        boolean result = membersClient.isEmailNotConfirmed("test@example.com");

        assertTrue(result);
    }

    @Test
    void shouldReturnFalseWhenEmailIsConfirmed() {
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("confirmed", true);
        mockWebClientCall("confirmed@example.com", Mono.just(responseMap));

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
        Map<String, Object> responseMap = new HashMap<>(); // no "confirmed"
        mockWebClientCall("missing@example.com", Mono.just(responseMap));

        boolean result = membersClient.isEmailNotConfirmed("missing@example.com");

        assertTrue(result);
    }

    @Test
    void shouldReturnTrueWhenConfirmedValueIsNotBoolean() {
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("confirmed", "yes"); // invalid type
        mockWebClientCall("invalid@example.com", Mono.just(responseMap));

        boolean result = membersClient.isEmailNotConfirmed("invalid@example.com");

        assertTrue(result);
    }

    @Test
    void shouldReturnTrueWhen404NotFoundExceptionIsThrown() {
        WebClient.RequestHeadersUriSpec uriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(membersWebClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri("/ecclesiaflow/members/{email}/confirmation-status", "notfound@example.com")).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);

        when(responseSpec.bodyToMono(Map.class)).thenThrow(
                WebClientResponseException.NotFound.create(404, "Not Found", null, null, null)
        );

        boolean result = membersClient.isEmailNotConfirmed("notfound@example.com");

        assertTrue(result);
    }

    @Test
    void shouldThrowForOtherHttpErrors() {
        WebClient.RequestHeadersUriSpec uriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(membersWebClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri("/ecclesiaflow/members/{email}/confirmation-status", "servererror@example.com")).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);

        when(responseSpec.bodyToMono(Map.class)).thenThrow(
                WebClientResponseException.create(500, "Internal Server Error", null, null, null)
        );

        assertThrows(WebClientResponseException.class, () -> {
            membersClient.isEmailNotConfirmed("servererror@example.com");
        });
    }
}
