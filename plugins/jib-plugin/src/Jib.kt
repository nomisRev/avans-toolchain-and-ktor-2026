package io.ktor.toolchain.jib

import com.google.cloud.tools.jib.api.buildplan.ImageFormat
import org.jetbrains.amper.plugins.Classpath
import org.jetbrains.amper.plugins.CompilationArtifact
import org.jetbrains.amper.plugins.ExecutionAvoidance
import org.jetbrains.amper.plugins.Input
import org.jetbrains.amper.plugins.ModuleSources
import org.jetbrains.amper.plugins.Output
import org.jetbrains.amper.plugins.TaskAction
import java.nio.file.Path
import kotlin.io.path.isDirectory

/** `jib`: builds the image and pushes it to `to.image`. Always runs, like `gradle jib`. */
@TaskAction(executionAvoidance = ExecutionAvoidance.Disabled)
fun jib(
    @Input runtimeClasspath: Classpath,
    @Input jar: CompilationArtifact,
    @Input classes: CompilationArtifact,
    @Input resources: ModuleSources,
    @Input extraDirectory: Path,
    from: BaseImageSettings,
    to: TargetImageSettings,
    container: ContainerSettings,
    @Input extraDirectories: ExtraDirectoriesSettings,
    dockerClient: DockerClientSettings,
    @Output outputPaths: OutputPathsSettings,
    allowInsecureRegistries: Boolean,
    containerizingMode: ContainerizingModeSetting,
    moduleName: String,
    moduleJdkVersion: Int,
    @Output outputDir: Path,
) {
    JibSettingsValues(from, to, container, extraDirectories, outputPaths, allowInsecureRegistries, containerizingMode)
        .toBuild(runtimeClasspath, jar, classes, resources, extraDirectory, moduleName, moduleJdkVersion, outputDir)
        .containerize(JibTarget.Registry)
}

/** `jibDockerBuild`: builds the image into the local Docker daemon. Always runs, like `gradle jibDockerBuild`. */
@TaskAction(executionAvoidance = ExecutionAvoidance.Disabled)
fun jibDockerBuild(
    @Input runtimeClasspath: Classpath,
    @Input jar: CompilationArtifact,
    @Input classes: CompilationArtifact,
    @Input resources: ModuleSources,
    @Input extraDirectory: Path,
    from: BaseImageSettings,
    to: TargetImageSettings,
    container: ContainerSettings,
    @Input extraDirectories: ExtraDirectoriesSettings,
    dockerClient: DockerClientSettings,
    @Output outputPaths: OutputPathsSettings,
    allowInsecureRegistries: Boolean,
    containerizingMode: ContainerizingModeSetting,
    moduleName: String,
    moduleJdkVersion: Int,
    @Output outputDir: Path,
) {
    JibSettingsValues(from, to, container, extraDirectories, outputPaths, allowInsecureRegistries, containerizingMode)
        .toBuild(runtimeClasspath, jar, classes, resources, extraDirectory, moduleName, moduleJdkVersion, outputDir)
        .containerize(JibTarget.DockerDaemon(dockerClient.executable, dockerClient.environment))
}

/** `jibBuildTar`: builds the image into a tarball, by default `<outputDir>/jib-image.tar`. */
@TaskAction
fun jibBuildTar(
    @Input runtimeClasspath: Classpath,
    @Input jar: CompilationArtifact,
    @Input classes: CompilationArtifact,
    @Input resources: ModuleSources,
    @Input extraDirectory: Path,
    from: BaseImageSettings,
    to: TargetImageSettings,
    container: ContainerSettings,
    @Input extraDirectories: ExtraDirectoriesSettings,
    dockerClient: DockerClientSettings,
    @Output outputPaths: OutputPathsSettings,
    allowInsecureRegistries: Boolean,
    containerizingMode: ContainerizingModeSetting,
    moduleName: String,
    moduleJdkVersion: Int,
    @Output outputDir: Path,
) {
    JibSettingsValues(from, to, container, extraDirectories, outputPaths, allowInsecureRegistries, containerizingMode)
        .toBuild(runtimeClasspath, jar, classes, resources, extraDirectory, moduleName, moduleJdkVersion, outputDir)
        .containerize(JibTarget.Tar(outputPaths.tar ?: outputDir.resolve("jib-image.tar")))
}

/** The settings a build needs; task actions receive them separately so `Path`s can be declared as inputs or outputs. */
private class JibSettingsValues(
    val from: BaseImageSettings,
    val to: TargetImageSettings,
    val container: ContainerSettings,
    val extraDirectories: ExtraDirectoriesSettings,
    val outputPaths: OutputPathsSettings,
    val allowInsecureRegistries: Boolean,
    val containerizingMode: ContainerizingModeSetting,
)

private fun JibSettingsValues.toBuild(
    runtimeClasspath: Classpath,
    jar: CompilationArtifact,
    classes: CompilationArtifact,
    resources: ModuleSources,
    defaultExtraDirectory: Path,
    moduleName: String,
    moduleJdkVersion: Int,
    outputDir: Path,
): JibBuild {
    val defaultOutputs = OutputPaths(outputDir)
    return JibBuild(
        module = ModuleOutputs(
            runtimeClasspath = runtimeClasspath.resolvedFiles,
            jar = jar.artifact,
            classes = classes.artifact,
            resourceDirectories = resources.sourceDirectories,
        ),
        from = JibImage(from.image ?: defaultBaseImage(moduleJdkVersion), credentials(from.auth, from.credHelper)),
        to = JibImage(to.image ?: moduleName, credentials(to.auth, to.credHelper)),
        tags = to.tags,
        platforms = from.platforms.map { it.os to it.architecture },
        container = JibContainerConfig(
            mainClass = container.mainClass,
            jvmFlags = container.jvmFlags,
            args = container.args,
            entrypoint = container.entrypoint,
            environment = container.environment,
            extraClasspath = container.extraClasspath,
            expandClasspathDependencies = container.expandClasspathDependencies,
            ports = container.ports,
            volumes = container.volumes,
            labels = container.labels,
            appRoot = container.appRoot,
            user = container.user,
            workingDirectory = container.workingDirectory,
            format = when (container.format) {
                ImageFormatSetting.Docker -> ImageFormat.Docker
                ImageFormatSetting.OCI -> ImageFormat.OCI
            },
            filesModificationTime = container.filesModificationTime,
            creationTime = container.creationTime,
        ),
        extraDirectories = extraDirectories.paths.map { ExtraDirectory(it.from, it.into, it.includes, it.excludes) }
            .ifEmpty { listOfNotNull(defaultExtraDirectory.takeIf { it.isDirectory() }?.let(::ExtraDirectory)) },
        permissions = extraDirectories.permissions,
        packaged = containerizingMode == ContainerizingModeSetting.Packaged,
        allowInsecureRegistries = allowInsecureRegistries,
        workDir = outputDir,
        outputPaths = OutputPaths(
            digest = outputPaths.digest ?: defaultOutputs.digest,
            imageId = outputPaths.imageId ?: defaultOutputs.imageId,
            imageJson = outputPaths.imageJson ?: defaultOutputs.imageJson,
        ),
    )
}

private fun credentials(auth: AuthSettings?, credHelper: CredHelperSettings?) = RegistryCredentials(
    username = auth?.username,
    password = auth?.password,
    credHelper = credHelper?.helper,
    credHelperEnvironment = credHelper?.environment.orEmpty(),
)
