---
layout: intro
class: section-slide
kodee: wave
---

# Formatting

## Let a tool settle the whitespace

---

# ktfmt and ktlint make different choices

| | ktfmt | ktlint |
| --- | --- | --- |
| Starting point | Pretty-print Kotlin source | Check a set of style rules |
| Formatting | Rewrites layout and wrapping | Corrects fixable rule violations |
| Configuration | Opinionated layout policy | Rules and `.editorconfig` |
| My default | Use it to format Kotlin | Use when its rules fit the team |

Choose one formatting policy; avoid tools fighting over the same file.

<div class="source"><a href="https://github.com/Kotlin/ktfmt">ktfmt</a> · <a href="https://github.com/ktlint/ktlint">ktlint</a></div>

<!--
ktfmt is based on google-java-format. Avoid claiming zero configuration: it has styles and options. ktlint is both a linter and a formatter, but not every finding is automatically fixable.
If combining ktfmt with ktlint or detekt's formatting rules, align/disable overlapping layout rules. Pick versions that parse the Kotlin syntax used by the project.
-->

---
class: compact
---

# Format locally; check in CI

```bash
# ktfmt: a pinned executable jar
java -jar tools/ktfmt.jar --kotlinlang-style backend/src

# CI: report differences and return a failing exit code
java -jar tools/ktfmt.jar --kotlinlang-style \
  --dry-run --set-exit-if-changed backend/src backend/test
```

```bash
# Alternative: ktlint's own formatting policy
ktlint --format 'backend/src/**/*.kt' 'backend/test/**/*.kt'
ktlint 'backend/src/**/*.kt' 'backend/test/**/*.kt'
```

Commit the formatter configuration alongside the code.

<!--
The jar name is a local alias for a pinned ktfmt distribution. Select source roots and exclude generated files explicitly. Directory traversal and flags were checked in the local ktfmt CLI source. --dry-run alone should not be assumed to enforce a failing exit code; show --set-exit-if-changed.
Quote ktlint globs. CI checks rather than silently rewriting checked-out code.
-->

---
class: compact
---

# Gradle can run the same formatter

```kts gradle
plugins {
  id("com.diffplug.spotless") version "8.8.0"
}

spotless {
  kotlin {
    target("src/**/*.kt")
    ktfmt("0.64").kotlinlangStyle()
  }
}
```

```bash
./gradlew spotlessApply   # local formatting
./gradlew spotlessCheck   # CI gate
```

<!--
Example pins match the Spotless version and ktfmt configuration found in ktor-arrow-example, where the configuration block is currently commented out. This is an enabled example, not a claim that the checkout enforces it. Adapt src to the project's source roots.
Spotless is the Gradle integration; ktfmt is the formatter. Existing Kotlin plugin and repositories are omitted. https://github.com/diffplug/spotless/blob/main/plugin-gradle/README.md
-->

---

# Coming: `kotlin format`

The planned direction is a built-in formatting command based on **ktfmt**.

```text
Today       ktfmt CLI / Gradle integration / local plugin
Next        kotlin format
```

Expected soon-ish; no release number promised here.

<!--
Roadmap context supplied directly by Simon in this conversation. Public research did not confirm a release/date. Present this as his forward-looking update, not as a command already available in the deck's 0.12.2 baseline. Don't execute it as part of today's demo.
Adopt formatting now while keeping the integration replaceable. No need to build a large custom formatting framework for this project.
-->
