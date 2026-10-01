package io.ktor.toolchain.jib

import com.google.cloud.tools.jib.api.Containerizer
import com.google.cloud.tools.jib.api.Credential
import com.google.cloud.tools.jib.api.DockerDaemonImage
import com.google.cloud.tools.jib.api.buildplan.ImageFormat
import com.google.cloud.tools.jib.api.ImageReference
import com.google.cloud.tools.jib.api.JavaContainerBuilder
import com.google.cloud.tools.jib.api.JibContainer
import com.google.cloud.tools.jib.api.JibContainerBuilder
import com.google.cloud.tools.jib.api.LogEvent
import com.google.cloud.tools.jib.api.RegistryImage
import com.google.cloud.tools.jib.api.TarImage
import com.google.cloud.tools.jib.api.buildplan.AbsoluteUnixPath
import com.google.cloud.tools.jib.api.buildplan.FileEntriesLayer
import com.google.cloud.tools.jib.api.buildplan.FilePermissions
import com.google.cloud.tools.jib.api.buildplan.Platform
import com.google.cloud.tools.jib.api.buildplan.Port
import com.google.cloud.tools.jib.frontend.CredentialRetrieverFactory
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.jar.Attributes
import java.util.jar.JarFile
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.writeText

/*
 * A plain Kotlin model of a Jib build, shared by the `jib-plugin` and `ktor-plugin` task actions.
 * `@Configurable` settings can't be shared between plugins, so each plugin maps its own settings onto this model.
 */

data class RegistryCredentials(
    val username: String? = null,
    val password: String? = null,
    val credHelper: String? = null,
    val credHelperEnvironment: Map<String, String> = emptyMap(),
)

data class JibImage(
    val reference: String,
    val credentials: RegistryCredentials = RegistryCredentials(),
)

data class ExtraDirectory(
    val from: Path,
    val into: String = "/",
    val includes: List<String> = emptyList(),
    val excludes: List<String> = emptyList(),
)

data class JibContainerConfig(
    val mainClass: String? = null,
    val jvmFlags: List<String> = emptyList(),
    val args: List<String>? = null,
    val entrypoint: List<String>? = null,
    val environment: Map<String, String> = emptyMap(),
    val extraClasspath: List<String> = emptyList(),
    val expandClasspathDependencies: Boolean = false,
    val ports: List<String> = emptyList(),
    val volumes: List<String> = emptyList(),
    val labels: Map<String, String> = emptyMap(),
    val appRoot: String = "/app",
    val user: String? = null,
    val workingDirectory: String? = null,
    val format: ImageFormat = ImageFormat.Docker,
    val filesModificationTime: String = "EPOCH_PLUS_SECOND",
    val creationTime: String = "EPOCH",
)

/** The module outputs that go into the image, resolved from `${module.*}` references. */
data class ModuleOutputs(
    val runtimeClasspath: List<Path>,
    val jar: Path,
    val classes: Path,
    val resourceDirectories: List<Path>,
)

data class JibBuild(
    val module: ModuleOutputs,
    val from: JibImage,
    val to: JibImage,
    val tags: List<String> = emptyList(),
    val platforms: List<Pair<String, String>> = emptyList(),
    val container: JibContainerConfig = JibContainerConfig(),
    val extraDirectories: List<ExtraDirectory> = emptyList(),
    val permissions: Map<String, String> = emptyMap(),
    val packaged: Boolean = false,
    val allowInsecureRegistries: Boolean = false,
    /** Directory for the application layer cache and default image metadata outputs. */
    val workDir: Path,
    val outputPaths: OutputPaths = OutputPaths(workDir),
)

data class OutputPaths(
    val digest: Path,
    val imageId: Path,
    val imageJson: Path,
) {
    constructor(dir: Path) : this(dir.resolve("jib-image.digest"), dir.resolve("jib-image.id"), dir.resolve("jib-image.json"))
}

sealed interface JibTarget {
    data object Registry : JibTarget
    data class DockerDaemon(val executable: String? = null, val environment: Map<String, String> = emptyMap()) : JibTarget
    data class Tar(val path: Path) : JibTarget
}

/** Base image Jib uses when none is configured, matching the Java version the module targets. */
fun defaultBaseImage(javaVersion: Int): String = "eclipse-temurin:$javaVersion-jre"

