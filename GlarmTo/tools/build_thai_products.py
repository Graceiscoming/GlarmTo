"""
Builds app/src/main/assets/thai_products.tsv: the products sold in Thailand from the Open Food Facts
database, reduced to what the barcode scanner needs (name, brand, serving size, calories and macros).

Open Food Facts (https://world.openfoodfacts.org) is a free, community-built food database. Its data is
available under the Open Database License (ODbL 1.0); see app/src/main/assets/OPEN_FOOD_FACTS_NOTICE.txt.

A product counts as Thai if it is tagged as sold in Thailand OR its barcode starts with 885 (the GS1
prefix for Thailand): many Thai products were scanned without a country tag.

Usage:
    # 1. download the full CSV export (about 1.3 GB):
    #    https://static.openfoodfacts.org/data/en.openfoodfacts.org.products.csv.gz
    # 2. build the asset:
    python tools/build_thai_products.py en.openfoodfacts.org.products.csv.gz [max_rows]

Output columns (tab separated, one product per line, most scanned first):
    code  name  brand  serving_g  kcal_100g  protein_100g  carbs_100g  fat_100g
The four nutrition columns are empty for products that have a name but no nutrition facts yet; the app
still shows the name, and the user fills in the calories.
"""

import csv
import gzip
import re
import sys

OUT_PATH = "app/src/main/assets/thai_products.tsv"
DEFAULT_MAX_ROWS = 30000
KJ_PER_KCAL = 4.184

csv.field_size_limit(10 ** 9)


def number(row, index):
    """A float from a CSV cell, or None if the cell is empty or not a number."""
    if index is None:
        return None
    value = row[index].strip() if index < len(row) else ""
    if not value:
        return None
    try:
        result = float(value)
    except ValueError:
        return None
    return result if result == result else None  # drop NaN


def text(row, index):
    return row[index].strip() if index is not None and index < len(row) else ""


def clean(value, limit):
    value = re.sub(r"[\t\r\n]+", " ", value).strip()
    return value[:limit].strip()


def kcal_from(row, cols):
    kcal = number(row, cols["energy-kcal_100g"])
    if kcal is not None:
        return kcal
    kj = number(row, cols["energy-kj_100g"])
    if kj is None:
        kj = number(row, cols["energy_100g"])  # this column is in kJ
    if kj is not None:
        return kj / KJ_PER_KCAL
    return None


def is_thai(row, cols, code):
    return code.startswith("885") or "en:thailand" in text(row, cols["countries_tags"]).split(",")


def best_name(row, cols, brand):
    for column in ("product_name", "generic_name", "abbreviated_product_name"):
        name = clean(text(row, cols[column]), 90)
        if len(name) >= 2:
            return name
    quantity = clean(text(row, cols["quantity"]), 20)
    if brand:
        return (brand + " " + quantity).strip()  # e.g. "Nestle 23.5 g": better than nothing
    return ""


def build(source, max_rows=DEFAULT_MAX_ROWS):
    """Reads the Open Food Facts CSV export; returns (output lines, statistics)."""
    examined = thai = 0
    dropped = {"no_name": 0, "bad_values": 0, "bad_code": 0}
    rows = {}

    with gzip.open(source, "rt", encoding="utf-8", errors="replace", newline="") as handle:
        reader = csv.reader(handle, delimiter="\t", quoting=csv.QUOTE_NONE)
        header = next(reader)
        index = {name: i for i, name in enumerate(header)}
        wanted = ["code", "product_name", "generic_name", "abbreviated_product_name", "brands", "quantity",
                  "countries_tags", "serving_quantity", "unique_scans_n", "energy-kcal_100g", "energy-kj_100g",
                  "energy_100g", "proteins_100g", "carbohydrates_100g", "fat_100g"]
        cols = {name: index.get(name) for name in wanted}
        missing = [n for n, i in cols.items() if i is None]
        if missing:
            raise ValueError("columns missing from the export: %s" % missing)

        for row in reader:
            examined += 1
            code = text(row, cols["code"])
            if not code or not is_thai(row, cols, code):
                continue
            thai += 1
            if not re.fullmatch(r"\d{8,14}", code):
                dropped["bad_code"] += 1
                continue

            brand = clean(text(row, cols["brands"]).split(",")[0], 30)
            name = best_name(row, cols, brand)
            if not name:
                dropped["no_name"] += 1
                continue

            protein = number(row, cols["proteins_100g"])
            carbs = number(row, cols["carbohydrates_100g"])
            fat = number(row, cols["fat_100g"])
            kcal = kcal_from(row, cols)
            if kcal is None and None not in (protein, carbs, fat):
                kcal = 4 * protein + 4 * carbs + 9 * fat

            nutrition = ["", "", "", ""]
            if kcal is not None:
                macros = [protein or 0.0, carbs or 0.0, fat or 0.0]
                if not (0 <= kcal <= 950) or any(m < 0 or m > 100 for m in macros) or sum(macros) > 105:
                    dropped["bad_values"] += 1  # implausible numbers: treat as "no nutrition data"
                else:
                    nutrition = ["%g" % round(kcal, 1)] + ["%g" % round(m, 1) for m in macros]

            serving = number(row, cols["serving_quantity"])
            serving_text = "%g" % round(serving, 1) if serving and 3 <= serving <= 2000 else ""
            scans = number(row, cols["unique_scans_n"]) or 0
            # products with nutrition first, then the most scanned
            rank = (1 if nutrition[0] else 0, scans)

            line = "\t".join([code, name, brand, serving_text] + nutrition)
            if code not in rows or rank > rows[code][0]:
                rows[code] = (rank, line)

    ranked = sorted(rows.values(), key=lambda item: item[0], reverse=True)[:max_rows]
    lines = [line for _, line in ranked]
    stats = {
        "examined": examined,
        "thai": thai,
        "dropped": dropped,
        "kept": len(lines),
        "with_nutrition": sum(1 for l in lines if l.split("\t")[4]),
        "with_serving": sum(1 for l in lines if l.split("\t")[3]),
    }
    return lines, stats


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(1)
    max_rows = int(sys.argv[2]) if len(sys.argv) > 2 else DEFAULT_MAX_ROWS
    lines, stats = build(sys.argv[1], max_rows)

    # Plain text on purpose: the Android build unzips and renames .gz assets, hiding the file from the app
    # (the APK compresses it anyway).
    with open(OUT_PATH, "w", encoding="utf-8", newline="\n") as out:
        for line in lines:
            out.write(line + "\n")

    print("rows examined:", stats["examined"])
    print("Thai products (tagged Thailand or barcode 885...):", stats["thai"])
    print("dropped:", stats["dropped"])
    print("kept:", stats["kept"], "->", OUT_PATH)
    print("  with nutrition facts:", stats["with_nutrition"])
    print("  name only:", stats["kept"] - stats["with_nutrition"])
    print("  with serving size:", stats["with_serving"])


if __name__ == "__main__":
    main()
