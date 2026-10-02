package college

import college.auth.AuthService
import college.auth.InMemoryUsers
import college.auth.JwtSettings
import college.auth.Passwords
import college.auth.Tokens
import college.auth.Users

class Dependencies(val users: Users, val auth: AuthService, val tokens: Tokens)

fun dependencies(settings: JwtSettings): Dependencies {
    val users = InMemoryUsers()
    val passwords = Passwords()
    val tokens = Tokens(settings)
    return Dependencies(users, AuthService(users, passwords, tokens), tokens)
}
