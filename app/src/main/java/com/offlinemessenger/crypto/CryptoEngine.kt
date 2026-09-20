package com.offlinemessenger.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.offlinemessenger.mesh.MeshPacket
import java.nio.ByteBuffer
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** End-to-end envelope: relays receive only ciphertext, recipient key-wrapped AES key, and signature. */
class CryptoEngine(private val alias: String) {
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private val keyPair get() = (keyStore.getEntry(alias, null) as? KeyStore.PrivateKeyEntry)
        ?: generateKeyPair().let { keyStore.getEntry(alias, null) as KeyStore.PrivateKeyEntry }

    fun publicKeyBytes(): ByteArray = keyPair.certificate.publicKey.encoded
    fun sign(bytes: ByteArray): ByteArray = Signature.getInstance("SHA256withRSA").run {
        initSign(keyPair.privateKey); update(bytes); sign()
    }
    fun verify(bytes: ByteArray, signature: ByteArray, senderPublicKey: ByteArray): Boolean = runCatching {
        Signature.getInstance("SHA256withRSA").run { initVerify(decode(senderPublicKey)); update(bytes); verify(signature) }
    }.getOrDefault(false)

    fun encrypt(plainText: ByteArray, recipientPublicKey: ByteArray): ByteArray {
        val aes = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        val nonce = ByteArray(12).also { java.security.SecureRandom().nextBytes(it) }
        val encrypted = Cipher.getInstance("AES/GCM/NoPadding").run { init(Cipher.ENCRYPT_MODE, aes, GCMParameterSpec(128, nonce)); doFinal(plainText) }
        val wrapped = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding").run { init(Cipher.ENCRYPT_MODE, decode(recipientPublicKey)); doFinal(aes.encoded) }
        return ByteBuffer.allocate(4 + wrapped.size + nonce.size + encrypted.size).putInt(wrapped.size).put(wrapped).put(nonce).put(encrypted).array()
    }

    fun decrypt(envelope: ByteArray): ByteArray {
        val input = ByteBuffer.wrap(envelope); val length = input.int
        require(length in 1..512) { "Invalid wrapped-key length" }
        val wrapped = ByteArray(length).also(input::get); val nonce = ByteArray(12).also(input::get); val cipherText = ByteArray(input.remaining()).also(input::get)
        val aesBytes = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding").run { init(Cipher.DECRYPT_MODE, keyPair.privateKey); doFinal(wrapped) }
        return Cipher.getInstance("AES/GCM/NoPadding").run { init(Cipher.DECRYPT_MODE, javax.crypto.spec.SecretKeySpec(aesBytes, "AES"), GCMParameterSpec(128, nonce)); doFinal(cipherText) }
    }

    private fun generateKeyPair() {
        KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore").initialize(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_DECRYPT)
                .setDigests(KeyProperties.DIGEST_SHA256).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP).setKeySize(2048).build()
        ).generateKeyPair()
    }
    private fun decode(encoded: ByteArray): PublicKey = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(encoded))
}
