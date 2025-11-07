package com.ecclesiaflow.springsecurity.business.domain.member;

/**
 * Service pour communiquer avec le module Members d'EcclesiaFlow.
 * <p>
 * Ce service permet au module d'authentification d'interroger le module Members
 * pour vérifier le statut de confirmation des membres sans dupliquer les données.
 * Respecte l'architecture inter-modules et la séparation des responsabilités.
 * </p>
 * 
 * <p><strong>Responsabilités :</strong></p>
 * <ul>
 *   <li>Vérification du statut de confirmation des membres</li>
 *   <li>Communication inter-modules (REST ou gRPC)</li>
 *   <li>Gestion des erreurs de communication inter-modules</li>
 * </ul>
 * 
 * <p><strong>Implémentations :</strong></p>
 * <ul>
 *   <li>{@code MembersClientImpl} - Communication REST via WebClient</li>
 *   <li>{@code MembersGrpcClient} - Communication gRPC (si grpc.enabled=true)</li>
 * </ul>
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
public interface MembersClient {
    
    /**
     * Vérifie si l'email d'un membre n'est PAS confirmé.
     * <p>
     * Cette méthode interroge le module Members pour vérifier le statut 
     * de confirmation d'un membre. Utilisée pour bloquer les connexions 
     * des membres non confirmés.
     * </p>
     * 
     * @param email l'email du membre à vérifier
     * @return true si le membre n'existe PAS OU n'est PAS confirmé, false si confirmé
     * @throws RuntimeException si la communication avec le module Members échoue
     */
    boolean isEmailNotConfirmed(String email);
}
