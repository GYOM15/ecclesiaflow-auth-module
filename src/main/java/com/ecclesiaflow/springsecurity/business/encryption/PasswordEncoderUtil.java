package com.ecclesiaflow.springsecurity.business.encryption;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Service concret d'encodage des mots de passe pour EcclesiaFlow.
 * <p>
 * Cette classe utilise BCrypt de façon permanente sans interface d'abstraction.
 * L'approche concrète est justifiée car changer l'algorithme d'encodage rendrait
 * tous les comptes existants inaccessibles (hachage irréversible).
 * </p>
 *
 * <p><strong>Philosophie de conception :</strong></p>
 * <ul>
 *   <li>Stabilité prioritaire sur flexibilité</li>
 *   <li>Pas d'abstraction pour éviter la sur-ingénierie</li>
 *   <li>BCrypt choisi pour sa robustesse et sa sécurité</li>
 * </ul>
 *
 * <p><strong>Cas d'utilisation typiques :</strong></p>
 * <ul>
 *   <li>Encodage des mots de passe lors de l'inscription</li>
 *   <li>Validation des mots de passe lors de l'authentification</li>
 *   <li>Changement de mot de passe utilisateur</li>
 * </ul>
 *
 * <p><strong>Garanties :</strong> Thread-safe, encodage sécurisé BCrypt.</p>
 *
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
@Component
public class PasswordEncoderUtil implements PasswordEncoder {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private static final int MAX_PASSWORD_BYTES = 72;

    /**
     * Encode un mot de passe en clair avec l'algorithme BCrypt.
     * <p>
     * Cette méthode ne fait aucune validation préalable car Jackson s'en charge déjà
     * au niveau des DTOs. Ne fait pas de trim() car les espaces font partie intégrante
     * du mot de passe et peuvent être volontaires.
     * </p>
     *
     * @param rawPassword le mot de passe en clair à encoder, non null
     * @return le mot de passe encodé avec BCrypt
     * @throws IllegalArgumentException si rawPassword est null
     *
     * @implNote Utilise BCryptPasswordEncoder avec les paramètres par défaut.
     */
    @Override
    public String encode(CharSequence rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("Le mot de passe brut (rawPassword) ne peut pas être nul.");
        }
        if (rawPassword.toString().getBytes().length > MAX_PASSWORD_BYTES) {
            throw new IllegalArgumentException(
                    "Le mot de passe dépasse la longueur maximale de BCrypt. Il doit être inférieur à " + MAX_PASSWORD_BYTES + " octets."
            );
        }

        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }
}
