package college.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.JWTVerifier
import java.time.Clock
import java.util.Base64
import kotlinx.serialization.Serializable

@Serializable
class JwtSettings(
    val secret: String,
    val issuer: String = "avans-college-auth",
    val audience: String = "avans-college-api",
) {
    init {
        require(secret.toByteArray().size >= 32) { "JWT signing key must contain at least 32 random bytes" }
        require(issuer.isNotBlank() && audience.isNotBlank())
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
                .withClaim("username", user.username)
                .withClaim("role", user.role.name)
                .withIssuedAt(now)
                .withExpiresAt(now.plusSeconds(900))
                .sign(algorithm)
        return AccessToken(token)
    }
}
