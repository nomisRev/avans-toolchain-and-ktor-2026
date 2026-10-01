---
layout: intro
class: section-slide
kodee: wave
---

# Authentication

## Passwords, JWTs and a typed principal

Kotlin Toolchain · Ktor 3.6.0 · Argon2id

<!--
After the infrastructure/architecture section: now use those boundaries for registration and login. This is a 10–15 minute walkthrough with a runnable local project in auth-demo. Its Ktor version is deliberately 3.6.0 for typed authentication; the earlier general build-tool snippets use 3.5.2.
-->

---

# Start from the RealWorld backend

## [nomisRev/ktor-full-stack-real-world](https://github.com/nomisRev/ktor-full-stack-real-world)

- **Argon2id** hashes passwords with a random salt
- **UserService** checks credentials and issues a JWT
- **Ktor authentication** validates tokens and loads the user

Our local example adds typed authentication and keeps the same manual DI split.

<!--
Inspected ~/Developer/realworld. Its git remote is the linked repository. Look at backend/src/main/kotlin/org/jetbrains/realworld/user/Argon2Hasher.kt, UserService.kt and config/JwtAuth.kt. The existing project uses the classic named-provider API; do not attribute the new typed API to it.
The new demo calls the password operation hash rather than encrypt. It also shares a bounded dispatcher between hash and verify, persists cost/version with the hash, uses MessageDigest.isEqual, requires JWT time claims and validates its signing-key configuration.
-->

---

# Hashing, signing and encryption

| Mechanism | What it protects |
| --- | --- |
| Argon2id password hash | Stored password verification; no decryption key |
| Signed JWT | Integrity and authenticity of the claims |
| HTTPS | Credentials and tokens in transit |

A signed JWT's payload is readable. Keep passwords and secrets out of it.

<!--
“User encryption” can mean different things. For passwords we want a deliberately expensive one-way password hash. A signature/MAC does not hide the token payload. JWE is a separate encryption format, outside this example. Encrypting other personal data at rest is a separate data-protection decision.
https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html
https://www.rfc-editor.org/rfc/rfc8725
-->

---

# Register → log in → call the API

| Request | Server work | Result |
| --- | --- | --- |
| `POST /auth/register` | Salt + hash password; store user | Public user record |
| `POST /auth/login` | Verify stored hash; sign token | Short-lived access token |
| `GET /me` + Bearer token | Verify JWT; resolve user | Typed `User` |

The password is used at login. Later requests carry the token.

<!--
Registration doesn't return password material or automatically sign the user in. User responses only contain id and username. In production all three requests use HTTPS. The sample stores users in memory so it can run without a database; point at the Users interface as the replacement boundary.
-->

---
class: compact
---

# Hash passwords with Argon2id

```kotlin
val salt = ByteArray(16).also(SecureRandom()::nextBytes)
val parameters = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
  .withVersion(Argon2Parameters.ARGON2_VERSION_13)
  .withSalt(salt)
  .withMemoryAsKB(65_536)
  .withIterations(3)
  .withParallelism(4)
  .build()

val hash = ByteArray(32)
Argon2BytesGenerator().apply { init(parameters) }
  .generateBytes(password.toCharArray(), hash)
```

Store **salt + hash + parameters**. Give every password a fresh random salt.

<!--
Excerpt of auth-demo/src/college/auth/Passwords.kt; the complete code clears temporary arrays and bounds hashing/verification to two concurrent operations. 64 MiB / t=3 / p=4 matches the inspected RealWorld cost and RFC 9106's memory-constrained recommendation. It is a starting point to benchmark, not a universal latency guarantee. A salt is public and unique, not a second secret key.
https://www.rfc-editor.org/rfc/rfc9106
-->

---
class: compact
---

# Verify the password before issuing a token

```kotlin
suspend fun login(username: String, password: String): AccessToken? {
  val stored = users.byUsername(username)
  val valid = passwords.verify(password, stored?.password ?: dummyHash)
  return if (stored != null && valid) tokens.issue(stored.user) else null
}
```

- Recompute using the stored salt and cost; compare in constant time
- Return the same login error for a wrong password or unknown account
- Bound expensive work and limit credential requests

<!--
Exact AuthService implementation. dummyHash is created once during dependency construction. It avoids skipping the expensive work for unknown accounts; it is not a guarantee of identical timing. Passwords.verify uses MessageDigest.isEqual and the same limited dispatcher as hash. The sample's rate limit is shared and in-process, sufficient for showing the concept, not a distributed abuse-prevention design.
-->

---
class: compact
---

# Issue a short-lived JWT

```kotlin
val token = JWT.create()
  .withIssuer(issuer)
  .withAudience(audience)
  .withSubject(user.id)
  .withIssuedAt(now)
  .withExpiresAt(now.plusSeconds(900))
  .sign(algorithm)
```

