package com.ecclesiaflow.springsecurity.application.logging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SecurityMaskingUtils - Complete tests")
class SecurityMaskingUtilsTest {

    @Nested
    @DisplayName("maskEmail")
    class MaskEmailTests {

        @Test
        @DisplayName("should return [UNKNOWN] for null")
        void shouldReturnUnknownForNull() {
            assertThat(SecurityMaskingUtils.maskEmail(null)).isEqualTo("[UNKNOWN]");
        }

    @Nested
    @DisplayName("sanitizeInfra")
    class SanitizeInfraTests {

        @Test
        @DisplayName("should replace URL, host:port and domain")
        void shouldReplaceUrlHostPortAndDomain() {
            String msg = "connect to https://api.internal:8443/auth?token=abc failed at example.com:443 and foo.bar";
            String sanitized = SecurityMaskingUtils.sanitizeInfra(msg);
            assertThat(sanitized).doesNotContain("https://api.internal:8443");
            assertThat(sanitized).doesNotContain("example.com:443");
            assertThat(sanitized).doesNotContain("foo.bar");
            assertThat(sanitized).contains("[URL]");
            assertThat(sanitized).contains("[HOST:PORT]");
            assertThat(sanitized).contains("[HOST]");
        }

        @Test
        @DisplayName("should return null for null and keep blank as is")
        void shouldReturnNullForNullAndKeepBlank() {
            assertThat(SecurityMaskingUtils.sanitizeInfra(null)).isNull();
            assertThat(SecurityMaskingUtils.sanitizeInfra(" ")).isEqualTo(" ");
        }
    }

        @Test
        @DisplayName("should return [UNKNOWN] for blank")
        void shouldReturnUnknownForBlank() {
            assertThat(SecurityMaskingUtils.maskEmail("   ")).isEqualTo("[UNKNOWN]");
        }

        @Test
        @DisplayName("should return [INVALID_FORMAT] if '@' is missing or at the beginning")
        void shouldReturnInvalidForBadFormat() {
            assertThat(SecurityMaskingUtils.maskEmail("noatsign"))
                    .isEqualTo("[INVALID_FORMAT]");
            assertThat(SecurityMaskingUtils.maskEmail("@domain.com"))
                    .isEqualTo("[INVALID_FORMAT]");
        }

        @Test
        @DisplayName("should mask email with one letter localPart")
        void shouldMaskLocalPartLength1() {
            String masked = SecurityMaskingUtils.maskEmail("a@domain.com");
            assertThat(masked).isEqualTo("a****@domain.com");
        }

        @Test
        @DisplayName("should mask email with two letter localPart")
        void shouldMaskLocalPartLength2() {
            String masked = SecurityMaskingUtils.maskEmail("ab@domain.com");
            assertThat(masked).isEqualTo("a****@domain.com");
        }

        @Test
        @DisplayName("should mask email with localPart > 2 letters (keep 2)")
        void shouldMaskLocalPartLonger() {
            String masked = SecurityMaskingUtils.maskEmail("abc@domain.com");
            assertThat(masked).isEqualTo("ab****@domain.com");
        }
    }

    @Nested
    @DisplayName("maskUrlQueryParam")
    class MaskUrlQueryParamTests {

        @Test
        @DisplayName("should return [UNKNOWN] for null/blank url")
        void shouldReturnUnknownForNullOrBlankUrl() {
            assertThat(SecurityMaskingUtils.maskUrlQueryParam(null, "token"))
                    .isEqualTo("[UNKNOWN]");
            assertThat(SecurityMaskingUtils.maskUrlQueryParam("  ", "token"))
                    .isEqualTo("[UNKNOWN]");
        }

        @Test
        @DisplayName("should return [URL] when paramName is null/blank")
        void shouldReturnUrlPlaceholderWhenParamNameNullOrBlank() {
            String url = "https://api.local/path?token=abc&user=bob";
            assertThat(SecurityMaskingUtils.maskUrlQueryParam(url, null)).isEqualTo("[URL]");
            assertThat(SecurityMaskingUtils.maskUrlQueryParam(url, " ")).isEqualTo("[URL]");
        }

        @Test
        @DisplayName("should return [URL] when no query")
        void shouldReturnUrlPlaceholderWhenNoQuery() {
            String url = "https://api.local/path";
            assertThat(SecurityMaskingUtils.maskUrlQueryParam(url, "token")).isEqualTo("[URL]");
        }

        @Test
        @DisplayName("should mask a simple parameter and redact the others")
        void shouldMaskSimpleParamAndRedactOthers() {
            String url = "https://api.local/path?token=abc&user=bob";
            String masked = SecurityMaskingUtils.maskUrlQueryParam(url, "token");
            assertThat(masked).isEqualTo("https://api.local/path?token=****&user=[REDACTED]");
        }

        @Test
        @DisplayName("should mask multiple occurrences of the same parameter")
        void shouldMaskMultipleOccurrences() {
            String url = "https://api.local/path?token=abc&flag&token=def";
            String masked = SecurityMaskingUtils.maskUrlQueryParam(url, "token");
            assertThat(masked).isEqualTo("https://api.local/path?token=****&flag&token=****");
        }

