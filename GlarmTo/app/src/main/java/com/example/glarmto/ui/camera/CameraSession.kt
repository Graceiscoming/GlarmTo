package com.example.glarmto.ui.camera

import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Owns everything a camera screen creates and must give back when the screen leaves: the camera
 * binding, the ML Kit detectors, and the analysis thread.
 *
 * Without this, closing the scanner overlay left the camera bound to the nav entry (camera indicator
 * still on, frames still analysed) and leaked the detectors and the executor every time it was opened.
 * [close] is safe to call more than once and from any thread. Anything registered after [close]
 * is released immediately, which covers the camera provider future completing after the screen
 * was already disposed.
 */
class CameraSession(private val executor: ExecutorService = Executors.newSingleThreadExecutor()) {
    private val lock = Any()
    private var closed = false
    private var unbind: (() -> Unit)? = null
    private val resources = mutableListOf<AutoCloseable>()

    /** Thread for the image analyzer. */
    val analysisExecutor: Executor get() = executor

    val isClosed: Boolean get() = synchronized(lock) { closed }

    /** Registers [resource] (e.g. a barcode scanner) to be closed with the session, and returns it. */
    fun <T : AutoCloseable> own(resource: T): T {
        val closeNow = synchronized(lock) {
            if (closed) true else { resources.add(resource); false }
        }
        if (closeNow) runCatching { resource.close() }
        return resource
    }

    /** Sets how to release the camera. Returns false, storing nothing, if the session is already closed. */
    fun onUnbind(action: () -> Unit): Boolean = synchronized(lock) {
        if (closed) return false
        unbind = action
        true
    }

    fun close() {
        val toUnbind: (() -> Unit)?
        val toClose: List<AutoCloseable>
        synchronized(lock) {
            if (closed) return
            closed = true
            toUnbind = unbind
            unbind = null
            toClose = resources.toList()
            resources.clear()
        }
        // One failing release must not stop the others.
        runCatching { toUnbind?.invoke() }
        toClose.forEach { runCatching { it.close() } }
        executor.shutdown()
    }
}
