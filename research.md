# Research notes

Checked 29 September 2026. The target is **Kotlin Toolchain 0.12.2**, not a Kotlin compiler release. This is a researched teaching draft, not a statement on behalf of the toolchain team. The students' project was not supplied, so its coverage pipeline has not been run.

## Answers and evidence

### Coverage: investigate the existing Maven bridge first

The release-tagged [Maven plugin guide](https://github.com/JetBrains/kotlin-toolchain/blob/v0.12.2/docs/src/user-guide/advanced/maven-plugins.md) supports JVM-only modules, declares plugin coordinates in `project.yaml`, enables goals in `module.yaml`, and documents explicit report tasks. It describes best-effort lifecycle integration, unsupported complex POJO parameters, single executions and no report aggregation.

The [only-jacoco fixture](https://github.com/JetBrains/kotlin-toolchain/tree/v0.12.2/sources/test-integration/test-projects/extensibility-maven/only-jacoco) declares JaCoCo 0.8.15 and enables `prepare-agent` plus `report`. A [second fixture](https://github.com/JetBrains/kotlin-toolchain/tree/v0.12.2/sources/test-integration/test-projects/extensibility-maven/surefire-plugin-with-jacoco) also enables Surefire. These are stronger evidence than guessing that Gradle plugins are the only option. They do not prove every project configuration or coverage gate works.

Recommendation: spike this integration on the actual backend before hand-maintaining the compiled-output path. Keep coverage verification separate; do not promise that nested `jacoco:check` rules work through a bridge that explicitly limits complex parameters.

### Manual JaCoCo fallback

[TestCommand.kt at v0.12.2](https://github.com/JetBrains/kotlin-toolchain/blob/v0.12.2/sources/amper-cli/src/org/jetbrains/amper/cli/commands/TestCommand.kt) forwards `--jvm-args` to test JVMs. [JaCoCo agent documentation](https://www.jacoco.org/jacoco/trunk/doc/agent.html) describes execution-file output and append behavior; [CLI documentation](https://www.jacoco.org/jacoco/trunk/doc/cli.html) documents report inputs/outputs and merge, with no threshold-check command.

Use pinned compatible tools and matching classes from the instrumented run. One execution file per writer prevents parallel writers or repeated processes overwriting each other. The slide's `append=false` example assumes one module and one JVM. Its variables assume a workspace path without spaces. A real script must validate jar paths, class roots, execution data, report counters and the final exit status. Do not rebuild before report generation. Define production scope and generated-code exclusions explicitly.

The proposed `build/artifacts/CompiledJvmArtifact/<module>jvm/kotlin-output/` path is from the students' question. No stable public contract for that exact path was found. The deck treats it as a version-dependent adapter, not as an official supported API or a proven path in an inspected backend.

### Linting: CLI today, plugin checks if useful

[ktlint CLI documentation source](https://github.com/pinterest/ktlint/blob/master/documentation/snapshot/docs/install/cli.md) describes CLI checking/formatting and file patterns. [detekt CLI documentation](https://detekt.dev/docs/gettingstarted/cli/) describes source/config/report options and classpath-based analysis. Pin tool releases compatible with the project's Kotlin syntax; the two tools are not equivalent to one another.

The release-tagged [checks guide](https://github.com/JetBrains/kotlin-toolchain/blob/v0.12.2/docs/src/user-guide/plugins/topics/checks.md) documents `check`, `show checks`, named checks and plugin registration. The toolchain's [detekt plugin](https://github.com/JetBrains/kotlin-toolchain/blob/v0.12.2/build-sources/detekt/plugin.yaml) passes module sources and compilation classpath into a task and registers a `detekt` check. It is an example used by that repository, not a globally installed check in every Kotlin project.

### CI caching

The [0.12 provisioning guide](https://kotlin-toolchain.org/0.12/cli/provisioning/) documents `KOTLIN_CLI_BOOTSTRAP_CACHE_DIR` for wrapper/toolchain provisioning and `KOTLIN_SHARED_CACHE_DIR` for shared dependencies, JDKs and tools. `--shared-cache-dir` overrides the latter. `KOTLIN_CLI_JAVA_OPTIONS` configures the CLI JVM itself, not the test JVM.

The proposed keys (OS, architecture, wrapper hash; plus module/template/catalog hashes for shared caches) are our engineering recommendation. They are not presented as an official recipe. Cache downloads first; keep test execution and fresh reports independent of cache hits. No need to cache the whole `build/` tree to get initial benefits.

### Tags and roadmap

The [0.12.2 test command](https://github.com/JetBrains/kotlin-toolchain/blob/v0.12.2/sources/amper-cli/src/org/jetbrains/amper/cli/commands/TestCommand.kt) has `--include-test`, `--include-classes`, `--exclude-classes` and module filters. Class filters use `*`/`?` wildcards. In multi-module projects, class/test filtering requires explicit module selection. There is no include/exclude-tag option in that command.

The [release notes](https://github.com/JetBrains/kotlin-toolchain/releases/tag/v0.12.2) describe the 0.12.2 deployment fix. The [FAQ](https://kotlin-toolchain.org/0.12/faq/) describes alpha status and broad development priorities. No dated public commitment for built-in coverage, linting or tag filtering was confirmed in this research. That is a limit of the evidence, not a claim that these features are absent from internal plans. Do not advise waiting for a specific release without a maintainer commitment.

## Local projects inspected

These are local checkout observations, including uncommitted state; latest commit dates are context, not a claim that every file changed on that date.

| Checkout | Evidence | Consequence for the deck |
| --- | --- | --- |
| `~/Developer/ktor-arrow-example` (latest commit 2026-08-25) | `build.gradle.kts` applies Kover; Spotless/ktfmt configuration is commented out. `.github/workflows/main.yml` runs Gradle build, uploads reports and conditionally uploads an existing detekt SARIF report. | Do not claim formatting, detekt or a coverage minimum is fully enforced merely because plugins/config/report paths exist. |
| Same checkout | `src/main/kotlin/io/github/nomisrev/env/Dependencies.kt` constructs services under `ResourceScope`; `tags/TagRoutes.kt` accepts `TagService`. | Explicit services and resource ownership are established patterns in recent work. |
| `~/Developer/realworld` (latest commit 2026-06-29) | `backend/src/main/kotlin/org/jetbrains/realworld/Application.kt` and `config/Dependencies.kt` build the graph and pass feature-specific dependencies to routes. | Keep feature wiring explicit; this checkout does not have the exact bootstrap/app separation. |
| `~/Developer/ktor-quickstart` | `Server.kt` separates main/bootstrap from `Application.app`. `Dependencies.kt` constructs resources and uses a custom `CleanUp` plugin. | Closest code reference for the composition split; `.cleanup()` is application code, not built-in Ktor API. |
| `~/Developer/gamelauncher` | Recent workspace with Kover conventions in `build-logic` and aggregate root coverage configuration. | Useful broader CI context, but not used as a Ktor-backend example. |

Other recent folders were checked for Ktor build declarations to avoid relying only on names or directory timestamps.

## Existing presentation references

- `../opinionated-ktor-services/lessons/lesson-3.md`: feature packaging, extract/process/respond, route parameters, bootstrap/application split.
- `../opinionated-ktor-services/lessons/lesson-4.md`: feature dependency modules and lifecycle.
- Shared theme, typography and cover/intro layouts from `../slidedev-theme-kotlin` and the Ktor Fundamentals frontmatter.
- `../advanced-dsl-in-kotlin/lessons/lesson-5.md`: named context parameters and providing a context with a receiver.

## Koin and context parameters

[Koin injection documentation](https://insert-koin.io/docs/reference/koin-core/injection/) supports lazy route injection; the [Ktor quickstart](https://insert-koin.io/docs/quickstart/ktor/) supports application-level setup and injection. A `by inject` local outside the handler is a captured lazy dependency, not automatically a new lookup per request. The argument is about dependency visibility and test setup. Koin can remain at the composition boundary; the deck does not claim manual DI is universally superior or all Koin validation is runtime-only.

[Kotlin context parameters](https://kotlinlang.org/docs/context-parameters.html) documents named declarations, type-based resolution, missing/ambiguous contexts, and the distinct experimental explicit-context-arguments feature. The slide uses `with(service)` and does not require that latter feature. Check the selected Kotlin compiler when discussing feature flags; the toolchain version is not the language version.

## Expanded tooling section (same research date)

The section now recommends **Kover as the Kotlin/JVM coverage toolkit** while retaining the JaCoCo Maven bridge as a practical existing integration for the students' toolchain setup. This supersedes the earlier presentation order that led with the Maven bridge. The examples remain illustrative and have not been run against the students' unavailable backend.

### Formatting and analysis

- [ktfmt repository](https://github.com/Kotlin/ktfmt), plus local `~/Developer/ktfmt/core/src/main/java/com/facebook/ktfmt/cli/ParsedArgs.kt`: formatting modes, directory inputs, `--dry-run` and `--set-exit-if-changed`.
- [Spotless Gradle integration](https://github.com/diffplug/spotless/blob/main/plugin-gradle/README.md): ktfmt integration and apply/check tasks. The slide's 8.8.0/0.64 example pins come from the inspected Ktor project's version catalog and commented configuration; they are not a claim that it currently runs formatting.
- [ktlint Gradle plugin](https://github.com/JLLeitschuh/ktlint-gradle): plugin 14.2.0, `ktlintCheck`, `ktlintFormat`, and separate engine configuration.
- [Versioned detekt Gradle reference](https://detekt.dev/docs/1.23.8/gettingstarted/gradle/), [CLI](https://detekt.dev/docs/1.23.8/gettingstarted/cli/) and [type resolution](https://detekt.dev/docs/gettingstarted/type-resolution/). Examples explicitly use 1.23.x syntax, not a claim that it is the newest release or accepts every newer compiler feature. Select compatible versions in a real project. The Gradle namespace changes in detekt 2.x.
- The `kotlin format` / ktfmt direction and “soon-ish” expectation were supplied by Simon in the follow-up request. No public release number or date was established. The slide is forward-looking and does not show this as a working 0.12.2 command. This is an explicit exception to the earlier blanket lack of roadmap information; no analogous dates are asserted for tags or coverage.
- The heavy-analysis tradeoffs and adoption policy are presenter recommendations, not quantitative benchmark claims. Baselines must not be regenerated automatically to suppress new findings.

### Standalone Kover

[Kover JVM agent documentation](https://kotlin.github.io/kotlinx-kover/jvm-agent/) specifies a settings file and `-javaagent:...=file:...`; it explicitly says to preserve the agent jar's name. [Kover CLI](https://kotlin.github.io/kotlinx-kover/cli/) documents reports from `.ic` data with original class roots and sources, plus merging. The shown CLI report does not enforce the Gradle verification DSL. [Kover Gradle](https://kotlin.github.io/kotlinx-kover/gradle-plugin/) documents `minBound` and report/verify tasks; default bound units are covered line percentage. The examples use 0.9.8, matching the existing Ktor project and guide. The agent example assumes one test JVM/writer and paths without spaces. Multi-shard runs need separate files and explicit aggregation.

### Local plugin source walkthrough

Read-only inspection of these local source trees:

| Directory | Files / findings |
| --- | --- |
| `~/Developer/ktor-build-plugins/toolchain-plugin` | `project.yaml` registers `./ktor`; `sample/module.yaml` enables it. `ktor/module.yaml` uses `jvm/amper-plugin` and Jib Core 0.28.1. `ktor/plugin.yaml` binds runtime classpath, module jar, settings and task output paths. `ktor/src/Jib.kt` contains annotated actions. |
| Same port | `README.md` lists `:sample:buildImage@ktor` (tarball), `:sample:publishImageToLocalRegistry@ktor` (actually the local Docker daemon) and `:sample:publishImage@ktor` (registry push). The source infers main class from the module jar, validates JRE/Java release, applies environment settings and handles registry credentials. No credentials are included in the deck. |
| `~/Developer/amper-jib-plugin-example` | Minimal local registration and Jib actions `jib`, `jibDockerBuild`, `jibBuildTar`. A useful smaller reference. |
| `~/Developer/kotlin-15-challenge/magic/jib-plugin` | Additional Jib plugin located as a demo reference. Not the source for the detailed slide snippets. |
| `~/Developer/ktor-amper/ktor-sample` | Local Jib plus ktfmt plugins. `ktfmt-plugin/plugin.yaml` defines `ktfmtFormat` and `ktfmtCheck`, but no `checks` block. Historical inspection only; the formatter plugin is now omitted from this deck’s local demo. |

The ktfmt prototype is not production-ready enforcement as inspected: it logs/swallow per-file exceptions, can succeed with no matched files, and passes/logs a style setting that does not select formatting options. Notes flag these limits. No changes were made to these external projects. The Ktor port is the container-image portion of the Gradle plugin, not a claim of complete feature parity. No image builds, Docker operations or registry pushes were performed.

## Restructured main session: build tools → CI → architecture

Revisited `kotlin-fundamentals/lessons/lesson-1.md` (progressively expanding module.yaml and Gradle configuration), `opinionated-ktor-services/lessons/lesson-1.md` (project layout and Ktor catalog), `ktor-fundamentals/lessons/lesson-1.md` (small server/test teaching style), and `ktor-full-stack-talk/lessons/lesson-2.md` (application/main class and build-plugin distinction). Also reread `~/Developer/academy-gradle-intro-main/README.MD`: its progression from wrappers/files through dependencies, catalogs and plugins is useful; its historical versions/IDE workarounds are not reused.

The main entry now imports amper-intro, gradle-intro, github-actions, the unchanged architecture chapter, and the existing optional next chapter. Detailed quality-tooling material remains in slides.tooling.md instead of interrupting the requested four-part route. Both intros have explicit short/long presenter cuts in README.md and notes.

GitHub Actions examples were based on current primary docs: [setup-gradle](https://github.com/gradle/actions/blob/main/setup-gradle/README.md), [setup-java](https://github.com/actions/setup-java), [cache](https://github.com/actions/cache), [upload-artifact](https://github.com/actions/upload-artifact), and [workflow quickstart](https://docs.github.com/en/actions/get-started/quickstart). The selected major versions are checkout v7, setup-java v6, setup-gradle v6, cache v6 and upload-artifact v7. The Gradle lifecycle is supported by the [Java plugin guide](https://docs.gradle.org/current/userguide/java_plugin.html). These are GitHub-hosted-runner examples, not promises about old self-hosted runner compatibility.

The Amper workflow uses documented wrapper/shared cache customization and normal toolchain provisioning. It saves explicit command logs with Bash pipefail rather than assuming Gradle-shaped test reports exist. Both workflows are baseline build/test setups, with quality gates added only after their integrations are configured. No GitHub workflow was published/run, and no repository rules were changed.

## Brief deployment addition

Condensed `opinionated-ktor-services/lessons/lesson-7.md` into three Gradle slides: Ktor packaging tasks, Jib registry publishing, and a Render live-demo route. Kept the current deck's Java 21/Ktor 3.5.2 baseline instead of importing the other talk's Java 25 image. Cross-checked `Docker.kt` in the local `ktor-build-plugins` project for the registry provider API, image tags and JRE selection, plus [Ktor's Docker documentation](https://ktor.io/docs/docker.html) and [Jib configuration](https://github.com/GoogleContainerTools/jib/tree/master/jib-gradle-plugin#extended-usage).

Render is the suggested host because its [prebuilt-image flow](https://render.com/docs/deploying-an-image) follows the plugin's output directly. Checked [port binding](https://render.com/docs/web-services#port-binding) and [free compute limits](https://render.com/docs/free). It requires linux/amd64, supports image references from registries, supplies PORT, and requires a redeploy trigger for changed image tags. Its free idle sleep is explicitly called out. Fly.io was considered, but its [trial](https://docs.fly.io/about/free-trial/) is limited to two VM hours or seven days, and trial machines stop after five minutes. No account, image push or deployment was performed.

## Authentication and runnable local example

Read `~/Developer/realworld/backend/src/main/kotlin/org/jetbrains/realworld/{user/Argon2Hasher.kt,user/UserService.kt,config/JwtAuth.kt}` and its dependency catalog. Confirmed its remote and public project at [nomisRev/ktor-full-stack-real-world](https://github.com/nomisRev/ktor-full-stack-real-world). The original uses Bouncy Castle Argon2id (64 MiB, three iterations, four lanes), random salts and Auth0 HS256 JWT issuance; authentication there is the classic Ktor API.

The new `auth-demo` uses the same core approach with bounded hash/verify dispatch, stored cost/version parameters, constant-time digest comparison, validated random-key configuration, required JWT claims and an existing-user check. It deliberately avoids copying the original `encrypt` method name or presenting hashing as encryption. Users are held in a replaceable in-memory repository and the demo covers access tokens, not a complete account lifecycle.

[Ktor typed authentication](https://ktor.io/docs/server-typed-auth.html) and the [3.6.0 release announcement](https://blog.jetbrains.com/ktor/2026/09/18/ktor-3-6-0-is-now-available/) establish the experimental `jwt<User>` / `authenticateWith` / `call.principal` API and context-parameter requirement. Cross-checked the local Ktor `TypedJwtAuthTest.kt` and `TypedJwtAuthConfig.kt`. The sample pins Ktor 3.6.0, Kotlin 2.4.10, JDK 21 and the existing checksum-pinned Kotlin Toolchain 0.12.2 wrappers copied from Kotlin Fundamentals exercises.

Security references: [OWASP password storage](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html), [Argon2 RFC 9106](https://www.rfc-editor.org/rfc/rfc9106), [JWT BCP RFC 8725](https://www.rfc-editor.org/rfc/rfc8725). The slides distinguish password hashing, JWT signing and transport encryption, and explicitly scope authorization, persistence, refresh/revocation, recovery and browser storage as further work.

## Local demo integration (2026-09-29)

Copied Ktor and Jib modules from `~/Developer/ktor-amper/ktor-sample` unchanged, using root `project.yaml`, wrapper 0.12.2 and Jib Core 0.28.2. The formatter plugin was removed rather than requiring a full JDK solely for its in-process compiler APIs. General formatting CLI/Gradle slides remain optional.

Auth demo targets Java 21 / linux-amd64. Docker Desktop credential metadata matches `vergauwensimon`; no secret is committed or shown. The image built locally and passed health/register/login/typed-principal smoke tests under a 512 MiB container limit, with its JWT key supplied only at runtime. All eight tests pass with the default Toolchain runtime after formatter removal. Docker Hub publication and Render deployment have not been performed. `auth-demo/render.yaml` follows Render’s image-service Blueprint configuration.
