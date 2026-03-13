package com.ecclesiaflow.springsecurity.io.keycloak;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Feign client for Keycloak Admin REST API.
 */
@FeignClient(name = "keycloak-admin", url = "${keycloak.admin.server-url:http://localhost:8180}")
public interface KeycloakAdminFeignClient {

    @PostMapping(value = "/admin/realms/{realm}/users", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Void> createUser(
            @RequestHeader("Authorization") String authorization,
            @PathVariable String realm,
            @RequestBody KeycloakUserRepresentation user
    );

    @PutMapping(value = "/admin/realms/{realm}/users/{userId}/reset-password", consumes = MediaType.APPLICATION_JSON_VALUE)
    void resetPassword(
            @RequestHeader("Authorization") String authorization,
            @PathVariable String realm,
            @PathVariable String userId,
            @RequestBody KeycloakCredential credential
    );

    @GetMapping("/admin/realms/{realm}/users")
    List<Map<String, Object>> findUsersByEmail(
            @RequestHeader("Authorization") String authorization,
            @PathVariable String realm,
            @RequestParam("email") String email,
            @RequestParam("exact") boolean exact
    );

    @GetMapping("/admin/realms/{realm}/users")
    List<Map<String, Object>> findUsersByUsername(
            @RequestHeader("Authorization") String authorization,
            @PathVariable String realm,
            @RequestParam("username") String username,
            @RequestParam("exact") boolean exact
    );

    @PutMapping(value = "/admin/realms/{realm}/users/{userId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    void updateUser(
            @RequestHeader("Authorization") String authorization,
            @PathVariable String realm,
            @PathVariable String userId,
            @RequestBody Map<String, Object> userRepresentation
    );

    @DeleteMapping("/admin/realms/{realm}/users/{userId}")
    void deleteUser(
            @RequestHeader("Authorization") String authorization,
            @PathVariable String realm,
            @PathVariable String userId
    );
}
