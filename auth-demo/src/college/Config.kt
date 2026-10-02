package college

import college.auth.JwtSettings
import kotlinx.serialization.Serializable

@Serializable
data class Config(
    val server: Server,
    val jwt: JwtSettings,
)