/** Reads the `Main-Class` the Kotlin Toolchain wrote into the module JAR manifest. */
fun mainClassOf(jar: Path): String = JarFile(jar.toFile()).use { file ->
    file.manifest?.mainAttributes?.getValue(Attributes.Name.MAIN_CLASS)
        ?: error("No main class configured or detected in ${jar.fileName}; set 'container.mainClass'")
}

fun JibBuild.containerize(target: JibTarget): JibContainer {
    val containerizer = when (target) {
        JibTarget.Registry -> Containerizer.to(to.toRegistryImage())
        is JibTarget.DockerDaemon -> Containerizer.to(
            DockerDaemonImage.named(to.reference).apply {
                target.executable?.let { setDockerExecutable(Path.of(it)) }
                setDockerEnvironment(target.environment)
            },
        )
        is JibTarget.Tar -> Containerizer.to(TarImage.at(target.path).named(to.reference))
    }
    tags.forEach(containerizer::withAdditionalTag)
    containerizer
        .setToolName("jib-kotlin-toolchain-plugin")
        .setAllowInsecureRegistries(allowInsecureRegistries)
        .setApplicationLayersCache(workDir.resolve("cache"))
        .addEventHandler(LogEvent::class.java, ::log)

    val container = containerBuilder().containerize(containerizer)
    writeOutputs(container)
    return container
}

private fun JibBuild.containerBuilder(): JibContainerBuilder {
    val appRoot = AbsoluteUnixPath.get(container.appRoot)
    val filesModificationTime = parseFilesModificationTime(container.filesModificationTime)
    val mainClass = container.mainClass ?: mainClassOf(module.jar)

    // Layer like the Jib Gradle plugin: dependencies, snapshot dependencies, project dependencies, then the module.
    val dependencies = module.runtimeClasspath.filter { it != module.jar && it.exists() }
    // Outputs of other modules live in the build directory next to the module JAR (`build/tasks/<task>/<jar>`).
    val buildDir = module.jar.parent.parent.parent
    val (projectDependencies, externalDependencies) = dependencies.partition { it.startsWith(buildDir) }
    val (snapshotDependencies, releaseDependencies) = externalDependencies.partition { "SNAPSHOT" in it.name }

    val javaBuilder = JavaContainerBuilder.from(from.toRegistryImage())
        .setAppRoot(appRoot)
        .setModificationTimeProvider { _, _ -> filesModificationTime }
        .addDependencies(releaseDependencies)
        .addSnapshotDependencies(snapshotDependencies)
        .addProjectDependencies(projectDependencies)
        .apply {
            if (packaged) {
                addToClasspath(module.jar)
            } else {
                // The Kotlin Toolchain copies resources into the classes directory; keep them in their own layer like Jib.
                val resourceDirectories = module.resourceDirectories.filter { it.isDirectory() }
                val resourceFiles = resourceDirectories.flatMap { dir ->
                    Files.walk(dir).use { files -> files.filter { it.isRegularFile() }.map { dir.relativize(it) }.toList() }
                }.toSet()
                resourceDirectories.forEach { addResources(it) }
                addClasses(module.classes) { module.classes.relativize(it) !in resourceFiles }
            }
        }
        .addJvmFlags(container.jvmFlags)
        .setMainClass(mainClass)

    val classpath = buildList {
        addAll(container.extraClasspath)
        if (packaged) {
            add("${appRoot.resolve("classpath")}/*")
        } else {
            if (module.resourceDirectories.any { it.isDirectory() }) add(appRoot.resolve("resources").toString())
            add(appRoot.resolve("classes").toString())
        }
        if (container.expandClasspathDependencies) {
            dependencies.forEach { add(appRoot.resolve("libs").resolve(it.name).toString()) }
        } else if (dependencies.isNotEmpty()) {
            add("${appRoot.resolve("libs")}/*")
        }
    }

    return javaBuilder.toContainerBuilder().apply {
        setEntrypoint(
            container.entrypoint ?: (listOf("java") + container.jvmFlags + listOf("-cp", classpath.joinToString(":"), mainClass)),
        )
        setProgramArguments(container.args)
        setEnvironment(container.environment)
        setExposedPorts(container.ports.flatMap(::parsePorts).toSet())
        setVolumes(container.volumes.map(AbsoluteUnixPath::get).toSet())
        setLabels(container.labels)
        setUser(container.user)
        setWorkingDirectory(container.workingDirectory?.let(AbsoluteUnixPath::get))
        setFormat(container.format)
        setCreationTime(parseCreationTime(container.creationTime))
        if (platforms.isNotEmpty()) setPlatforms(platforms.map { (os, architecture) -> Platform(architecture, os) }.toSet())
        extraDirectories.forEach { addFileEntriesLayer(it.toLayer(permissions, filesModificationTime)) }
    }
}

