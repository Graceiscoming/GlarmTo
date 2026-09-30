package com.example.glarmto

import com.example.glarmto.data.util.BarcodeNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BarcodeNormalizerTest {

    @Test
    fun `an EAN-13 stays as it is`() {
        assertEquals("8850999320052", BarcodeNormalizer.normalize("8850999320052"))
    }

    @Test
    fun `an EAN-8 stays as it is`() {
        assertEquals("96385074", BarcodeNormalizer.normalize("96385074"))
    }

    @Test
    fun `a 12 digit UPC-A gets a leading zero`() {
        assertEquals("0012000001086", BarcodeNormalizer.normalize("012000001086"))
    }

    @Test
    fun `a 14 digit GTIN with a leading zero becomes EAN-13`() {
        assertEquals("8850999320052", BarcodeNormalizer.normalize("08850999320052"))
    }

    @Test
    fun `a 14 digit GTIN that really uses 14 digits is kept`() {
        assertEquals("18850999320059", BarcodeNormalizer.normalize("18850999320059"))
    }

    @Test
    fun `surrounding whitespace is ignored`() {
        assertEquals("8850999320052", BarcodeNormalizer.normalize("  8850999320052\n"))
    }

    @Test
    fun `things that are not product barcodes are rejected`() {
        listOf(
            null, "", "   ", "abc", "885099932005A", "8850-9993-2005", "https://example.com/8850999320052",
            "1234567", "123456789", "12345678901", "123456789012345", "88509993200523456"
        ).forEach { assertNull("should be rejected: $it", BarcodeNormalizer.normalize(it)) }
    }

    @Test
    fun `candidates start with the canonical form`() {
        assertEquals("8850999320052", BarcodeNormalizer.candidates("8850999320052").first())
    }

    @Test
    fun `a Thai barcode has just one spelling to try`() {
        assertEquals(listOf("8850999320052"), BarcodeNormalizer.candidates("8850999320052"))
    }

    @Test
    fun `a UPC-A is tried in both its 13 and 12 digit spellings`() {
        assertEquals(listOf("0012000001086", "012000001086"), BarcodeNormalizer.candidates("012000001086"))
        assertEquals(listOf("0012000001086", "012000001086"), BarcodeNormalizer.candidates("0012000001086"))
    }

    @Test
    fun `candidates never repeat`() {
        listOf("8850999320052", "012000001086", "96385074", "08850999320052").forEach {
            val list = BarcodeNormalizer.candidates(it)
            assertEquals(list.size, list.toSet().size)
        }
    }

    @Test
    fun `no candidates for something that is not a barcode`() {
        assertTrue(BarcodeNormalizer.candidates("hello").isEmpty())
        assertTrue(BarcodeNormalizer.candidates(null).isEmpty())
        assertTrue(BarcodeNormalizer.candidates("").isEmpty())
    }
}
