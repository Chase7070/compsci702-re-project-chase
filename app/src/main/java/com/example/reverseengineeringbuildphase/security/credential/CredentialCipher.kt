package com.example.reverseengineeringbuildphase.security.credential

import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal object CredentialCipher {

    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_BITS = 128
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    fun decrypt(combinedIvAndCiphertext: ByteArray, key: SecretKey): ByteArray {
        require(combinedIvAndCiphertext.size > GCM_IV_LENGTH) { "credential blob too short" }
        val iv = combinedIvAndCiphertext.copyOfRange(0, GCM_IV_LENGTH)
        val ciphertext = combinedIvAndCiphertext.copyOfRange(GCM_IV_LENGTH, combinedIvAndCiphertext.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(ciphertext)
    }
}
