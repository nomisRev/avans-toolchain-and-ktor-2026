---
layout: intro
class: section-slide
kodee: sitting
---

# Local Kotlin Toolchain plugins

## Ktor tasks backed by Jib Core

---
class: compact
---

# The demo includes the plugin source

```text
avans-college/
├── project.yaml
├── libs.versions.toml
├── auth-demo/module.yaml
└── plugins/
    ├── ktor-plugin/  → container tasks + Ktor settings
    └── jib-plugin/   → shared Jib Core implementation
```

Copied from `ktor-amper/ktor-sample`; built with our 0.12.2 wrapper.

<!--
Open these local files rather than switching projects. Ktor depends on the shared Jib module. This is the container part of the Ktor Gradle plugin, not full feature parity. The app enables ktor-plugin; jib-plugin's standalone commands are available if separately enabled. The formatter plugin is deliberately omitted.
-->

---
class: compact
---

# Register locally, enable per module

```yaml toolchain
# project.yaml
modules: [auth-demo, plugins/jib-plugin, plugins/ktor-plugin]
plugins: [plugins/jib-plugin, plugins/ktor-plugin]
```

```yaml toolchain
# auth-demo/module.yaml
plugins:
  ktor-plugin:
    enabled: true
    docker:
      jreVersion: 21
      localImageName: avans-college-auth
      imageTag: demo-1
```

<!--
The real app configuration also fixes linux/amd64, HOST and the Docker Hub destination. Plugin folder names are the IDs used in module settings. These build plugins are separate from Ktor server plugins installed in Application.
-->

---
class: compact
---

# The plugin is a Kotlin module

```yaml toolchain
# plugins/ktor-plugin/module.yaml
product: jvm/amper-plugin
pluginInfo:
  settingsClass: io.ktor.toolchain.KtorSettings
dependencies:
  - ../jib-plugin
```

```yaml toolchain
# plugins/jib-plugin/module.yaml
dependencies:
  - $libs.jib.core: exported
```

Jib Core **0.28.2** is pinned in `libs.versions.toml`.

<!--
The second excerpt shows only the dependencies block; Jib's module also declares its plugin product and settings class. Keep the actual jvm/amper-plugin identifier despite the Kotlin Toolchain branding. Open src/settings.kt to show typed @Configurable settings and defaults.
-->

---
class: compact
---

# Declare inputs instead of searching `build/`

```yaml toolchain
# plugins/ktor-plugin/plugin.yaml
tasks:
  buildImage:
    action: !io.ktor.toolchain.buildImage
      runtimeClasspath: ${module.runtimeClasspath}
      jar: ${module.jar}
      classes: ${module.classes}
      resources: ${module.resources}
      docker: ${pluginSettings.docker}
      moduleJdkVersion: ${module.settings.jvm.jdk.version}
      moduleJavaRelease: ${module.settings.jvm.release}
      outputDir: ${taskOutputDir}
```

The build supplies artifacts and tracks the declared inputs.

<!--
Exact task bindings from the copied plugin. The jar supplies main-class metadata; classes, resources and dependencies become separate image layers. These are container-build inputs, not a claim about the correct production-class roots for coverage reporting.
-->

---
class: compact
---

# The task action is ordinary Kotlin

```kotlin
@TaskAction
fun buildImage(
    @Input runtimeClasspath: Classpath,
    @Input jar: CompilationArtifact,
    @Input classes: CompilationArtifact,
    @Input resources: ModuleSources,
    docker: DockerSettings,
    moduleJdkVersion: Int,
    moduleJavaRelease: Int?,
    @Output outputDir: Path,
) {
    // Configure Jib; write outputDir/jib-image.tar
}
```

<!--
Signature from plugins/ktor-plugin/src/Docker.kt. Open its body, then the shared containerize implementation in plugins/jib-plugin/src/JibBuild.kt. Task annotations come from org.jetbrains.amper.plugins; Path is java.nio.file.Path. Publishing and Docker-daemon actions disable execution avoidance because they perform external side effects.
-->

---
class: compact
---

# One implementation, three destinations

```bash
# Tarball — no Docker daemon required
./kotlin do buildImage

# Load into Docker Desktop
./kotlin do publishImageToLocalRegistry

# Push to Docker Hub with credHelper: desktop
./kotlin do publishImage
```

Use `./kotlin task :auth-demo:buildImage@ktor-plugin` to select explicitly.

<!--
Run from avans-college. The first command was built and the linux/amd64 image smoke-tested locally. Despite its name, publishImageToLocalRegistry loads the Docker daemon rather than a separate registry server. Publishing is a deliberate demo action; nothing was pushed while preparing the deck. The image contains no JWT signing key. Render receives that key through its runtime environment.
-->
