# gor-mobile-rules-default

[![pack version](https://img.shields.io/github/v/tag/gorban-dev/gor-mobile-rules-default?label=pack&color=blue)](https://github.com/gorban-dev/gor-mobile-rules-default/tags)
[![license](https://img.shields.io/github/license/gorban-dev/gor-mobile-rules-default)](./LICENSE)
[![compat](https://img.shields.io/badge/gor--mobile-%E2%89%A50.1.0%20%3C2.0.0-informational)](https://github.com/gorban-dev/gor-mobile)
[![stack](https://img.shields.io/badge/stack-android-green)]()

Default rules pack for [`gor-mobile`](https://github.com/gorban-dev/gor-mobile) — a CLI that
delegates Android code generation to local LLMs and wires a superpowers-style workflow
into Claude Code.

## Install

The pack is cloned automatically by `gor-mobile init`. To switch to this pack manually:

```sh
gor-mobile rules use https://github.com/gorban-dev/gor-mobile-rules-default.git
```

## Layout

```
rules/               # Sectional rule references (loaded on demand by commands)
  core.md            # ALWAYS loaded — the hard contract (injected by SessionStart hook)
  architecture.md    # Layer structure and dependencies
  base-viewmodel.md  # BaseSharedViewModel<State, Action, Event> contract
  error-handling.md  # ApiClient -> ApiResult/ApiError -> errors in ViewState
  theme-system.md    # {App}Theme.colors / .typography rules
  modification.md    # MODIFY mode — safe extension of existing features
  testing-*.md       # Testable design, mocking strategy, anti-patterns
  debug-*.md         # Defense-in-depth vs. root-cause tracing

examples/            # Kotlin reference files, used as templates by /implement
  presentation/      # Screen, View, ViewModel, ViewState/Event/Action, UiText/Content/UserMessage, LightDarkPreview
  usecase/           # UseCase<Params, T> (operator invoke), domain models
  repository/        # I{Feature}Repository + impl
  data/              # DataSources on ApiClient, Ktor models, mappers
  network/           # ApiClient, ApiResult, ApiError
  di/                # Koin module (Compiler Plugin DSL)

manifest.json        # name, version, compat range, section → file map
examples/index.json  # layer → file list, used by /implement for example selection
```

## Forking

Teams typically fork this pack and substitute company-specific names / patterns. Point
`gor-mobile` at the fork:

```sh
gor-mobile rules use git@github.com:my-company/gor-mobile-rules-corp.git
```

Keep `manifest.json` valid (run `gor-mobile rules validate`) and preserve the section
names listed in `sections` — commands reference those by key.

## Migrating to 1.1.0

- Errors are values: DataSources use `ApiClient` and return `ApiResult<T>`; see `rules/error-handling.md`.
- UseCase: `suspend operator fun invoke` instead of `execute`.
- The Kodein example is removed. Kodein projects: migrate with the `di-migration` skill from
  [`InsertKoinIO/koin-skills`](https://github.com/InsertKoinIO/koin-skills), or pin the pack to `v1.0.0`.

## Versioning

Follow SemVer. Breaking changes (renaming sections, removing example layers, changing
`core.md` shape) bump the major version. `manifest.compatible_with` tells gor-mobile
which CLI versions understand this pack.
