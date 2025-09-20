package com.ecclesiaflow.springsecurity.web.mappers;

import com.ecclesiaflow.springsecurity.business.domain.member.MemberRegistration;
import com.ecclesiaflow.springsecurity.business.domain.password.SigninCredentials;
import com.ecclesiaflow.springsecurity.web.payloads.SignUpRequest;
import com.ecclesiaflow.springsecurity.web.payloads.SigninRequest;

/**
 * Mapper statique pour la conversion entre les DTOs web et les objets métier.
 * <p>
 * Cette classe fournit des méthodes utilitaires pour transformer les requêtes HTTP
 * en objets du domaine métier utilisables par les services. Respecte le pattern
 * Static Utility Class avec des méthodes purement fonctionnelles.
 * </p>
 * 
 * <p><strong>Responsabilités principales :</strong></p>
 * <ul>
 *   <li>Conversion des DTOs web vers les objets métier</li>
 *   <li>Séparation claire entre couche web et couche métier</li>
 *   <li>Validation implicite des transformations</li>
 * </ul>
 * 
 * <p><strong>Cas d'utilisation typiques :</strong></p>
 * <ul>
 *   <li>Transformation des requêtes d'inscription en objets métier</li>
 *   <li>Transformation des requêtes de connexion en identifiants</li>
 *   <li>Orchestration par les contrôleurs REST</li>
 * </ul>
 * 
 * <p><strong>Garanties :</strong> Thread-safe, stateless, opérations pures.</p>
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
public class MemberMapper {
    
    /**
     * Convertit une requête d'inscription web en objet métier MemberRegistration.
     * <p>
     * Cette méthode effectue une transformation directe des champs sans validation
     * supplémentaire, la validation ayant été effectuée au niveau DTO.
     * </p>
     * 
     * @param req la requête d'inscription provenant de la couche web, non null
     * @return un objet MemberRegistration contenant les données d'inscription
     * @throws NullPointerException si req est null
     */
    public static MemberRegistration fromSignUpRequest(SignUpRequest req) {
        return new MemberRegistration(
            req.getEmail(),
            req.getPassword()
        );
    }

    /**
     * Convertit une requête de connexion web en objet métier SigninCredentials.
     * <p>
     * Cette méthode effectue une transformation directe des identifiants sans
     * validation supplémentaire, la validation ayant été effectuée au niveau DTO.
     * </p>
     * 
     * @param req la requête de connexion provenant de la couche web, non null
     * @return un objet SigninCredentials contenant les identifiants de connexion
     * @throws NullPointerException si req est null
     */
    public static SigninCredentials fromSigninRequest(SigninRequest req) {
        return new SigninCredentials(
            req.getEmail(),
            req.getPassword()
        );
    }
}
