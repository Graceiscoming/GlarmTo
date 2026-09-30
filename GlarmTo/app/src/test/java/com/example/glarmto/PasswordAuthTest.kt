package com.example.glarmto

import com.example.glarmto.data.local.entity.UserEntity
import com.example.glarmto.data.repository.GlarmToRepository
import com.example.glarmto.data.util.PasswordHasher
import com.example.glarmto.testsupport.MutableFakeSessionManager
import com.example.glarmto.testsupport.RecordingFakeGlarmToDao
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Register / login with hashed passwords, plus the upgrade path for accounts saved by older versions. */
class PasswordAuthTest {

    private lateinit var dao: RecordingFakeGlarmToDao
    private lateinit var session: MutableFakeSessionManager
    private lateinit var repo: GlarmToRepository

    // Small iteration counts keep the tests fast; the real default is exercised in one test below.
    private val iterations = 20

    @Before
    fun setup() {
        dao = RecordingFakeGlarmToDao()
        session = MutableFakeSessionManager()
        repo = GlarmToRepository(dao, session, passwordIterations = iterations)
    }

    private fun stored() = dao.mockUserFlow.value!!.password

    // ---------------- register ----------------

    @Test
    fun `register stores a hash and never the plaintext`() = runBlocking {
        assertTrue(repo.register("anna", "Pa55word!"))

        val saved = dao.insertedUsers.single()
        assertTrue(PasswordHasher.isHashed(saved.password))
        assertFalse(saved.password.contains("Pa55word!"))
        assertTrue(PasswordHasher.verify("Pa55word!", saved.password))
        assertFalse(PasswordHasher.verify("wrong", saved.password))
    }

    @Test
    fun `register logs the new user in with a profile that still needs setup`() = runBlocking {
        repo.register("anna", "Pa55word!")

        assertEquals("anna", session.getCurrentUser())
        assertFalse(session.isProfileSetup())
    }

    @Test
    fun `two users with the same password get different hashes`() = runBlocking {
        repo.register("anna", "same-password")
        dao.mockUserFlow.value = null
        repo.register("ben", "same-password")

        val (a, b) = dao.insertedUsers
        assertTrue(a.password != b.password)
    }

    @Test
    fun `register with a taken username fails and changes nothing`() = runBlocking {
        dao.mockUserFlow.value = UserEntity(username = "anna", password = "old")

        assertFalse(repo.register("anna", "new"))

        assertTrue(dao.insertedUsers.isEmpty())
        assertNull(session.getCurrentUser())
    }

    @Test
    fun `register fails and does not log in when the database ignores the insert - a double tap`() = runBlocking {
        val ignoring = object : RecordingFakeGlarmToDao() {
            override fun insertUser(user: UserEntity): Long = -1L
        }
        val r = GlarmToRepository(ignoring, session, passwordIterations = iterations)

        assertFalse(r.register("anna", "Pa55word!"))

        assertNull(session.getCurrentUser())
    }

    // ---------------- login with a hash ----------------

    @Test
    fun `login succeeds with the registered password`() = runBlocking {
        repo.register("anna", "Pa55word!")
        session.logoutUser()

        assertTrue(repo.login("anna", "Pa55word!"))
        assertEquals("anna", session.getCurrentUser())
    }

    @Test
    fun `login fails with a wrong password and stays logged out`() = runBlocking {
        repo.register("anna", "Pa55word!")
        session.logoutUser()

        assertFalse(repo.login("anna", "pa55word!"))
        assertFalse(repo.login("anna", "Pa55word! "))
        assertFalse(repo.login("anna", ""))
        assertNull(session.getCurrentUser())
    }

    @Test
    fun `login for an unknown user fails`() = runBlocking {
        dao.mockUserFlow.value = null

        assertFalse(repo.login("ghost", "whatever"))
        assertNull(session.getCurrentUser())
    }

    @Test
    fun `login copies profileSetup from the stored user`() = runBlocking {
        dao.mockUserFlow.value = UserEntity(
            username = "anna", password = PasswordHasher.hash("pw", iterations), profileSetup = true
        )

        assertTrue(repo.login("anna", "pw"))
        assertTrue(session.isProfileSetup())
    }

    @Test
    fun `a failed login does not touch the stored password`() = runBlocking {
        val hash = PasswordHasher.hash("pw", iterations)
        dao.mockUserFlow.value = UserEntity(username = "anna", password = hash)

        repo.login("anna", "nope")

        assertTrue(dao.passwordUpdates.isEmpty())
        assertEquals(hash, stored())
    }

