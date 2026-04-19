# gor-mobile-rules-default

Default rules pack for [`gor-mobile`](https://github.com/gorban/gor-mobile) — a CLI that
delegates Android code generation to local LLMs and wires a superpowers-style workflow
into Claude Code.

## Install

The pack is cloned automatically by `gor-mobile init`. To switch to this pack manually:

```sh
gor-mobile rules use https://github.com/gorban/gor-mobile-rules-default.git
```

## Layout

```
rules/               # Sectional rule references (loaded on demand by commands)
  core.md            # ALWAYS loaded — the hard contract (injected by SessionStart hook)
  architecture.md    # Layer structure and dependencies
  base-viewmodel.md  # BaseSharedViewModel<State, Action, Event> contract
  theme-system.md    # {App}Theme.colors / .typography rules
  modification.md    # MODIFY mode — safe extension of existing features
  testing-*.md       # Testable design, mocking strategy, anti-patterns
  debug-*.md         # Defense-in-depth vs. root-cause tracing

examples/            # Kotlin reference files, used as templates by /implement
  presentation/      # Screen, View, ViewModel, ViewState/Event/Action
  usecase/           # UseCase<Params, T>
  repository/        # I{Feature}Repository + impl
  data/              # DataSources, DTOs, persistence models
  di/                # Koin and Kodein module patterns

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

## Versioning

Follow SemVer. Breaking changes (renaming sections, removing example layers, changing
`core.md` shape) bump the major version. `manifest.compatible_with` tells gor-mobile
which CLI versions understand this pack.
