@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package college.auth

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import java.time.Instant

fun userAuthentication(tokens: Tokens, users: Users) =
    jwt<User>("access-token") {
        realm = "avans-college"
        verifier(tokens.verifier)
        validate { credential ->
            User(
                id = credential.payload.subject,
                username = credential.payload.getClaim("username").asString()
            )
        }
        onUnauthorized = {
            call.respond(
                HttpStatusCode.Unauthorized,
                ApiError("Missing or invalid access token")
            )
        }
    }
