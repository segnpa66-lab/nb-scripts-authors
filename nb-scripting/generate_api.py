#!/usr/bin/env python3
"""Generate NB Luau declarations and an IDE schema from the official Markdown API.

The parser reads the documented signatures instead of copying another project's
declarations. Unknown enum members stay unknown; CSV names are validated by the
IDE's separately updated catalogue.
"""
from __future__ import annotations

import html
import json
import re
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent
SOURCE = ROOT / "reference.md"
REFERENCE_URL = "https://raw.githubusercontent.com/nulls-mods-community/scripting-docs/284923726dedaf143b242e577e3010361a5f48a1/reference.md"
OUT = ROOT
CATALOG = OUT / "lookup-names.json"

LINK = re.compile(r"\[([^]]+)\]\([^)]*\)")
TAG = re.compile(r"<[^>]+>")
METHOD = re.compile(r"^\((.*)\)\s*⇒\s*(.+)$")

PARENTS = {
    "LogicCharacter": "LogicGameObject",
    "LogicProjectile": "LogicGameObject",
    "LogicAreaEffect": "LogicGameObject",
    "LogicItem": "LogicGameObject",
    "GameObjectData": "Data",
    "ProjectileData": "GameObjectData",
    "CharacterData": "GameObjectData",
    "ItemData": "GameObjectData",
    "AreaEffectData": "GameObjectData",
    "TileData": "Data",
    "SkillData": "Data",
    "LocationData": "Data",
    "SkinData": "Data",
    "StatusEffectData": "Data",
    "AccessoryData": "Data",
    "TraitData": "Data",
}
ITERABLE_RETURNS = {
    "getCharacters": "Iterable<LogicCharacter>",
    "getAreaEffects": "Iterable<LogicAreaEffect>",
    "getItems": "Iterable<LogicItem>",
    "getProjectiles": "Iterable<LogicProjectile>",
}
LIST_FIELDS = {
    "takingDamageListeners": "JavaList<DamageEventListener>",
    "dealingDamageListeners": "JavaList<DamageEventListener>",
    "deathListeners": "JavaList<SourceListener>",
    "skillUseListeners": "JavaList<SkillEventListener>",
    "startOverchargeListeners": "JavaList<BasicListener>",
}
OPTIONAL_FIELDS = {
    "locationData", "linkedCharacter", "shotCharacter", "triggeredCharacter",
    "ownerCharacter", "accessory",
}
OPTIONAL_RESULTS = {"getObject", "getPet", "getSkill", "getComponent", "getTile",
                    "addStatusEffect", "addStatusEffectSelf"}
ENUMS = {"AttackOrigin", "CharacterType", "TraitType", "GameMode"}
LOOKUP_RESULTS = {
    6: "ProjectileData", 15: "LocationData", 16: "CharacterData",
    17: "AreaEffectData", 18: "ItemData", 20: "SkillData",
    23: "Data", 27: "TileData", 29: "SkinData",
    50: "AccessoryData", 52: "Data", 68: "Data",
    108: "TraitData", 117: "StatusEffectData",
}


def plain(value: str) -> str:
    value = LINK.sub(r"\1", value)
    value = TAG.sub("", value)
    value = html.unescape(value)
    return value.replace("…", "").replace("**", "").strip()


def luau_type(value: str) -> str:
    value = plain(value).replace("^(readonly)", "").replace("(readonly)", "").strip()
    value = re.sub(r"\bvoid\b", "()", value)
    value = re.sub(r"\bObject\b", "any", value)
    value = re.sub(r"\bList\b", "JavaList<any>", value)
    value = re.sub(r"\bIterable\b", "Iterable<any>", value)
    return value or "any"


def parse_reference(markdown: str) -> dict:
    classes: dict[str, dict] = {}
    name = None
    mode = None
    for line in markdown.splitlines():
        if line.startswith("# "):
            name = line[2:].strip()
            classes[name] = {"description": "", "parent": PARENTS.get(name),
                             "fields": [], "methods": []}
            mode = None
        elif line.startswith("### Поля"):
            mode = "fields"
        elif line.startswith("### Методы"):
            mode = "methods"
        elif name and line.startswith("|") and mode:
            cells = [plain(cell) for cell in line.split("|")[1:-1]]
            if len(cells) < 3 or not cells[0] or cells[0] in {"Название", "Название "}:
                continue
            if all(set(cell) <= {"-", ":", " "} for cell in cells):
                continue
            item_name, signature, description = cells[:3]
            item_name = re.sub(r"\s*\[\[.*$", "", item_name).strip()
            if not re.fullmatch(r"[A-Za-z_]\w*", item_name):
                continue
            classes[name][mode].append({
                "name": item_name,
                "signature": signature,
                "description": description,
                "readonly": "(readonly)" in signature,
            })
        elif name and mode is None and line.strip() and not line.startswith(("#", "<", "---")):
            classes[name]["description"] += (" " if classes[name]["description"] else "") + plain(line)
    return classes


