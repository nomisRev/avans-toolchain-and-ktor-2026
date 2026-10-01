---
layout: intro
class: section-slide
kodee: wave
---

# Optional: context parameters

## A declared dependency, supplied from the surrounding scope

<!--
Optional two-slide bridge. If basic extension functions or receiver lambdas are unfamiliar, switch to Kotlin Fundamentals first.
-->

---
class: compact
---

# Keep the dependency in the declaration

```kotlin
context(posts: PostService)
fun Route.postRoutes() {
  get("/posts/{id}") {
    val id = call.parameters["id"]?.toLongOrNull()
      ?: return@get call.respond(HttpStatusCode.BadRequest)
    val post = posts.find(id)
    if (post == null) call.respond(HttpStatusCode.NotFound)
    else call.respond(post)
  }
}

// At the application boundary
routing {
  with(dependencies.posts) { postRoutes() }
}
```

<!--
Alternative to the parameter version, not an additional overload in the same sample. Kotlin resolves the context from the surrounding scope at compile time. with supplies the PostService receiver while Route remains available from routing.
Kotlin Toolchain 0.12.2 and the Kotlin language/compiler version are separate. Check the compiler selected by module settings; older compilers may require -Xcontext-parameters. This example does not need the separate experimental explicit-context-arguments feature.
Source: https://kotlinlang.org/docs/context-parameters.html
-->

---

# Context does not construct the service

| Explicit parameter | Context parameter |
| --- | --- |
| Value passed at each call | Value supplied by the surrounding scope |
| Easy to read in isolation | Useful across related DSL operations |
| A good default for one dependency | Adds scope and resolution rules to learn |

Neither approach is a runtime service lookup.

<div class="source"><a href="https://kotlinlang.org/docs/context-parameters.html">Kotlin context parameters</a></div>

<!--
No magic container: something still creates the PostService and owns its resources. Missing or ambiguous matching context values are compiler errors.
For a single service on a route function, the ordinary parameter is usually enough. Context parameters become more interesting when many related operations share a capability. This is the bridge to the spreadsheet DSL's context(formulas: Formulas).
-->

---
layout: default
kodee: greeting
---

# Where shall we go next?

**[Ktor Fundamentals](https://nomisrev.github.io/ktor-fundamentals/)**  
Routes, requests, testing and authentication

**[Kotlin Fundamentals](https://nomisrev.github.io/kotlin-fundamentals/)**  
Functions, types, extensions and receiver lambdas

**[Advanced DSLs in Kotlin](https://nomisrev.github.io/advanced-dsl-in-kotlin/)**  
Receivers, scopes and context parameters

<!--
Decision point, not three more mandatory chapters. Ask what would help their proftaak most.
Local launch commands and exact lesson files are in README.md. For Ktor testing use lesson 8; for Kotlin DSL mechanics use lesson 5; for advanced context parameters use lesson 5, or lesson 0 for a receiver refresher.
-->
