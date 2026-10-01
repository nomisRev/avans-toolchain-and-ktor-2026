---
layout: intro
class: section-slide
kodee: sitting
---

# 2 · Gradle

## Plugins configure a graph of tasks

Same Ktor backend · another build definition

<!--
5-minute core: project files, plugins, production dependencies, task commands, task graph and a quick packaging overview. Choose testing/catalog details or the three-slide deployment demo to extend toward 15 minutes.
Sources: Kotlin Fundamentals lesson 1; Opinionated Ktor Services lesson 1; the older academy-gradle-intro README's teaching progression. Modern syntax/versions replace that old course's historical examples.
-->

---
class: tree-dense
---

# Know which file you are editing

<ul class="tree" aria-label="backend with gradle/wrapper, src/main/kotlin, src/test/kotlin, build.gradle.kts, settings.gradle.kts and the gradlew and gradlew.bat wrapper scripts">
  <li class="dir"><span>backend</span>
    <ul>
      <li class="dir"><span>gradle/wrapper</span><em>distribution + wrapper files</em></li>
      <li class="src-root"><span>src/main/kotlin</span><em>production code</em></li>
      <li class="test-root"><span>src/test/kotlin</span><em>tests</em></li>
      <li class="gradle new"><span>build.gradle.kts</span><em>plugins, dependencies, tasks</em></li>
      <li class="gradle new"><span>settings.gradle.kts</span><em>project name and modules</em></li>
      <li class="script"><span>gradlew</span><em>pinned Gradle wrapper</em></li>
      <li class="script"><span>gradlew.bat</span></li>
    </ul>
  </li>
</ul>

Use `./gradlew`, so local development and CI agree on Gradle.

<!--
Contrast with the module.yaml/src/test layout just shown. The Kotlin DSL is Kotlin code configuring the build, not the application's source code. Start from a generated Ktor/Gradle project; no need to install global Gradle.
-->

---
class: compact
---

# Plugins give the project capabilities

```kts gradle
// build.gradle.kts
plugins {
  kotlin("jvm") version "2.4.10"
  application
}

kotlin { jvmToolchain(21) }

application {
  mainClass.set("college.ApplicationKt")
}
```

Kotlin compiles the code; `application` adds running and distributions.

<!--
Assume package college and a top-level main in Application.kt. Change the main class to the actual source package/file. Ktor's Gradle plugin is useful for fat jars/images but not required just to run a Ktor server. Don't confuse io.ktor Gradle plugin with server plugins installed inside Application.
-->

---
class: compact
---

# Dependencies have a configuration

```kts gradle
repositories {
  mavenCentral()
}

dependencies {
  implementation("io.ktor:ktor-server-netty:3.5.2")
  testImplementation("io.ktor:ktor-server-test-host:3.5.2")
}
```

`implementation` is application code; `testImplementation` is test-only.

<!--
This block extends the previous build.gradle.kts; it is not a replacement. Compare the exact same coordinates to Amper. Gradle configurations model different classpaths; don't dig into api/compileOnly until a real need appears.
Kotlin/JUnit dependencies for executing the earlier test are on the optional testing slide. A full real project needs them even if skipping their explanation in the 5-minute route.
-->

---

# Ask Gradle which tasks exist

```bash
./gradlew tasks
./gradlew run
./gradlew test
./gradlew build
```

A task name comes from a plugin or from your own build logic.

<!--
Same feedback loop as Amper, but explicit tasks supplied by plugins. tasks --all can show more detail. For a multi-module build, :backend:test selects a particular module; the shown commands assume a standalone backend at the repository root.
-->

---

# `build` includes packaging and verification

<div class="flow">
  <div><strong>build</strong><small>The lifecycle task</small></div>
  <span>→</span>
  <div><strong>assemble</strong><small>Create outputs</small></div>
  <span>+</span>
  <div><strong>check</strong><small>Run tests + attached checks</small></div>
</div>

Formatting, analysis and coverage need their own configuration.

<!--
For the short route, jump next to “The Ktor plugin adds packaging tasks”. This is the JVM/Java plugin lifecycle: build depends on assemble and check; check depends on test. Gradle follows dependencies, not the textual order of blocks in the build file. It may reuse up-to-date/cached task outputs, so build does not mean executing every task from scratch.
Do not claim applying a coverage plugin creates an appropriate minimum automatically. https://docs.gradle.org/current/userguide/java_plugin.html
-->

---
class: compact
---

# Make the test engine explicit

