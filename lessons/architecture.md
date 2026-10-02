---
layout: intro
class: section-slide
kodee: wave
---

# Architecture that helps testing

## The same split as Opinionated Ktor Services

---
layout: intro
class: section-slide
kodee: sitting
---

# Two ways to start Ktor

## Who owns `main`?

---

# `EngineMain` reads a file instead

```kotlin
fun main(args: Array<String>): Unit =
  io.ktor.server.netty.EngineMain.main(args)
```

```yaml
ktor:
  deployment:
    port: "$PORT:8080"
  application:
    modules:
      - nl.avans.ApplicationKt.module
```

The file names the module; Ktor finds it by reflection.

<!--
This is what start.ktor.io generates. `-port=8081` and `-config=prod.yaml` on the command line override the file. The engine is in the class name: netty, cio, jetty.jakarta, tomcat.jakarta.
A top-level `module` in Application.kt compiles to the class ApplicationKt, hence the string. Rename the file or the function and nothing fails until the server starts.
Adapted from ktor-fundamentals lesson 2.
-->

---
class: compact
---

# The module reads its own keys

```kotlin
fun Application.module() {
  val url = environment.config.property("database.url").getString()
  val service = ExposedPostService(connect(url))

  install(ContentNegotiation) { json() }
  routing { postRoutes(service) }
}
```

Configuration, construction and wiring in one function.

<!--
`property("github")` (io.ktor.server.config.property) deserialises a whole section into a @Serializable class. Ktor 3.2+ can also inject module parameters with `@Property("github")` through ktor-server-di; mention, don't demo.
The point for this course: a test that wants a fake PostService has no seam here. The module constructs the database itself.
-->

---

# `embeddedServer`: you own `main` and configuration

```yaml
port: "$PORT:8080"
host: "0.0.0.0"
```

---

# `embeddedServer`: you own `main`

```kotlin
fun main() {
  val config = ApplicationConfig("application.yaml").getAs<Config>()
  embeddedServer(Netty, host = config.host, port = config.port) {
    val dependencies = dependencies(config)
    app(dependencies)
  }.start(wait = true)
}
```

Plain function calls: load, construct, wire, start.

<!--
The order is visible and the compiler checks every call. A typo in the module name can't exist because there is no module name.
The lambda is `suspend Application.() -> Unit`: everything in it runs before the server accepts traffic, and it counts as startup time.
-->

---

# Choose who starts the server

| | `EngineMain` | `embeddedServer` |
| --- | --- | --- |
| `main` | Ktor's | Yours |
| Modules | Strings in YAML, reflection | Function calls |
| Wrong name | Fails at startup | Fails to compile |
| In tests | Auto-loads YAML modules | `application { app(fakes) }` |

Both are fine. I prefer `embeddedServer` for explicit control.

<!--
EngineMain shines for development mode / auto-reload and for WAR deployments where a container owns main. embeddedServer shines when you want control over ordering and the dependency graph.
The last row is the next slide.
-->

---
layout: intro
class: section-slide
kodee: sitting
---

# Configuration

## Load first, fail fast, then start

---

# Configuration lives outside the code

```yaml
host: "$HOST:0.0.0.0"
port: "$PORT:8080"
database:
  url: "$DATABASE_URL"
  username: "$DATABASE_USER:postgres"
  password: "$DATABASE_PASSWORD"
```

`$VAR:default` has a fallback. `$VAR` alone is mandatory.

<!--
No fallback for secrets or anything environment-specific: a container started without DATABASE_URL refuses to start instead of connecting to the wrong place. The same build runs on every machine; only the environment differs.
-->

---
class: compact
---

# Configuration is a `@Serializable` data class

```kotlin
@Serializable
data class Config(
  val host: String,
  val port: Int,
  val database: DatabaseConfig,
)

@Serializable
data class DatabaseConfig(
  val url: String,
  val username: String,
  val password: String,
)
```

One nested class per feature, the same keys as the YAML.

<!--
Primitives, collections and enums. Keep complex types out of the configuration. The feature classes can live in the feature package.
-->

---
magicMove: true
class: compact
---

# Load the config first, then start the server

