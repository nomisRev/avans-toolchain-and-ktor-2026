package college

import college.auth.JwtSettings
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.runBlocking

fun main() {
    // Fail before opening a port if the signing key is absent or malformed.
    val settings = JwtSettings.fromEnvironment()
    val deps = runBlocking { dependencies(settings) }
    val host = System.getenv("HOST") ?: "127.0.0.1"
    val port = System.getenv("PORT")?.toInt() ?: 8080
    embeddedServer(Netty, host = host, port = port) { app(deps) }.start(wait = true)
}
