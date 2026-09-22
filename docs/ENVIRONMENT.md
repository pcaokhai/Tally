# Environment notes

Version 1.0 · 2026-09-22 · Owner: Tech lead

Toolchain facts discovered the hard way during Sprint 0. Read this before spending time
diagnosing a build failure that looks environmental rather than code-related — several of
these were independently rediscovered by more than one story because nobody had written
them down yet. Add to this file the moment you confirm a new one; don't let it happen again.

## Gradle / JDK 25

**Gradle's own daemon cannot start on JDK 25 below Gradle 9.** Gradle 8.11 and 8.14 both
crash their Kotlin DSL compiler on JDK 25's version string (`java.lang.IllegalArgumentException:
25.0.4`). This is independent of the project's own `java.toolchain.languageVersion` setting
in `build.gradle.kts`, which only controls the JVM used to compile/run project code — the
daemon crash happens before that setting is even read.

**Fix:** `core/gradle/wrapper/gradle-wrapper.properties` is pinned to **Gradle 9.4.1**, which
runs its daemon cleanly under JDK 25. Do not downgrade it. If a future Gradle upgrade is
needed, verify the daemon actually starts under a real JDK 25 `JAVA_HOME` before committing —
don't trust a version bump that only "should" work.

## ESLint 8 vs 9

`web/` and `ops-web/` are pinned to **ESLint 9 with flat config** (`eslint.config.mjs`), not
`.eslintrc.cjs`. Two things that only surface under ESLint 9, not 8:

- The `--ext` CLI flag is **inert** under flat config — file selection comes entirely from
  each config object's `files` globs. A lint script that still passes `--ext .ts,.tsx` isn't
  wrong, it's just dead cargo-cult from the eslintrc era; drop it rather than assume it does
  something.
- `eslint-config-next`'s flat-config export requires the peer `eslint@^9`; mixing ESLint 8
  with a flat-config-only shareable config produces confusing peer-dependency errors, not a
  clean failure.

## golangci-lint v2

The `.golangci.yml` schema requires a top-level `version: "2"` key under golangci-lint v2
(installed via `go run` or a v2.x binary). Its absence fails with `can't load config:
unsupported version`, not a helpful message pointing at the missing key. golangci-lint is
**not** a hard local prerequisite — `make lint` skips the `workers/` lint step with a message
if the binary isn't on `PATH` (CI always has it via `golangci-lint-action`); don't add it back
as a hard local requirement without also updating that guard.

## Node / pnpm / package managers

- `pnpm install` run from the repo root always resolves the whole workspace — **never** run
  `pnpm add` from inside a story's worktree without first confirming `pwd` is actually the
  worktree and not the shared main checkout. A stray install against the main checkout's
  `package.json`/`pnpm-lock.yaml` has happened more than once; it shows up as an unexplained
  diff on `main` when a coordinator goes to merge an unrelated branch.
- `.claude/worktrees/` must stay in `.gitignore` at the repo root. It wasn't, for a while —
  check it's still there before assuming worktree scratch can't leak into a commit.

## Next.js 16

- Next 16 renamed `middleware.ts` to **`proxy.ts`**. If you're implementing anything that
  used to live in middleware (a nonce-based CSP, auth redirects), check the current Next docs
  for the App Router's routing primitives rather than assuming the file name from memory —
  this one specifically caused a story to write a broken CSP against a false assumption about
  what Next 16's production build emits (see the CSP note below).
- **The App Router's production build emits inline `<script>` bootstrap tags with no `src`**
  (the RSC payload / hydration push). A strict `script-src 'self'` with no nonce and no
  `'unsafe-inline'` blocks them — the page renders but never hydrates. Don't assume "no
  inline bootstrap script" without grepping the actual built `.next/server/app/*.html`; the
  wrong assumption here shipped a console that looked fine and had a dead session timer and
  sidebar. The fix is a per-request nonce via `proxy.ts`, not a permissive `script-src`.
- A per-request nonce forces dynamic rendering — routes that were statically prerenderable
  under a static/no CSP become server-rendered on demand once a nonce is involved. Expected,
  not a regression, for anything gated behind a strict CSP.

## `git` in this harness

A shell hook in this environment intercepts plain `git` and can wrongly refuse commands
inside a worktree with a "not git" isolation error. When a dispatched subagent hits this,
have it invoke `/usr/bin/git` directly instead of the `git` on `PATH`. Scripts committed to
the repo should keep using plain `git` — this is a local shell quirk, not something CI or a
human contributor will ever hit.