```kotlin
fun main() {
  val config = ApplicationConfig("application.yaml").getAs<Config>()
  embeddedServer(Netty, host = config.host, port = config.port) {
    val dependencies = dependencies(config)
    app(dependencies)
  }.start(wait = true)
}
```

No valid configuration, no server.

---
magicMove: true
class: compact
---

# Load the config first, then start the server

```kotlin
fun main() {
  val config = ApplicationConfig("application.yaml")
    .mergeWith(ApplicationConfig("application-local.yaml"))
    .getAs<Config>()
    
  embeddedServer(Netty, host = config.host, port = config.port) {
    val dependencies = dependencies(config)
    app(dependencies)
  }.start(wait = true)
}
```

`mergeWith` layers files: shared defaults, then local overrides.

<!--
The same mechanism gives tests an application-test.yaml when they need the real config shape. Most route tests don't need a Config at all: they pass dependencies directly.
-->

---
magicMove: true
class: compact
---


# Load the config first, then start the server

```kotlin
fun main() {
  val isDev = System.getProperties("io.ktor.development")
  val env = if(isDev) "dev" else "prod"
    
  val config = ApplicationConfig("application.yaml")
    .mergeWith(ApplicationConfig("application-$env.yaml"))
    .getAs<Config>()
    
  embeddedServer(Netty, host = config.host, port = config.port) {
    val dependencies = dependencies(config)
    app(dependencies)
  }.start(wait = true)
}
```

`mergeWith` layers files: shared defaults, then local overrides.

<!--
The same mechanism gives tests an application-test.yaml when they need the real config shape. Most route tests don't need a Config at all: they pass dependencies directly.
-->

---
layout: intro
class: section-slide
kodee: sitting
---

# Structure

## Bootstrap, application, feature

---
class: compact
---

# Bootstrap constructs; application wires

```kotlin
fun Application.dependencies(config: Config): Dependencies {
  val database = database(config.database)
  val posts = ExposedPostService(database)
  return Dependencies(posts = posts)
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
`main` from the previous section calls both. dependencies owns construction and cleanup; app only configures HTTP behaviour. `database(...)` is shown in the lifecycle section.
-->

---

# Keep feature code together

<ul class="tree" aria-label="src with Server.kt, Config.kt, Application.kt and Dependencies.kt, a post package holding PostRoutes.kt, PostService.kt and PostRepository.kt, and a profile package with the same shape">
  <li class="dir"><span>src</span>
    <ul>
      <li class="kt"><span>Server.kt</span><em>bootstrap</em></li>
      <li class="kt"><span>Config.kt</span><em>typed configuration</em></li>
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
This mirrors the three phases in lesson 3 of Opinionated Ktor Services. The dependency is a parameter: that is dependency injection without a framework, and it's what makes the tests later trivial.
Post is serializable and the application installs JSON content negotiation. The source-only examples are teaching snippets, not a compiled sample backend.
-->

---
magicMove: true
class: compact
---

# Ktor converts the type for you

```kotlin
fun Route.postRoutes(posts: PostService) {
  get("/posts/{id}") {
    val id: Long by call.pathParameters

    val post = posts.find(id)

    if (post == null) call.respond(HttpStatusCode.NotFound)
    else call.respond(post)
  }
}
```

Missing or not a `Long`: `400 Bad Request`, without our code.

<!--
Optional slide; drop it first when time is short. The property name is the key. Nullable types make the parameter optional, List<Int> collects repeated values, enums work too.
Testing angle: less hand-written parsing means fewer branches to test. Still write one test for the 400 to pin the behaviour.
-->

---
magicMove: true
class: compact
---

# Routes group by path, like packages by feature

```kotlin
fun Route.postRoutes(posts: PostService) = route("/posts") {
  get("/{id}") {
    val id: Long by call.pathParameters

    val post = posts.find(id)

    if (post == null) call.respond(HttpStatusCode.NotFound)
    else call.respond(post)
  }

  post { }
  put("/{id}") { }
  delete("/{id}") { }
}
```

One function per feature, one service per function.

<!--
The prefix is written once. The function signature is the natural place to declare what this feature depends on.
-->

---
layout: intro
class: section-slide
kodee: sitting
---

# How plugins work

## The pipeline around every handler

---

# Plugins refine the request and the response

<KtorPipeline />

The handler does the route-specific part. Plugins do the rest.

<!--
Adapted from ktor-fundamentals lesson 1. Each installed plugin hooks into the call pipeline before the handler (parse, authenticate, validate) and after it (serialise, compress, add headers). Routing itself is a plugin.
Order of installation matters for plugins that hook the same phase.
-->

---
class: compact
---

# `ContentNegotiation` wraps the handler

<KtorPipeline plugin="ContentNegotiation" />

```kotlin
post("/posts") {
  val draft = call.receive<PostDraft>()
  call.respond(HttpStatusCode.Created, posts.create(draft))
}
```

Before: JSON in, `PostDraft` out. After: `Post` in, JSON out.

<!--
`app` installs it once with `install(ContentNegotiation) { json() }`; every route below benefits.
Without the install, receive<PostDraft>() fails with 415 Unsupported Media Type and respond(post) fails at runtime. That's exactly the bug a feature-level test hits when it forgets to install the plugin.
-->

---
class: compact
---

# A plugin can be scoped to a route

```kotlin
fun Route.postRoutes(posts: PostService) = route("/posts") {
  install(RequestValidation) {
    validate<PostDraft> { draft ->
      if (draft.title.isBlank()) ValidationResult.Invalid("title is blank")
      else ValidationResult.Valid
    }
  }
  post {
    val draft = call.receive<PostDraft>()
    call.respond(HttpStatusCode.Created, posts.create(draft))
  }
}
```

`route { }` has its own `install`. `authenticate { }` works the same way.

<!--
Only this subtree validates. A failed validation throws RequestValidationException; map it to 400 with StatusPages. Adapted from ktor-fundamentals lesson 8.
Bridges to the authentication lesson: authenticate("jwt") { ... } is a route-scoped plugin.
-->

---
class: compact
---

# A plugin is a value with hooks

```kotlin
class ApiVersionConfig {
  var version: String = "1"
}

