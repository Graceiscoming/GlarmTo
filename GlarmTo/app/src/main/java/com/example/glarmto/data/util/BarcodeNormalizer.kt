package com.example.glarmto.data.util

/**
 * Barcodes reach the app in different shapes: EAN-13 (almost every Thai product), EAN-8 for small packs,
 * UPC-A (12 digits, imported goods) and sometimes GTIN-14. A product may be stored under any of them, so a
 * lookup tries the plausible spellings of the same number.
 */
object BarcodeNormalizer {

    /** The canonical form (8 or 13 digits), or null if [raw] is not a product barcode at all. */
    fun normalize(raw: String?): String? {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty() || !text.all { it in '0'..'9' }) return null
        return when (text.length) {
            8, 13 -> text
            12 -> "0$text"
            14 -> if (text.startsWith("0")) text.drop(1) else text
            else -> null
        }
    }

    /** Every spelling worth trying for [raw], best first; empty if it is not a product barcode. */
    fun candidates(raw: String?): List<String> {
        val canonical = normalize(raw) ?: return emptyList()
        val variants = mutableListOf(canonical)
        if (canonical.length == 13 && canonical.startsWith("0")) {
            variants.add(canonical.drop(1)) // the UPC-A spelling some databases keep
        }
        return variants.distinct()
    }
}
