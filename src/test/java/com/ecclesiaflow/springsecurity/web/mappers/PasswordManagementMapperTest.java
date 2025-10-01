package com.ecclesiaflow.springsecurity.web.mappers;

import com.ecclesiaflow.springsecurity.business.domain.token.UserTokens;
import com.ecclesiaflow.springsecurity.web.dto.PasswordManagementResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PasswordManagementMapper - Tests Statiques de Mapping")
class PasswordManagementMapperTest {

    private static final String MESSAGE = "Authentification réussie.";
    private static final String ACCESS_TOKEN = "jwt.access.token";
    private static final String REFRESH_TOKEN = "jwt.refresh.token";
    private static final Long EXPIRES_IN = 3600L; // 1 heure

    @Test
    @DisplayName("toDtoWithTokens - Devrait mapper correctement tous les champs de UserTokens au DTO")
    void toDtoWithTokens_ShouldMapAllFields() {
        // Arrange
        UserTokens userTokens = new UserTokens(ACCESS_TOKEN, REFRESH_TOKEN);

        // Act
        PasswordManagementResponse dto = PasswordManagementMapper.toDtoWithTokens(MESSAGE, userTokens, EXPIRES_IN);

        // Assert
        assertThat(dto).isNotNull();
        assertThat(dto.getMessage()).isEqualTo(MESSAGE);
        assertThat(dto.getAccessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(dto.getRefreshToken()).isEqualTo(REFRESH_TOKEN);
        assertThat(dto.getExpiresIn()).isEqualTo(EXPIRES_IN);
    }

    @Test
    @DisplayName("toDtoWithTokens - Devrait gérer correctement les valeurs nulles pour les tokens")
    void toDtoWithTokens_ShouldHandleNullTokens() {
        // Arrange
        UserTokens userTokens = new UserTokens(null, null);

        // Act
        PasswordManagementResponse dto = PasswordManagementMapper.toDtoWithTokens(MESSAGE, userTokens, EXPIRES_IN);

        // Assert
        assertThat(dto).isNotNull();
        assertThat(dto.getAccessToken()).isNull();
        assertThat(dto.getRefreshToken()).isNull();
    }
}