---
layout: intro
class: section-slide
kodee: wave
---

# Architecture that helps testing

## The same split as Opinionated Ktor Services

---

# Three places, three responsibilities

<div class="flow">
  <div><strong>Bootstrap</strong><small>Read config<br>Construct & own resources</small></div>
  <span>→</span>
  <div><strong>Application</strong><small>Install plugins<br>Wire feature routes</small></div>
  <span>→</span>
  <div><strong>Feature</strong><small>HTTP boundary<br>Service / persistence</small></div>
</div>

Pass each route the service it needs.

<!--
Adapted from opinionated-ktor-services lessons 3 and 4 and ktor-quickstart Server.kt/Dependencies.kt.
The arrows show wiring, not a mandatory runtime call stack. Package by feature; introduce build modules only when they help. A Dependencies aggregate is convenient at composition time, but should not become a service locator passed everywhere.
-->

---
class: compact
---

# Bootstrap constructs; application wires

```kotlin
fun main() {
  val config = loadConfig()
  embeddedServer(Netty, port = config.port) {
    val dependencies = dependencies(config)
    app(dependencies)
  }.start(wait = true)
}

fun Application.app(dependencies: Dependencies) {
  install(ContentNegotiation) { json() }
  routing {
    postRoutes(dependencies.posts)
  }
}
```

Tests can call `app(...)` with a different dependency graph.

<!--
Schematic composition example: loadConfig, Config and dependencies are application functions, not Ktor APIs. The sample route below uses a PostService interface; the production graph can construct a database-backed implementation.
The production bootstrap owns the graph and its cleanup. app only configures HTTP behavior.
-->

---

# Keep feature code together

<ul class="tree" aria-label="src with Server.kt, Application.kt and Dependencies.kt, a post package holding PostRoutes.kt, PostService.kt and PostRepository.kt, and a profile package with the same shape">
  <li class="dir"><span>src</span>
    <ul>
      <li class="kt"><span>Server.kt</span><em>bootstrap</em></li>
      <li class="kt"><span>Application.kt</span><em>plugins + route wiring</em></li>
      <li class="kt"><span>Dependencies.kt</span><em>graph construction</em></li>
      <li class="pkg"><span>post</span>
        <ul>
          <li class="kt new"><span>PostRoutes.kt</span><em>HTTP</em></li>
          <li class="kt new"><span>PostService.kt</span><em>use cases</em></li>
          <li class="kt new"><span>PostRepository.kt</span><em>persistence</em></li>
        </ul>
      </li>
      <li class="pkg"><span>profile</span><em>same shape …</em></li>
    </ul>
  </li>
</ul>

A package boundary is enough to start.

<!--
Follow the feature packaging of the opinionated talk. Don't create an interface for every class automatically; use a boundary where alternate implementations or focused tests help.
realworld sometimes passes a repository directly to a route. Introduce a service when there is an actual use case to encapsulate, not merely to add a layer.
-->

---
class: compact
---

# Extract → process → respond

```kotlin
fun Route.postRoutes(posts: PostService) {
  get("/posts/{id}") {
    val id = call.parameters["id"]?.toLongOrNull()
      ?: return@get call.respond(HttpStatusCode.BadRequest)

    val post = posts.find(id)

    if (post == null) call.respond(HttpStatusCode.NotFound)
    else call.respond(post)
  }
}
```

`PostService` knows the domain; the route knows HTTP.

<!--
This mirrors the three phases in lesson 3 of Opinionated Ktor Services. Manual parsing keeps the example independent of newer Ktor parameter-delegate conveniences.
Post is serializable and the application installs JSON content negotiation. The source-only examples are teaching snippets, not a compiled sample backend.
-->

---
class: compact
---

# What does this function need?

```kotlin
import org.koin.ktor.ext.inject

fun Route.postRoutes() {
  val posts by inject<PostService>()

  get("/posts/{id}") {
    val id = call.parameters["id"]?.toLongOrNull()
      ?: return@get call.respond(HttpStatusCode.BadRequest)
    val post = posts.find(id)
    if (post == null) call.respond(HttpStatusCode.NotFound)
    else call.respond(post)
  }
}
```

The dependency is hidden in the function body.

<!--
Koin's supported Route extension. The delegate is declared when routes are registered; resolution is lazy on first access. Do not claim this performs a fresh container lookup on every request: the local lazy delegate is captured by the handler.
The criticism here is visibility and coupling, not an invented performance problem. Tests now need Koin configuration to install these routes.
Source: https://insert-koin.io/docs/reference/koin-core/injection/
-->

---
magicMove: true
class: compact
---

# A parameter already does the job

```kotlin
fun Route.postRoutes(posts: PostService) {
  get("/posts/{id}") {
    val id = call.parameters["id"]?.toLongOrNull()
      ?: return@get call.respond(HttpStatusCode.BadRequest)
    val post = posts.find(id)
    if (post == null) call.respond(HttpStatusCode.NotFound)
    else call.respond(post)
  }
}
```

