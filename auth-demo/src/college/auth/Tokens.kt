package college.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.JWTVerifier
import java.time.Clock
import java.util.Base64
import kotlinx.serialization.Serializable

class JwtSettings(
    secret: ByteArray,
    val issuer: String = "avans-college-auth",
    val audience: String = "avans-college-api",
) {
    internal val secret = secret.copyOf()

    init {
        require(secret.size >= 32) { "JWT signing key must contain at least 32 random bytes" }
        require(issuer.isNotBlank() && audience.isNotBlank())
    }

    companion object {
        fun fromEnvironment(env: Map<String, String> = System.getenv()): JwtSettings {
            val encoded =
                requireNotNull(env["JWT_SECRET_BASE64"]) {
                    "Set JWT_SECRET_BASE64 to a Base64-encoded random key (openssl rand -base64 32)"
                }
            val secret =
                try {
                    Base64.getDecoder().decode(encoded)
                } catch (_: IllegalArgumentException) {
                    error("JWT_SECRET_BASE64 must be valid Base64")
                }
            return JwtSettings(
                secret,
                env["JWT_ISSUER"] ?: "avans-college-auth",
                env["JWT_AUDIENCE"] ?: "avans-college-api",
            )
        }
    }
}

@Serializable
data class AccessToken(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long = 900,
)

class Tokens(settings: JwtSettings, private val clock: Clock = Clock.systemUTC()) {
    private val algorithm = Algorithm.HMAC256(settings.secret)
    private val issuer = settings.issuer
    private val audience = settings.audience

    val verifier: JWTVerifier =
        JWT.require(algorithm)
            .withIssuer(issuer)
            .withAudience(audience)
            .withClaimPresence("sub")
            .withClaimPresence("iat")
            .withClaimPresence("exp")
            .build()

    fun issue(user: User): AccessToken {
        val now = clock.instant()
        val token =
            JWT.create()
                .withIssuer(issuer)
                .withAudience(audience)
                .withSubject(user.id)
                .withIssuedAt(now)
                .withExpiresAt(now.plusSeconds(900))
                .sign(algorithm)
        return AccessToken(token)
    }
}