val ApiVersion = createApplicationPlugin("ApiVersion", ::ApiVersionConfig) {
  val version = pluginConfig.version
  onCall { call ->
    call.response.headers.append("X-Api-Version", version)
  }
}
```

`onCall`, `onCallReceive`, `onCallRespond`, `on(CallFailed)`, …

<!--
createApplicationPlugin lives in io.ktor.server.application; createRouteScopedPlugin for a plugin that can be installed in route { }.
Write your own when StatusPages / CallLogging / CallId don't cover it: correlation IDs, tenant resolution, response headers.
-->

---
class: compact
---

# A plugin is tested with a one-line route

```kotlin
@Test
fun addsVersionHeader() = testApplication {
  application {
    install(ApiVersion) { version = "2" }
    routing { get("/") { call.respondText("ok") } }
  }

  val response = client.get("/")
  assertEquals("2", response.headers["X-Api-Version"])
}
```

No features, no database: only the plugin and a dummy handler.

---
layout: intro
class: section-slide
kodee: sitting
---

# Testing the same architecture

## Feature, application, database

---
class: compact
---

# Test one feature at a time

```kotlin
@Test
fun missingPostReturns404() = testApplication {
  application {
    install(ContentNegotiation) { json() }
    routing { postRoutes(FakePostService()) }
  }

  val response = client.get("/posts/42")
  assertEquals(HttpStatusCode.NotFound, response.status)
}
```

Install the plugins the routes rely on, nothing more.

<!--
The narrowest HTTP test: one feature, one fake. Forget ContentNegotiation and the success-path test fails, which is a nice reminder of the previous section.
-->

---
class: compact
---

# Test the whole app with a small fake

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

Real plugins and wiring. No database.

<!--
The fake implements exactly the boundary this test needs. app(...) installs every plugin, so this catches wiring mistakes the feature test can't.
Complement this with tests of invalid IDs, successful JSON responses, service behaviour and real database queries. A fast fake-based route test does not replace integration tests.
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
layout: intro
class: section-slide
kodee: sitting
---

# Lifecycle

## Construction also means ownership

---
class: compact
---

# Dependencies share the `Application` lifecycle

```kotlin
fun Application.dataSource(config: DatabaseConfig): HikariDataSource {
  val hikari = HikariDataSource(HikariConfig().apply {
    jdbcUrl = config.url
    username = config.username
    password = config.password
  })

  monitor.subscribe(ApplicationStopped) { hikari.close() }
  return hikari
}
```

An extension on `Application`: created on start, closed on stop.

<!--
One pool per running instance. ApplicationStopped fires after in-flight requests are done and all application coroutines have completed, so nothing still uses the pool.
testApplication stops the application at the end of the test, so the same hook closes the pool in tests.
ApplicationStopped alone doesn't cover a failure halfway through construction; ktor-quickstart's CleanUp plugin and Arrow's ResourceScope handle that. Bridge to the opinionated talk for detail.
-->

---
class: compact
---

# Hide the infrastructure, expose services

```kotlin
fun Application.database(config: DatabaseConfig): Database =
  Database.connect(dataSource(config))

