package com.ecclesiaflow.springsecurity.mappers;

import com.ecclesiaflow.springsecurity.domain.AuthenticationResult;
import com.ecclesiaflow.springsecurity.dto.JwtAuthenticationResponse;

/**
 * Mapper pour convertir entre les objets métier et les DTOs web
 * Responsabilité : isolation entre couches service et web
 */
public final class AuthenticationMapper {
    /**
     * Convertit un résultat d'authentification métier en DTO web
     * @param authResult le résultat d'authentification métier (garanti non-null par l'architecture)
     * @return le DTO pour la couche web
     */
    public static JwtAuthenticationResponse toDto(AuthenticationResult authResult) {
        JwtAuthenticationResponse dto = new JwtAuthenticationResponse();
        dto.setToken(authResult.getAccessToken());
        dto.setRefreshToken(authResult.getRefreshToken());
        return dto;
    }
}
