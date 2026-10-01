---
layout: intro
class: section-slide
kodee: wave
---

# 1 · Toolchain

## Describe what you want to build

Kotlin Toolchain 0.12.2 · `./kotlin`

<!--
5-minute core: first six slides, at a brisk pace. Up to 15 minutes: add the test and plugin slides and demo running/testing the project.
Based on Kotlin Fundamentals lesson 1: start from product: jvm/app and add dependencies and settings. Use the name Amper to match the requested section; don't switch between old ./amper and current ./kotlin commands.
-->

---

# Install Kotlin Toolchain

```console
curl -fsSL https://kotl.in/install.sh | sh
```
```console
powershell -ExecutionPolicy ByPass -c "irm 'https://kotl.in/install.ps1' | iex"
```

---
magic-move
---

# Install Kotlin Toolchain

```console
curl -fsSL https://kotl.in/install.sh | sh
```
```console
powershell -ExecutionPolicy ByPass -c "irm 'https://kotl.in/install.ps1' | iex"
```
```console
kotlin init
```

---
class: tree-dense
---

# Packages _can be_ folders like in Maven or Gradle

<ul class="tree" aria-label="backend with src/main/kotlin/college/Application.kt, src/test/kotlin/college/ApplicationTest.kt, module.yaml and the kotlin and kotlin.bat wrapper scripts">
  <li class="dir"><span>backend</span>
    <ul>
      <li class="src-root new"><span>src/main/kotlin</span><em>production code</em>
        <ul>
          <li class="pkg"><span>college</span><em>package college</em>
            <ul><li class="kt"><span>Application.kt</span></li></ul>
          </li>
        </ul>
      </li>
      <li class="test-root new"><span>src/test/kotlin</span><em>tests</em>
        <ul>
          <li class="pkg"><span>college</span><em>same package</em>
            <ul><li class="kt"><span>ApplicationTest.kt</span></li></ul>
          </li>
        </ul>
      </li>
      <li class="yaml"><span>module.yaml</span><em>layout: maven-like</em></li>
      <li class="script"><span>kotlin</span><em>pinned toolchain wrapper</em></li>
      <li class="script"><span>kotlin.bat</span></li>
    </ul>
  </li>
</ul>

`layout: maven-like` keeps the Maven and Gradle source roots.

<!--
Familiar starting point: Maven/Gradle source roots plus package-per-folder. Opt in with layout: maven-like in module.yaml (verified on toolchain 0.12.2: it looks in src/main/kotlin and src/main/java). Next slide shows the default layout: src and test directly under the module, without package folders. The default still allows package folders; auth-demo uses src/college.
-->

---
class: tree-dense
---

# A project starts with a module and a wrapper

<ul class="tree" aria-label="backend with src/Application.kt, test/ApplicationTest.kt, module.yaml and the kotlin and kotlin.bat wrapper scripts">
  <li class="dir"><span>backend</span>
    <ul>
      <li class="src-root"><span>src</span><em>production code</em>
        <ul><li class="kt"><span>Application.kt</span></li></ul>
      </li>
      <li class="test-root"><span>test</span><em>tests</em>
        <ul><li class="kt"><span>ApplicationTest.kt</span></li></ul>
      </li>
      <li class="yaml new"><span>module.yaml</span><em>what this module builds</em></li>
      <li class="script"><span>kotlin</span><em>pinned toolchain wrapper</em></li>
      <li class="script"><span>kotlin.bat</span></li>
    </ul>
  </li>
</ul>

Generate a Ktor project with `kotlin init` or IntelliJ IDEA.

Commit the wrapper so everyone uses the same toolchain version.

<!--
For a brand-new project, the installed Kotlin Toolchain provides kotlin init and its Ktor server template. Once generated, use the checked-in ./kotlin script. Single-module project: module.yaml is enough; project.yaml appears when describing a larger project or declaring local plugins.
This is the default Amper source layout, not Gradle's src/main/kotlin. The wrapper provisions the toolchain; don't tell everyone to globally upgrade to an arbitrary version.
-->

---
class: compact
---

# Start with the product

```yaml toolchain
# module.yaml
product: jvm/app
```

