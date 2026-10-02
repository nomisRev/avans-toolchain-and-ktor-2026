package college.auth

import college.Dependencies
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticateWith
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.utils.io.ExperimentalKtorApi
import kotlinx.serialization.Serializable

@Serializable
class Credentials(val username: String, val password: String)

@Serializable
data class ApiError(val message: String)

private val usernamePattern = Regex("[a-zA-Z0-9_]{3,32}")

@OptIn(ExperimentalKtorApi::class)
fun Route.authRoutes(deps: Dependencies) {
    post("/auth/register") {
        val input = call.receive<Credentials>()
        if (!usernamePattern.matches(input.username) || input.password.length !in 15..128) {
            call.respond(
                HttpStatusCode.BadRequest,
                ApiError("Use a 3–32 character username (letters, digits, _) and a 15–128 character password")
            )
        } else {
            val user = deps.auth.register(input.username, input.password)
            if (user == null) call.respond(HttpStatusCode.Conflict, ApiError("Username unavailable"))
            else call.respond(HttpStatusCode.Created, user)
        }
    }

    post("/auth/login") {
        val input = call.receive<Credentials>()
        val token =
            if (usernamePattern.matches(input.username) && input.password.length in 1..128) {
                deps.auth.login(input.username, input.password)
            } else null
        call.response.headers.append(HttpHeaders.CacheControl, "no-store")
        if (token == null)
            call.respond(HttpStatusCode.Unauthorized, ApiError("Invalid username or password"))
        else call.respond(token)
    }

    val userAuth = userAuthentication(deps.tokens)
    authenticateWith(userAuth) {
        get("/me") {
            val user: User = call.principal
            call.response.headers.append(HttpHeaders.CacheControl, "no-store")
            call.respond(user)
        }
    }
}
