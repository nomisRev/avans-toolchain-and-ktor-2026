package io.ktor.toolchain

import org.jetbrains.amper.plugins.Configurable
import org.jetbrains.amper.plugins.EnumValue

/**
 * Mirrors the `ktor { }` extension of the Ktor Gradle plugin.
 *
 * Only `docker` needs a plugin: the Kotlin Toolchain covers `ktor.development` (`kotlin run` with `settings.ktor`),
 * `fatJar` (`kotlin package -f executable-jar`), the BOM (`settings.ktor.applyBom`) and the OpenAPI compiler plugin
 * (`settings.kotlin.compilerPlugins`) natively.
 */
@Configurable
interface KtorSettings {
    /** Mirrors `ktor.docker`. Images are built with Jib, see the `jib-plugin` for the full Jib configuration. */
    val docker: DockerSettings
}

@Configurable
interface DockerSettings {
    /** The JRE version of the image. Defaults to the module JDK version (`settings.jvm.jdk.version`). */
    val jreVersion: Int?

    /** The tag applied to the image, in addition to `latest`. */
    val imageTag: String
        get() = "latest"

    /** The image name used by `buildImage`, `publishImageToLocalRegistry` and `runDocker`. */
    val localImageName: String
        get() = "ktor-docker-image"

    /** A base image to use instead of `eclipse-temurin:<jreVersion>-jre`. Set [jreVersion] to the Java version it provides. */
    val customBaseImage: String?

    /**
     * Platforms to build for, such as `linux/amd64` and `linux/arm64`. Defaults to `linux/amd64`, like Jib.
     * Multiple platforms are only supported by `publishImage`; a Docker daemon or tarball holds a single platform.
     */
    val platforms: List<String>
        get() = emptyList()

    /** Allow pulling and pushing over plain HTTP, like `jib.allowInsecureRegistries`. */
    val allowInsecureRegistries: Boolean
        get() = false

    /** The registry `publishImage` pushes to. */
    val externalRegistry: ExternalRegistrySettings?

    /** Port mappings for `runDocker`. */
    val portMappings: List<PortMappingSettings>
        get() = emptyList()

    /** Environment variables set in the image and passed to `runDocker`. */
    val environmentVariables: Map<String, String>
        get() = emptyMap()

    /** Names of host environment variables forwarded to the container by `runDocker`. */
    val hostEnvironmentVariables: List<String>
        get() = emptyList()
}

/**
 * The image name is `hostname/namespace/project`, where [hostname] and [namespace] are optional.
 * Docker Hub is `namespace: <user>`, Google Container Registry is `hostname: gcr.io, namespace: <gcp-project>`.
 */
@Configurable
interface ExternalRegistrySettings {
    val project: String
    val hostname: String?
    val namespace: String?
    val username: String?
    val password: String?

    /** Docker Credential Helper to use instead of [username] and [password]. */
    val credHelper: String?
}

@Configurable
interface PortMappingSettings {
    val outsideDocker: Int
    val insideDocker: Int?
    val protocol: PortProtocol
        get() = PortProtocol.Tcp
}

enum class PortProtocol {
    @EnumValue("tcp") Tcp,
    @EnumValue("udp") Udp,
}
