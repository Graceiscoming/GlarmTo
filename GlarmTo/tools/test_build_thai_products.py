"""
Tests for build_thai_products.py. Run from the GlarmTo folder:
    python -m unittest discover -s tools -p "test_*.py"
They use a tiny made-up export, so they need neither the network nor the 1.3 GB file.
"""

import gzip
import os
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.dirname(__file__))
import build_thai_products as builder  # noqa: E402

COLUMNS = ["code", "product_name", "generic_name", "abbreviated_product_name", "brands", "quantity",
           "countries_tags", "serving_quantity", "unique_scans_n", "energy-kcal_100g", "energy-kj_100g",
           "energy_100g", "proteins_100g", "carbohydrates_100g", "fat_100g", "unrelated_column"]


def row(**values):
    return [str(values.get(name, "")) for name in COLUMNS]


def build(*rows, max_rows=1000, columns=COLUMNS):
    with tempfile.TemporaryDirectory() as folder:
        path = os.path.join(folder, "export.csv.gz")
        with gzip.open(path, "wt", encoding="utf-8", newline="") as f:
            f.write("\t".join(columns) + "\n")
            for r in rows:
                f.write("\t".join(r) + "\n")
        return builder.build(path, max_rows)


def fields(line):
    return line.split("\t")


class SelectionTest(unittest.TestCase):
    def test_a_product_tagged_thailand_is_kept(self):
        lines, stats = build(row(code="1234567890123", product_name="Snack", countries_tags="en:france,en:thailand",
                                 **{"energy-kcal_100g": 400}))
        self.assertEqual(1, len(lines))
        self.assertEqual(1, stats["thai"])

    def test_a_product_with_the_thai_barcode_prefix_is_kept_even_without_a_country_tag(self):
        lines, _ = build(row(code="8851717021008", product_name="Yoghurt", **{"energy-kcal_100g": 72}))
        self.assertEqual("8851717021008", fields(lines[0])[0])

    def test_other_countries_are_skipped(self):
        lines, stats = build(row(code="3046920028721", product_name="Chocolate", countries_tags="en:france",
                                 **{"energy-kcal_100g": 565}))
        self.assertEqual([], lines)
        self.assertEqual(0, stats["thai"])

    def test_tag_matching_is_exact(self):
        lines, _ = build(row(code="3046920028721", product_name="Chocolate", countries_tags="en:thailand-border",
                             **{"energy-kcal_100g": 565}))
        self.assertEqual([], lines)

    def test_bad_barcodes_are_dropped(self):
        lines, stats = build(
            row(code="885ABC", product_name="X", countries_tags="en:thailand"),
            row(code="8851234", product_name="Too short", countries_tags="en:thailand"),
            row(code="88512345678901234", product_name="Too long", countries_tags="en:thailand"),
        )
        self.assertEqual([], lines)
        self.assertEqual(3, stats["dropped"]["bad_code"])


