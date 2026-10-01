# Local Ktor and Jib plugins

Copied from `~/Developer/ktor-amper/ktor-sample/{ktor-plugin,jib-plugin}`. The source plugins are unchanged; their original project is not modified. The formatter plugin is omitted.

`ktor-plugin` ports Ktor's container tasks and depends on `jib-plugin` for Jib Core image assembly. This is not a port of every Gradle feature. `libs.versions.toml` pins Jib Core 0.28.2, and the root wrapper pins Kotlin Toolchain 0.12.2.

Read root `project.yaml` for registration, `auth-demo/module.yaml` for enablement, then `ktor-plugin/plugin.yaml`, `ktor-plugin/src/Docker.kt` and `jib-plugin/src/JibBuild.kt`. Tasks receive the module's typed artifacts rather than guessing build output paths.

From `avans-college`:

```sh
./kotlin check
./kotlin do buildImage
# Optional: load the image into Docker Desktop
./kotlin do publishImageToLocalRegistry
# Publish when ready (external registry write)
./kotlin do publishImage
```

The explicit task form is `./kotlin task :auth-demo:buildImage@ktor-plugin`. `buildImage` writes `build/tasks/_auth-demo_buildImage@ktor-plugin/jib-image.tar` with this wrapper. That is this task's output, not a promised stable compiler artifact directory.

The app enables Ktor's task facade. Jib's standalone commands (`jib`, `jibDockerBuild`, `jibBuildTar`) require enabling `jib-plugin` on a consuming module. See [deployment](../examples/deployment/README.md) for Desktop credentials and Render runtime configuration.
