package io.ktor.toolchain.jib

import org.jetbrains.amper.plugins.Configurable
import org.jetbrains.amper.plugins.EnumValue
import java.nio.file.Path

/**
 * Mirrors the `jib { }` extension of the Jib Gradle plugin.
 *
 * Gradle-only options (`skaffold`, `pluginExtensions`, `configurationName`) have no Kotlin Toolchain equivalent.
 */
@Configurable
interface JibSettings {
    /** The base image (`jib.from`). */
    val from: BaseImageSettings

    /** The target image (`jib.to`). */
    val to: TargetImageSettings

    /** The container configuration (`jib.container`). */
    val container: ContainerSettings

    /** Extra files added to the image root (`jib.extraDirectories`). */
    val extraDirectories: ExtraDirectoriesSettings

    /** The Docker client used by `jibDockerBuild` (`jib.dockerClient`). */
    val dockerClient: DockerClientSettings

    /** Where image metadata is written (`jib.outputPaths`). Defaults to the task output directory. */
    val outputPaths: OutputPathsSettings

    /** Allow pushing to and pulling from registries over plain HTTP (`jib.allowInsecureRegistries`). */
    val allowInsecureRegistries: Boolean
        get() = false

    /** Whether to add the application as class/resource directories or as its JAR (`jib.containerizingMode`). */
    val containerizingMode: ContainerizingModeSetting
        get() = ContainerizingModeSetting.Exploded
}

@Configurable
interface BaseImageSettings {
    /** The base image reference. Defaults to `eclipse-temurin:<jdk>-jre` for the module JDK version. */
    val image: String?

    /** Credentials used to pull the base image. */
    val auth: AuthSettings?

    /** Docker Credential Helper used to pull the base image. */
    val credHelper: CredHelperSettings?

    /** Platforms to build for. Multiple platforms require a registry base and target image. Defaults to `linux/amd64`. */
    val platforms: List<PlatformSettings>
        get() = emptyList()
}

@Configurable
interface TargetImageSettings {
    /** The target image reference. Defaults to the module name. */
    val image: String?

    /** Additional tags applied to the target image. */
    val tags: List<String>
        get() = emptyList()

    /** Credentials used to push the target image. */
    val auth: AuthSettings?

    /** Docker Credential Helper used to push the target image. */
    val credHelper: CredHelperSettings?
}

@Configurable
interface AuthSettings {
    val username: String
    val password: String
}

@Configurable
interface CredHelperSettings {
    /** The credential helper suffix (`desktop` for `docker-credential-desktop`) or an absolute path to it. */
    val helper: String

    /** Environment variables passed to the credential helper. */
    val environment: Map<String, String>
        get() = emptyMap()
}

@Configurable
interface PlatformSettings {
    val os: String
        get() = "linux"
    val architecture: String
        get() = "amd64"
}

@Configurable
interface ContainerSettings {
    /** The main class. Defaults to the main class configured or detected by the Kotlin Toolchain. */
    val mainClass: String?

    /** JVM flags passed to `java` in the default entrypoint. */
    val jvmFlags: List<String>
        get() = emptyList()

    /** Program arguments (the image `CMD`). */
    val args: List<String>?

    /** Replaces the default `java` entrypoint. `jvmFlags`, `mainClass` and classpath settings are then ignored. */
    val entrypoint: List<String>?

    /** Environment variables set in the image. */
    val environment: Map<String, String>
        get() = emptyMap()

    /** Paths in the container prepended to the computed classpath. */
    val extraClasspath: List<String>
        get() = emptyList()

    /** List every dependency JAR on the classpath instead of the `libs` directory wildcard. */
    val expandClasspathDependencies: Boolean
        get() = false

    /** Exposed ports, such as `8080`, `8080/udp` or `1000-1003`. */
    val ports: List<String>
        get() = emptyList()

    /** Volume mount points. */
    val volumes: List<String>
        get() = emptyList()

    /** Image labels. */
    val labels: Map<String, String>
        get() = emptyMap()

    /** Root directory of the application in the image. */
    val appRoot: String
        get() = "/app"

    /** The user (and optionally group) the container runs as. */
    val user: String?

    /** The working directory of the container. */
    val workingDirectory: String?

    /** The image manifest format. */
    val format: ImageFormatSetting
        get() = ImageFormatSetting.Docker

    /** Modification time of application files: `EPOCH_PLUS_SECOND` or an ISO 8601 date-time. */
    val filesModificationTime: String
        get() = "EPOCH_PLUS_SECOND"

    /** Image creation time: `EPOCH`, `USE_CURRENT_TIMESTAMP` or an ISO 8601 date-time. */
    val creationTime: String
        get() = "EPOCH"
}

@Configurable
interface ExtraDirectoriesSettings {
    /** Directories copied into the image. Defaults to the module's `jib` directory, when it exists. */
    val paths: List<ExtraDirectorySettings>
        get() = emptyList()

    /** Octal file permissions by glob of the path in the image, e.g. `/app/run.sh: 755`. */
    val permissions: Map<String, String>
        get() = emptyMap()
}

@Configurable
interface ExtraDirectorySettings {
    val from: Path
    val into: String
        get() = "/"
    val includes: List<String>
        get() = emptyList()
    val excludes: List<String>
        get() = emptyList()
}

@Configurable
interface DockerClientSettings {
    /** Path to the `docker` executable. Defaults to `docker` on the `PATH`. */
    val executable: String?

    /** Environment variables for the `docker` executable. */
    val environment: Map<String, String>
        get() = emptyMap()
}

@Configurable
interface OutputPathsSettings {
    val digest: Path?
    val imageId: Path?
    val imageJson: Path?

    /** Output of `jibBuildTar`. */
    val tar: Path?
}

enum class ContainerizingModeSetting {
    @EnumValue("exploded") Exploded,
    @EnumValue("packaged") Packaged,
}

enum class ImageFormatSetting {
    @EnumValue("Docker") Docker,
    @EnumValue("OCI") OCI,
}
