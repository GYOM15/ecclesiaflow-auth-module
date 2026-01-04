package com.ecclesiaflow.springsecurity.business.exceptions;

import io.grpc.Status;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GrpcCommunicationExceptionTest {

    @Test
    void getMessage_ShouldBeGeneric() {
        // Given
        String raw = "connect to https://api.service.local:7443 failed at server.local:8080";
        GrpcCommunicationException ex = new GrpcCommunicationException(
                "Members", "getMember", Status.Code.UNAVAILABLE, raw, null);

        // When
        String msg = ex.getMessage();

        // Then
        assertThat(msg).isEqualTo("gRPC error [UNAVAILABLE]");
    }
}
