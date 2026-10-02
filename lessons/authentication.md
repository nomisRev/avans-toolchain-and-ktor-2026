---
layout: intro
class: section-slide
kodee: wave
---

# Authentication

## Passwords, JWTs, roles and an Android client

Kotlin Toolchain · Ktor 3.6.0 · Argon2id

<!--
After the infrastructure/architecture section: now use those boundaries for registration and login. This is a 10–15 minute walkthrough with a runnable local project in auth-demo; the Authorization header, Android client and role-based access sections add another 10–15 minutes. Its Ktor version is deliberately 3.6.0 for typed authentication; the earlier general build-tool snippets use 3.5.2.
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
  val stored = users.byUsername(username) ?: return null
  val valid = passwords.verify(password, stored.password)
  return if (valid) tokens.issue(stored.user) else null
}
```

- Recompute using the stored salt and cost; compare in constant time
- Return the same login error for a wrong password or unknown account
- Bound expensive work and limit credential requests

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
      val userId = credential.payload.subject
      
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
class: compact
---

# The token travels in the `Authorization` header

```http
GET /me HTTP/1.1
Host: avans-college-auth.onrender.com
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOi…
```

```http
HTTP/1.1 200 OK
Content-Type: application/json

{"id":"c6c99c96-…","username":"student"}
```

`Bearer` is the scheme: whoever holds the token is the caller. HTTPS only.

No header, a changed byte or an expired token → **401**.

<!--
This is what demo.http sends; now look at it as raw HTTP, because the Android client has to produce exactly this header. "Bearer" (RFC 6750) means possession is enough: there is no proof that the sender is the user the token was issued to. That is why it must never leave HTTPS, never be logged, and is short-lived (15 minutes here).
The demo's onUnauthorized replaces Ktor's default challenge, so its 401 carries a JSON ApiError and no WWW-Authenticate header (checked against the running server).
https://www.rfc-editor.org/rfc/rfc6750
-->

---
class: compact
---

# A JWT is three Base64URL parts

```text
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9 . eyJpc3MiOi… . pX0oqGPfaB…
              header                    payload      signature
```

```json
{"alg":"HS256","typ":"JWT"}
{"iss":"avans-college-auth","aud":"avans-college-api",
 "sub":"c6c99c96-…","iat":1790856303,"exp":1790857203}
```

`signature = HMAC-SHA256(secret, header + "." + payload)`

Anyone can decode the first two parts. Only the key holder can make the third.

<!--
Decoded from a real token issued by the demo: cut on the dots and Base64URL-decode. exp − iat = 900 seconds. Live: paste a token into jwt.io (only demo tokens!) or run: echo "$TOKEN" | cut -d. -f2 | base64 -d
Changing one character of the payload (e.g. sub) makes the server recompute a different HMAC, so the verifier rejects it. This is why the payload can be readable but not editable, and why the signing key must stay on the server: the Android app never sees it.
-->

---
class: compact
---

# Ktor turns the header into a typed principal

| Step | Where in the demo | On failure |
| --- | --- | --- |
| Read `Authorization: Bearer <token>` | `jwt<User>("access-token")` | **401** |
| Check `alg`, signature, `iss`, `aud`, `exp` | `verifier(tokens.verifier)` | **401** |
| Load the user named by `sub` | `validate { … users::byId }` | **401** |
| Check the roles the route requires | `withRoles { … }` | **403** |
| Run the handler with `call.principal` | `authenticateWith(…)` | — |

Every request repeats these steps. The server keeps no session.

<!--
The first three rows are the slides we already saw, now ordered as the request pipeline runs them. The roles row is the next section. "Stateless" means: no server-side session store, every request proves itself again. The consequence is that logging out on the client does not invalidate a copied token; it stays valid until exp.
-->

---
class: compact
---

# One Ktor `HttpClient` for the whole Android app

```kotlin
const val ApiHost = "avans-college-auth.onrender.com"

fun apiClient(store: TokenStore): HttpClient =
  HttpClient(OkHttp) {
    defaultRequest { url("https://$ApiHost/") }
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    install(Auth) {
      bearer {
        loadTokens {
          store.accessToken?.let { BearerTokens(it, refreshToken = null) }
        }
        refreshTokens { null }
        sendWithoutRequest { request -> request.url.host == ApiHost }
      }
    }
  }
