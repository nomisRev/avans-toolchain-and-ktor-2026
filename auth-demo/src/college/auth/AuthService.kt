package college.auth

class AuthService(
    private val users: Users,
    private val passwords: Passwords,
    private val tokens: Tokens,
) {
    suspend fun register(username: String, password: String): User? =
        users.create(username, passwords.hash(password))

    suspend fun login(username: String, password: String): AccessToken? {
        val stored = users.byUsername(username) ?: return null
        val valid = passwords.verify(password, stored.password)
        return if (valid) tokens.issue(stored.user) else null
    }
}
