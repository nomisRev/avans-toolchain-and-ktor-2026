@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package college

import college.auth.ApiError
import college.auth.User
import college.auth.authRoutes
import college.auth.userAuthentication
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authenticateWith
import io.ktor.server.auth.principal
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.bodylimit.RequestBodyLimit
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.util.getOrFail
import kotlin.time.Duration.Companion.minutes

fun Application.app(deps: Dependencies) {
    install(ContentNegotiation) { json() }
    install(RequestBodyLimit) { bodyLimit { 4096 } }
    install(StatusPages) {
        exception<BadRequestException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, ApiError("Invalid request body"))
        }
    }
    install(RateLimit) {
        register(RateLimitName("credentials")) {
            // One shared budget for this local demo, not an IP/account policy.
            rateLimiter(limit = 20, refillPeriod = 1.minutes)
        }
    }
    val userAuth = userAuthentication(deps.tokens, deps.users)
    routing {


        fun RoutingContext.userId(): Long =
            call.pathParameters
                .getOrFail<Long>("userId")

        context(context: RoutingContext)
        suspend fun User.respond() {
            context.call.respond(this)
        }

        get("/health") {
            val id = userId()


            call.respondText("OK")
        }

        get {
            val id = userId()
        }

        rateLimit(RateLimitName("credentials")) { authRoutes(deps.auth) }

        val config = HikariConfig()


        authenticateWith(userAuth) {
            get("/me") {
                val user: User = call.principal
                call.response.headers.append(HttpHeaders.CacheControl, "no-store")
                // call.respond(user)
                user.respond()
            }
        }
    }
}
