# GitHub Actions examples

Choose the file matching the backend's build tool and copy it to `.github/workflows/build.yml` **in the backend repository**. These files are outside `.github/workflows` here, so they do not run in the slide project.

Both examples assume a standalone JVM backend with its executable wrapper committed at the repository root, GitHub-hosted Ubuntu runners, and `main` as the default branch. Adjust those choices for the actual project. The Gradle example installs JDK 21 to match the intro. The Amper example uses the Kotlin Toolchain 0.12.2 `./kotlin` wrapper and its normal JDK provisioning.

- `gradle.yml`: checkout, Java setup, Gradle caching, `./gradlew build`, report upload even on failure.
- `amper.yml`: checkout, toolchain download caching, Java for the CLI jars, pinned and cached ktfmt/ktlint/detekt/Kover downloads, the three source checks, tests with the Kover agent, a Kover report, a coverage minimum, and upload of `build/reports/` even on failure. Copy `.editorconfig`, `config/detekt.yml` and `scripts/coverage-minimum.py` along with it, and list your modules in `MODULES`: every check runs on all of them and coverage is aggregated into one report.

The Gradle file is a **build/test baseline**: add `spotlessCheck`, `detektMain` and `koverVerify` once those plugins and a Kover rule are configured. The Amper file already runs the quality gates as pinned CLIs; no build plugins are needed. Its steps were run in order from a clean build on Kotlin Toolchain 0.12.2. Two compromises: detekt runs without type resolution, and the Kover report reads classes from an internal toolchain path (`build/artifacts/CompiledJvmArtifact/<module>jvm/kotlin-output`). JaCoCo through the Maven bridge avoids the path and the script: replace the coverage steps with `./kotlin task :<module>:jacoco-maven-plugin.check` and upload `build/maven-target/reports/` too.

For a monorepo, set each run step's working directory to the backend, and update wrapper hashes, cache paths and upload paths to match. Do not assume `defaults.run.working-directory` also rewrites action inputs.

Test the chosen workflow on a PR: deliberately break an assertion, inspect the failure/report, restore it, and then make the job a required merge check. YAML has been checked locally; no hosted Actions runs have been triggered from this workspace.

Primary references checked 2026-09-29: [GitHub quickstart](https://docs.github.com/en/actions/get-started/quickstart), [setup-java](https://github.com/actions/setup-java), [setup-gradle](https://github.com/gradle/actions/blob/main/setup-gradle/README.md), [cache](https://github.com/actions/cache), [upload-artifact](https://github.com/actions/upload-artifact), [Kotlin wrapper provisioning](https://kotlin-toolchain.org/0.12/cli/provisioning/).
