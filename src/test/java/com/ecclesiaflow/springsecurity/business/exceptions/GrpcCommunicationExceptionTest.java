package com.ecclesiaflow.springsecurity.business.exceptions;

import io.grpc.Status;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GrpcCommunicationExceptionTest {

    @Test
    void getMessage_ShouldSanitizeInfra() {
        // Given
        String raw = "connect to https://api.service.local:7443 failed at server.local:8080";
        GrpcCommunicationException ex = new GrpcCommunicationException(
                "Members", "getMember", Status.Code.UNAVAILABLE, raw, null);

        // When
        String msg = ex.getMessage();

        // Then
        assertThat(msg).contains("[URL]");
        assertThat(msg).contains("[HOST:PORT]");
        assertThat(msg).doesNotContain("https://api.service.local:7443");
        assertThat(msg).doesNotContain("server.local:8080");
        assertThat(msg).contains("gRPC error [UNAVAILABLE] calling Members.getMember:");
    }
}