private fun ExtraDirectory.toLayer(permissions: Map<String, String>, modificationTime: Instant): FileEntriesLayer {
    val fileSystem = FileSystems.getDefault()
    val includeMatchers = includes.map { fileSystem.getPathMatcher("glob:$it") }
    val excludeMatchers = excludes.map { fileSystem.getPathMatcher("glob:$it") }
    val permissionMatchers = permissions.map { (glob, octal) ->
        fileSystem.getPathMatcher("glob:$glob") to FilePermissions.fromOctalString(octal)
    }
    val intoPath = AbsoluteUnixPath.get(into)
    val builder = FileEntriesLayer.builder().setName("extra files")
    Files.walk(from).use { files ->
        files.filter { it.isRegularFile() }.sorted().forEach { file ->
            val relative = from.relativize(file)
            val included = includeMatchers.isEmpty() || includeMatchers.any { it.matches(relative) }
            if (!included || excludeMatchers.any { it.matches(relative) }) return@forEach
            val target = intoPath.resolve(relative)
            val filePermissions = permissionMatchers.lastOrNull { (matcher, _) -> matcher.matches(Path.of(target.toString())) }
                ?.second ?: FilePermissions.DEFAULT_FILE_PERMISSIONS
            builder.addEntry(file, target, filePermissions, modificationTime)
        }
    }
    return builder.build()
}

/** Parses `8080`, `8080/udp` or `1000-1003[/tcp]` like the Jib plugins do. */
private fun parsePorts(spec: String): List<Port> {
    val (range, protocol) = spec.split("/", limit = 2).let { it[0] to it.getOrElse(1) { "tcp" } }
    val bounds = range.split("-").map { it.trim().toInt() }
    return (bounds.first()..bounds.last()).map { Port.parseProtocol(it, protocol) }
}

private fun parseFilesModificationTime(value: String): Instant = when (value) {
    "EPOCH_PLUS_SECOND" -> Instant.ofEpochSecond(1)
    else -> DateTimeFormatter.ISO_DATE_TIME.parse(value, Instant::from)
}

private fun parseCreationTime(value: String): Instant = when (value) {
    "EPOCH" -> Instant.EPOCH
    "USE_CURRENT_TIMESTAMP" -> Instant.now()
    else -> DateTimeFormatter.ISO_DATE_TIME.parse(value, Instant::from)
}

private fun JibImage.toRegistryImage(): RegistryImage {
    val imageReference = ImageReference.parse(reference)
    val registryImage = RegistryImage.named(imageReference)
    val (username, password, credHelper, credHelperEnvironment) = credentials
    val factory = CredentialRetrieverFactory.forImage(imageReference, ::log, credHelperEnvironment)
    require(credHelper == null || username == null) {
        "Only one of 'auth' and 'credHelper' may be configured for image $imageReference"
    }
    if (username != null && password != null) {
        registryImage.addCredentialRetriever(factory.known(Credential.from(username, password), "auth"))
    }
    if (credHelper != null) {
        // Like the Jib plugins: a path or full name is used as is, a suffix such as `desktop` is expanded.
        val helper = if ('/' in credHelper || credHelper.startsWith("docker-credential-")) credHelper else "docker-credential-$credHelper"
        registryImage.addCredentialRetriever(factory.dockerCredentialHelper(helper))
    }
    registryImage.addCredentialRetriever(factory.dockerConfig())
    registryImage.addCredentialRetriever(factory.wellKnownCredentialHelpers())
    registryImage.addCredentialRetriever(factory.googleApplicationDefaultCredentials())
    return registryImage
}

private fun JibBuild.writeOutputs(container: JibContainer) {
    outputPaths.digest.also { Files.createDirectories(it.parent) }.writeText(container.digest.toString())
    outputPaths.imageId.also { Files.createDirectories(it.parent) }.writeText(container.imageId.toString())
    val tags = container.tags.joinToString(",") { "\"$it\"" }
    outputPaths.imageJson.also { Files.createDirectories(it.parent) }.writeText(
        """{"image":"${container.targetImage}","imageId":"${container.imageId}","imageDigest":"${container.digest}","tags":[$tags],"imagePushed":${container.isImagePushed}}""",
    )
}

private fun log(event: LogEvent) {
    if (event.level.ordinal <= LogEvent.Level.LIFECYCLE.ordinal) println(event.message)
}
