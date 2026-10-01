---
layout: intro
class: section-slide
kodee: sitting
---

# Quality checks for your backend

## Format → analyse → test → measure

<!--
Start with the development loop, then show how CI repeats it. Kotlin Toolchain 0.12.2 is our baseline. Compiler versions and tool/plugin versions are separate choices.
-->

---

# Give each tool a clear job

| Question | Tool |
| --- | --- |
| How should this code look? | ktfmt or ktlint formatting |
| Which patterns deserve attention? | ktlint rules and detekt |
| Does the behavior work? | Tests |
| What did those tests execute? | kotlinx-kover or JaCoCo |

Run the same checks locally and in CI.

<!--
ktlint appears twice deliberately: formatting is one capability of a linter. Static analysis is broader than formatting, and none of these tools proves the application is correct.
-->

---
src: ./formatting.md
---
---
src: ./analysis.md
---
---
src: ./coverage.md
---
---
src: ./plugins.md
---
---
src: ./ci-operations.md
---

---

# A small, useful set of CI checks

1. One formatter, in check mode
2. Selected analysis rules with actionable findings
3. Tests plus a coverage report and an explicit gate
4. Cached downloads and fresh reports

Use existing integrations; write a local plugin for the missing glue.

<!--
Recommendation: ktfmt for formatting, a deliberately chosen analysis ruleset, and Kover for Kotlin/JVM coverage. For this existing 0.12.2 backend, the tested JaCoCo Maven bridge is also a pragmatic integration option.
The upcoming kotlin format direction is supplied by Simon, without an exact release commitment. Do not extend that to promises about coverage, linting or JUnit tags.
Transition: these checks become easier to write when dependencies and HTTP boundaries are explicit.
-->
