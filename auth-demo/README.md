# Ktor authentication with Kotlin Toolchain

A small runnable companion to Avans College: register a user, hash the password with Argon2id, issue your own signed JWT, then use Ktor's typed authentication to protect `/me`.

Based on Simon's [Ktor Full Stack RealWorld](https://github.com/nomisRev/ktor-full-stack-real-world), especially its `Argon2Hasher` and `UserService`. This example adapts that approach to **Ktor 3.6.0 typed authentication** (`jwt<User>`, `authenticateWith`, `call.principal`). The API is experimental and uses context parameters. The project pins Kotlin Toolchain **0.12.2**, Kotlin **2.4.10**, and Java **21**. Earlier presentation examples use Ktor 3.5.2; this project deliberately uses the release with typed authentication.

## Run

From this directory on macOS/Linux:

```sh
./kotlin build
./kotlin test
export JWT_SECRET="$(openssl rand -base64 32)"
./kotlin run
```

The wrapper provisions the toolchain and JDK. Windows has the equivalent `kotlin.bat`; set `JWT_SECRET` to a Base64-encoded cryptographically random 32-byte key in the process environment.

Open `demo.http` in IntelliJ's HTTP Client to run registration → login → `/me`, then the failure cases. The server listens on `127.0.0.1:8080`; stop it with Ctrl-C. No signing key is committed and startup fails if it is absent, malformed or shorter than 32 bytes. Preserve the same key across restarts/deployments when tokens should stay valid; changing it invalidates existing tokens.

Optional environment settings: `HOST`, `PORT`, `JWT_ISSUER`, `JWT_AUDIENCE`. For a container use `HOST=0.0.0.0` and HTTPS at the ingress. The plaintext loopback URL is for local development.

## Docker Hub and Render

This module belongs to the parent Toolchain project, alongside the [local Ktor and Jib plugins](../plugins/README.md). From `avans-college/`:

```sh
./kotlin check
./kotlin do buildImage
# When ready to publish to Docker Hub:
./kotlin do publishImage
```

The configured destination is `docker.io/vergauwensimon/avans-college-auth:demo-1` (also tagged `latest`). Jib uses `credHelper: desktop` to read Docker Desktop's stored Docker Hub login. No registry secret is written to this project. The image targets `linux/amd64` with Java 21 and binds to `0.0.0.0`.

[render.yaml](render.yaml) defines an image-backed web service, `/health`, port 10000 and bounded JVM memory. Publish the image before creating the service. Supply `JWT_SECRET` privately in Render; `sync: false` leaves it for setup. For manual service creation, copy the same environment and health settings. Keep Docker Command empty. A private Docker Hub repository needs separate pull credentials in Render; Desktop's credentials remain local.

The signing key is injected at runtime and is not baked into the image. Users are in memory and reset on each deployment. Use the Render HTTPS URL as `@base` in `demo.http`. See the [deployment walkthrough](../examples/deployment/README.md) for the webhook demo.

## Curl walkthrough

With the server running (login extraction uses `jq`):

```sh
curl -i http://localhost:8080/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"student","password":"a long classroom passphrase"}'

AUTH_TOKEN=$(curl -fsS http://localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"student","password":"a long classroom passphrase"}' \
  | jq -r .accessToken)

curl -i http://localhost:8080/me -H "Authorization: Bearer $AUTH_TOKEN"
curl -i http://localhost:8080/me
unset AUTH_TOKEN
```

The first registration returns 201; a duplicate username returns 409. Correct login returns a 15-minute bearer token. `/me` returns the user with a valid token and 401 without one. Tokens are credentials; avoid logging them or pasting live tokens into online decoders.

## Code walkthrough

| File | Responsibility |
| --- | --- |
| `src/college/Server.kt` | Environment, dependency construction, Netty |
| `src/college/Dependencies.kt` | Manual DI; one shared password hasher |
| `src/college/Application.kt` | HTTP plugins, routes, typed principal |
| `src/college/auth/Passwords.kt` | Salted Argon2id hashing and verification |
| `src/college/auth/Users.kt` | Repository boundary and in-memory implementation |
| `src/college/auth/AuthService.kt` | Register/login use cases |
| `src/college/auth/Tokens.kt` | JWT settings, issuing and verification policy |
| `src/college/auth/UserAuthentication.kt` | Verified JWT → existing `User` |
| `src/college/auth/AuthRoutes.kt` | Request validation and public credential endpoints |

Passwords are hashed, not reversibly encrypted. Each stored record contains a fresh 16-byte salt, a 32-byte hash, Argon2 version and cost parameters. The RealWorld-style cost is 64 MiB, three iterations and four lanes. Bouncy Castle implements Argon2id. Both hash and verify use the same two-operation concurrency limit; verification uses constant-time byte comparison. Stored parameters allow changing defaults without breaking older records. The code clears temporary arrays; JVM request strings cannot be reliably wiped.

Login performs a dummy-hash verification for unknown usernames and returns the same generic 401 for unknown users and incorrect passwords. This reduces an obvious timing difference; it does not prove identical response timing. The public registration endpoint deliberately exposes username availability.

JWTs use HS256 and contain only `sub`, `iss`, `aud`, `iat` and `exp`. The verifier pins the algorithm, issuer and audience and requires the time/subject claims. It verifies signatures and time claims before resolving the current user by ID. The payload is readable; it contains no password, salt, hash or signing key. Anyone holding the shared HS256 key can also mint tokens—use an asymmetric key architecture when verifiers must not be issuers.

## Teaching scope

Users are **in memory** and disappear on restart. Tokens for missing users are rejected. A database implementation must persist the hash, salt, cost/version fields and unique username constraint. This project has no refresh tokens, revocation store, password reset, MFA or account recovery. Tokens last up to 15 minutes; logging out a client alone does not revoke a stolen token. Authentication identifies a caller; authorization still checks ownership/permissions for each operation.

There is a 4 KiB request-body limit and a shared in-process budget of 20 credential requests per minute (429 once exhausted). This keeps the classroom demo bounded; a deployed service needs an appropriate distributed abuse policy, persistent storage, key rotation and lifecycle design. Benchmark the Argon2 cost on the deployment hardware. The local HTTP Client demo intentionally avoids choosing a browser token-storage design; that also requires addressing XSS, and CSRF when cookies are used.

Tests exercise hashing/salts, stored cost compatibility, the complete HTTP login flow, malformed input, size/rate limits, duplicate users, weak keys and rejected JWTs (including wrong algorithm/signature/issuer/audience, expiry, missing claims and unknown users). `./kotlin check` runs these tests too.

References: [RealWorld source](https://github.com/nomisRev/ktor-full-stack-real-world), [Ktor typed authentication](https://ktor.io/docs/server-typed-auth.html), [Argon2 RFC 9106](https://www.rfc-editor.org/rfc/rfc9106), [OWASP password storage](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html), [JWT best practices](https://www.rfc-editor.org/rfc/rfc8725).
