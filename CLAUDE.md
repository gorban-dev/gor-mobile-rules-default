# gor-mobile-rules-default — instructions for Claude

Rules pack for the [gor-mobile](https://github.com/gorban-dev/gor-mobile) CLI.

## Structure (do not change without reason)

```
manifest.json          # version, compatible_with, sections map
rules/                 # sections (referenced by slash commands)
  core.md              # ALWAYS in context — hard contract
  architecture.md
  base-viewmodel.md
  theme-system.md
  modification.md
  testing-*.md
  debug-*.md
examples/              # Kotlin references per layer
  presentation/  usecase/  repository/  data/  di/
examples/index.json    # used by /implement to pick examples
```

## When making changes

**Added/removed/renamed a section in `rules/`:**
1. Update `manifest.json` → `sections`.
2. Verify no slash command in the CLI repo references the old name.
3. Bump version in `manifest.json`:
   - Renamed/removed section → **major**.
   - Added a new section → **minor**.
   - In-section edits → **patch**.

**Added an example under `examples/`:**
1. Update `examples/index.json`.
2. Bump minor.

**Changed the shape of `core.md`** (heading structure, anchors parsed by the CLI):
- Bump major + update `compatible_with` in `manifest.json`.

## Compatibility

`manifest.compatible_with` — SemVer range of CLI versions that understand this pack.
On `rules update` the CLI refuses to update if the version falls outside the range.
Always cross-check with the current CLI version in `gor-mobile-rules-default/manifest.json`.

## Tag after merge

Create a git tag `vX.Y.Z` matching `manifest.json/version` — users pin the pack by tag.

## Naming

- Org: **`gorban-dev/`**, not `gorban/`.
- Sections in `manifest.json/sections` — kebab-case.
- Examples in `examples/` — PascalCase `.kt` files.

## Checklist

- [ ] `manifest.json` up to date (sections, version, compatible_with)
- [ ] `examples/index.json` in sync with `examples/` contents
- [ ] Bump per SemVer
- [ ] No slash command in gor-mobile broke from a rename
