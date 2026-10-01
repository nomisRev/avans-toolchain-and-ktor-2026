package college.auth

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.serialization.Serializable

@Serializable data class User(val id: String, val username: String)

class StoredUser(val user: User, val password: PasswordHash)

interface Users {
    fun byUsername(username: String): StoredUser?

    fun byId(id: String): User?

    fun create(username: String, password: PasswordHash): User?
}

// Teaching storage: users disappear on restart. Replace this boundary with a database.
class InMemoryUsers : Users {
    private val users = ConcurrentHashMap<String, StoredUser>()

    override fun byUsername(username: String): StoredUser? = users[username]

    override fun byId(id: String): User? = users.values.firstOrNull { it.user.id == id }?.user

    override fun create(username: String, password: PasswordHash): User? {
        val user = User(UUID.randomUUID().toString(), username)
        return if (users.putIfAbsent(username, StoredUser(user, password)) == null) user else null
    }
}
