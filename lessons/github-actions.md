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

# Kotlin: the wrapper provisions the toolchain

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

# Save Kotlin diagnostics too

```yaml
- name: Save test reports and logs
  if: failure()
  uses: actions/upload-artifact@v7
  with:
    name: kotlin-reports
    path: |
      build/reports/
```

<!--
The full example also saves build output and any configured reports/ or build/maven-target/ output. Do not pretend the toolchain produces Gradle's report layout by default. The explicit log artifact works even before adding report plugins.
Bash pipefail preserves failure from the command on the left of tee. No continue-on-error for required tests. Full file: examples/github-actions/amper.yml.
-->

---
class: compact
---

# Save Kotlin diagnostics too

```yaml
- name: Save test reports and logs
  uses: actions/upload-artifact@v7
  with:
    name: kotlin-reports
    path: |
      build/reports/
      build/logs/
```

<!--
The full example also saves build output and any configured reports/ or build/maven-target/ output. Do not pretend the toolchain produces Gradle's report layout by default. The explicit log artifact works even before adding report plugins.
Bash pipefail preserves failure from the command on the left of tee. No continue-on-error for required tests. Full file: examples/github-actions/amper.yml.
-->

---

# Quality gates are pinned CLIs in CI

> Be wary of the defaults, only use what you want to enforce.

| Gate | Tool | Fails the step when |
| --- | --- | --- |
| Formatting | ktfmt 0.64 | a file would change |
| Style | ktlint 1.8.0 | a rule is violated |
| Analysis | detekt 1.23.8 | there are findings |
| Coverage | Kover 0.9.11 | line coverage < 80% |

<!--
No build plugins: every tool is a pinned jar downloaded in the workflow. Each command was run on a Kotlin Toolchain 0.12.2 project, from a clean build, in the workflow's order. Students run the same commands locally.
kotlin format (ktfmt-based) is planned, not assumed on 0.12.2. Kover is my Kotlin/JVM default; JaCoCo through the Maven bridge is the alternative at the end.
-->

---
class: compact
zoom: 0.9
---

# Pin every tool's version

```yaml
env:
  DETEKT: 1.23.8
  GH: https://github.com

steps:
  - name: Download CLI tools
    run: |
      mkdir -p tools && cd tools
      curl -fsSLO $GH/detekt/detekt/releases/download/v$DETEKT/detekt-cli-$DETEKT-all.jar
      ...
```

The wrapper brings its own JDK; the jars need one on `PATH`.

<!--
The full file also downloads kover-jvm-agent and kover-cli from Maven Central, marks ktlint executable, and caches tools/ keyed on the four versions (≈210 MB uncached). curl -f fails the step on a 404 instead of saving an error page.
-->

---
class: compact
---

# Each check is one command

```yaml
- name: Analysis (detekt)
  run: >-
    java -jar tools/detekt-cli-$DETEKT-all.jar --input $(echo $SOURCES | tr ' ' ',')
    --config config/detekt.yml --build-upon-default-config
    --report sarif:build/reports/detekt.sarif
```

A nonzero exit code fails the step.

<!--
ktfmt takes $SOURCES as-is; ktlint takes `$(printf '%s/**/*.kt ' $SOURCES)` after `set -f`, because ktlint expands ** itself (see the full file). Exit codes checked by injecting a violation into the shared module: ktfmt 1, ktlint 1, detekt 2, so every module really is checked. Locally, drop --dry-run --set-exit-if-changed to let ktfmt rewrite the files.
detekt runs without --classpath here, so type-aware rules stay silent; the toolchain doesn't print a classpath for it. Baseline existing debt with --create-baseline --baseline detekt-baseline.xml, then pass --baseline in CI.
-->

---
class: compact
zoom: 0.9
---

# Maven plugins in toolchain

```yaml toolchain
# project.yaml
mavenPlugins:
  - org.jacoco:jacoco-maven-plugin:0.8.15
```

```yaml toolchain
# auth-demo/module.yaml
mavenPlugins:
  jacoco-maven-plugin.prepare-agent: enabled
  jacoco-maven-plugin.report: enabled
  jacoco-maven-plugin.check:
    enabled: true
    configuration:
      haltOnFailure: true
      rules:
        - >-
          <rule><element>BUNDLE</element><limits><limit>
          <counter>LINE</counter><value>COVEREDRATIO</value>
          <minimum>0.80</minimum></limit></limits></rule>
```

`./kotlin task :auth-demo:jacoco-maven-plugin.check` → tests, then the gate

<!--
The alternative to Kover's CLI + script: configuration only, no downloads, and a real gate. Verified in a scratch project on 0.12.2. prepare-agent attaches to every ./kotlin test once enabled, so pick JaCoCo or Kover, not both. Report: build/maven-target/reports/jacoco/index.html.
Two gotchas: without haltOnFailure: true a violation is only a warning (Maven's default isn't applied). And a clean `./kotlin build`/`test` over all modules hits a race in build/maven-target/classes when plugin modules exist; scope commands with -m auth-demo.
-->

---

# Prove the workflow catches a regression

1. Commit the chosen workflow and open a pull request
2. Change one assertion so the test fails
3. Find the failed step and download `kotlin-reports`
4. Restore the assertion and confirm the job passes

Make that CI job a required check for merging.

<!--
Demo exercise, not actions taken on a remote repository by this task. Workflows are supplied as examples only. Variation: break formatting instead and watch the ktfmt step fail before any test runs. After the check has run, select its actual name in the repository ruleset/branch protection settings. Required checks should run for every PR they are required on; avoid path filters that leave the expected check pending.
Transition: “Now that tests run on every PR, how do we structure the backend so they are easy to write?” Go directly to the existing architecture section.
-->
