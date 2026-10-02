@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package college.auth

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.AuthenticationScheme
import io.ktor.server.auth.AuthenticationSchemeWithRoles
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.withRoles
import io.ktor.server.response.respond

fun userAuthentication(tokens: Tokens) =
    jwt<User>("access-token") {
        realm = "avans-college"
        verifier(tokens.verifier)
        validate { credential ->
            User(
                id = credential.payload.subject,
                username = credential.payload.getClaim("username").asString(),
                role = Role.valueOf(credential.payload.getClaim("role").asString())
            )
        }
        onUnauthorized = {
            call.respond(
                HttpStatusCode.Unauthorized,
                ApiError("Missing or invalid access token")
            )
        }
    }.withRoles { setOf(it.role) }
