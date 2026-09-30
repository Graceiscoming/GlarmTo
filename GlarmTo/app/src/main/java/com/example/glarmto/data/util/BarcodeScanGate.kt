package com.example.glarmto.data.util

/**
 * Decides which camera frames are allowed to start a barcode lookup.
 *
 * The camera analyzer fires on every frame, so without a gate the same barcode was looked up again and
 * again (one network call per frame) while the "product not found" dialog was still open. The rules:
 *  - only one lookup at a time;
 *  - a barcode that already came back "not found" is never looked up again in this scan session;
 *  - after a "not found", the gate stays busy until the dialog is dismissed, so scanning something
 *    else behind the dialog can't start another lookup.
 *
 * Methods are synchronized because frames arrive on the camera executor while the result and the
 * dialog are handled on the main thread.
 */
class BarcodeScanGate {
    private val ignored = mutableSetOf<String>()
    private var busy = false
    private var notFound: String? = null

    val isBusy: Boolean @Synchronized get() = busy

    /** The barcode the "not found" dialog is showing, or null when no dialog is open. */
    val notFoundBarcode: String? @Synchronized get() = notFound

    /** True if a lookup for [rawValue] may start now; the caller must then report the outcome. */
    @Synchronized
    fun tryBegin(rawValue: String?): Boolean {
        if (rawValue.isNullOrBlank() || busy || rawValue in ignored) return false
        busy = true
        return true
    }

    /** The lookup found a product. The gate stays busy: the scanner screen is about to close. */
    @Synchronized
    fun onFound() {
        notFound = null
    }

    /** The lookup found nothing: remember the barcode and keep the gate closed until [dismissNotFound]. */
    @Synchronized
    fun onNotFound(rawValue: String) {
        ignored.add(rawValue)
        notFound = rawValue
    }

    /** The "not found" dialog was dismissed ("scan another"): allow scanning again. */
    @Synchronized
    fun dismissNotFound() {
        notFound = null
        busy = false
    }
}