```kts gradle
// Add to build.gradle.kts
dependencies {
  testImplementation(kotlin("test-junit5"))
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.13.4")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.13.4")
}

tasks.test {
  useJUnitPlatform()
}
```

Place the same Ktor test in `src/test/kotlin/`.

<!--
Optional 2–3 minute demo: run the same passing/failing assertion as Amper. This deliberately pins one coherent JUnit 5 release family rather than relying on accidental transitive runtime dependencies. This is a working configuration pattern, not a newest-release recommendation.
Use the project's existing dependency catalog/BOM if present. Tests still live in a JUnit-discoverable class.
-->

---
class: compact
---

# A catalog shares dependency versions

```kts gradle
// settings.gradle.kts
rootProject.name = "backend"
dependencyResolutionManagement {
  versionCatalogs {
    create("ktorLibs") {
      from("io.ktor:ktor-version-catalog:3.5.2")
    }
  }
}
```

```kts gradle
// build.gradle.kts — replace the explicit Ktor coordinates
dependencies {
  implementation(ktorLibs.server.netty)
  testImplementation(ktorLibs.server.testHost)
}
```

<!--
Optional 2-minute detail from Opinionated Ktor Services. Keep repositories { mavenCentral() } in the module from the earlier slide; this snippet illustrates catalog import. Catalog accessors improve discovery and centralize versions; this does not require turning every declaration into a multi-module build.
The 5-minute route omits this slide.
-->

---
class: compact
---

# The Ktor plugin adds packaging tasks

```kts gradle
plugins {
  kotlin("jvm") version "2.4.10"
  id("io.ktor.plugin") version "3.5.2"
}
application { mainClass.set("college.ApplicationKt") }
```

| Command | Result |
| --- | --- |
| `./gradlew buildFatJar` | App + dependencies in one runnable JAR |
| `./gradlew buildImage` | Container image in `build/jib-image.tar` |
| `./gradlew publishImage` | Push image to a configured registry |

The Ktor plugin applies `application` and uses Jib for container images.

<!--
30-second packaging overview for the short route; start of a 3–5 minute deployment demo for the longer route. Adapted from Opinionated Ktor Services lesson 7. Replace the earlier plugins block, retaining dependencies and jvmToolchain(21); the explicit application plugin is now redundant. The catalog alternative is alias(ktorLibs.plugins.ktor).
A fat JAR still needs a compatible JVM on the host. A container includes the chosen runtime. Building or publishing an image does not start a hosted service; that is the next step. Jib builds/pushes without a Docker daemon or Dockerfile; running the image locally needs a container runtime. These packaging tasks are alternatives, not a mandatory sequence: Jib does not require buildFatJar first.
https://ktor.io/docs/docker.html
Local source: ~/Developer/ktor-build-plugins/plugin/src/main/kotlin/io/ktor/plugin/features/Docker.kt
-->

---
class: compact
---

# Publish the image with Gradle

```kts gradle
import io.ktor.plugin.features.DockerImageRegistry.dockerHub

ktor {
  docker {
    jreVersion.set(JavaVersion.VERSION_21)
    imageTag.set("demo-1")
    externalRegistry.set(dockerHub(
      appName = provider { "avans-college-auth" },
      username = providers.environmentVariable("DOCKERHUB_USERNAME"),
      password = providers.environmentVariable("DOCKERHUB_TOKEN"),
    ))
  }
}
```

`./gradlew publishImage` → `docker.io/<username>/avans-college-auth:demo-1`

The registry stores the image. The hosting platform runs it.

<!--
Optional demo, adapted from Opinionated Ktor Services lesson 7. Import belongs at the top of build.gradle.kts. Prepare a Docker Hub account, public avans-college-auth repository and scoped write token beforehand; provide environment variables in the terminal without projecting the token. This matches the simple Docker Hub namespace helper: username is also the image namespace.
Render needs linux/amd64. Jib defaults to that platform, independently of the presenter's Mac architecture; retain that target if the project has custom platforms. Deeper Jib configuration belongs in the handout. No runDocker or Docker daemon is required for publishImage.
https://ktor.io/docs/docker.html
https://github.com/GoogleContainerTools/jib/tree/master/jib-gradle-plugin#extended-usage
-->

---
class: compact
---

# Publish the same app with Kotlin Toolchain