fun Application.dependencies(config: Config): Dependencies {
  val database = database(config.database)
  return Dependencies(posts = ExposedPostService(database))
}
```

Routes see `PostService`, never the pool.

---

# Application startup

<div class="lifecycle" aria-label="Ktor application startup: start(wait = true), ApplicationStarting, Application Setup, ApplicationModulesLoading, the suspend Application lambda, ApplicationStarted, NettyEngine.start(), ServerReady, Listening at host and port">
  <div class="lc-box">start(wait = true)</div>
  <div class="lc-step" v-click="1">
    <div class="lc-arrow"><span>ApplicationStarting</span></div>
    <div class="lc-box">Application Setup
      <ul class="lc-notes"><li>default pipelines &amp; interceptors</li><li>shutdown-hook listener</li></ul>
    </div>
  </div>
  <div class="lc-step" v-click="2">
    <div class="lc-arrow"><span>ApplicationModulesLoading</span></div>
    <div class="lc-box">suspend Application.() -&gt; Unit
      <span class="lc-timer" v-click="5">Start-up time</span>
    </div>
  </div>
  <div class="lc-step" v-click="3">
    <div class="lc-arrow"><span>ApplicationStarted</span></div>
    <div class="lc-box">NettyEngine.<wbr>start()</div>
  </div>
  <div class="lc-step" v-click="4">
    <div class="lc-arrow"><span>ServerReady</span></div>
    <div class="lc-box">Listening @ host:port</div>
  </div>
</div>

<!--
From opinionated-ktor-services lesson 4. Everything in the embeddedServer lambda (dependencies + app) runs in the ApplicationModulesLoading phase, before the engine accepts connections. Slow construction is slow startup.
-->

---

# Shutting down

<div class="lifecycle" aria-label="Ktor application shutdown: Listening at host and port, stop() or SIGINT or SIGTERM through the shutdown-hook listener, ApplicationStopPreparing, NettyEngine.stop(), ApplicationStopping, Application.cancelAndJoin(), ApplicationStopped, cleanup dependencies">
  <div class="lc-box">Listening @ host:port</div>
  <div class="lc-step" v-click="1">
    <div class="lc-arrow"><span>stop() / SIGINT / SIGTERM</span></div>
    <div class="lc-box">shutdown-hook listener</div>
  </div>
  <div class="lc-step" v-click="2">
    <div class="lc-arrow"><span>ApplicationStopPreparing</span></div>
    <div class="lc-box">NettyEngine.<wbr>stop()
      <ul class="lc-notes"><li>grace period</li><li>in-flight requests complete</li></ul>
    </div>
  </div>
  <div class="lc-step" v-click="3">
    <div class="lc-arrow"><span>ApplicationStopping</span></div>
    <div class="lc-box">Application.<wbr>cancelAndJoin()
      <ul class="lc-notes"><li>all coroutines completed</li></ul>
    </div>
  </div>
  <div class="lc-step" v-click="4">
    <div class="lc-arrow"><span>ApplicationStopped</span></div>
    <div class="lc-box">Cleanup dependencies</div>
  </div>
</div>

<!--
SIGTERM is what Docker / Kubernetes send. The grace period lets in-flight requests finish, then application coroutines are cancelled and joined, and only then ApplicationStopped runs the cleanup from the dataSource slide.
One owner per resource; close in reverse order of construction.
-->
