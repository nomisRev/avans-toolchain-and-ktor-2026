---
class: compact
---

# Cache downloads; rebuild evidence

```yaml
# GitHub Actions: job-level environment
env:
  KOTLIN_CLI_BOOTSTRAP_CACHE_DIR: ${{ github.workspace }}/.cache/kotlin/bootstrap
  KOTLIN_SHARED_CACHE_DIR: ${{ github.workspace }}/.cache/kotlin/shared
```

| Cache | Contains |
| --- | --- |
| Bootstrap | Toolchain distribution / wrapper provisioning |
| Shared | Downloaded dependencies, JDKs and tools |

Restore before running `./kotlin`; save through the CI cache facility.

<div class="source"><a href="https://kotlin-toolchain.org/0.12/cli/provisioning/">Documented cache locations</a></div>

<!--
These environment variables are documented. The cache-key policy on the next slide is our recommendation, not an official mandated recipe. Both directories are deliberately under the workspace so the runner configuration is explicit.
-->

---

# Make cache misses harmless

```text
Bootstrap key: OS + architecture + wrapper hash
Shared key:    OS + architecture + wrapper + build-config hash
```

- Hash module files, templates and version catalogs
- Restore compatible shared downloads as a fallback
- Run checks on every build, including cache hits
- Upload fresh reports as artifacts, even when checks fail

<!--
Practical starting policy; refine after measuring hit rates. Avoid caching the entire build/ directory initially, especially execution data and reports. A cache is an optimization; deleting it must not change correctness. Don't skip tests because dependency caches were restored.
Ensure lint, tests and verification are required CI checks. A downstream report upload should use the CI provider's always-run behavior and must not mask an earlier failure.
-->

---

# JUnit tags: engine capability ≠ CLI option

```bash
# 0.12.2 supports class patterns, not just literal names
./kotlin test -m backend --include-classes '*IntegrationTest'
./kotlin test -m backend --exclude-classes '*IntegrationTest'
```

The 0.12.2 test command has no include/exclude-tag options.

Use class or module boundaries now; no confirmed tag-support date.

<div class="source"><a href="https://github.com/JetBrains/kotlin-toolchain/blob/v0.12.2/sources/amper-cli/src/org/jetbrains/amper/cli/commands/TestCommand.kt">0.12.2 TestCommand</a></div>

<!--
The source also supports --include-test. Patterns use * and ? wildcards, not arbitrary regex; quote them to avoid shell expansion. Multi-module filtering requires module selection. No planned date was confirmed; absence of an option is not evidence the feature will never be added.
A separate JUnit launcher or Surefire integration is possible territory to explore, but adds classpath/test-discovery maintenance. Don't casually recommend a JVM -D flag as a substitute for launcher tag filters.
-->

