package com.example.glarmto.testsupport

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Sets [Dispatchers.Main] to a [TestDispatcher] for ViewModel / coroutine tests on the JVM.
 * Same role as androidx.lifecycle.testing.MainDispatcherRule; kept local so unit tests compile
 * without relying on lifecycle-runtime-testing resolution on the classpath.
 */
class MainDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
