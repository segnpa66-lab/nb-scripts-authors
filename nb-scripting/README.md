# NB Scripting declarations for Nulls Brawl 69.252

[`types.d.luau`](types.d.luau) is generated from the [official NB Scripting reference](https://github.com/nulls-mods-community/scripting-docs/blob/main/reference.md) and data from an installed Nulls Brawl 69.252 APK. It contains 32 documented API classes, 157 fields and methods, 853 members in four enums (`AttackOrigin`, `CharacterType`, `TraitType`, `GameMode`), readonly and nullable annotations, typed `lookup()` / `createObject()` overloads, and 15,650 internal names across 14 `lookup()` types. [`lookup-names.json`](lookup-names.json) holds the lookup names and enum values; [`api-schema.json`](api-schema.json) supports IDE completion and reference pages.

The names are tied to **game version 69.252**. Regenerate them after a game update, otherwise valid new names may be missing. Character and trait enum members are reconstructed from game CSV data. Attack origins and supported game modes are explicit sets in the extraction script; game mode names are checked against the APK. The declaration file has no inline source tags. Its size mainly comes from full CSV name unions and documentation comments; size alone does not establish better type accuracy.

To regenerate from a matching installed APK:

```sh
python3 extract_apk_csv.py /path/to/base.apk --version 69.252
python3 generate_api.py
```

The generator downloads the [pinned official reference](https://github.com/nulls-mods-community/scripting-docs/blob/284923726dedaf143b242e577e3010361a5f48a1/reference.md) for reproducible output. To use newer API docs, place a `reference.md` beside `generate_api.py`. The scripts use only Python's standard library. No declaration code is copied from NBL.
