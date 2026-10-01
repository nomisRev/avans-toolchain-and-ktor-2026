package college.auth

import java.security.MessageDigest
import java.security.SecureRandom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters

// Store these parameters with each hash, so future cost changes can be migrated.
data class Argon2Cost(
    val memoryKiB: Int = 65_536,
    val iterations: Int = 3,
    val parallelism: Int = 4,
)

// Deliberately not a serializable API model; never return it to a client.
class PasswordHash
internal constructor(
    internal val salt: ByteArray,
    internal val digest: ByteArray,
    internal val cost: Argon2Cost,
    internal val version: Int = Argon2Parameters.ARGON2_VERSION_13,
)

class Passwords(private val cost: Argon2Cost = Argon2Cost()) {
    private val random = SecureRandom()
    // Both hash AND verify share this bound: at most two memory-heavy operations.
    private val dispatcher = Dispatchers.IO.limitedParallelism(2)

    suspend fun hash(password: String): PasswordHash =
        withContext(dispatcher) {
            val salt = ByteArray(16).also(random::nextBytes)
            PasswordHash(salt, derive(password, salt, cost), cost)
        }

    suspend fun verify(password: String, stored: PasswordHash): Boolean =
        withContext(dispatcher) {
            val actual = derive(password, stored.salt, stored.cost, stored.version)
            try {
                MessageDigest.isEqual(stored.digest, actual)
            } finally {
                actual.fill(0)
            }
        }

    private fun derive(
        password: String,
        salt: ByteArray,
        cost: Argon2Cost,
        version: Int = Argon2Parameters.ARGON2_VERSION_13,
    ): ByteArray {
        val parameters =
            Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(version)
                .withSalt(salt)
                .withMemoryAsKB(cost.memoryKiB)
                .withIterations(cost.iterations)
                .withParallelism(cost.parallelism)
                .build()
        val chars = password.toCharArray()
        return try {
            ByteArray(32).also { output ->
                Argon2BytesGenerator().apply { init(parameters) }.generateBytes(chars, output)
            }
        } finally {
            chars.fill('\u0000')
            parameters.clear()
        }
    }
}
