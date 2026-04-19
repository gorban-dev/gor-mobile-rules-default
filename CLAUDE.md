# gor-mobile-rules-default — инструкции для Claude

Rules-pack для CLI [gor-mobile](https://github.com/gorban-dev/gor-mobile).

## Структура (не менять без повода)

```
manifest.json          # version, compatible_with, sections map
rules/                 # секции (на них ссылаются slash-commands)
  core.md              # ВСЕГДА в контексте — hard contract
  architecture.md
  base-viewmodel.md
  theme-system.md
  modification.md
  testing-*.md
  debug-*.md
examples/              # Kotlin-эталоны по слоям
  presentation/  usecase/  repository/  data/  di/
examples/index.json    # используется /implement для подбора примеров
```

## При изменениях

**Добавил/удалил/переименовал секцию в `rules/`:**
1. Обновить `manifest.json` → `sections`.
2. Проверить, что ни один slash-command в CLI-репо не ссылается на старое имя.
3. Bump version в `manifest.json`:
   - Переименовал/удалил секцию → **major**.
   - Добавил новую секцию → **minor**.
   - Правки внутри секции → **patch**.

**Добавил пример в `examples/`:**
1. Обновить `examples/index.json`.
2. Bump minor.

**Изменил форму `core.md`** (структура заголовков, якоря, которые парсит CLI):
- Bump major + обновить `compatible_with` в `manifest.json`.

## Совместимость

`manifest.compatible_with` — SemVer range CLI-версий, которые понимают pack.
CLI при `rules update` откажется обновляться, если версия вне диапазона.
Всегда сверяйся с актуальной версией CLI в `gor-mobile-rules-default/manifest.json`.

## Tag после merge

Создать git-тег `vX.Y.Z` совпадающий с `manifest.json/version` — пользователи пинят pack по тегу.

## Naming

- Организация: **`gorban-dev/`**, не `gorban/`.
- Секции в `manifest.json/sections` — kebab-case.
- Примеры в `examples/` — PascalCase `.kt` файлы.

## Чеклист

- [ ] `manifest.json` актуален (sections, version, compatible_with)
- [ ] `examples/index.json` синхронен с содержимым `examples/`
- [ ] Bump согласно SemVer
- [ ] Ни один slash-command в gor-mobile не сломался от переименования
