package com.lowdragmc.lowdraglib2.gui.ui.compose

import androidx.compose.runtime.mutableStateOf
import com.lowdragmc.lowdraglib2.gui.sync.bindings.IDataSource
import kotlinx.coroutines.Dispatchers
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** The parts of [ComposeHost] and the state bridges that can be exercised without a live UI. */
class HostPlumbingTest {

    @Test
    fun `continuations dispatched from other threads all run on the next drain`() {
        // What a delay() or a withContext(IO) inside an effect does: resume from a foreign thread.
        val dispatcher = RenderThreadDispatcher()
        val ran = AtomicInteger()
        val pool = Executors.newFixedThreadPool(8)
        val done = CountDownLatch(8)
        repeat(8) {
            pool.execute {
                repeat(1_000) { dispatcher.dispatch(Dispatchers.Unconfined, Runnable { ran.incrementAndGet() }) }
                done.countDown()
            }
        }
        done.await(10, TimeUnit.SECONDS)
        pool.shutdown()

        assertEquals(0, ran.get(), "nothing runs until the render thread drains")
        dispatcher.drain()
        assertEquals(8_000, ran.get())
    }

    @Test
    fun `work queued while draining runs in the same drain`() {
        val dispatcher = RenderThreadDispatcher()
        val order = mutableListOf<String>()
        dispatcher.dispatch(Dispatchers.Unconfined, Runnable {
            order += "first"
            dispatcher.dispatch(Dispatchers.Unconfined, Runnable { order += "queued by first" })
        })
        dispatcher.drain()
        assertEquals(listOf("first", "queued by first"), order)
    }

    @Test
    fun `a binding handed back by two compositions ends up on the original source`() {
        val original = IDataSource.of<Int>({ }, { 0 })
        // A installs itself, then B on top of it; A leaves first, then B.
        val a = StateDataSource(mutableStateOf(1), original)
        val b = StateDataSource(mutableStateOf(2), a)
        a.active = false
        b.active = false
        assertSame(original, b.restorable())
    }

    @Test
    fun `a binding handed back while the other composition is still there goes to it`() {
        val original = IDataSource.of<Int>({ }, { 0 })
        val a = StateDataSource(mutableStateOf(1), original)
        val b = StateDataSource(mutableStateOf(2), a)
        b.active = false
        assertSame(a, b.restorable())
    }
}
