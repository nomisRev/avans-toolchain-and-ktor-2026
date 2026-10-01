package io.ktor.toolchain

import io.ktor.toolchain.jib.JibBuild
import io.ktor.toolchain.jib.JibContainerConfig
import io.ktor.toolchain.jib.JibImage
import io.ktor.toolchain.jib.JibTarget
import io.ktor.toolchain.jib.ModuleOutputs
import io.ktor.toolchain.jib.RegistryCredentials
import io.ktor.toolchain.jib.containerize
import io.ktor.toolchain.jib.defaultBaseImage
import org.jetbrains.amper.plugins.Classpath
import org.jetbrains.amper.plugins.CompilationArtifact
import org.jetbrains.amper.plugins.ExecutionAvoidance
import org.jetbrains.amper.plugins.Input
import org.jetbrains.amper.plugins.ModuleSources
import org.jetbrains.amper.plugins.Output
import org.jetbrains.amper.plugins.TaskAction
import java.nio.file.Path

/** `buildImage`: builds the image into `<outputDir>/jib-image.tar`. */
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
    docker.toBuild(ModuleOutputs(runtimeClasspath.resolvedFiles, jar.artifact, classes.artifact, resources.sourceDirectories), moduleJdkVersion, moduleJavaRelease, outputDir)
        .containerize(JibTarget.Tar(outputDir.resolve("jib-image.tar")))
}

/** `publishImageToLocalRegistry`: builds the image into the local Docker daemon. */
@TaskAction(executionAvoidance = ExecutionAvoidance.Disabled)
fun publishImageToLocalRegistry(
    @Input runtimeClasspath: Classpath,
    @Input jar: CompilationArtifact,
    @Input classes: CompilationArtifact,
    @Input resources: ModuleSources,
    docker: DockerSettings,
    moduleJdkVersion: Int,
    moduleJavaRelease: Int?,
    @Output outputDir: Path,
) {
    docker.toBuild(ModuleOutputs(runtimeClasspath.resolvedFiles, jar.artifact, classes.artifact, resources.sourceDirectories), moduleJdkVersion, moduleJavaRelease, outputDir)
        .containerize(JibTarget.DockerDaemon())
}

/** `publishImage`: builds the image and pushes it to `docker.externalRegistry`. */
@TaskAction(executionAvoidance = ExecutionAvoidance.Disabled)
fun publishImage(
    @Input runtimeClasspath: Classpath,
    @Input jar: CompilationArtifact,
    @Input classes: CompilationArtifact,
    @Input resources: ModuleSources,
    docker: DockerSettings,
    moduleJdkVersion: Int,
    moduleJavaRelease: Int?,
    @Output outputDir: Path,
) {
    val registry = requireNotNull(docker.externalRegistry) {
        "'publishImage' requires 'docker.externalRegistry' in the ktor-plugin settings"
    }
    val image = listOfNotNull(registry.hostname, registry.namespace, registry.project).joinToString("/")
    docker.toBuild(ModuleOutputs(runtimeClasspath.resolvedFiles, jar.artifact, classes.artifact, resources.sourceDirectories), moduleJdkVersion, moduleJavaRelease, outputDir)
        .copy(to = JibImage(image, RegistryCredentials(registry.username, registry.password, registry.credHelper)))
        .containerize(JibTarget.Registry)
}

/** `runDocker`: builds the image into the local Docker daemon and runs it with `docker run`. */
@TaskAction(executionAvoidance = ExecutionAvoidance.Disabled)
fun runDocker(
    @Input runtimeClasspath: Classpath,
    @Input jar: CompilationArtifact,
    @Input classes: CompilationArtifact,
    @Input resources: ModuleSources,
    docker: DockerSettings,
    moduleJdkVersion: Int,
    moduleJavaRelease: Int?,
    @Output outputDir: Path,
) {
    docker.toBuild(ModuleOutputs(runtimeClasspath.resolvedFiles, jar.artifact, classes.artifact, resources.sourceDirectories), moduleJdkVersion, moduleJavaRelease, outputDir)
        .containerize(JibTarget.DockerDaemon())

    val portMappings = docker.portMappings.map { "${it.outsideDocker}:${it.insideDocker ?: it.outsideDocker}/${it.protocol.name.lowercase()}" }
        .ifEmpty { listOf("8080:8080/tcp") }
    val command = buildList {
        addAll(listOf("docker", "run", "--rm"))
        portMappings.forEach { addAll(listOf("-p", it)) }
        docker.environmentVariables.forEach { (name, value) -> addAll(listOf("-e", "$name=$value")) }
        docker.hostEnvironmentVariables.forEach { addAll(listOf("-e", it)) }
        add("${docker.localImageName}:${docker.imageTag}")
    }
    println(command.joinToString(" "))
    val exitCode = ProcessBuilder(command).inheritIO().start().waitFor()
    check(exitCode == 0) { "'docker run' exited with code $exitCode" }
}

private fun DockerSettings.toBuild(
    module: ModuleOutputs,
    moduleJdkVersion: Int,
    moduleJavaRelease: Int?,
    outputDir: Path,
): JibBuild {
    val imageJreVersion = jreVersion ?: moduleJdkVersion
    val targetJavaVersion = moduleJavaRelease ?: moduleJdkVersion
    require(imageJreVersion >= targetJavaVersion) {
        "You're trying to build an image with JRE $imageJreVersion while the module targets Java $targetJavaVersion. " +
            "Set 'docker.jreVersion' to at least $targetJavaVersion or lower 'settings.jvm.release'."
    }
    return JibBuild(
        module = module,
        from = JibImage(customBaseImage ?: defaultBaseImage(imageJreVersion)),
        to = JibImage(localImageName),
        tags = listOf(imageTag),
        platforms = platforms.map { platform ->
            val (os, architecture) = platform.split("/", limit = 2).takeIf { it.size == 2 }
                ?: error("Invalid platform '$platform' in 'docker.platforms', expected '<os>/<architecture>' such as 'linux/arm64'")
            os to architecture
        },
        container = JibContainerConfig(environment = environmentVariables),
        allowInsecureRegistries = allowInsecureRegistries,
        workDir = outputDir,
    )
}
