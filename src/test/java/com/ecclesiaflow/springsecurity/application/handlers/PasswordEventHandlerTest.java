package com.ecclesiaflow.springsecurity.application.handlers;

import com.ecclesiaflow.springsecurity.business.events.PasswordSetEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Unit tests for {@link PasswordEventHandler}.
 * <p>
 * The handler is currently a no-op placeholder kept for future password-related actions.
 * Welcome emails are now handled by the Members module via MemberActivatedEvent.
 * </p>
 */
@DisplayName("PasswordEventHandler - Unit Tests")
class PasswordEventHandlerTest {

    private final PasswordEventHandler eventHandler = new PasswordEventHandler();

    private static final String EMAIL = "user@test.com";

    @Nested
    @DisplayName("handlePasswordSet - Current behavior (no-op)")
    class HandlePasswordSetTests {

        @Test
        @DisplayName("Should not throw any exception when handling event")
        void shouldNotThrowWhenHandlingEvent() {
            PasswordSetEvent event = new PasswordSetEvent(this, EMAIL);

            assertThatCode(() -> eventHandler.handlePasswordSet(event))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle events with special characters in email")
        void shouldHandleEmailWithSpecialCharacters() {
            PasswordSetEvent event = new PasswordSetEvent(this, "user+test@example.com");

            assertThatCode(() -> eventHandler.handlePasswordSet(event))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle multiple sequential events without error")
        void shouldHandleMultipleEventsSequentially() {
            PasswordSetEvent event1 = new PasswordSetEvent(this, "user1@test.com");
            PasswordSetEvent event2 = new PasswordSetEvent(this, "user2@test.com");

            assertThatCode(() -> {
                eventHandler.handlePasswordSet(event1);
                eventHandler.handlePasswordSet(event2);
            }).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("PasswordSetEvent - Event properties")
    class PasswordSetEventTests {

        @Test
        @DisplayName("Should preserve email in event")
        void shouldPreserveEmailInEvent() {
            PasswordSetEvent event = new PasswordSetEvent(this, EMAIL);

            assertThat(event.getEmail()).isEqualTo(EMAIL);
        }

        @Test
        @DisplayName("Should preserve source in event")
        void shouldPreserveSourceInEvent() {
            PasswordSetEvent event = new PasswordSetEvent(this, EMAIL);

            assertThat(event.getSource()).isEqualTo(this);
        }
    }
}