Visible dependency. Direct test substitution. Ordinary Kotlin.

<!--
Same handler, one signature change, no delegate. This is still dependency injection: the caller supplies the dependency. It doesn't need a framework.
The compiler verifies that a value is supplied; it does not prove the entire application's lifecycle or configuration is correct.
-->

---
class: compact
---

# Koin can stay at the application boundary

```kotlin
import org.koin.ktor.plugin.Koin
import org.koin.ktor.ext.get

fun Application.module() {
  install(Koin) {
    modules(productionModule)
  }

  val posts = get<PostService>()
  app(Dependencies(posts))
}
```

Koin builds the graph. Routes still take parameters.

<!--
productionModule defines PostService and its collaborators; Dependencies is the small aggregate shown earlier. This is an alternative bootstrap to manual construction, not something to install alongside it.
Eager get here resolves before route registration. Keep request-scoped values in the request scope; don't capture them in this application-wide graph.
Source: https://insert-koin.io/docs/quickstart/ktor/
-->

---

# Choose how to build the graph

| | Manual construction | Koin |
| --- | --- | --- |
| Wiring | Kotlin calls | Definitions and resolution |
| Useful when | The graph is small and clear | Modules, scopes and overrides help |
| Validation | Constructor / function types | Validate container configuration too |
| Route API | `postRoutes(posts)` | `postRoutes(posts)` |

The route signature does not have to change with the DI framework.

<!--
This is a tradeoff, not a blanket objection to Koin. Koin has configuration verification and optional compiler tooling; do not claim all possible Koin setups are runtime-only. Our example uses the ordinary runtime DSL.
Manual DI also needs lifecycle design and tests. For a simple application-wide service, by inject inside a router adds container coupling with little benefit over the existing parameter.
-->

---
class: compact
---

# Test HTTP behavior with a small fake

```kotlin
interface PostService {
  suspend fun find(id: Long): Post?
}

@Test
fun missingPostReturns404() = testApplication {
  val posts = object : PostService {
    override suspend fun find(id: Long): Post? = null
  }
  application { app(Dependencies(posts)) }

  val response = client.get("/posts/42")
  assertEquals(HttpStatusCode.NotFound, response.status)
}
```

No database or DI container is needed for this route test.

<!--
The fake implements exactly the boundary this test needs. Define Post and Dependencies in the sample domain if turning these slides into a runnable project.
Complement this with tests of invalid IDs, successful JSON responses, service behavior and real database queries. A fast fake-based route test does not replace integration tests.
-->

---
class: compact
---

# Test the real database with Testcontainers

```kotlin
val postgres = PostgreSQLContainer("postgres:17-alpine")
  .apply { start() }

@Test
fun storedPostReturns200() = testApplication {
  val database = Database.connect(
    url = postgres.jdbcUrl,
    user = postgres.username,
    password = postgres.password,
  )
  val posts = ExposedPostService(database)
  val id = posts.create(title = "Hello Avans")
  application { app(Dependencies(posts)) }

  val response = client.get("/posts/$id")
  assertEquals(HttpStatusCode.OK, response.status)
}
```

Same `app(...)`, same route. Only the dependency is real.

<!--
Testcontainers starts a throwaway PostgreSQL in Docker; jdbcUrl, username and password come from the running container, so no local database or shared test DB is needed. Docker must be available locally and on the CI runner (GitHub Actions ubuntu runners have it).
Dependency: org.testcontainers:testcontainers-postgresql (Testcontainers 2.x; in 1.x the artifact is org.testcontainers:postgresql). ExposedPostService is the production implementation; run the schema/migrations (e.g. Flyway or SchemaUtils.create) before the test inserts data.
Starting the container once per test class keeps it fast; Ryuk removes it when the JVM exits. This is the integration counterpart of the fake-based test: slower, but it proves the SQL, mapping and HTTP layer together.
Source: https://java.testcontainers.org/modules/databases/postgres/
-->

---

# Construction also means ownership

```text
Application starts
  → acquire connection pool / HTTP client
  → construct services
  → accept requests

Application stops
  → drain work
  → close resources in the right order
```

One owner per resource. Cleanup on shutdown and failed startup.

<!--
ktor-quickstart installs its custom CleanUp plugin and registers the client, Hikari pool, Exposed database and registry. That .cleanup extension is project code, not a standard Ktor method.
ktor-arrow-example uses Arrow ResourceScope for dependency acquisition. The principle is shared ownership/lifetime, not one mandatory library. ApplicationStopped callbacks alone do not demonstrate correct cleanup if construction fails halfway through.
Keep this brief; bridge to the opinionated talk for detailed lifecycle discussion.
-->
