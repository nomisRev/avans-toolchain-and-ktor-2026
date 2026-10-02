package college.auth

import college.app
import college.dependencies
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.text.Charsets.UTF_8

class AuthenticationTest {
    private val key = ByteArray(32) { (it + 1).toByte() } // Test-only key, never used by main().

    private fun settings() = JwtSettings(key.toString(UTF_8))

    private val password = "a sufficiently long demo password"

    private suspend fun HttpClient.credentials(
        path: String,
        username: String = "alice",
        secret: String = password,
    ) =
        post(path) {
            contentType(ContentType.Application.Json)
            setBody(Credentials(username, secret))
        }

    @Test
    fun `register login and access typed principal without leaking password material`() =
        testApplication {
            val deps = dependencies(settings())
            application { app(deps) }
            val http = createClient { install(ContentNegotiation) { json() } }

            assertEquals(HttpStatusCode.Unauthorized, http.get("/me").status)
            val registered = http.credentials("/auth/register")
            assertEquals(HttpStatusCode.Created, registered.status)
            val user = registered.body<User>()
            assertFalse(registered.bodyAsText().contains("password"))
            assertFalse(registered.bodyAsText().contains("salt"))

            val login = http.credentials("/auth/login")
            assertEquals(HttpStatusCode.OK, login.status)
            assertEquals("no-store", login.headers[HttpHeaders.CacheControl])
            val token = login.body<AccessToken>()
            val claims = JWT.decode(token.accessToken)
            assertEquals(user.id, claims.subject)
            assertEquals(900_000, claims.expiresAt.time - claims.issuedAt.time)
            assertEquals(setOf("sub", "iss", "aud", "iat", "exp"), claims.claims.keys)
            assertEquals(900, token.expiresIn)

            val me = http.get("/me") { bearerAuth(token.accessToken) }
            assertEquals(HttpStatusCode.OK, me.status)
            assertEquals(user, me.body<User>())
        }

    @Test
    fun `wrong password and unknown account return the same response`() = testApplication {
        val deps = dependencies(settings())
        application { app(deps) }
        val http = createClient { install(ContentNegotiation) { json() } }
        http.credentials("/auth/register")

        val wrong = http.credentials("/auth/login", secret = "incorrect password")
        val absent = http.credentials("/auth/login", username = "unknown")
        assertEquals(HttpStatusCode.Unauthorized, wrong.status)
        assertEquals(wrong.status, absent.status)
        assertEquals(wrong.bodyAsText(), absent.bodyAsText())
    }

    @Test
    fun `reject forged expired misdirected and incomplete tokens`() = testApplication {
        val config = settings()
        val deps = dependencies(config)
        val user = deps.auth.register("alice", password)!!
        application { app(deps) }
        val now = Instant.now()
        fun token(
            algorithm: Algorithm = Algorithm.HMAC256(key),
            issuer: String = config.issuer,
            audience: String = config.audience,
            subject: String? = user.id,
            issuedAt: Instant? = now,
            expiresAt: Instant? = now.plusSeconds(900),
        ): String =
            JWT.create()
                .withIssuer(issuer)
                .withAudience(audience)
                .apply {
                    subject?.let { withSubject(it) }
                    issuedAt?.let { withIssuedAt(it) }
                    expiresAt?.let { withExpiresAt(it) }
                }
                .sign(algorithm)

        val invalid =
            mapOf(
                "wrong signature" to token(algorithm = Algorithm.HMAC256(ByteArray(32) { 42 })),
                "unsigned" to token(algorithm = Algorithm.none()),
                "wrong algorithm" to token(algorithm = Algorithm.HMAC384(key)),
                "wrong issuer" to token(issuer = "another-service"),
                "wrong audience" to token(audience = "another-api"),
                "expired" to
                    token(issuedAt = now.minusSeconds(100), expiresAt = now.minusSeconds(10)),
                "future issued-at" to token(issuedAt = now.plusSeconds(600)),
                "no expiry" to token(expiresAt = null),
                "no issued-at" to token(issuedAt = null),
                "no subject" to token(subject = null),
                "unknown user" to token(subject = "missing"),
                "malformed" to "not-a-jwt",
            )
        for ((name, value) in invalid) {
            assertEquals(
                HttpStatusCode.Unauthorized,
                client.get("/me") { bearerAuth(value) }.status,
                name,
            )
        }
        assertEquals(HttpStatusCode.OK, client.get("/me") { bearerAuth(token()) }.status)
    }

    @Test
    fun `reject invalid registration duplicate username and malformed body`() = testApplication {
        val deps = dependencies(settings())
        application { app(deps) }
        val http = createClient { install(ContentNegotiation) { json() } }
        assertEquals(
            HttpStatusCode.BadRequest,
            http.credentials("/auth/register", secret = "short").status,
        )
        assertEquals(
            HttpStatusCode.BadRequest,
            http.credentials("/auth/register", username = "bad name").status,
        )
        assertEquals(HttpStatusCode.Created, http.credentials("/auth/register").status)
        assertEquals(HttpStatusCode.Conflict, http.credentials("/auth/register").status)
        val malformed =
            client.post("/auth/register") {
                contentType(ContentType.Application.Json)
                setBody("{not json}")
            }
        assertEquals(HttpStatusCode.BadRequest, malformed.status)
    }

    @Test
    fun `bound request bodies and credential endpoint traffic`() = testApplication {
        val deps = dependencies(settings())
        application { app(deps) }
        val large =
            client.post("/auth/register") {
                contentType(ContentType.Application.Json)
                setBody("x".repeat(5000))
            }
        assertEquals(HttpStatusCode.PayloadTooLarge, large.status)
        repeat(20) {
            client.post("/auth/login") {
                contentType(ContentType.Application.Json)
                setBody("{}")
            }
        }
        assertEquals(HttpStatusCode.TooManyRequests, client.post("/auth/login").status)
        assertEquals(HttpStatusCode.OK, client.get("/health").status)
    }
}