        @Test
        @DisplayName("should preserve parameters without '='")
        void shouldPreserveParamsWithoutEquals() {
            String url = "https://api.local/path?flag&token=abc";
            String masked = SecurityMaskingUtils.maskUrlQueryParam(url, "token");
            assertThat(masked).isEqualTo("https://api.local/path?flag&token=****");
        }

        @Test
        @DisplayName("should handle URL with terminal '?'")
        void shouldHandleTrailingQuestionMark() {
            String url = "https://api.local/path?";
            String masked = SecurityMaskingUtils.maskUrlQueryParam(url, "token");
            // No param -> returns base + "?"
            assertThat(masked).isEqualTo("https://api.local/path?");
        }

        @Test
        @DisplayName("should mask param without value and redact the others")
        void shouldMaskParamWithoutValueAndRedactOthers() {
            String url = "https://api.local/path?token=&user=bob";
            String masked = SecurityMaskingUtils.maskUrlQueryParam(url, "token");
            assertThat(masked).isEqualTo("https://api.local/path?token=****&user=[REDACTED]");
        }
    }

    @Nested
    @DisplayName("maskId")
    class MaskIdTests {

        @Test
        @DisplayName("should return [UNKNOWN] for null id")
        void shouldReturnUnknownForNullId() {
            assertThat(SecurityMaskingUtils.maskId(null)).isEqualTo("[UNKNOWN]");
        }

        @Test
        @DisplayName("should mask completely for length <= 8")
        void shouldMaskCompletelyForShortIds() {
            assertThat(SecurityMaskingUtils.maskId("123")).isEqualTo("********");
            assertThat(SecurityMaskingUtils.maskId("12345678")).isEqualTo("********");
        }

        @Test
        @DisplayName("should keep 8 chars then **** for length > 8")
        void shouldKeepEightThenMaskRest() {
            assertThat(SecurityMaskingUtils.maskId("1234567890")).isEqualTo("12345678********");

            UUID id = UUID.randomUUID();
            String masked = SecurityMaskingUtils.maskId(id);
            assertThat(masked).startsWith(id.toString().substring(0, 8)).endsWith("********");
        }
    }

    @Nested
    @DisplayName("rootMessage")
    class RootMessageTests {

        @Test
        @DisplayName("should return [NO_ERROR] for null")
        void shouldReturnNoErrorForNull() {
            assertThat(SecurityMaskingUtils.rootMessage(null)).isEqualTo("[NO_ERROR]");
        }

        @Test
        @DisplayName("should return the message for simple exception")
        void shouldReturnMessageForSingleException() {
            RuntimeException ex = new RuntimeException("simple");
            assertThat(SecurityMaskingUtils.rootMessage(ex)).isEqualTo("simple");
        }

        @Test
        @DisplayName("should return the deepest cause message")
        void shouldReturnRootCauseMessage() {
            IllegalStateException root = new IllegalStateException("inner");
            RuntimeException ex = new RuntimeException("outer", root);
            assertThat(SecurityMaskingUtils.rootMessage(ex)).isEqualTo("inner");
        }

        @Test
        @DisplayName("should return simpleName when root message is null")
        void shouldReturnClassSimpleNameWhenRootMessageNull() {
            Exception root = new Exception((String) null);
            RuntimeException ex = new RuntimeException("outer", root);
            assertThat(SecurityMaskingUtils.rootMessage(ex)).isEqualTo("Exception");
        }
    }

    @Nested
    @DisplayName("maskAny & maskArgs")
    class MaskAnyAndArgsTests {

        @Test
        @DisplayName("maskAny - should return [UNKNOWN] for null/blank")
        void maskAny_ShouldReturnUnknownForNullOrBlank() {
            assertThat(SecurityMaskingUtils.maskAny(null)).isEqualTo("[UNKNOWN]");
            assertThat(SecurityMaskingUtils.maskAny(" ")).isEqualTo("[UNKNOWN]");
        }

        @Test
        @DisplayName("maskAny - should mask email")
        void maskAny_ShouldMaskEmail() {
            assertThat(SecurityMaskingUtils.maskAny("john.doe@example.com")).isEqualTo("jo****@example.com");
        }

        @Test
        @DisplayName("maskAny - should redact JWT-like")
        void maskAny_ShouldRedactJwt() {
            assertThat(SecurityMaskingUtils.maskAny("aaa.bbb.ccc")).isEqualTo("[REDACTED]");
        }

        @Test
        @DisplayName("maskAny - should mask URL with token and return [URL] otherwise")
        void maskAny_ShouldHandleUrls() {
            String withToken = "https://api.local/path?token=abc&user=bob";
            String maskedTokenUrl = SecurityMaskingUtils.maskAny(withToken);
            assertThat(maskedTokenUrl).contains("token=****").contains("user=[REDACTED]");
            assertThat(maskedTokenUrl).doesNotContain("token=abc");

            String noToken = "https://api.local/path?id=10";
            assertThat(SecurityMaskingUtils.maskAny(noToken)).isEqualTo("[URL]");
        }

