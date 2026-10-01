package college.auth

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class PasswordsTest {
    @Test
    fun `equal passwords receive different salts and both verify`() = runBlocking {
        val passwords = Passwords()
        val first = passwords.hash("a long password for this test")
        val second = passwords.hash("a long password for this test")
        assertFalse(first.salt.contentEquals(second.salt))
        assertFalse(first.digest.contentEquals(second.digest))
        assertTrue(passwords.verify("a long password for this test", first))
        assertTrue(passwords.verify("a long password for this test", second))
        assertFalse(passwords.verify("a different long password", first))
    }

    @Test
    fun `verification uses stored parameters after changing the default cost`() = runBlocking {
        val original = Passwords(Argon2Cost(memoryKiB = 19_456, iterations = 2, parallelism = 1))
        val stored = original.hash("migration keeps this password working")
        assertTrue(Passwords().verify("migration keeps this password working", stored))
    }
}
