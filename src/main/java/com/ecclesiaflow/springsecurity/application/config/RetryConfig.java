package com.ecclesiaflow.springsecurity.application.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Enables Spring Retry for retryable gRPC and external service calls.
 */
@Configuration
@EnableRetry
public class RetryConfig {
}
