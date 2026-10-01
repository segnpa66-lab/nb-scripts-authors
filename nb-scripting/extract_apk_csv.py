#!/usr/bin/env python3
"""Build a lookup-name catalogue from a Nulls Brawl APK.

Usage: python tools/extract_apk_csv.py /path/to/base.apk
Reads only CSV members from the ZIP; does not unpack the whole APK.
"""
from __future__ import annotations

import argparse
import csv
import io
import json
import re
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent
GROUPS = {
    6: ("projectiles_logic", "projectiles_skin"),
    15: ("locations",),
    16: ("characters",),
    17: ("area_effects_logic", "area_effects_skin"),
    18: ("items_logic", "items_skin", "items"),
    20: ("skills",),
    23: ("cards",),
    27: ("tiles",),
    29: ("skins",),
    50: ("accessories",),
    52: ("emotes",),
    68: ("sprays",),
    108: ("traits",),
    117: ("status_effects_logic", "status_effects_skin"),
}
ATTACK_ORIGINS = (
    "ACCESSORY", "COMPONENT", "EXTERNAL", "GAME_MODE", "INCOMING_DAMAGE",
    "MODIFIER", "NEW_ROUND", "OVERCHARGE", "OVERCHARGE_ABILITY",
    "PASSIVE_CHARGING", "PASSIVE_HEALING", "REDIRECT_SHIELD", "STAR_POWER",
    "ULTI", "UNUSED_14", "UNUSED_15", "UNUSED_16", "UNUSED_17",
    "UNUSED_9", "WEAPON",
)
GAME_MODES = (
    "AIR_HOCKEY", "AIR_HOCKEY_2_V_2", "AIR_HOCKEY_5_V_5", "ARENA",
    "BASKET_BRAWL", "BASKET_BRAWL_2_V_2", "BOUNTY", "BRAWL_BALL",
    "DUO_SHOWDOWN", "GEM_GRAB", "GEM_GRAB_2_V_2", "GEM_GRAB_5_V_5",
    "HEIST", "KNOCKOUT", "KNOCKOUT_2_V_2", "KNOCKOUT_5_V_5",
    "SHOWDOWN", "TRIO_SHOWDOWN",
)


def enum_key(value: str) -> str:
    value = re.sub(r"([a-z0-9])([A-Z])", r"\1_\2", value)
    value = re.sub(r"([A-Z])([A-Z][a-z])", r"\1_\2", value)
    value = re.sub(r"([A-Za-z])([0-9])", r"\1_\2", value)
    value = re.sub(r"([0-9])([A-Za-z])", r"\1_\2", value)
    return re.sub(r"[^A-Za-z0-9]+", "_", value).upper().strip("_")


def column_values(archive: zipfile.ZipFile, filename: str, column: str) -> set[str]:
    text = archive.read(f"assets/csv_logic/{filename}.csv").decode("utf-8-sig")
    rows = csv.DictReader(io.StringIO(text))
    next(rows, None)  # Supercell type hints
    return {row[column] for row in rows if row.get(column)}


def extract(apk: Path) -> dict:
    catalogue: dict[str, list[str]] = {}
    files: dict[str, list[str]] = {}
    with zipfile.ZipFile(apk) as archive:
        members = set(archive.namelist())
        for type_id, names in GROUPS.items():
            values: set[str] = set()
            used: list[str] = []
            for name in names:
                member = f"assets/csv_logic/{name}.csv"
                if member not in members:
                    continue
                used.append(name + ".csv")
                text = archive.read(member).decode("utf-8-sig")
                rows = csv.reader(io.StringIO(text))
                header = next(rows, [])
                if not header or header[0] != "Name":
                    raise ValueError(f"Unexpected first column in {member}")
                next(rows, None)  # Supercell type hints
                values.update(row[0] for row in rows if row and row[0])
            if not values:
                raise ValueError(f"No CSV names found for lookup type {type_id}")
            catalogue[str(type_id)] = sorted(values)
            files[str(type_id)] = used
        characters = {enum_key(value) for value in column_values(archive, "characters", "Name")
                      if not re.fullmatch(r"[0-9a-f]{40}", value)}
        traits = {enum_key(value) for value in column_values(archive, "traits", "Type")}
        available_modes = {enum_key(value) for value in
                           column_values(archive, "game_mode_variations", "Name")}
        if not set(GAME_MODES).issubset(available_modes):
            raise ValueError("Expected game modes are missing in this APK")
        enums = {"AttackOrigin": sorted(ATTACK_ORIGINS),
                 "CharacterType": sorted(characters),
                 "TraitType": sorted(traits),
                 "GameMode": sorted(GAME_MODES)}
    return {"format": 1, "files": files,
            "names": catalogue, "enums": enums}


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("apk", type=Path)
    parser.add_argument("--version", required=True)
    parser.add_argument("--output", type=Path, default=ROOT / "lookup-names.json")
    args = parser.parse_args()
    data = extract(args.apk)
    data["gameVersion"] = args.version
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(data, ensure_ascii=False, separators=(",", ":")) + "\n",
                           encoding="utf-8")
    print(f"Extracted {sum(map(len, data['names'].values()))} names for {len(data['names'])} lookup types")


if __name__ == "__main__":
    main()
