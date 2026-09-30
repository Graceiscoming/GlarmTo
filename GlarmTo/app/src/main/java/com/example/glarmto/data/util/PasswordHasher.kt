package com.example.glarmto.data.util

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Password storage: PBKDF2-HMAC-SHA256 with a random salt per password.
 *
 * Stored form: `pbkdf2-sha256$<iterations>$<salt hex>$<hash hex>`. The iteration count is stored with
 * the hash, so it can be raised later and old hashes still verify ([needsRehash] says when to upgrade).
 *
 * PBKDF2 is implemented directly on top of HMAC-SHA256 rather than `SecretKeyFactory`: that factory is
 * missing before Android 8 and its handling of non-ASCII passwords differs between providers, which
 * could lock a user out after an OS update. This version behaves the same on every device (passwords
 * are UTF-8) and is checked against the standard PBKDF2 test vectors.
 */
object PasswordHasher {
    private const val SCHEME = "pbkdf2-sha256"
    const val DEFAULT_ITERATIONS = 120_000

    // A corrupt or tampered record must not be able to make a login spin for minutes.
    private const val MAX_ITERATIONS = 5_000_000
    private const val SALT_BYTES = 16
    private const val KEY_BYTES = 32
    private const val HMAC = "HmacSHA256"
    private const val HASH_BYTES = 32

    fun hash(
        password: String,
        iterations: Int = DEFAULT_ITERATIONS,
        random: SecureRandom = SecureRandom()
    ): String {
        require(iterations in 1..MAX_ITERATIONS) { "iterations out of range: $iterations" }
        val salt = ByteArray(SALT_BYTES).also { random.nextBytes(it) }
        val key = pbkdf2Sha256(password.toByteArray(Charsets.UTF_8), salt, iterations, KEY_BYTES)
        return listOf(SCHEME, iterations.toString(), salt.toHex(), key.toHex()).joinToString("$")
    }

    /** True if [stored] is in this class's format (as opposed to a legacy plaintext password). */
    fun isHashed(stored: String): Boolean = stored.startsWith("$SCHEME$")

    /** Checks [password] against a value made by [hash]. Malformed input is simply "no match". */
    fun verify(password: String, stored: String): Boolean {
        val parsed = parse(stored) ?: return false
        val actual = pbkdf2Sha256(password.toByteArray(Charsets.UTF_8), parsed.salt, parsed.iterations, parsed.hash.size)
        return MessageDigest.isEqual(actual, parsed.hash)
    }

    /** True if [stored] is valid but was made with fewer than [iterations] rounds. */
    fun needsRehash(stored: String, iterations: Int = DEFAULT_ITERATIONS): Boolean {
        val parsed = parse(stored) ?: return false
        return parsed.iterations < iterations
    }

    private class Parsed(val iterations: Int, val salt: ByteArray, val hash: ByteArray)

    private fun parse(stored: String): Parsed? {
        val parts = stored.split('$')
        if (parts.size != 4 || parts[0] != SCHEME) return null
        val iterations = parts[1].toIntOrNull()?.takeIf { it in 1..MAX_ITERATIONS } ?: return null
        val salt = parts[2].hexToBytesOrNull()?.takeIf { it.isNotEmpty() } ?: return null
        val hash = parts[3].hexToBytesOrNull()?.takeIf { it.isNotEmpty() } ?: return null
        return Parsed(iterations, salt, hash)
    }

    /** PBKDF2 (RFC 8018) with HMAC-SHA256 as the pseudo-random function. */
    internal fun pbkdf2Sha256(password: ByteArray, salt: ByteArray, iterations: Int, keyLength: Int): ByteArray {
        require(iterations >= 1 && keyLength >= 1)
        val mac = Mac.getInstance(HMAC)
        // An HMAC key of a single zero byte is the same as the empty key; SecretKeySpec rejects empty arrays.
        mac.init(SecretKeySpec(if (password.isEmpty()) ByteArray(1) else password, HMAC))

        val blocks = (keyLength + HASH_BYTES - 1) / HASH_BYTES
        val out = ByteArray(blocks * HASH_BYTES)
        for (block in 1..blocks) {
            mac.update(salt)
            mac.update(byteArrayOf((block ushr 24).toByte(), (block ushr 16).toByte(), (block ushr 8).toByte(), block.toByte()))
            var u = mac.doFinal()
            val t = u.copyOf()
            for (i in 2..iterations) {
                u = mac.doFinal(u)
                for (j in t.indices) t[j] = (t[j].toInt() xor u[j].toInt()).toByte()
            }
            System.arraycopy(t, 0, out, (block - 1) * HASH_BYTES, HASH_BYTES)
        }
        return out.copyOf(keyLength)
    }

    /** Equality check that doesn't stop at the first differing byte. */
    fun constantTimeEquals(a: String, b: String): Boolean =
        MessageDigest.isEqual(a.toByteArray(Charsets.UTF_8), b.toByteArray(Charsets.UTF_8))

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private fun String.hexToBytesOrNull(): ByteArray? {
        if (length % 2 != 0) return null
        val out = ByteArray(length / 2)
        for (i in out.indices) {
            val hi = Character.digit(this[2 * i], 16)
            val lo = Character.digit(this[2 * i + 1], 16)
            if (hi < 0 || lo < 0) return null
            out[i] = ((hi shl 4) or lo).toByte()
        }
        return out
    }
}
