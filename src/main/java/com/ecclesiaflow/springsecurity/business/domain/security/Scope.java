package com.ecclesiaflow.springsecurity.business.domain.security;

import lombok.Getter;

/**
 * Enumerates the security scopes available in EcclesiaFlow.
 * <p>
 * Each scope value follows the naming convention <code>ef:resource:action:scope</code>.
 * </p>
 */
@Getter
public enum Scope {
    // Members - Read
    EF_MEMBERS_READ_OWN("ef:members:read:own"),
    EF_MEMBERS_READ_ALL("ef:members:read:all"),

    // Members - Write
    EF_MEMBERS_WRITE_OWN("ef:members:write:own"),
    EF_MEMBERS_WRITE_ALL("ef:members:write:all"),

    // Members - Delete
    EF_MEMBERS_DELETE_OWN("ef:members:delete:own"),
    EF_MEMBERS_DELETE_ALL("ef:members:delete:all"),

    // Profile
    EF_PROFILE_READ_OWN("ef:profile:read:own"),
    EF_PROFILE_WRITE_OWN("ef:profile:write:own");

    private final String value;

    Scope(String value) {
        this.value = value;
    }

}
