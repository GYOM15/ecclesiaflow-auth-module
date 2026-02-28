package com.ecclesiaflow.springsecurity.application.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Configuration for asynchronous task execution.
 * <p>
 * Enables @Async annotation support to allow asynchronous method execution,
 * particularly for sending emails via event listeners.
 * </p>
 * 
 * <p><strong>Use cases:</strong></p>
 * <ul>
 *   <li>Sending emails without blocking main transactions</li>
 *   <li>Background event processing</li>
 *   <li>Improving application responsiveness</li>
 * </ul>
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    // Spring default configuration is sufficient for our use case
    // If customization is needed, add a @Bean TaskExecutor
}