def method_type(owner: str, item: dict) -> str:
    match = METHOD.match(item["signature"])
    if not match:
        return f"(self: {owner}) -> any"
    args, result = match.groups()
    args = args.strip()
    result = ITERABLE_RETURNS.get(item["name"], luau_type(result))
    if item["name"] in OPTIONAL_RESULTS and not result.endswith("?"):
        result += "?"
    result = result.replace("JavaList<any>", "JavaList<any>")
    parts = []
    if args:
        for arg in args.split(","):
            if ":" in arg:
                key, value = arg.split(":", 1)
                parts.append(f"{key.strip()}: {luau_type(value)}")
            else:
                parts.append(luau_type(arg))
    return f"(self: {owner}{', ' if parts else ''}{', '.join(parts)}) -> {result}"


def field_type(item: dict) -> str:
    value = LIST_FIELDS.get(item["name"], luau_type(item["signature"]))
    if item["name"] in OPTIONAL_FIELDS and not value.endswith("?"):
        value += "?"
    return value


def generate(classes: dict, catalog: dict | None = None) -> str:
    lines = [
        "--!strict",
        "-- Generated from nulls-mods-community/scripting-docs/reference.md",
        "-- Run: python generate_api.py",
        "-- Runtime values are Java userdata; these are editor declarations only.",
        "",
        "type Iterable<T> = { T }",
        "type JavaList<T> = {",
        "    get: (self: JavaList<T>, index: number) -> T,",
        "    add: (self: JavaList<T>, item: T) -> boolean,",
        "    size: (self: JavaList<T>) -> number,",
        "    indexOf: (self: JavaList<T>, item: T) -> number,",
        "}",
        "type DamageEventListener = (source: LogicCharacter, projectile: LogicProjectile?, damage: number, data: Data?, origin: AttackOrigin) -> ()",
        "type SkillEventListener = (skill: Skill) -> ()",
        "type SourceListener = (origin: AttackOrigin) -> ()",
        "type BasicListener = () -> ()",
        "",
    ]
    for name in classes:
        if name in ENUMS:
            # The public reference does not list every enum constant.
            lines.extend([f"type {name} = {{ [string]: any }}", f"declare {name}: {name}", ""])
            continue
        if name == "ArrayList":
            continue
        cls = classes[name]
        parent = cls["parent"]
        if cls["description"]:
            lines.append("-- " + cls["description"][:180])
        prefix = f"{parent} & " if parent else ""
        lines.append(f"type {name} = {prefix}{{")
        for field in cls["fields"]:
            if field["description"]:
                lines.append("    -- " + field["description"].replace("\n", " "))
            access = "read " if field["readonly"] else ""
            lines.append(f"    {access}{field['name']}: {field_type(field)},")
        for method in cls["methods"]:
            if method["description"]:
                lines.append("    -- " + method["description"].replace("\n", " "))
            lines.append(f"    {method['name']}: {method_type(name, method)},")
        lines.extend(["}", f"declare {name}: {name}", ""])
    names = (catalog or {}).get("names", {})
    if names:
        lines.extend([
            f"-- Internal lookup names from Nulls Brawl {catalog.get('gameVersion', 'unknown')}",
            "-- For another game version, regenerate from its APK.", "",
        ])
        for type_id, values in names.items():
            if int(type_id) not in LOOKUP_RESULTS:
                continue
            lines.append(f"type CsvName{type_id} =")
            for value in values:
                lines.append("    | " + json.dumps(value, ensure_ascii=False))
            lines.append("")
    lines.extend([
        "declare server: Server",
        "declare lookup: " + "\n    & ".join(
            f"((type: {type_id}, name: {'CsvName' + str(type_id) if str(type_id) in names else 'string'}) -> {result}?)"
            for type_id, result in LOOKUP_RESULTS.items()),
        "    & ((type: number, name: string) -> Data?)",
        "declare createObject: ((data: CharacterData) -> LogicCharacter)",
        "    & ((data: ProjectileData) -> LogicProjectile)",
        "    & ((data: AreaEffectData) -> LogicAreaEffect)",
        "    & ((data: ItemData) -> LogicItem)",
        "declare createCallback: ((class: \"DamageEventListener\", callback: DamageEventListener) -> any)",
        "    & ((class: \"SkillEventListener\", callback: SkillEventListener) -> any)",
        "    & ((class: \"SourceListener\", callback: SourceListener) -> any)",
        "    & ((class: \"BasicListener\", callback: BasicListener) -> any)",
        "declare function log(...: any): ()",
        "declare json: { encode: (value: any) -> string, decode: (value: string) -> any }",
        "",
    ])
    return "\n".join(lines)


def main() -> None:
    if SOURCE.exists():
        reference = SOURCE.read_text(encoding="utf-8")
    else:
        with urllib.request.urlopen(REFERENCE_URL, timeout=20) as response:
            reference = response.read().decode("utf-8")
    classes = parse_reference(reference)
    catalog = json.loads(CATALOG.read_text(encoding="utf-8")) if CATALOG.exists() else None
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / "api-schema.json").write_text(
        json.dumps({"source": "nulls-mods-community/scripting-docs",
                    "classes": classes}, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8")
    (OUT / "types.d.luau").write_text(generate(classes, catalog), encoding="utf-8")
    print(f"Generated {len(classes)} classes")


if __name__ == "__main__":
    main()
