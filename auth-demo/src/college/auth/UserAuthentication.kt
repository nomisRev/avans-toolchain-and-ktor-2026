@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package college.auth

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.response.respond

fun userAuthentication(tokens: Tokens, users: Users) =
    jwt<User>("access-token") {
        realm = "avans-college"
        verifier(tokens.verifier)
        validate { credential ->
            credential.payload.subject?.takeIf(String::isNotBlank)?.let(users::byId)
        }
        onUnauthorized = {
            call.respond(
                HttpStatusCode.Unauthorized,
                ApiError("Missing or invalid access token"))
        }
    }
