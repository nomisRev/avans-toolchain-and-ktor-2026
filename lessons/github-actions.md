---
layout: intro
class: section-slide
kodee: wave
---

# 3 · GitHub Actions

## Repeat the local build on every pull request

<!--
Suggested 10–15 minutes. One workflow per chosen build tool, not a migration or a requirement to run both in the same repository. Complete copyable examples are in examples/github-actions, outside .github/workflows so they do not accidentally run in this presentation repository.
-->

---

# A workflow is a sequence of steps

<div class="flow">
  <div><strong>Prepare</strong><small>Check out code<br>Restore tools and downloads</small></div>
  <span>→</span>
  <div><strong>Check</strong><small>Build and test<br>Run configured quality gates</small></div>
  <span>→</span>
  <div><strong>Inspect</strong><small>Read the failing step<br>Download its reports</small></div>
</div>

Start with one Linux job. Add a matrix when it answers a real need.

<!--
GitHub-hosted runners start fresh. A job has its own runner; steps in that job share the workspace and run in order. Actions such as checkout and setup-java are reusable steps; run executes our own command.
Don't start with deployment, container publishing, multiple OSes or every possible tool. First make the existing local build repeatable.
-->

---
class: compact
---

# Put the workflow in the backend repository

```yaml
# .github/workflows/build.yml
name: Backend CI
on:
  pull_request:
  push:
    branches: [main]
permissions:
  contents: read
jobs:
  build:
    runs-on: ubuntu-latest
    timeout-minutes: 15
    steps:
      - uses: actions/checkout@v7
```

The next slides fill in the steps for each build tool.

<!--
This is the shared workflow shape, not a complete checking workflow yet. PR updates trigger feedback; pushes to main verify the merged branch. Change main if their default branch differs. Commit the executable wrapper together with all build configuration. A monorepo needs working-directory/path adjustments; examples assume backend is the repository root.
Action versions checked against current primary docs on 2026-09-29. Files in examples/github-actions contain each complete variant.
-->

---
class: compact
---

# Amper: the wrapper provisions the toolchain

```yaml
# Under jobs.build.steps, after checkout
- name: Build
  run: ./kotlin build

- name: Tests and registered checks
  run: ./kotlin check
```

No separate Java setup is needed for the normal wrapper path.

`check` runs tests and the checks registered by your plugins.

<!--
Use ./kotlin for the 0.12.2 Kotlin Toolchain, even though we call the chapter Amper. The wrapper provisions its runtime and the toolchain provisions configured JDKs. Do not set KOTLIN_CLI_JAVA_HOME unless intentionally taking over provisioning.
The actual copyable workflow tees output into ci-output with pipefail so failed commands still fail. That diagnostic plumbing is shown later; this slide introduces only the two essential commands.
-->

---
class: compact
---

# Save Amper diagnostics too

```yaml
# Check step: retain output without hiding a failure
- name: Tests and registered checks
  shell: bash
  run: |
    mkdir -p ci-output
    set -o pipefail
    ./kotlin check 2>&1 | tee ci-output/check.log

- name: Save diagnostics
  if: always()
  uses: actions/upload-artifact@v7
  with:
    name: amper-reports
    path: ci-output/
```

<!--
The full example also saves build output and any configured reports/ or build/maven-target/ output. Do not pretend the toolchain produces Gradle's report layout by default. The explicit log artifact works even before adding report plugins.
Bash pipefail preserves failure from the command on the left of tee. No continue-on-error for required tests. Full file: examples/github-actions/amper.yml.
-->

---

# Add quality gates after the basic build works

| Gate | Gradle | Amper / Kotlin Toolchain |
| --- | --- | --- |
| Formatting | `spotlessCheck` / `ktlintCheck` | CLI or a registered local check |
| Analysis | `detektMain` | detekt CLI / local plugin |
| Coverage | Kover report + `koverVerify` | Agent/CLI or JaCoCo Maven bridge + verification |

Configure the tool first; then add its command as a workflow step.

<!--
This preserves the original questions without making the audience learn every tool before their first workflow. Detailed syntax is available in slides.tooling.md.
ktfmt is my formatting default; kotlin format is planned, not assumed on 0.12.2. Choose ktlint rules if useful, and avoid fighting formatters. Kover is my Kotlin/JVM coverage default. A generated HTML/XML report alone is not a failing threshold gate. Amper check doesn't auto-register any of these integrations.
-->

---
class: compact
---

# Example: add the configured Kover gate

```yaml
# Gradle steps, once Kover and its threshold are configured
- name: Coverage report
  run: ./gradlew koverHtmlReport koverXmlReport

- name: Coverage minimum
  run: ./gradlew koverVerify
```

Keep the final report upload on `if: always()`.

For Amper, use the same pattern: **produce report → verify → upload**.

<!--
The baseline Gradle build step already runs tests. Kover may rerun/instrument relevant tests as needed; Gradle task reuse is managed by the plugin. Don't infer a minimum just from applying Kover; show the existing minBound example in the optional deck if asked.
For Amper's Maven bridge, ./kotlin task :backend:jacoco-maven-plugin.report requires enabling the Maven goals first and selecting the actual module name. A separate verifier must reject absent/empty coverage. We haven't run that on the students' backend, so the baseline Amper YAML doesn't pretend to enforce coverage yet.
-->

---

# Prove the workflow catches a regression

1. Commit the chosen workflow and open a pull request
2. Change one assertion so the test fails
3. Find the failed step and download its reports
4. Restore the assertion and confirm the job passes

Make that CI job a required check for merging.

<!--
Demo exercise, not actions taken on a remote repository by this task. Workflows are supplied as examples only. After the check has run, select its actual name in the repository ruleset/branch protection settings. Required checks should run for every PR they are required on; avoid path filters that leave the expected check pending.
Transition: “Now that tests run on every PR, how do we structure the backend so they are easy to write?” Go directly to the existing architecture section.
-->
