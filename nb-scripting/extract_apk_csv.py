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
    return {"format": 1, "source": "installed Nulls Brawl APK", "files": files,
            "names": catalogue}


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
