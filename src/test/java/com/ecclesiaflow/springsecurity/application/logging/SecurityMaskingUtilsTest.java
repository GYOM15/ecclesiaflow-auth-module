package com.ecclesiaflow.springsecurity.application.logging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SecurityMaskingUtils - Tests complets")
class SecurityMaskingUtilsTest {

    @Nested
    @DisplayName("maskEmail")
    class MaskEmailTests {

        @Test
        @DisplayName("devrait retourner [UNKNOWN] pour null")
        void shouldReturnUnknownForNull() {
            assertThat(SecurityMaskingUtils.maskEmail(null)).isEqualTo("[UNKNOWN]");
        }

    @Nested
    @DisplayName("sanitizeInfra")
    class SanitizeInfraTests {

        @Test
        @DisplayName("devrait remplacer URL, host:port et domaine")
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
        @DisplayName("devrait retourner null pour null et conserver blanc tel quel")
        void shouldReturnNullForNullAndKeepBlank() {
            assertThat(SecurityMaskingUtils.sanitizeInfra(null)).isNull();
            assertThat(SecurityMaskingUtils.sanitizeInfra(" ")).isEqualTo(" ");
        }
    }

        @Test
        @DisplayName("devrait retourner [UNKNOWN] pour blanc")
        void shouldReturnUnknownForBlank() {
            assertThat(SecurityMaskingUtils.maskEmail("   ")).isEqualTo("[UNKNOWN]");
        }

        @Test
        @DisplayName("devrait retourner [INVALID_FORMAT] si '@' absent ou au début")
        void shouldReturnInvalidForBadFormat() {
            assertThat(SecurityMaskingUtils.maskEmail("noatsign"))
                    .isEqualTo("[INVALID_FORMAT]");
            assertThat(SecurityMaskingUtils.maskEmail("@domain.com"))
                    .isEqualTo("[INVALID_FORMAT]");
        }

        @Test
        @DisplayName("devrait masquer email avec localPart d'une lettre")
        void shouldMaskLocalPartLength1() {
            String masked = SecurityMaskingUtils.maskEmail("a@domain.com");
            assertThat(masked).isEqualTo("a****@domain.com");
        }

        @Test
        @DisplayName("devrait masquer email avec localPart de deux lettres")
        void shouldMaskLocalPartLength2() {
            String masked = SecurityMaskingUtils.maskEmail("ab@domain.com");
            assertThat(masked).isEqualTo("a****@domain.com");
        }

        @Test
        @DisplayName("devrait masquer email avec localPart > 2 lettres (garder 2)")
        void shouldMaskLocalPartLonger() {
            String masked = SecurityMaskingUtils.maskEmail("abc@domain.com");
            assertThat(masked).isEqualTo("ab****@domain.com");
        }
    }

    @Nested
    @DisplayName("maskUrlQueryParam")
    class MaskUrlQueryParamTests {

        @Test
        @DisplayName("devrait retourner [UNKNOWN] pour url null/blanche")
        void shouldReturnUnknownForNullOrBlankUrl() {
            assertThat(SecurityMaskingUtils.maskUrlQueryParam(null, "token"))
                    .isEqualTo("[UNKNOWN]");
            assertThat(SecurityMaskingUtils.maskUrlQueryParam("  ", "token"))
                    .isEqualTo("[UNKNOWN]");
        }

        @Test
        @DisplayName("devrait retourner [URL] quand paramName est null/blanc")
        void shouldReturnUrlPlaceholderWhenParamNameNullOrBlank() {
            String url = "https://api.local/path?token=abc&user=bob";
            assertThat(SecurityMaskingUtils.maskUrlQueryParam(url, null)).isEqualTo("[URL]");
            assertThat(SecurityMaskingUtils.maskUrlQueryParam(url, " ")).isEqualTo("[URL]");
        }

        @Test
        @DisplayName("devrait retourner [URL] quand pas de query")
        void shouldReturnUrlPlaceholderWhenNoQuery() {
            String url = "https://api.local/path";
            assertThat(SecurityMaskingUtils.maskUrlQueryParam(url, "token")).isEqualTo("[URL]");
        }

        @Test
        @DisplayName("devrait masquer un paramètre simple et rédiger les autres")
        void shouldMaskSimpleParamAndRedactOthers() {
            String url = "https://api.local/path?token=abc&user=bob";
            String masked = SecurityMaskingUtils.maskUrlQueryParam(url, "token");
            assertThat(masked).isEqualTo("https://api.local/path?token=****&user=[REDACTED]");
        }

        @Test
        @DisplayName("devrait masquer plusieurs occurrences du même paramètre")
        void shouldMaskMultipleOccurrences() {
            String url = "https://api.local/path?token=abc&flag&token=def";
            String masked = SecurityMaskingUtils.maskUrlQueryParam(url, "token");
            assertThat(masked).isEqualTo("https://api.local/path?token=****&flag&token=****");
        }

