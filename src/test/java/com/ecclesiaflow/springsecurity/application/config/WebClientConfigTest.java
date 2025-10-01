package com.ecclesiaflow.springsecurity.application.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.reactive.function.client.ClientResponse.create;
import static org.springframework.web.reactive.function.client.ExchangeFilterFunction.ofRequestProcessor;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = WebClientConfigTest.TestConfig.class)
@TestPropertySource(properties = "ecclesiaflow.members.base-url=http://api.ecclesiaflow.com/members/v1")
@DisplayName("WebClientConfig - Tests de Configuration")
class WebClientConfigTest {

    @Autowired
    private WebClient authWebClient;

    @Test
    @DisplayName("Le WebClient doit être créé avec le baseUrl injecté par la propriété")
    void authWebClient_ShouldHaveCorrectBaseUrl() {
        // Arrange
        final String EXPECTED_BASE_URL = "http://api.ecclesiaflow.com/members/v1";
        final String TEST_PATH = "/test";

        // Variable pour capturer l'URI construite par le WebClient
        final URI[] capturedUri = new URI[1];

        // 1. Définir un filtre qui capture l'URI et court-circuite l'appel HTTP réel
        ExchangeFilterFunction capturingFilter = ExchangeFilterFunction.ofRequestProcessor(request -> {
            capturedUri[0] = request.url();
            // Retourne la requête modifiée (ou l'originale si aucune modification n'est nécessaire)
            return reactor.core.publisher.Mono.just(request);
        });

        // 2. Définir un ExchangeFunction factice pour terminer la chaîne d'appel (sans appel réseau réel)
        WebClient.RequestHeadersSpec<?> spec = authWebClient.mutate()
                // Ajouter le filtre de capture
                .filter(capturingFilter)
                // Remplacer l'ExchangeFunction par un mock qui retourne une réponse vide et OK
                .exchangeFunction(clientRequest ->
                        reactor.core.publisher.Mono.just(
                                create(HttpStatus.OK).body("").build()
                        )
                )
                .build()
                .get()
                .uri(TEST_PATH);

        // Act: Exécuter la requête factice (l'URI est capturée dans le filtre)
        spec.retrieve().bodyToMono(Void.class).block();

        // Assert
        assertThat(capturedUri[0])
                .as("L'URI capturée ne devrait pas être nulle")
                .isNotNull();

        String fullUri = capturedUri[0].toString();

        assertThat(fullUri)
                .as("L'URI complète doit commencer par la Base URL et inclure le chemin de test.")
                .startsWith(EXPECTED_BASE_URL)
                .isEqualTo(EXPECTED_BASE_URL + TEST_PATH);
    }

    // Configuration de test pour importer uniquement la classe à tester
    @Configuration
    @Import(WebClientConfig.class)
    static class TestConfig {}
}