package com.example.glarmto

import com.example.glarmto.ui.camera.CameraSession
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraSessionTest {

    private class Probe : AutoCloseable {
        val closed = AtomicInteger(0)
        override fun close() { closed.incrementAndGet() }
    }

    private class Exploding : AutoCloseable {
        override fun close() = throw IllegalStateException("boom")
    }

    @Test
    fun `a new session is open`() {
        val session = CameraSession()

        assertFalse(session.isClosed)
        session.close()
    }

    @Test
    fun `close releases the camera then closes every owned resource`() {
        val session = CameraSession()
        val order = mutableListOf<String>()
        val first = object : AutoCloseable { override fun close() { order.add("first") } }
        val second = object : AutoCloseable { override fun close() { order.add("second") } }
        session.own(first)
        session.own(second)
        session.onUnbind { order.add("unbind") }

        session.close()

        assertEquals(listOf("unbind", "first", "second"), order)
        assertTrue(session.isClosed)
    }

    @Test
    fun `own returns the same object so it can be used inline`() {
        val session = CameraSession()
        val probe = Probe()

        assertSame(probe, session.own(probe))
        session.close()
    }

    @Test
    fun `close is idempotent - nothing is released twice`() {
        val session = CameraSession()
        val probe = session.own(Probe())
        val unbinds = AtomicInteger(0)
        session.onUnbind { unbinds.incrementAndGet() }

        session.close()
        session.close()
        session.close()

        assertEquals(1, probe.closed.get())
        assertEquals(1, unbinds.get())
    }

    @Test
    fun `close shuts down the analysis thread`() {
        val executor = Executors.newSingleThreadExecutor()
        val session = CameraSession(executor)

        session.close()

        assertTrue(executor.isShutdown)
    }

    @Test
    fun `analysis executor really runs work until the session is closed`() {
        val session = CameraSession()
        val ran = CountDownLatch(1)

        session.analysisExecutor.execute { ran.countDown() }

        assertTrue(ran.await(2, java.util.concurrent.TimeUnit.SECONDS))
        session.close()
    }

    @Test
    fun `a resource that throws on close does not stop the others`() {
        val session = CameraSession()
        val before = session.own(Probe())
        session.own(Exploding())
        val after = session.own(Probe())

        session.close()

        assertEquals(1, before.closed.get())
        assertEquals(1, after.closed.get())
    }

    @Test
    fun `an unbind that throws does not stop resources from closing`() {
        val session = CameraSession()
        val probe = session.own(Probe())
        session.onUnbind { throw IllegalStateException("camera gone") }

        session.close()

        assertEquals(1, probe.closed.get())
    }

    @Test
    fun `a resource registered after close is closed immediately - the late camera provider case`() {
        val session = CameraSession()
        session.close()

        val late = session.own(Probe())

        assertEquals(1, late.closed.get())
    }

    @Test
    fun `an exploding late resource does not throw`() {
        val session = CameraSession()
        session.close()

        session.own(Exploding())
    }

    @Test
    fun `onUnbind after close refuses and stores nothing so the caller unbinds itself`() {
        val session = CameraSession()
        session.close()
        val called = AtomicInteger(0)

        val accepted = session.onUnbind { called.incrementAndGet() }

        assertFalse(accepted)
        session.close()
        assertEquals("a refused action must never be run by the session", 0, called.get())
    }

    @Test
    fun `onUnbind before close is accepted`() {
        val session = CameraSession()

        assertTrue(session.onUnbind { })
        session.close()
    }

    @Test
    fun `the latest onUnbind wins`() {
        val session = CameraSession()
        val calls = mutableListOf<String>()
        session.onUnbind { calls.add("old") }
        session.onUnbind { calls.add("new") }

        session.close()

        assertEquals(listOf("new"), calls)
    }

    @Test
    fun `closing from many threads releases exactly once`() {
        val session = CameraSession()
        val probe = session.own(Probe())
        val unbinds = AtomicInteger(0)
        session.onUnbind { unbinds.incrementAndGet() }
        val pool = Executors.newFixedThreadPool(8)
        val start = CountDownLatch(1)
        val done = CountDownLatch(64)

        repeat(64) {
            pool.execute {
                start.await()
                session.close()
                done.countDown()
            }
        }
        start.countDown()
        done.await()
        pool.shutdown()

        assertEquals(1, probe.closed.get())
        assertEquals(1, unbinds.get())
    }
}
