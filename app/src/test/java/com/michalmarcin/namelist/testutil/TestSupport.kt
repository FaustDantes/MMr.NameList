package com.michalmarcin.namelist.testutil

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/** viewModelScope runs on Dispatchers.Main; tests replace it with an immediate dispatcher. */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(UnconfinedTestDispatcher())

    override fun finished(description: Description) = Dispatchers.resetMain()
}

/**
 * Waits until the state matches. Room delivers results on its own threads, so a ViewModel's
 * state changes a moment after the action; asserting at once would be flaky.
 */
fun <T> StateFlow<T>.await(timeoutMs: Long = 5_000, condition: (T) -> Boolean): T =
    runBlocking { withTimeout(timeoutMs) { first(condition) } }
