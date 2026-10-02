---
theme: kotlin
title: Avans College — Ktor in practice
favicon: /ktor.svg
fonts:
  sans: JetBrains Sans
  mono: JetBrains Mono
  provider: none
  local:
    - JetBrains Sans
    - JetBrains Mono
transition: fade
layout: cover
class: college-cover
canvasWidth: 1440
aspectRatio: 16/9
colorSchema: both
highlighter: shiki
themeConfig:
  kodee: greeting
  drawnAnnotation:
    connect: false
kodee: welcome
---

# Ktor in practice

## From a local build to a tested backend

Simon Vergauwen · Avans College

<!--
The main route is Amper, Gradle, GitHub Actions, architecture, then authentication.
Amper means the Kotlin Toolchain 0.12.2 used in the students' question; commands use its ./kotlin wrapper.
Each build-tool intro has a 5-minute core and optional demo/detail to extend it to 15 minutes. See README.md for the exact cuts.
-->

---

# From your laptop to a pull request

1. **Toolchain** — describe, build and test a module
2. **GitHub Actions** — run either build on every PR
3. **Architecture** — make the backend easy to test
4. **Authentication** — Argon2id, JWTs, roles and a Ktor client

<!--
Opening motivation, spoken rather than a separate wall of questions: “Dan bouw ik liever niet zelf iets wat straks vanzelf komt.”
First understand each build locally. Then repeat it in CI. Then discuss the application boundaries that make those tests straightforward.
Keep detailed formatter/analysis/coverage/plugin comparisons available in slides.tooling.md, not as required detours in this route.
-->

---
src: ./lessons/amper-intro.md
---
---
src: ./lessons/github-actions.md
---
---
src: ./lessons/architecture.md
---
---
src: ./lessons/authentication.md
---
---
src: ./lessons/next.md
---
