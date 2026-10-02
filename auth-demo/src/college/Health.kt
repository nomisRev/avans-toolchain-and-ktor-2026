package college

import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get

fun Routing.health() {
    get("/health") {
        call.respondText("OK")
    }
}