A JVM application, with conventions for sources and tests.

The build tool resolves dependencies and invokes the compiler.

<!--
Product tells the tool which kind of output to produce. Kotlin compiler, build tool and JDK are separate version choices. Keep this slide short; the next two progressively add to the same file.
-->

---
magicMove: true
class: compact
---

# Add Ktor and its test host

```yaml toolchain
product: jvm/app

dependencies:
  - io.ktor:ktor-server-netty:3.5.2

test-dependencies:
  - io.ktor:ktor-server-test-host:3.5.2
```

Production dependencies and test-only dependencies stay separate.

<!--
Explicit Maven coordinates make the comparison with Gradle easy; the toolchain also offers built-in Ktor settings/catalog aliases. Netty provides the Ktor server dependencies needed by this small example. 3.5.2 matches the 0.12.2-era toolchain baseline; this is not a claim it is the latest release.
The toolchain configures kotlin.test/JUnit by convention. Don't copy dependencies for JSON/auth/etc until the example needs them.
-->

---
magicMove: true
class: compact
---

# Override the defaults you care about

```yaml toolchain
product: jvm/app

dependencies:
  - io.ktor:ktor-server-netty:3.5.2

test-dependencies:
  - io.ktor:ktor-server-test-host:3.5.2

settings:
  kotlin:
    version: 2.4.10
  jvm:
    jdk: { version: 21 }
    release: 21
```

<!--
We deliberately use JDK 21 for the parallel Gradle example too. Toolchain 0.12 defaults are different; this is an explicit override. release controls the target bytecode/API level; the JDK is the compiler/runtime toolchain. Settings are build configuration, not application.yaml's runtime configuration.
Main class can be detected; configure settings.jvm.mainClass explicitly when needed. For 15 minutes, show project.yaml and a shared module template in the IDE rather than adding every setting here.
-->

---

# Run the same commands from any checkout

```bash
./kotlin build        # compile / package
./kotlin run          # start the app
./kotlin test         # run tests
./kotlin check        # tests + registered plugin checks
```

`check` only runs the additional checks your project has registered.

<!--
End of the 5-minute core. Built-in tests do not imply built-in detekt, formatting or coverage gates. Use ./kotlin --help, test --help and show checks to discover capabilities on the pinned version.
Transition for the short route: “That's the project model and local feedback loop; let's express it in Gradle.” Skip the next two slides if time is tight.
-->

---
class: compact
---

# A test runs Ktor without starting Netty

```kotlin
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
  @Test
  fun healthResponds() = testApplication {
    application {
      routing { get("/health") { call.respondText("OK") } }
    }
    assertEquals("OK", client.get("/health").bodyAsText())
  }
}
```

`test/ApplicationTest.kt` → `./kotlin test`

<!--
Optional demo, 3–5 minutes. ApplicationTest is a JUnit-discoverable test class. Run it, change expected OK to broken, run again, then restore. This is a test-host demonstration; later architecture tests will install the actual app instead of defining a route inline.
Same Kotlin test body works in the Gradle layout with the dependencies on the following slides.
-->

---
class: tree-dense
---

# One project, three modules

<ul class="tree" aria-label="college with project.yaml, server, shared and android modules each with their own module.yaml, and the kotlin and kotlin.bat wrapper scripts at the root">
  <li class="dir"><span>college</span>
    <ul>
      <li class="yaml new"><span>project.yaml</span><em>lists the modules</em></li>
      <li class="dir"><span>server</span><em>src, test: our Ktor app</em>
        <ul><li class="yaml"><span>module.yaml</span></li></ul>
      </li>
      <li class="dir new"><span>shared</span><em>code for both apps</em>
        <ul><li class="yaml"><span>module.yaml</span></li></ul>
      </li>
      <li class="dir new"><span>android</span><em>the Android app</em>
        <ul><li class="yaml"><span>module.yaml</span></li></ul>
      </li>
      <li class="script"><span>kotlin</span><em>one wrapper for all modules</em></li>
      <li class="script"><span>kotlin.bat</span></li>
    </ul>
  </li>
</ul>

Move `src`, `test` and `module.yaml` into `server/`.

