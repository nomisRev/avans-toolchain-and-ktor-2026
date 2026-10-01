# GitHub Actions examples

Choose the file matching the backend's build tool and copy it to `.github/workflows/build.yml` **in the backend repository**. These files are outside `.github/workflows` here, so they do not run in the slide project.

Both examples assume a standalone JVM backend with its executable wrapper committed at the repository root, GitHub-hosted Ubuntu runners, and `main` as the default branch. Adjust those choices for the actual project. The Gradle example installs JDK 21 to match the intro. The Amper example uses the Kotlin Toolchain 0.12.2 `./kotlin` wrapper and its normal JDK provisioning.

- `gradle.yml`: checkout, Java setup, Gradle caching, `./gradlew build`, report upload even on failure.
- `amper.yml`: checkout, explicit bootstrap/shared cache locations, download caching, build, tests plus registered checks, diagnostic/report upload even on failure. Bash `pipefail` prevents `tee` from hiding a failure.

These are **build/test baselines**. They do not silently install ktfmt, ktlint, detekt or a coverage policy. After configuring the relevant integrations in the backend, add their commands as required steps. For example, Gradle can run `spotlessCheck`, `detektMain`, and `koverVerify`; Amper can run a registered local check or a pinned CLI. `koverVerify` requires a configured rule; generating a JaCoCo/Kover report requires separate verification to enforce a threshold.

For a monorepo, set each run step's working directory to the backend, and update wrapper hashes, cache paths and upload paths to match. Do not assume `defaults.run.working-directory` also rewrites action inputs.

Test the chosen workflow on a PR: deliberately break an assertion, inspect the failure/report, restore it, and then make the job a required merge check. YAML has been checked locally; no hosted Actions runs have been triggered from this workspace.

Primary references checked 2026-09-29: [GitHub quickstart](https://docs.github.com/en/actions/get-started/quickstart), [setup-java](https://github.com/actions/setup-java), [setup-gradle](https://github.com/gradle/actions/blob/main/setup-gradle/README.md), [cache](https://github.com/actions/cache), [upload-artifact](https://github.com/actions/upload-artifact), [Kotlin wrapper provisioning](https://kotlin-toolchain.org/0.12/cli/provisioning/).