```

`loadTokens` supplies the header. `sendWithoutRequest` limits it to our host.

<!--
Dependencies: io.ktor:ktor-client-okhttp, ktor-client-auth, ktor-client-content-negotiation, ktor-serialization-kotlinx-json (3.6.0). OkHttp is the usual Android engine. Create the client once (Application, or your DI graph) and close it when the app scope ends.
sendWithoutRequest: without it the plugin first sends the request without a token, waits for the 401 and then retries. Restricting it to our host stops the token leaking to any other URL the same client calls.
refreshTokens is called after a 401. The demo has no refresh tokens, so it returns null and the 401 reaches the app, which shows the login screen. With refresh tokens, this is where you call the refresh endpoint and return new BearerTokens.
ignoreUnknownKeys: the server's AccessToken also contains tokenType; without it the client crashed while decoding the login response. The server may add fields; the client should not break.
This code and the next two slides compiled and ran against the demo server: 401 before login, 200 after, 403 for a student on a teacher route.
-->

---
class: compact
---

# Log in once, then the plugin adds the header

```kotlin
class AuthApi(private val client: HttpClient, private val store: TokenStore) {
  suspend fun login(username: String, password: String): Boolean {
    val response = client.post("auth/login") {
      contentType(ContentType.Application.Json)
      setBody(Credentials(username, password))
    }
    if (response.status != HttpStatusCode.OK) return false
    store.accessToken = response.body<AccessToken>().accessToken
    client.clearAuthTokens()
    return true
  }

  suspend fun me(): User? {
    val response = client.get("me")
    return if (response.status == HttpStatusCode.OK) response.body() else null
  }
}
```

The bearer provider caches its token: `clearAuthTokens()` after login and logout.

<!--
Credentials, AccessToken and User are @Serializable copies of the server's DTOs on the client side (or a shared KMP module). TokenStore is a small class holding accessToken: String?; logout() sets it to null and calls clearAuthTokens() too.
Without clearAuthTokens() the provider keeps the token it loaded first, so logging in as another account would keep sending the old Authorization header.
me() returns null on 401 instead of throwing; expectSuccess is false by default.
-->

---
class: compact
---

# The `ViewModel` sees a `User` or a logged-out state

```kotlin
sealed interface ProfileState {
  data object Loading : ProfileState
  data class Loaded(val user: User) : ProfileState
  data object LoggedOut : ProfileState
}

class ProfileViewModel(private val api: AuthApi) : ViewModel() {
  private val _state = MutableStateFlow<ProfileState>(ProfileState.Loading)
  val state: StateFlow<ProfileState> = _state

  fun load() {
    viewModelScope.launch {
      val user = api.me()
      _state.value = user?.let(ProfileState::Loaded) ?: ProfileState.LoggedOut
    }
  }
}
```

HTTP, headers and tokens stay below the `ViewModel`.

<!--
Compose collects state with collectAsStateWithLifecycle() and navigates to the login screen on LoggedOut. Same layering argument as in the architecture section: the UI does not know about Authorization headers, the AuthApi does not know about screens.
-->

---

# Android details that bite

- The emulator reaches your laptop's `localhost:8080` at `10.0.2.2:8080`
- Plain HTTP is blocked by default since Android 9: use HTTPS or a debug-only network security config
- Add `<uses-permission android:name="android.permission.INTERNET" />`
- Keep the token in memory, or encrypt it with an Android Keystore key
- Logging plugin: `sanitizeHeader { it == HttpHeaders.Authorization }`

<!--
The deployed demo on Render uses HTTPS, so it works without a network security config. For the local server, either allow cleartext only for 10.0.2.2 in a debug network_security_config.xml, or run ./kotlin run and use adb reverse tcp:8080 tcp:8080 to reach it via localhost.
Storage: in memory means "log in again after process death" — fine for a 15-minute token. Persisting it: DataStore with values encrypted by a Keystore key. Never SharedPreferences in plain text, never in Logcat.
-->

---
layout: intro
class: section-slide
kodee: wave
---

# Role-based access

## Authentication says who. Authorization says what.

<!--
Back on the server. Until now every authenticated user could call every protected route. The /me route was safe because it only returns the caller themselves.
-->

---

# 401 asks who you are, 403 says no

| Status | Meaning | Android reaction |
| --- | --- | --- |
| **401** Unauthorized | No valid token: unknown caller | Show the login screen |
| **403** Forbidden | Known caller, not allowed | Show "no access"; logging in again won't help |

Authentication produces a `User`. Authorization decides what that `User` may do.

<!--
The name "Unauthorized" for 401 is a historic misnomer: it really means unauthenticated. Treating a 403 like a 401 in the app creates a login loop: the user logs in, gets the same token claims, and the same 403.
-->

---
class: compact
---

# Roles are a type the scheme resolves

```kotlin
enum class Role : AuthenticationRole { Student, Teacher, Admin }

fun roleAuthentication(tokens: Tokens, users: Users) =
  userAuthentication(tokens, users).withRoles { user ->
    users.roles(user.id)
  }