    @Test
    fun `a successful login with an up to date hash does not rewrite it`() = runBlocking {
        dao.mockUserFlow.value = UserEntity(username = "anna", password = PasswordHasher.hash("pw", iterations))

        repo.login("anna", "pw")

        assertTrue(dao.passwordUpdates.isEmpty())
    }

    @Test
    fun `a hash made with fewer iterations is upgraded on login`() = runBlocking {
        val weak = PasswordHasher.hash("pw", iterations = 2)
        dao.mockUserFlow.value = UserEntity(username = "anna", password = weak)

        assertTrue(repo.login("anna", "pw"))

        assertEquals(1, dao.passwordUpdates.size)
        assertTrue(stored().startsWith("pbkdf2-sha256\$$iterations\$"))
        assertTrue(PasswordHasher.verify("pw", stored()))
    }

    @Test
    fun `login works again after the upgrade`() = runBlocking {
        dao.mockUserFlow.value = UserEntity(username = "anna", password = PasswordHasher.hash("pw", iterations = 2))
        repo.login("anna", "pw")
        session.logoutUser()

        assertTrue(repo.login("anna", "pw"))
        assertFalse(repo.login("anna", "other"))
    }

    @Test
    fun `the real default iteration count works end to end`() = runBlocking {
        val real = GlarmToRepository(dao, session)

        assertTrue(real.register("anna", "Pa55word!"))
        session.logoutUser()

        assertTrue(real.login("anna", "Pa55word!"))
        assertTrue(stored().startsWith("pbkdf2-sha256\$${PasswordHasher.DEFAULT_ITERATIONS}\$"))
    }

    @Test
    fun `unicode passwords work`() = runBlocking {
        repo.register("anna", "รหัสผ่าน-ลับ-🔒")
        session.logoutUser()

        assertTrue(repo.login("anna", "รหัสผ่าน-ลับ-🔒"))
        assertFalse(repo.login("anna", "รหัสผ่าน-ลับ"))
    }

    // ---------------- legacy plaintext accounts ----------------

    @Test
    fun `a legacy plaintext account logs in and is upgraded to a hash`() = runBlocking {
        dao.mockUserFlow.value = UserEntity(username = "old", password = "secretpassword", profileSetup = true)

        assertTrue(repo.login("old", "secretpassword"))

        assertTrue("the plaintext must be replaced", PasswordHasher.isHashed(stored()))
        assertTrue(PasswordHasher.verify("secretpassword", stored()))
        assertTrue(session.isProfileSetup())
    }

    @Test
    fun `a legacy plaintext account rejects a wrong password and stays plaintext`() = runBlocking {
        dao.mockUserFlow.value = UserEntity(username = "old", password = "secretpassword")

        assertFalse(repo.login("old", "wrong"))
        assertFalse(repo.login("old", "SECRETPASSWORD"))
        assertFalse(repo.login("old", ""))

        assertEquals("secretpassword", stored())
        assertTrue(dao.passwordUpdates.isEmpty())
    }

    @Test
    fun `an upgraded legacy account keeps working`() = runBlocking {
        dao.mockUserFlow.value = UserEntity(username = "old", password = "secretpassword")
        repo.login("old", "secretpassword")
        session.logoutUser()

        assertTrue(repo.login("old", "secretpassword"))
        assertFalse(repo.login("old", "wrong"))
    }

    // ---------------- accounts from before passwords existed ----------------

    @Test
    fun `an account with no password takes the first password used - not any password forever`() = runBlocking {
        dao.mockUserFlow.value = UserEntity(username = "legacy", password = "", profileSetup = true)

        assertTrue(repo.login("legacy", "my-new-password"))
        session.logoutUser()

        assertTrue(PasswordHasher.verify("my-new-password", stored()))
        assertTrue(repo.login("legacy", "my-new-password"))
        assertFalse("a different password no longer works", repo.login("legacy", "anything-else"))
    }

    @Test
    fun `an account with no password does not accept an empty password`() = runBlocking {
        dao.mockUserFlow.value = UserEntity(username = "legacy", password = "")

        assertFalse(repo.login("legacy", ""))

        assertEquals("", stored())
        assertNull(session.getCurrentUser())
    }

    // ---------------- side effects ----------------

    @Test
    fun `password upgrades change only the password column`() = runBlocking {
        dao.mockUserFlow.value = UserEntity(
            username = "old", password = "secretpassword", xp = 777, level = 5, dailyGoal = 3100, profileSetup = true
        )

        repo.login("old", "secretpassword")

        val user = dao.mockUserFlow.value!!
        assertEquals(777L, user.xp)
        assertEquals(5, user.level)
        assertEquals(3100, user.dailyGoal)
        assertNull("no whole-row update that could overwrite concurrent XP changes", dao.lastUpdatedUser)
    }
}