class NameTest(unittest.TestCase):
    def name_of(self, **values):
        lines, _ = build(row(code="8851717021008", **values))
        return fields(lines[0])[1] if lines else None

    def test_product_name_is_preferred(self):
        self.assertEqual("Main", self.name_of(product_name="Main", generic_name="Generic"))

    def test_generic_name_is_the_fallback(self):
        self.assertEqual("Generic", self.name_of(generic_name="Generic"))

    def test_abbreviated_name_is_the_next_fallback(self):
        self.assertEqual("Abbr", self.name_of(abbreviated_product_name="Abbr"))

    def test_brand_and_quantity_are_used_as_a_last_resort(self):
        self.assertEqual("Nestle 23.5 g", self.name_of(brands="Nestle", quantity="23.5 g"))

    def test_a_brand_alone_is_enough(self):
        self.assertEqual("Nestle", self.name_of(brands="Nestle"))

    def test_no_name_at_all_is_dropped(self):
        lines, stats = build(row(code="8851717021008"))
        self.assertEqual([], lines)
        self.assertEqual(1, stats["dropped"]["no_name"])

    def test_the_first_brand_is_used_and_tabs_are_removed(self):
        lines, _ = build(row(code="8851717021008", product_name="Has  spaces", brands="First, Second"))
        f = fields(lines[0])
        self.assertEqual(8, len(f))
        self.assertEqual("Has  spaces", f[1])
        self.assertEqual("First", f[2])

    def test_tabs_and_newlines_inside_text_become_spaces(self):
        self.assertEqual("a b c d", builder.clean("a\tb\r\nc\n\nd", 90))

    def test_long_names_are_cut(self):
        lines, _ = build(row(code="8851717021008", product_name="x" * 500))
        self.assertLessEqual(len(fields(lines[0])[1]), 90)

    def test_thai_text_survives(self):
        lines, _ = build(row(code="8850393800440", product_name="นมเปรี้ยว บีทาเก้น", brands="บีทาเกน"))
        self.assertEqual(["นมเปรี้ยว บีทาเก้น", "บีทาเกน"], fields(lines[0])[1:3])


class NutritionTest(unittest.TestCase):
    def nutrition_of(self, **values):
        lines, _ = build(row(code="8851717021008", product_name="Prod", **values))
        return fields(lines[0])[4:8]

    def test_kcal_is_used_when_given(self):
        self.assertEqual(["72", "5", "19", "0"], self.nutrition_of(**{"energy-kcal_100g": 72, "proteins_100g": 5,
                                                                      "carbohydrates_100g": 19, "fat_100g": 0}))

    def test_kilojoules_are_converted(self):
        self.assertEqual("250", self.nutrition_of(**{"energy-kj_100g": 1046})[0])

    def test_plain_energy_is_kilojoules(self):
        self.assertEqual("100", self.nutrition_of(**{"energy_100g": 418.4})[0])

    def test_kcal_wins_over_kilojoules(self):
        self.assertEqual("300", self.nutrition_of(**{"energy-kcal_100g": 300, "energy-kj_100g": 9999})[0])

    def test_calories_come_from_the_macros_when_energy_is_missing(self):
        kcal = float(self.nutrition_of(**{"proteins_100g": 10, "carbohydrates_100g": 20, "fat_100g": 5})[0])
        self.assertAlmostEqual(4 * 10 + 4 * 20 + 9 * 5, kcal, places=1)

    def test_only_some_macros_do_not_invent_calories(self):
        self.assertEqual(["", "", "", ""], self.nutrition_of(**{"proteins_100g": 10}))

    def test_missing_macros_become_zero_when_calories_are_known(self):
        self.assertEqual(["100", "0", "0", "0"], self.nutrition_of(**{"energy-kcal_100g": 100}))

    def test_no_nutrition_leaves_the_columns_empty(self):
        self.assertEqual(["", "", "", ""], self.nutrition_of())

    def test_implausible_values_are_treated_as_no_nutrition(self):
        for bad in ({"energy-kcal_100g": 5000}, {"energy-kcal_100g": -5},
                    {"energy-kcal_100g": 100, "proteins_100g": 150},
                    {"energy-kcal_100g": 100, "proteins_100g": 60, "carbohydrates_100g": 60}):
            self.assertEqual(["", "", "", ""], self.nutrition_of(**bad), bad)

    def test_implausible_values_are_counted(self):
        _, stats = build(row(code="8851717021008", product_name="Prod", **{"energy-kcal_100g": 5000}))
        self.assertEqual(1, stats["dropped"]["bad_values"])

    def test_garbage_numbers_are_ignored(self):
        self.assertEqual(["", "", "", ""], self.nutrition_of(**{"energy-kcal_100g": "abc", "proteins_100g": "nan"}))

    def test_values_are_rounded_to_one_decimal(self):
        self.assertEqual("72.3", self.nutrition_of(**{"energy-kcal_100g": 72.34567})[0])