```yaml toolchain
# auth-demo/module.yaml → plugins.ktor-plugin.docker
jreVersion: 21
platforms: [linux/amd64]
localImageName: avans-college-auth
imageTag: demo-1
externalRegistry:
  namespace: vergauwensimon
  project: avans-college-auth
  credHelper: desktop
```

```bash
./kotlin do publishImage
```

Jib reads Docker Desktop credentials through its credential helper.

<!--
This continues the local plugin shown in the opening section. These settings are already in auth-demo/module.yaml; show their nesting in the editor. The source port lives in plugins/ktor-plugin/src/Docker.kt, backed by plugins/jib-plugin. The namespace matches the logged-in Desktop account. No username/password is committed. This command really pushes, so rehearse buildImage first and publish deliberately during the deployment demo. JWT_SECRET_BASE64 belongs in Render's runtime environment, never in the image. The tarball was built and smoke-tested locally.
-->

---
class: compact
---

# Live demo: run our auth service on Render

```text
docker.io/vergauwensimon/avans-college-auth:demo-1
```

1. **New → Web Service → Existing Image**; paste the reference
2. Set `HOST=0.0.0.0`; Render supplies `PORT`
3. Add a random 32-byte Base64 `JWT_SECRET_BASE64`
4. Health check `/health`; deploy and open the HTTPS URL

The same settings live in `auth-demo/render.yaml`.

Users are in memory: restarting the service resets the demo.

<!--
Publish the image first. Choose Frankfurt and the desired compute plan. Leave Docker Command unset to keep Jib's generated entrypoint. The blueprint also sets JAVA_TOOL_OPTIONS=-Xmx256m -XX:MaxDirectMemorySize=64m; mirror this when creating manually. Generate the JWT key privately with openssl rand -base64 32 and enter it as a secret environment variable. Preserve it across deployments. Public Docker Hub images need no pull credentials; private images require a Render registry credential, separate from Desktop's local credential helper.
Use auth-demo/demo.http with the assigned HTTPS base URL for register/login/me. Free services sleep after 15 minutes idle; wake before presenting. Argon2 is intentionally costly, so rehearse on the chosen instance. Image services need a manual deployment or deploy hook for updates.
https://render.com/docs/deploying-an-image
https://render.com/docs/blueprint-spec
https://render.com/docs/free
-->

---
clicks: 4
---

# Build, publish, deploy

<DeploymentFlow :step="$clicks" />

<!--
Begin with three separate responsibilities: Gradle or Kotlin Toolchain builds, Docker Hub stores, Render runs.
[click] The Ktor/Jib task pushes the image to Docker Hub. No deployment yet.
[click] Initially, click Deploy in Render. Render requests the image from Docker Hub; the layers travel back to Render, which starts the service.
[click] Transform the manual trigger into automation. In the Docker Hub repository's Webhooks tab, add the secret Render Deploy Hook URL from the service settings. Docker Hub sends a POST after an image push. This direct connection follows the documented Docker Hub POST and Render deploy-hook contracts; rehearse the integration before presenting.
[click] Push the next build: notification first, then Render pulls and deploys. The webhook carries a notification, not the image. This demo keeps Render configured for the same mutable :demo-1 tag. A changed tag in Docker Hub's JSON payload does not select a new Render image tag. For immutable tags/digests, trigger the hook from CI using imgURL instead. Repository hooks can fire for other tags too; use a dedicated demo repository. Ktor/Jib may publish latest as well as the named tag, so more than one notification is possible.
The initial image deployment was manual. With this configured hook, subsequent publishes trigger deployment automatically; image-backed Render services do not watch registry changes by themselves.
https://docs.docker.com/docker-hub/repos/manage/webhooks/
https://render.com/docs/deploy-hooks
-->

---

# Plugins give quality checks task names

| Need | Integration | CI command |
| --- | --- | --- |
| Format with ktfmt | Spotless | `spotlessCheck` |
| Style rules | ktlint Gradle plugin | `ktlintCheck` |
| Static analysis | detekt | `detektMain` |
| Coverage gate | kotlinx-kover | `koverVerify` |

Configure the tools locally before asking CI to run them.

<!--
Optional 2–3 minute discussion. Choose one formatting policy, select useful analysis rules, and define the coverage threshold. More rules are not automatically better feedback. My Kotlin/JVM default is Kover; both it and JaCoCo have non-Gradle routes.
Detailed CLI/Gradle configuration, static-analysis pros/cons and JaCoCo's Maven bridge remain in slides.tooling.md. The next chapter starts with a build/test workflow and then shows where these commands are added.
-->
