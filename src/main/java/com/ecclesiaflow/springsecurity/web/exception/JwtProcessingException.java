package com.ecclesiaflow.springsecurity.web.exception;

/**
 * Exception métier spécifique pour les erreurs de traitement JWT dans EcclesiaFlow.
 * <p>
 * Cette exception rend explicites les erreurs qui peuvent survenir lors des opérations
 * JWT (génération, validation, extraction). Elle permet une gestion d'erreur plus
 * précise et un debugging facilité des problèmes liés aux tokens.
 * </p>
 * 
 * <p><strong>Cas d'utilisation typiques :</strong></p>
 * <ul>
 *   <li>Erreur de signature lors de la génération de tokens</li>
 *   <li>Token malformé ou corrompu</li>
 *   <li>Clé de signature invalide ou manquante</li>
 *   <li>Erreur de parsing des claims JWT</li>
 *   <li>Problèmes de configuration JWT</li>
 * </ul>
 * 
 * <p><strong>Garanties :</strong> Thread-safe, hérite du comportement RuntimeException.</p>
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
public class JwtProcessingException extends RuntimeException {
    
    /**
     * Crée une nouvelle exception avec un message d'erreur.
     * 
     * @param message le message décrivant l'erreur de traitement JWT, non null
     */
    public JwtProcessingException(String message) {
        super(message);
    }
    
    /**
     * Crée une nouvelle exception avec un message d'erreur et une cause.
     * 
     * @param message le message décrivant l'erreur de traitement JWT, non null
     * @param cause la cause sous-jacente de l'erreur, peut être null
     */
    public JwtProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