class ServingAndRankingTest(unittest.TestCase):
    def test_serving_size_is_kept_when_believable(self):
        lines, _ = build(row(code="8851717021008", product_name="Prod", serving_quantity="125"))
        self.assertEqual("125", fields(lines[0])[3])

    def test_silly_serving_sizes_are_dropped(self):
        for silly in ("0", "1", "5000", "abc", ""):
            lines, _ = build(row(code="8851717021008", product_name="Prod", serving_quantity=silly))
            self.assertEqual("", fields(lines[0])[3], silly)

    def test_products_with_nutrition_come_first_then_the_most_scanned(self):
        lines, _ = build(
            row(code="8850000000001", product_name="name only, popular", unique_scans_n=9000),
            row(code="8850000000002", product_name="nutrition, few scans", unique_scans_n=1, **{"energy-kcal_100g": 100}),
            row(code="8850000000003", product_name="nutrition, many scans", unique_scans_n=500, **{"energy-kcal_100g": 100}),
        )
        self.assertEqual(["8850000000003", "8850000000002", "8850000000001"], [fields(l)[0] for l in lines])

    def test_a_duplicate_code_keeps_the_better_row(self):
        lines, _ = build(
            row(code="8850000000001", product_name="without nutrition", unique_scans_n=100),
            row(code="8850000000001", product_name="with nutrition", unique_scans_n=1, **{"energy-kcal_100g": 50}),
        )
        self.assertEqual(1, len(lines))
        self.assertEqual("with nutrition", fields(lines[0])[1])

    def test_the_row_limit_is_applied_after_ranking(self):
        lines, stats = build(
            row(code="8850000000001", product_name="aaa", unique_scans_n=1),
            row(code="8850000000002", product_name="bbb", unique_scans_n=2, **{"energy-kcal_100g": 1}),
            row(code="8850000000003", product_name="ccc", unique_scans_n=3),
            max_rows=2,
        )
        self.assertEqual(2, len(lines))
        self.assertEqual("8850000000002", fields(lines[0])[0])
        self.assertEqual(2, stats["kept"])

    def test_every_line_has_eight_columns(self):
        lines, _ = build(row(code="8851717021008", product_name="Prod"), row(code="8851717021015", product_name="Quad",
                                                                          **{"energy-kcal_100g": 5}))
        for l in lines:
            self.assertEqual(8, len(fields(l)))

    def test_statistics_add_up(self):
        _, stats = build(
            row(code="8850000000001", product_name="aaa", **{"energy-kcal_100g": 1}, serving_quantity=30),
            row(code="8850000000002", product_name="bbb"),
            row(code="1111111111116", product_name="not thai", countries_tags="en:france"),
        )
        self.assertEqual(3, stats["examined"])
        self.assertEqual(2, stats["thai"])
        self.assertEqual(2, stats["kept"])
        self.assertEqual(1, stats["with_nutrition"])
        self.assertEqual(1, stats["with_serving"])


class InputTest(unittest.TestCase):
    def test_a_missing_column_is_reported(self):
        with self.assertRaises(ValueError):
            build(row(code="8851717021008"), columns=[c for c in COLUMNS if c != "fat_100g"])

    def test_short_rows_do_not_crash_and_keep_what_they_have(self):
        with tempfile.TemporaryDirectory() as folder:
            path = os.path.join(folder, "export.csv.gz")
            with gzip.open(path, "wt", encoding="utf-8", newline="") as f:
                f.write("\t".join(COLUMNS) + "\n")
                f.write("8851717021008\tshort\n")
            lines, _ = builder.build(path, 10)
        self.assertEqual(["8851717021008\tshort\t\t\t\t\t\t"], lines)

    def test_an_empty_export_gives_nothing(self):
        lines, stats = build()
        self.assertEqual([], lines)
        self.assertEqual(0, stats["examined"])


if __name__ == "__main__":
    unittest.main()
