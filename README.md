# Avans College — Ktor in practice

In [auth-demo](/auth-demo) you can find the example project used for the live demo, and authentication section of the slides.
The slides are in the GitHub Pages [here](https://nomisrev.github.io/avans-toolchain-and-ktor-2026/).

# Hoe met de slides werken
## Present

Node.js 20.12 or newer:

```bash
cd /Users/simonvergauwen/Developer/presentations/avans-college
npm ci
npm run dev
```

Open <http://localhost:3042>. Presenter mode is <http://localhost:3042/presenter>.

```bash
npm run build       # static site + theme-generated handouts in dist/
npm run export      # PDF, requires Playwright Chromium
npm run screenshot # PNGs
```

The local theme is intentionally a `file:../slidedev-theme-kotlin` dependency, as in Opinionated Ktor Services. Keep that sibling folder when moving the deck. The Vite configuration includes the theme's annotation editor.

## Main session route

The main deck now follows **Amper → Gradle → GitHub Actions → architecture → authentication**. It uses the students' Kotlin Toolchain 0.12.2 baseline (`./kotlin`) in the Amper chapter.

| Section | Time | Source |
| --- | --- | --- |
| Amper intro | 5–15 min | `lessons/amper-intro.md` |
| Gradle intro | 5–15 min | `lessons/gradle-intro.md` |
| GitHub Actions for either build | 10–15 min | `lessons/github-actions.md` |
| Architecture and DI | 10–15 min | `lessons/architecture.md` (unchanged) |
| Authentication and local demo | 10–15 min | `lessons/authentication.md` |
| Optional context parameters / next talk | 3–5 min | `lessons/next.md` |

Allow roughly 40 minutes for the shortest route, or 60–75 minutes with demos/discussion. The main deck introduces the project and local commands before CI. Quality tools appear when adding CI gates; their full configuration is optional material.

### Five-minute versus fifteen-minute intros

**Amper, 5 minutes:** project files → minimal product → dependencies → settings → wrapper commands. Stop at “Run the same commands from any checkout”. **Up to 15 minutes:** add “A test runs Ktor without starting Netty”, demonstrate a passing/failing test, and show the local Ktor/Jib plugin from “Our local plugins live beside the app”.

**Gradle, 5 minutes:** project files → plugins → dependencies → task commands → `build`/`assemble`/`check`, then a quick overview of “The Ktor plugin adds packaging tasks”. **Up to 15 minutes:** choose the test/catalog details or the deployment demo: publish a container with Jib and run it on Render. Finish with quality-plugin task names as the bridge to CI. The short route skips explaining the full test setup, not configuring it in a real demo project.

The deployment slides condense Opinionated Ktor Services lesson 7: fat JAR versus container, publishing to a registry, then hosting. “Build, publish, deploy” adds a diagram with four clicks: publish, manual pull, replace the manual trigger with a webhook, then automatic deployment. The [Render demo guide](examples/deployment/README.md) covers preparation, source, commands, webhook setup and the free-tier idle delay. Rehearse beforehand; allow extra time if presenting every optional detail.

The matching configuration snippets are progressive additions, not independent complete projects. A generated backend supplies its application code and wrapper. Examples have not been executed against the students' unavailable backend.

### GitHub Actions handout

Complete baseline workflows are in [examples/github-actions](examples/github-actions/README.md). Choose the Gradle or Amper variant and copy it into the backend repository. The slides build these up in readable pieces. They cover triggers, checkout, provisioning, caches, failure diagnostics, report uploads and adding configured quality gates. They are not installed as workflows in this presentation project.

## Authentication project

[auth-demo](auth-demo/README.md) is a runnable Kotlin Toolchain project: Argon2id password storage, self-issued JWTs and Ktor typed authentication. It follows the architecture chapter's manual DI split and links to [Ktor Full Stack RealWorld](https://github.com/nomisRev/ktor-full-stack-real-world) on the slides.

Run `./kotlin test` from `auth-demo`, then follow its README to set a random signing key and start the server. `demo.http` demonstrates register → login → `/me` and failure cases. The project uses Ktor **3.6.0**, which introduces the experimental typed-auth API, while the earlier build-tool snippets retain **3.5.2**. Its user repository is in memory; persistence and the wider account lifecycle are explicitly outside this teaching example.

## Optional tooling deck

The previous detailed tool-by-tool walkthrough is preserved in `slides.tooling.md`:

```bash
npm run dev:tooling    # serves the optional deck on port 3044
npm run build:tooling  # static output in dist-tooling/
```

This is an optional launch command; no presentation server is started automatically.

Its chapters are `lessons/formatting.md`, `analysis.md`, `coverage.md`, `plugins.md`, and `ci-operations.md`, composed by `lessons/ci.md`. Use it when questions call for:

- ktfmt vs ktlint, CLI/Gradle configuration, and the planned `kotlin format` command;
- detekt, type resolution and the trade-offs of heavier static analysis;
- Kover agent/CLI/Gradle verification and JaCoCo through Maven;
- the complete local Ktor/Jib plugin walkthrough;
- cache details and 0.12.2 JUnit filtering limits.

Speaker notes and `research.md` record sources and version caveats. The upcoming ktfmt-based `kotlin format` direction is supplied by Simon, with no release number promised. Keep the deeper tooling examples out of the core route unless the audience needs them.

## Switch to another deck

The final slide links to the published talks. To use local copies, start the chosen deck in a second terminal with its own port (install its dependencies first if needed):

```bash
cd ../ktor-fundamentals
npm run dev -- --port 3043
```

Or use the same command from `../kotlin-fundamentals` or `../advanced-dsl-in-kotlin`. Use the Slidev overview to jump to a section, rather than relying on slide numbers that may change.

| Follow-up | Suggested entry |
| --- | --- |
| Ktor Fundamentals | `lessons/lesson-1.md` for a first server; `lessons/lesson-8.md` for status pages, testing and metrics |
| Kotlin Fundamentals | `lessons/lesson-5.md` for extension functions and DSL mechanics |
| Advanced DSLs in Kotlin | `lessons/lesson-0.md` for receivers; `lessons/lesson-5.md` for context parameters |
| Opinionated Ktor Services | `lessons/lesson-3.md` for routes; `lessons/lesson-4.md` for dependency ownership |

The upcoming ktfmt-based `kotlin format` direction is supplied by Simon; it is marked as planned with no release number. Other roadmap dates remain unconfirmed. Recheck version-specific commands before a live demo.

## Local plugin demo

The [copied Ktor and Jib plugins](plugins/README.md) are part of this project. Start at `project.yaml`, then `auth-demo/module.yaml`, `plugins/ktor-plugin/plugin.yaml` and its `src/Docker.kt`. Both the opening Toolchain section and deployment section use these same files. The formatter plugin is omitted.

From this directory, `./kotlin check` runs the demo tests and `./kotlin do buildImage` creates its container tarball. See the [deployment walkthrough](examples/deployment/README.md) for publishing through Docker Desktop credentials and configuring Render. No image has been pushed or service deployed during preparation.
