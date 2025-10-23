package com.ecclesiaflow.springsecurity.business.services.mappers;

import com.ecclesiaflow.springsecurity.business.domain.member.Role;
import com.ecclesiaflow.springsecurity.business.domain.security.Scope;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScopeMapperTest {

    private final ScopeMapper scopeMapper = new ScopeMapper();

    @Test
    @DisplayName("mapRoleToScopes - MEMBER should receive only own/member scopes")
    void mapRoleToScopes_ShouldReturnMemberScopes() {
        Set<String> scopes = scopeMapper.mapRoleToScopes(Role.MEMBER);

        assertThat(scopes)
                .containsExactlyInAnyOrder(
                        Scope.EF_MEMBERS_READ_OWN.getValue(),
                        Scope.EF_MEMBERS_WRITE_OWN.getValue(),
                        Scope.EF_MEMBERS_DELETE_OWN.getValue()
                );

        assertThatThrownBy(() -> scopes.add("new:scope"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("mapRoleToScopes - ADMIN should receive tenant-level scopes")
    void mapRoleToScopes_ShouldReturnAdminScopes() {
        Set<String> scopes = scopeMapper.mapRoleToScopes(Role.ADMIN);

        assertThat(scopes)
                .containsExactlyInAnyOrder(
                        Scope.EF_MEMBERS_READ_ALL.getValue(),
                        Scope.EF_MEMBERS_WRITE_ALL.getValue(),
                        Scope.EF_MEMBERS_DELETE_ALL.getValue(),
                        Scope.EF_MEMBERS_READ_OWN.getValue(),
                        Scope.EF_MEMBERS_WRITE_OWN.getValue(),
                        Scope.EF_MEMBERS_DELETE_OWN.getValue()
                );

        assertThatThrownBy(() -> scopes.remove(Scope.EF_MEMBERS_READ_ALL.getValue()))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