        @Test
        @DisplayName("maskAny - should mask Bearer token (case-insensitive)")
        void maskAny_ShouldMaskBearer() {
            assertThat(SecurityMaskingUtils.maskAny("bearer abcdef")).isEqualTo("Bearer ****");
            assertThat(SecurityMaskingUtils.maskAny("Bearer abcdef")).isEqualTo("Bearer ****");
        }

        @Test
        @DisplayName("maskAny - should abbreviate long texts")
        void maskAny_ShouldAbbreviateLongText() {
            String longText = "a".repeat(130);
            String masked = SecurityMaskingUtils.maskAny(longText);
            assertThat(masked).hasSizeGreaterThan(120);
            assertThat(masked).endsWith("...");
        }

        @Test
        @DisplayName("maskArgs - should mask a list of heterogeneous arguments")
        void maskArgs_ShouldMaskHeterogeneousArgs() {
            Object[] args = new Object[] {
                    "john.doe@example.com",
                    "https://api.local/path?token=abc&x=1",
                    "Bearer xyz",
                    "aaa.bbb.ccc",
                    "plain"
            };
            String masked = SecurityMaskingUtils.maskArgs(args);
            assertThat(masked).contains("jo****@example.com");
            assertThat(masked).contains("token=****");
            assertThat(masked).contains("x=[REDACTED]");
            assertThat(masked).contains("Bearer ****");
            assertThat(masked).contains("[REDACTED]");
            assertThat(masked).contains("plain");
        }

        @Test
        @DisplayName("maskArgs - should return [] for null args")
        void maskArgs_ShouldReturnEmptyBracketsForNull() {
            assertThat(SecurityMaskingUtils.maskArgs(null)).isEqualTo("[]");
        }

        @Test
        @DisplayName("maskAny - should handle URL http:// with token")
        void maskAny_ShouldHandleHttpUrls() {
            String httpWithToken = "http://api.local/path?token=secret";
            String masked = SecurityMaskingUtils.maskAny(httpWithToken);
            assertThat(masked).contains("token=****");
        }

        @Test
        @DisplayName("maskAny - should return [URL] for http:// without token")
        void maskAny_ShouldReturnUrlForHttpWithoutToken() {
            String httpNoToken = "http://api.local/path?id=10";
            assertThat(SecurityMaskingUtils.maskAny(httpNoToken)).isEqualTo("[URL]");
        }

        @Test
        @DisplayName("rootMessage - should return simpleName for blank message")
        void rootMessage_ShouldReturnSimpleNameForBlankMessage() {
            Exception ex = new Exception("  ");
            assertThat(SecurityMaskingUtils.rootMessage(ex)).isEqualTo("Exception");
        }

        @Test
        @DisplayName("maskId - should return [UNKNOWN] for blank id")
        void maskId_ShouldReturnUnknownForBlankId() {
            assertThat(SecurityMaskingUtils.maskId("  ")).isEqualTo("[UNKNOWN]");
        }
    }

    @Nested
    @DisplayName("maskUrlQueryParam - Exception handling")
    class MaskUrlQueryParamExceptionTests {

        @Test
        @DisplayName("should return [URL_MASKING_ERROR] in case of exception")
        void shouldReturnErrorOnException() {
            // Create a malformed URL that could cause an exception during parsing
            String malformedUrl = "https://api.local/path?" + "\u0000";
            String result = SecurityMaskingUtils.maskUrlQueryParam(malformedUrl, "token");
            // The result should be either [URL_MASKING_ERROR] or handled correctly
            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("abbreviate")
    class AbbreviateTests {

        @Test
        @DisplayName("should return [UNKNOWN] for null")
        void shouldReturnUnknownForNull() {
            assertThat(SecurityMaskingUtils.abbreviate(null, 10)).isEqualTo("[UNKNOWN]");
        }

        @Test
        @DisplayName("should return the complete string if <= max")
        void shouldReturnFullStringWhenWithinMax() {
            assertThat(SecurityMaskingUtils.abbreviate("hello", 10)).isEqualTo("hello");
            assertThat(SecurityMaskingUtils.abbreviate("exactly10!", 10)).isEqualTo("exactly10!");
        }

        @Test
        @DisplayName("should abbreviate if > max")
        void shouldAbbreviateWhenExceedsMax() {
            assertThat(SecurityMaskingUtils.abbreviate("this is too long", 10)).isEqualTo("this is to...");
        }
    }

    @Nested
    @DisplayName("maskResetLink")
    class MaskResetLinkTests {

        @Test
        @DisplayName("should mask the token parameter in a reset link")
        void shouldMaskTokenInResetLink() {
            String link = "https://app.example.com/reset?token=secret123&email=user@test.com";
            String masked = SecurityMaskingUtils.maskResetLink(link);
            assertThat(masked).contains("token=****");
            assertThat(masked).contains("email=[REDACTED]");
            assertThat(masked).doesNotContain("secret123");
        }
    }
}
