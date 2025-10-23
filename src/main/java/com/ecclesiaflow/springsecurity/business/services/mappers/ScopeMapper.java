package com.ecclesiaflow.springsecurity.business.services.mappers;

import com.ecclesiaflow.springsecurity.business.domain.member.Role;
import com.ecclesiaflow.springsecurity.business.domain.security.Scope;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Maps domain {@link Role} instances to the set of security scopes that should
 * be embedded in JWT tokens.
 */
@Component
public class ScopeMapper {

    /**
     * Returns the scopes associated with the provided {@link Role}.
     * 
     * <p><strong>Logique des scopes :</strong></p>
     * <ul>
     *   <li><strong>MEMBER :</strong> Peut lire/modifier/supprimer uniquement ses propres données</li>
     *   <li><strong>ADMIN :</strong> Peut tout faire (ses données + toutes les données des autres membres)</li>
     * </ul>
     *
     * @param role authenticated member role
     * @return immutable set of scope values
     */
    public Set<String> mapRoleToScopes(Role role) {
        return switch (role) {
            case MEMBER -> Set.of(
                    Scope.EF_MEMBERS_READ_OWN.getValue(),
                    Scope.EF_MEMBERS_WRITE_OWN.getValue(),
                    Scope.EF_MEMBERS_DELETE_OWN.getValue()
            );
            case ADMIN -> Set.of(
                    Scope.EF_MEMBERS_READ_ALL.getValue(),
                    Scope.EF_MEMBERS_WRITE_ALL.getValue(),
                    Scope.EF_MEMBERS_DELETE_ALL.getValue(),
                    Scope.EF_MEMBERS_READ_OWN.getValue(),
                    Scope.EF_MEMBERS_WRITE_OWN.getValue(),
                    Scope.EF_MEMBERS_DELETE_OWN.getValue()
            );
        };
    }
}
