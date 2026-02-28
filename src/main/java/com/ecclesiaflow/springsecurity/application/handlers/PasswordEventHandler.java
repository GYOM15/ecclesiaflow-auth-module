package com.ecclesiaflow.springsecurity.application.handlers;

import com.ecclesiaflow.springsecurity.business.events.PasswordSetEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Handler for password-related domain events.
 * <p>
 * Note: Welcome email sending has been moved to the Members module.
 * The Members module handles welcome emails via MemberActivatedEvent after account activation.
 * </p>
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class PasswordEventHandler {

    /**
     * Handles password setup completion event.
     * <p>
     * Note: Welcome email is now sent by the Members module via MemberActivatedEvent.
     * This handler is kept for potential future password-related actions.
     * </p>
     * 
     * @param event Event containing user email
     */
    @EventListener
    @Async
    public void handlePasswordSet(PasswordSetEvent event) {
        // Welcome email now handled by Members module via MemberActivatedEvent
        // This method is kept for potential future password-related actions
    }
}
