package com.nexuserp.config.domain.port.out;

/**
 * Port OUT — chiffrement/déchiffrement symétrique des valeurs secrètes.
 *
 * Contrat :
 *  - {@link #encrypt(String)} prend une valeur en clair et retourne un texte chiffré
 *    auto-portant (incluant l'IV) encodé en base64.
 *  - {@link #decrypt(String)} effectue l'opération inverse.
 *
 * Implémentation par défaut : AES-256-GCM (voir AesGcmSecretCipher).
 */
public interface SecretCipher {

    /** @param plain valeur en clair (non nulle). @return texte chiffré base64 auto-portant. */
    String encrypt(String plain);

    /** @param cipherText texte chiffré base64 produit par {@link #encrypt(String)}. @return valeur en clair. */
    String decrypt(String cipherText);
}