<!--
Refactor of the single module two slides back: the existing module becomes server/ unchanged. Every folder with a module.yaml is a module; project.yaml at the root ties them together. Verified on toolchain 0.12.2 in a scratch project: ./kotlin run -m server and ./kotlin build -m android both succeed.
-->

---
class: compact
---

# `project.yaml` lists the modules

```yaml toolchain
# project.yaml
modules:
  - server
  - shared
  - android
```

`./kotlin build` builds them all; `-m server` picks one.

<!--
Paths are relative to the root. Commands from the root apply to every module; ./kotlin run -m server runs just the Ktor app. Same idea as include(...) in settings.gradle.kts.
-->

---
class: compact
---

# `shared` is a library for both platforms

```yaml toolchain
# shared/module.yaml
product:
  type: lib
  platforms: [jvm, android]
```

`src` is common code; `src@jvm` and `src@android` add platform-specific code.

<ul class="tree" aria-label="shared with src for common code, src@jvm for the server, src@android for the Android app, and module.yaml">
  <li class="dir"><span>shared</span>
    <ul>
      <li class="src-root"><span>src</span><em>both platforms</em></li>
      <li class="src-root new"><span>src@jvm</span><em>only the server</em></li>
      <li class="src-root new"><span>src@android</span><em>only the Android app</em></li>
      <li class="yaml"><span>module.yaml</span></li>
    </ul>
  </li>
</ul>

<!--
Put DTOs, validation and API paths here: code both apps need. No Ktor server and no Android SDK in shared/src, or the other platform can't compile it. The @platform folder suffix is Amper's convention instead of Gradle's jvmMain/androidMain source sets.
-->

---
class: compact
---

# Both apps depend on `shared` by path

```yaml toolchain
# server/module.yaml
product: jvm/app

dependencies:
  - ../shared
  - io.ktor:ktor-server-netty:3.5.2

test-dependencies:
  - io.ktor:ktor-server-test-host:3.5.2
```

<!--
Same module.yaml as before, plus ../shared. A local module is a relative path; a library is Maven coordinates. Settings from the override slide stay here too, or move into a shared module template once several modules repeat them.
-->

---
magicMove: true
class: compact
---

# Both apps depend on `shared` by path

```yaml toolchain
# android/module.yaml
product: android/app

dependencies:
  - ../shared
```

`android/src` holds `AndroidManifest.xml` next to the Kotlin code.

<!--
android/app needs an AndroidManifest.xml in src. The toolchain provisions the Android SDK itself on the first build, so the first ./kotlin build -m android is slow. Same ../shared line as the server: both apps compile against the same code.
-->

---
class: compact
---

# Our local plugins live beside the app

```yaml toolchain
# project.yaml
modules:
  - auth-demo
  - plugins/jib-plugin
  - plugins/ktor-plugin
plugins:
  - plugins/jib-plugin
  - plugins/ktor-plugin
```

**Ktor** supplies container tasks; **Jib** supplies the image builder.

<!--
Open this deck's project.yaml, then plugins/ktor-plugin/module.yaml and plugin.yaml. These are copied from ~/Developer/ktor-amper/ktor-sample, pinned to this project's 0.12.2 wrapper. Ktor depends on the shared Jib module. This ports the container tasks, not every Ktor Gradle plugin feature. No formatter plugin is included.
-->

---
class: compact
---

# Enable the plugin for our auth demo

```yaml toolchain
# auth-demo/module.yaml — alongside dependencies/settings
plugins:
  ktor-plugin:
    enabled: true
    docker:
      jreVersion: 21
      platforms: [linux/amd64]
      localImageName: avans-college-auth
      imageTag: demo-1
```

```bash
./kotlin do buildImage
```

Follow `plugin.yaml` → Kotlin task action → Jib Core.

<!--
Optional 2–4 minute demo. Run from avans-college. This command builds a tarball; registry publishing comes in the deployment chapter. Show module.runtimeClasspath, module.jar, module.classes and module.resources as declared task inputs rather than manually searching build/artifacts. The full walkthrough is in slides.tooling.md.
Formatting stays a separate tooling discussion: the planned ktfmt-based kotlin format is Simon's roadmap update, not a command available in 0.12.2.
-->
