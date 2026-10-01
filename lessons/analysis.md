---
layout: intro
class: section-slide
kodee: sitting
---

# Static analysis

## Which findings are worth interrupting a build?

---

# Style rules and code analysis overlap

| Tool | Typical feedback |
| --- | --- |
| ktlint | Naming, imports, spacing and code conventions |
| detekt | Complexity, suspicious constructs and maintainability |
| detekt with type resolution | Rules that need to know what a symbol means |

Use the compiler too: warnings are often the cheapest feedback.

<!--
ktlint is static analysis focused on style. detekt also contains style rules, and optional rule sets can overlap with ktlint. Don't enable the same formatting policy through three integrations.
Type resolution is not enabled merely by supplying source paths to the CLI. A resolved compilation classpath and appropriate JVM settings are required for the relevant rules.
-->

---
class: compact
---

# Both tools have a CLI

```bash
ktlint 'backend/src/**/*.kt' 'backend/test/**/*.kt'

detekt --input backend/src,backend/test \
  --config config/detekt.yml \
  --build-upon-default-config \
  --report sarif:reports/detekt.sarif
```

For type-aware rules, add `--classpath "$COMPILE_CLASSPATH"`.

Pin tool versions and keep nonzero exit codes visible to CI.

<div class="source"><a href="https://github.com/ktlint/ktlint">ktlint</a> · <a href="https://detekt.dev/docs/1.23.8/gettingstarted/cli/">detekt CLI</a></div>

<!--
Uses the detekt 1.23.x CLI vocabulary. Select a release compatible with the project's Kotlin compiler; an older parser may not accept newer Kotlin syntax. COMPILE_CLASSPATH must come from the build model. Configure a matching JVM target as needed.
Uploading SARIF helps navigation but does not replace failing the check. Avoid piping away the exit status.
-->

---
class: compact
---

# ktlint through Gradle

```kts gradle
plugins {
  id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
}

repositories { mavenCentral() }

ktlint {
  ignoreFailures.set(false)
}
```

```bash
./gradlew ktlintCheck
./gradlew ktlintFormat
```

The Gradle plugin version and ktlint engine version are separate.

<!--
Assumes the project's Kotlin plugin is applied. Pin the engine through the extension's version property in a real project; don't conflate the wrapper's version with the ktlint release.
Use ktlintFormat only when ktlint owns formatting. https://github.com/JLLeitschuh/ktlint-gradle
-->

---
class: compact
---

# detekt through Gradle

```kts gradle
plugins {
  id("io.gitlab.arturbosch.detekt") version "1.23.8"
}

detekt {
  buildUponDefaultConfig = true
  config.setFrom(files("config/detekt.yml"))
  ignoreFailures = false
}
```

```bash
./gradlew detekt                 # source analysis
./gradlew detektMain detektTest  # JVM type-aware analysis
```

<!--
Deliberately uses versioned detekt 1.23.8 documentation, matching the example in Kotlin Toolchain 0.12.2. Not a latest-version recommendation. Check Kotlin/Gradle compatibility; detekt 2.x uses a different plugin namespace.
For JVM projects with the Kotlin plugin, detektMain/detektTest provide source-set-aware type resolution. A generic detekt task is not equivalent. https://detekt.dev/docs/1.23.8/gettingstarted/gradle/
-->

---

# More rules bring benefits and costs

| Benefits | Costs |
| --- | --- |
| Catch repeated mistakes early | False positives and suppression noise |
| Share conventions across a team | Configuration and upgrade maintenance |
| Point reviewers at risky code | Longer builds with type resolution |
| Check architectural constraints | Refactoring to satisfy a metric |

An actionable finding is more valuable than a large rule count.

<!--
Opinion/discussion, not a benchmark. A complexity warning can prompt useful design discussion; a blanket function-length limit can split a readable Ktor DSL into fragments that are harder to follow. Generated code and test DSLs may need different policies.
Ask which recent bug a proposed rule would have caught. Static analysis is useful; maximal configuration is not automatically maximal value.
-->

---

# Start strict where the signal is strong

1. Enable rules the team understands and can act on
2. Agree exceptions for tests, DSLs and generated code
3. Baseline existing debt; fail on new findings
4. Remove noisy rules and review suppressions

Don't weaken a rule merely to make today's build green.

<!--
A baseline is an adoption mechanism, not a scheduled command that continually hides new issues. Avoid blindly regenerating it in CI.
Apply new rules in a dedicated change, with fixes and a rationale. Separate formatting-only churn from behavior changes. Keep useful fast checks on each PR; heavier analysis can be a distinct required job when its feedback justifies the time.
-->
