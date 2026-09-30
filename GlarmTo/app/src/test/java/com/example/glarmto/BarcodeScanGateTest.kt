package com.example.glarmto

import com.example.glarmto.data.util.BarcodeScanGate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BarcodeScanGateTest {

    @Test
    fun `first barcode may start a lookup`() {
        val gate = BarcodeScanGate()

        assertTrue(gate.tryBegin("8851234567890"))
        assertTrue(gate.isBusy)
    }

    @Test
    fun `blank or null barcodes never start a lookup`() {
        val gate = BarcodeScanGate()

        assertFalse(gate.tryBegin(null))
        assertFalse(gate.tryBegin(""))
        assertFalse(gate.tryBegin("   "))
        assertFalse(gate.isBusy)
    }

    @Test
    fun `only one lookup at a time`() {
        val gate = BarcodeScanGate()
        assertTrue(gate.tryBegin("111"))

        assertFalse("same barcode on the next frame", gate.tryBegin("111"))
        assertFalse("a different barcode while busy", gate.tryBegin("222"))
    }

    @Test
    fun `not found keeps the gate closed so the dialog is not hammered - the reported bug`() {
        val gate = BarcodeScanGate()
        gate.tryBegin("111")
        gate.onNotFound("111")

        assertEquals("111", gate.notFoundBarcode)
        assertTrue(gate.isBusy)
        repeat(100) { assertFalse("frame $it must not start another lookup", gate.tryBegin("111")) }
        assertFalse("scanning something else behind the dialog", gate.tryBegin("222"))
    }

    @Test
    fun `after dismissing the dialog a different barcode can be scanned`() {
        val gate = BarcodeScanGate()
        gate.tryBegin("111")
        gate.onNotFound("111")

        gate.dismissNotFound()

        assertNull(gate.notFoundBarcode)
        assertFalse(gate.isBusy)
        assertTrue(gate.tryBegin("222"))
    }

    @Test
    fun `a barcode that was not found is ignored for the rest of the session`() {
        val gate = BarcodeScanGate()
        gate.tryBegin("111")
        gate.onNotFound("111")
        gate.dismissNotFound()

        assertFalse("user is still pointing at the same unknown product", gate.tryBegin("111"))
    }

    @Test
    fun `a found product leaves the gate busy and clears any dialog`() {
        val gate = BarcodeScanGate()
        gate.tryBegin("111")
        gate.onFound()

        assertNull(gate.notFoundBarcode)
        assertTrue("the screen is closing, no further lookups", gate.isBusy)
        assertFalse(gate.tryBegin("222"))
    }

    @Test
    fun `several not found barcodes are all remembered`() {
        val gate = BarcodeScanGate()
        listOf("1", "2", "3").forEach {
            assertTrue(gate.tryBegin(it))
            gate.onNotFound(it)
            gate.dismissNotFound()
        }

        listOf("1", "2", "3").forEach { assertFalse(gate.tryBegin(it)) }
        assertTrue(gate.tryBegin("4"))
    }

    @Test
    fun `many frames at once start exactly one lookup`() {
        val gate = BarcodeScanGate()
        val started = AtomicInteger(0)
        val pool = Executors.newFixedThreadPool(8)
        val ready = CountDownLatch(1)
        val done = CountDownLatch(200)

        repeat(200) {
            pool.execute {
                ready.await()
                if (gate.tryBegin("111")) started.incrementAndGet()
                done.countDown()
            }
        }
        ready.countDown()
        done.await()
        pool.shutdown()

        assertEquals(1, started.get())
    }
}
