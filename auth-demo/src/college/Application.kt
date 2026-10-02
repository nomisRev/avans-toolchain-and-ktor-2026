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
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlin.time.Duration.Companion.minutes

private val RateLimitName = RateLimitName("credentials")

fun Application.app(deps: Dependencies) {
    install(ContentNegotiation) { json() }
    install(RequestBodyLimit) { bodyLimit { 4096 } }
    install(StatusPages) {
        exception<BadRequestException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, ApiError("Invalid request body"))
        }
    }

    install(RateLimit) {
        register(RateLimitName) {
            rateLimiter(limit = 10, refillPeriod = 1.minutes)
        }
    }

    routing {
        health()
        rateLimit(RateLimitName) { authRoutes(deps) }
    }
}
