package college.auth

class AuthService(
    private val users: Users,
    private val passwords: Passwords,
    private val tokens: Tokens,
    private val dummyHash: PasswordHash,
) {
    suspend fun register(username: String, password: String): User? =
        users.create(username, passwords.hash(password))

    suspend fun login(username: String, password: String): AccessToken? {
        val stored = users.byUsername(username)
        // Do comparable expensive work even when the account does not exist.
        val valid = passwords.verify(password, stored?.password ?: dummyHash)
        return if (stored != null && valid) tokens.issue(stored.user) else null
    }
}