`algorithm` is `Algorithm.HMAC256(secret)` with a random 32-byte key.

The server reads the key from the environment. The client receives only the token.

<!--
From Tokens.kt. A cryptographic library performs signing; we are not implementing JWT crypto ourselves. The demo requires JWT_SECRET_BASE64, decodes it and refuses keys shorter than 32 bytes. A length check cannot establish entropy: generate it with openssl rand -base64 32. Never derive this signing key from a user's password. HS256 uses a shared secret, so every verifier holding it can also issue tokens. Asymmetric signing is the next step when that trust model does not fit.
https://www.rfc-editor.org/rfc/rfc8725
-->

---
class: compact
---

# Verify the signature and the claims

```kotlin
val verifier = JWT.require(algorithm)
  .withIssuer(issuer)
  .withAudience(audience)
  .withClaimPresence("sub")
  .withClaimPresence("iat")
  .withClaimPresence("exp")
  .build()
```

Pin the algorithm. Require the claims. Reject expired or invalid tokens.

Decoding a token only reads its contents; verification establishes trust.

<!--
Exact Tokens.kt verifier. Auth0 also checks present exp/nbf/iat time claims by default; requiring exp prevents accidentally accepting a signed token with no expiry. The tests cover unsigned/wrong-algorithm tokens, wrong keys, issuer/audience, expired/future-issued tokens, missing claims and unknown users. Then the typed scheme resolves the subject against the user store.
https://github.com/auth0/java-jwt
https://www.rfc-editor.org/rfc/rfc8725
-->

---
class: compact
---

# The authentication scheme produces a User

```kotlin
fun userAuthentication(tokens: Tokens, users: Users) =
  jwt<User>("access-token") {
    verifier(tokens.verifier)
    validate { credential ->
      credential.payload.subject
        ?.takeIf(String::isNotBlank)
        ?.let(users::byId)
    }
  }
```

Invalid token or missing user → **401**. Success → **User**.

<div class="caption">Ktor 3.6.0 · opt in to ExperimentalKtorApi</div>

<!--
UserAuthentication.kt also sets a realm and a generic unauthorized response. AuthenticationScheme is a value returned by jwt<User>, not just a string name shared between distant blocks. Its validator runs after cryptographic verification. Do not load identity from unverified JWT.decode output.
https://ktor.io/docs/server-typed-auth.html
-->

---
class: compact
---

# Typed authentication at the route boundary

```kotlin
val userAuth = userAuthentication(deps.tokens, deps.users)

routing {
  authenticateWith(userAuth) {
    get("/me") {
      val user: User = call.principal
      call.respond(user)
    }
  }
}
```

The principal has a concrete, non-null type inside the protected route.

<!--
This is compiled in auth-demo/src/college/Application.kt with ExperimentalKtorApi opt-in. Imports include io.ktor.server.auth.authenticateWith and io.ktor.server.auth.principal. Ktor 3.6 uses context parameters for these builders. Kotlin 2.4.10 supports them by default: a natural bridge to the optional context-parameter slides or Advanced DSLs in Kotlin.
Having a User identifies the caller; it does not grant access to every user's records. Check ownership/roles in the use case; authenticated callers without permission should receive 403 as appropriate.
https://ktor.io/docs/server-typed-auth.html
-->

---
class: compact
---

# Run the local authentication project

```bash
cd avans-college/auth-demo
./kotlin test
export JWT_SECRET_BASE64="$(openssl rand -base64 32)"
./kotlin run
```

Open `demo.http`: **register → login → /me**.

Then remove the token, change it, or send a wrong password. Expect **401**.

<div class="caption">In-memory users · reset on restart · loopback port 8080</div>

<!--
Run from the presentations workspace root, or just cd auth-demo from the slide deck folder. Generate the random signing key off-screen, keep it for the session, and do not display it. The script exports the value without echoing it. Stop Netty with Ctrl-C when finished.
The test suite includes the full happy path and rejection cases. Open module.yaml to show pinned toolchain/compiler/library versions, then Passwords, Tokens and Application. If the credential rate limit returns 429 after repeated demonstrations, wait for the one-minute refill or restart the local demo.
-->

---

# Authentication is one part of the user lifecycle

- **Authorization:** check ownership and permissions
- **Token lifecycle:** expiry, refresh, revocation and key rotation
- **Accounts:** durable storage, recovery and stronger sign-in when needed

The demo covers password verification and access tokens. Use the RealWorld project to explore the wider backend.

[github.com/nomisRev/ktor-full-stack-real-world](https://github.com/nomisRev/ktor-full-stack-real-world)

<!--
Scope the example honestly. It deliberately omits persistence, refresh tokens, reset flows, MFA and revocation. Logging out a client does not invalidate a copied token. A browser frontend needs its own token/session-storage and XSS/CSRF design. An identity provider is often the practical choice for a production account lifecycle; this exercise explains what happens underneath.
-->