```

`withRoles` runs after the JWT is verified and the `User` is loaded.

<div class="caption">Ktor 3.6.0 · io.ktor.server.auth.withRoles · ExperimentalKtorApi</div>

<!--
Users gets one extra function: fun roles(id: String): Set<Role>. In a real database this is a user_roles table. The resolver is a suspend lambda with RoutingContext as receiver, so it can query a database or cache.
withRoles wraps the existing typed scheme: the userAuthentication from before is reused unchanged. Compiled against Ktor 3.6.0 in a scratch copy of auth-demo; the live-coding project itself is untouched.
https://ktor.io/docs/server-typed-auth.html
-->

---
class: compact
---

# The route declares the roles it needs

```kotlin
val roleAuth = roleAuthentication(deps.tokens, deps.users)

routing {
  authenticateWith(roleAuth, roles = setOf(Role.Teacher)) {
    get("/grades") {
      val roles: Set<Role> = call.principal.roles
      call.respond(roles.map(Role::name))
    }
  }
}
```

No token → **401**. Student → **403**. Teacher → **200**.

<!--
Verified end to end with the Ktor client from the Android slides: student1 got 403 Forbidden on /grades, teacher1 got 200 OK.
call.principal is still a User; .roles is an extension that is only available inside a role-aware authenticateWith block (context parameters again), so you cannot accidentally read roles on a route that never resolved them.
-->

---
class: compact
---

# `roles` requires every role in the set

```kotlin
authenticateWith(
  roleAuth,
  roles = setOf(Role.Teacher, Role.Admin),
  onForbidden = {
    val error = ApiError("Requires Teacher and Admin")
    call.respond(HttpStatusCode.Forbidden, error)
  },
) {
  get("/admin/grades") { call.respond("ok") }
}
```

Ktor checks `containsAll`: a Teacher without Admin gets **403**.

For *any of*, omit `roles` and check in the handler.

<!--
Read in ktor-server-auth 3.6.0 sources: validateRoles uses resolvedRoles.containsAll(requiredRoles). This is the easiest mistake to make with the API: setOf(Teacher, Admin) reads like "teachers or admins".
onForbidden can be set per route (here) or once on withRoles(onForbidden = …). The default is an empty 403.
-->

---
class: compact
---

# Ownership is a check in the handler

```kotlin
authenticateWith(roleAuth) {
  get("/students/{id}/grades") {
    val user: User = call.principal
    val student = call.parameters.getOrFail("id")
    val allowed = student == user.id || Role.Teacher in user.roles
    if (!allowed) return@get call.respond(HttpStatusCode.Forbidden)
    call.respond(grades.of(student))
  }
}
```

Roles say *what kind* of user. Ownership says *whose* data.

<!--
Without roles = …, Ktor resolves roles but does not enforce any: the handler decides. A role alone would let every student read every student's grades (an IDOR / broken object level authorization, number one in the OWASP API Top 10).
In the architecture from earlier, move this check into the use case (GradeService.gradesOf(caller, studentId)) so it is unit-testable without HTTP.
https://owasp.org/API-Security/editions/2023/en/0xa1-broken-object-level-authorization/
-->

---

# Look roles up; don't trust a stale claim

| | Role claim in the JWT | Lookup per request (demo) |
| --- | --- | --- |
| Cost | No extra lookup | One query or cache hit |
| Revoking a role | Valid until `exp` | Immediate |
| Android can read it | Yes, the payload is readable | Through an API such as `/me` |

The app may hide a teacher button. Only the server's **403** protects the data.

<!--
Both are valid designs. A roles claim is signed, so the client cannot change it, but a demoted teacher keeps their role until the token expires. Short tokens shrink that window. If you put roles in the token, still resolve them with withRoles: read the claim from the verified payload instead of the database.
Client-side checks are UX only: anyone with the token can call the API with curl, as we did with demo.http.
-->

---

# Authentication is one part of the user lifecycle

- **Authorization:** fine-grained policies and audit logs beyond roles
- **Token lifecycle:** refresh tokens for `refreshTokens { }`, revocation and key rotation
- **Accounts:** durable storage, recovery and stronger sign-in when needed

The demo covers password verification and access tokens. Use the RealWorld project to explore the wider backend.

[github.com/nomisRev/ktor-full-stack-real-world](https://github.com/nomisRev/ktor-full-stack-real-world)

<!--
Scope the example honestly. It deliberately omits persistence, refresh tokens, reset flows, MFA and revocation. Logging out a client does not invalidate a copied token. A browser frontend needs its own token/session-storage and XSS/CSRF design. An identity provider is often the practical choice for a production account lifecycle; this exercise explains what happens underneath.
-->