        @Test
        @DisplayName("devrait préserver les paramètres sans '='")
        void shouldPreserveParamsWithoutEquals() {
            String url = "https://api.local/path?flag&token=abc";
            String masked = SecurityMaskingUtils.maskUrlQueryParam(url, "token");
            assertThat(masked).isEqualTo("https://api.local/path?flag&token=****");
        }

        @Test
        @DisplayName("devrait gérer URL avec '?' terminal")
        void shouldHandleTrailingQuestionMark() {
            String url = "https://api.local/path?";
            String masked = SecurityMaskingUtils.maskUrlQueryParam(url, "token");
            // Pas de param -> retourne base + "?"
            assertThat(masked).isEqualTo("https://api.local/path?");
        }

        @Test
        @DisplayName("devrait masquer param sans valeur et rédiger les autres")
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
        @DisplayName("devrait retourner [UNKNOWN] pour id null")
        void shouldReturnUnknownForNullId() {
            assertThat(SecurityMaskingUtils.maskId(null)).isEqualTo("[UNKNOWN]");
        }

        @Test
        @DisplayName("devrait masquer complètement pour longueur <= 8")
        void shouldMaskCompletelyForShortIds() {
            assertThat(SecurityMaskingUtils.maskId("123")).isEqualTo("********");
            assertThat(SecurityMaskingUtils.maskId("12345678")).isEqualTo("********");
        }

        @Test
        @DisplayName("devrait garder 8 chars puis **** pour longueur > 8")
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
        @DisplayName("devrait retourner [NO_ERROR] pour null")
        void shouldReturnNoErrorForNull() {
            assertThat(SecurityMaskingUtils.rootMessage(null)).isEqualTo("[NO_ERROR]");
        }

        @Test
        @DisplayName("devrait retourner le message pour exception simple")
        void shouldReturnMessageForSingleException() {
            RuntimeException ex = new RuntimeException("simple");
            assertThat(SecurityMaskingUtils.rootMessage(ex)).isEqualTo("simple");
        }

        @Test
        @DisplayName("devrait retourner le message de la cause la plus profonde")
        void shouldReturnRootCauseMessage() {
            IllegalStateException root = new IllegalStateException("inner");
            RuntimeException ex = new RuntimeException("outer", root);
            assertThat(SecurityMaskingUtils.rootMessage(ex)).isEqualTo("inner");
        }

        @Test
        @DisplayName("devrait retourner le simpleName quand message root est null")
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
        @DisplayName("maskAny - devrait retourner [UNKNOWN] pour null/blanc")
        void maskAny_ShouldReturnUnknownForNullOrBlank() {
            assertThat(SecurityMaskingUtils.maskAny(null)).isEqualTo("[UNKNOWN]");
            assertThat(SecurityMaskingUtils.maskAny(" ")).isEqualTo("[UNKNOWN]");
        }

        @Test
        @DisplayName("maskAny - devrait masquer email")
        void maskAny_ShouldMaskEmail() {
            assertThat(SecurityMaskingUtils.maskAny("john.doe@example.com")).isEqualTo("jo****@example.com");
        }

        @Test
        @DisplayName("maskAny - devrait rédiger JWT-like")
        void maskAny_ShouldRedactJwt() {
            assertThat(SecurityMaskingUtils.maskAny("aaa.bbb.ccc")).isEqualTo("[REDACTED]");
        }

        @Test
        @DisplayName("maskAny - devrait masquer URL avec token et retourner [URL] sinon")
        void maskAny_ShouldHandleUrls() {
            String withToken = "https://api.local/path?token=abc&user=bob";
            String maskedTokenUrl = SecurityMaskingUtils.maskAny(withToken);
            assertThat(maskedTokenUrl).contains("token=****").contains("user=[REDACTED]");
            assertThat(maskedTokenUrl).doesNotContain("token=abc");

            String noToken = "https://api.local/path?id=10";
            assertThat(SecurityMaskingUtils.maskAny(noToken)).isEqualTo("[URL]");
        }

        @Test
        @DisplayName("maskAny - devrait masquer Bearer token (case-insensitive)")
        void maskAny_ShouldMaskBearer() {
            assertThat(SecurityMaskingUtils.maskAny("bearer abcdef")).isEqualTo("Bearer ****");
            assertThat(SecurityMaskingUtils.maskAny("Bearer abcdef")).isEqualTo("Bearer ****");
        }

        @Test
        @DisplayName("maskAny - devrait abréger les textes longs")
        void maskAny_ShouldAbbreviateLongText() {
            String longText = "a".repeat(130);
            String masked = SecurityMaskingUtils.maskAny(longText);
            assertThat(masked).hasSizeGreaterThan(120);
            assertThat(masked).endsWith("...");
        }

        @Test
        @DisplayName("maskArgs - devrait masquer une liste d'arguments hétérogènes")
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
    }
}
