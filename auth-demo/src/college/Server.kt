package college

import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.getAs
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.serialization.Serializable

@Serializable
data class Server(val host: String, val port: Int)

fun main() {
    val config = ApplicationConfig("application.yaml").getAs<Config>()

    embeddedServer(Netty, host = config.server.host, port = config.server.port) {
        val deps = dependencies(config.jwt)
        app(deps)
    }.start(wait = true)
}
