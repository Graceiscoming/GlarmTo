package com.example.glarmto

import com.example.glarmto.data.util.PasswordHasher
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class PasswordHasherTest {

    private fun ByteArray.hex() = joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private fun pbkdf2(password: String, salt: ByteArray, iterations: Int, length: Int) =
        PasswordHasher.pbkdf2Sha256(password.toByteArray(Charsets.UTF_8), salt, iterations, length).hex()

    // Expected values below were produced with Python's hashlib.pbkdf2_hmac('sha256', ...), and the first
    // three are the standard PBKDF2-HMAC-SHA256 test vectors.

    @Test
    fun `standard vector - 1 iteration`() {
        assertEquals(
            "120fb6cffcf8b32c43e7225256c4f837a86548c92ccc35480805987cb70be17b",
            pbkdf2("password", "salt".toByteArray(), 1, 32)
        )
    }

    @Test
    fun `standard vector - 2 iterations`() {
        assertEquals(
            "ae4d0c95af6b46d32d0adff928f06dd02a303f8ef3c251dfd6e2d85a95474c43",
            pbkdf2("password", "salt".toByteArray(), 2, 32)
        )
    }

    @Test
    fun `standard vector - 4096 iterations`() {
        assertEquals(
            "c5e478d59288c841aa530db6845c4c8d962893a001ce4e11a4963873aa98134a",
            pbkdf2("password", "salt".toByteArray(), 4096, 32)
        )
    }

    @Test
    fun `long password and salt with a 40 byte output spanning two blocks`() {
        assertEquals(
            "348c89dbcbd32b2f32d814b8116e84cf2b17347ebc1800181c4e2a1fb8dd53e1c635518c7dac47e9",
            pbkdf2("passwordPASSWORDpassword", "saltSALTsaltSALTsaltSALTsaltSALTsalt".toByteArray(), 4096, 40)
        )
    }

    @Test
    fun `64 byte output - the first block equals the 32 byte output`() {
        val sixtyFour = pbkdf2("password", "salt".toByteArray(), 1, 64)

        assertEquals(
            "120fb6cffcf8b32c43e7225256c4f837a86548c92ccc35480805987cb70be17b" +
                "4dbf3a2f3dad3377264bb7b8e8330d4efc7451418617dabef683735361cdc18c",
            sixtyFour
        )
    }

    @Test
    fun `empty password gives the correct value instead of crashing`() {
        assertEquals(
            "5ddf839afa2d5fb4be56e1a0f48917617559bef61ec122bfca1c7f75ac8f401d",
            pbkdf2("", "salt".toByteArray(), 3, 32)
        )
    }

    @Test
    fun `unicode password is treated as UTF-8`() {
        assertEquals(
            "b147269208979137108774b3978930f2cde3c696df183e2ac95989372eee2166",
            pbkdf2("pässwörd-ภาษาไทย-🔒", ByteArray(16) { it.toByte() }, 1000, 32)
        )
    }

    @Test
    fun `agrees with the JDK implementation`() {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        listOf("hunter2", "correct horse battery staple", "a", "p@ss w0rd!").forEachIndexed { i, pw ->
            val salt = ByteArray(16) { (it * 7 + i).toByte() }
            val iterations = 1 + i * 137
            val jdk = factory.generateSecret(PBEKeySpec(pw.toCharArray(), salt, iterations, 256)).encoded
            assertArrayEquals(pw, jdk, PasswordHasher.pbkdf2Sha256(pw.toByteArray(), salt, iterations, 32))
        }
    }

    // ---------------- hash / verify ----------------

    @Test
    fun `hash has the documented format and never contains the password`() {
        val stored = PasswordHasher.hash("s3cret-Pa55", iterations = 10)

        val parts = stored.split('$')
        assertEquals(4, parts.size)
        assertEquals("pbkdf2-sha256", parts[0])
        assertEquals("10", parts[1])
        assertEquals(32, parts[2].length) // 16 byte salt
        assertEquals(64, parts[3].length) // 32 byte hash
        assertTrue(parts[2].all { it in "0123456789abcdef" } && parts[3].all { it in "0123456789abcdef" })
        assertFalse(stored.contains("s3cret"))
    }

    @Test
    fun `the same password hashes differently each time because of the salt`() {
        val a = PasswordHasher.hash("same", iterations = 10)
        val b = PasswordHasher.hash("same", iterations = 10)

        assertNotEquals(a, b)
        assertTrue(PasswordHasher.verify("same", a))
        assertTrue(PasswordHasher.verify("same", b))
    }

    @Test
    fun `verify accepts the right password and rejects others`() {
        val stored = PasswordHasher.hash("Correct-Horse", iterations = 10)

        assertTrue(PasswordHasher.verify("Correct-Horse", stored))
        assertFalse(PasswordHasher.verify("correct-horse", stored))
        assertFalse(PasswordHasher.verify("Correct-Horse ", stored))
        assertFalse(PasswordHasher.verify(" Correct-Horse", stored))
        assertFalse(PasswordHasher.verify("", stored))
        assertFalse(PasswordHasher.verify("Correct-Hors", stored))
    }

    @Test
    fun `an empty password can be hashed and only matches empty`() {
        val stored = PasswordHasher.hash("", iterations = 5)

        assertTrue(PasswordHasher.verify("", stored))
        assertFalse(PasswordHasher.verify("x", stored))
    }

    @Test
    fun `unicode and very long passwords round trip`() {
        val thai = "รหัสผ่านลับ123"
        val long = "x".repeat(20_000)

        assertTrue(PasswordHasher.verify(thai, PasswordHasher.hash(thai, iterations = 5)))
        assertTrue(PasswordHasher.verify(long, PasswordHasher.hash(long, iterations = 5)))
        assertFalse(PasswordHasher.verify(long + "y", PasswordHasher.hash(long, iterations = 5)))
    }

    @Test
    fun `verify uses the iteration count stored with the hash`() {
        val low = PasswordHasher.hash("pw", iterations = 3)
        val high = PasswordHasher.hash("pw", iterations = 300)

        assertTrue(PasswordHasher.verify("pw", low))
        assertTrue(PasswordHasher.verify("pw", high))
    }

    @Test
    fun `changing the stored iteration count breaks verification`() {
        val stored = PasswordHasher.hash("pw", iterations = 10)
        val tampered = stored.replaceFirst("\$10\$", "\$11\$")

        assertFalse(PasswordHasher.verify("pw", tampered))
    }

    @Test
    fun `flipping one digit of the stored hash breaks verification`() {
        val stored = PasswordHasher.hash("pw", iterations = 10)
        val last = stored.last()
        val tampered = stored.dropLast(1) + (if (last == '0') '1' else '0')

        assertFalse(PasswordHasher.verify("pw", tampered))
    }

    @Test
    fun `hash rejects iteration counts that make no sense`() {
        listOf(0, -1, Int.MIN_VALUE, 5_000_001, Int.MAX_VALUE).forEach {
            try {
                PasswordHasher.hash("pw", iterations = it)
                fail("expected an error for $it iterations")
            } catch (expected: IllegalArgumentException) {
            }
        }
    }

    // ---------------- malformed input ----------------

    @Test
    fun `verify treats malformed stored values as no match and never throws`() {
        val valid = PasswordHasher.hash("pw", iterations = 2)
        val salt = valid.split('$')[2]
        val hash = valid.split('$')[3]
        val malformed = listOf(
            "",
            "pw",
            "pbkdf2-sha256",
            "pbkdf2-sha256$",
            "pbkdf2-sha256\$2\$$salt",
            "pbkdf2-sha256\$2\$$salt\$$hash\$extra",
            "pbkdf2-sha256\$abc\$$salt\$$hash",
            "pbkdf2-sha256\$0\$$salt\$$hash",
            "pbkdf2-sha256\$-5\$$salt\$$hash",
            "pbkdf2-sha256\$5000001\$$salt\$$hash",
            "pbkdf2-sha256\$99999999999999\$$salt\$$hash",
            "pbkdf2-sha256\$2\$${salt.dropLast(1)}\$$hash",
            "pbkdf2-sha256\$2\$zz${salt.drop(2)}\$$hash",
            "pbkdf2-sha256\$2\$\$$hash",
            "pbkdf2-sha256\$2\$$salt\$",
            "pbkdf2-sha256\$2\$$salt\$${hash.dropLast(1)}",
            "pbkdf2-sha512\$2\$$salt\$$hash",
            "PBKDF2-SHA256\$2\$$salt\$$hash"
        )

        malformed.forEach { assertFalse("should not verify: $it", PasswordHasher.verify("pw", it)) }
    }

    @Test
    fun `the valid value used to build the malformed cases does verify`() {
        assertTrue(PasswordHasher.verify("pw", PasswordHasher.hash("pw", iterations = 2)))
    }

    // ---------------- isHashed / needsRehash ----------------

    @Test
    fun `isHashed recognises the format and not plaintext`() {
        assertTrue(PasswordHasher.isHashed(PasswordHasher.hash("pw", iterations = 2)))
        assertFalse(PasswordHasher.isHashed(""))
        assertFalse(PasswordHasher.isHashed("secretpassword"))
        assertFalse(PasswordHasher.isHashed("pbkdf2-sha256"))
    }

    @Test
    fun `needsRehash is true only for valid hashes with fewer iterations than wanted`() {
        val stored = PasswordHasher.hash("pw", iterations = 100)

        assertTrue(PasswordHasher.needsRehash(stored, iterations = 200))
        assertFalse(PasswordHasher.needsRehash(stored, iterations = 100))
        assertFalse(PasswordHasher.needsRehash(stored, iterations = 50))
        assertFalse(PasswordHasher.needsRehash("not a hash", iterations = 200))
        assertFalse(PasswordHasher.needsRehash("", iterations = 200))
    }

    @Test
    fun `default iteration count is a sensible work factor`() {
        assertTrue(PasswordHasher.DEFAULT_ITERATIONS >= 100_000)
    }

    @Test
    fun `constantTimeEquals compares exactly`() {
        assertTrue(PasswordHasher.constantTimeEquals("abc", "abc"))
        assertTrue(PasswordHasher.constantTimeEquals("", ""))
        assertFalse(PasswordHasher.constantTimeEquals("abc", "abd"))
        assertFalse(PasswordHasher.constantTimeEquals("abc", "abcd"))
        assertFalse(PasswordHasher.constantTimeEquals("abc", "ABC"))
        assertFalse(PasswordHasher.constantTimeEquals("", "a"))
    }
}
