package college

import college.auth.AuthService
import college.auth.InMemoryUsers
import college.auth.JwtSettings
import college.auth.Passwords
import college.auth.Tokens
import college.auth.Users
import java.util.UUID

class Dependencies(val users: Users, val auth: AuthService, val tokens: Tokens)

suspend fun dependencies(settings: JwtSettings): Dependencies {
    val users = InMemoryUsers()
    val passwords = Passwords()
    val tokens = Tokens(settings)
    val dummyHash = passwords.hash(UUID.randomUUID().toString())
    return Dependencies(users, AuthService(users, passwords, tokens, dummyHash), tokens)
}
