package com.lowdragmc.lowdraglib2.gui.ui.compose

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** The mechanism that makes a state read inside a `style` / `layout` block work. */
class NodeObserverTest {

    private val observer = NodeObserver().also { it.start() }

    @AfterEach
    fun tearDown() = observer.dispose()

    /** A node that re-runs its block on re-apply, the way [AppliedProps] does. */
    private inner class Node(private val block: () -> Unit) : Reappliable {
        var applied = 0

        fun apply() {
            applied++
            observer.observe(this) { block() }
        }

        override fun reapply() = apply()
    }

    private fun write(state: MutableState<Int>, value: Int) {
        Snapshot.withMutableSnapshot { state.value = value }
        Snapshot.sendApplyNotifications()
    }

    @Test
    fun `changing state the block read re-applies that node`() {
        val state = mutableStateOf(1)
        var seen = 0
        val node = Node { seen = state.value }
        node.apply()
        assertEquals(1, seen)

        write(state, 2)
        observer.flush()

        assertEquals(2, node.applied)
        assertEquals(2, seen)
    }

    @Test
    fun `a node that read nothing is left alone`() {
        val state = mutableStateOf(1)
        val node = Node { }
        node.apply()

        write(state, 2)
        observer.flush()

        assertEquals(1, node.applied)
    }

    @Test
    fun `changing an unrelated state is left alone`() {
        val read = mutableStateOf(1)
        val other = mutableStateOf(1)
        val node = Node { read.value }
        node.apply()

        write(other, 2)
        observer.flush()

        assertEquals(1, node.applied)
    }

    @Test
    fun `a flush with nothing pending does nothing`() {
        val node = Node { }
        node.apply()
        observer.flush()
        observer.flush()
        assertEquals(1, node.applied)
    }

    @Test
    fun `re-applying re-registers, so the next change lands too`() {
        val state = mutableStateOf(1)
        val node = Node { state.value }
        node.apply()

        write(state, 2)
        observer.flush()
        write(state, 3)
        observer.flush()

        assertEquals(3, node.applied)
    }

    @Test
    fun `state the block stopped reading stops invalidating it after one more pass`() {
        val first = mutableStateOf(1)
        val second = mutableStateOf(1)
        var readFirst = true
        val node = Node { if (readFirst) first.value else second.value }
        node.apply()

        readFirst = false
        write(first, 99)
        observer.flush()
        // Re-applied because of `first`, and while re-applying it read `second` instead.
        assertEquals(2, node.applied)

        write(first, 100)
        observer.flush()
        assertEquals(2, node.applied)

        write(second, 99)
        observer.flush()
        assertEquals(3, node.applied)
    }

    @Test
    fun `a forgotten node is not re-applied`() {
        val state = mutableStateOf(1)
        val node = Node { state.value }
        node.apply()

        observer.forget(node)
        write(state, 2)
        observer.flush()

        assertEquals(1, node.applied)
    }

    @Test
    fun `changes that arrive after disposal are dropped`() {
        val state = mutableStateOf(1)
        val node = Node { state.value }
        node.apply()

        observer.dispose()
        write(state, 2)
        observer.flush()

        assertEquals(1, node.applied)
    }

    @Test
    fun `a disposed observer still runs blocks, just without tracking`() {
        observer.dispose()
        var ran = false
        val node = Node { ran = true }
        node.apply()
        assertEquals(true, ran)
    }

    @Test
    fun `two nodes reading the same state are both re-applied`() {
        val state = mutableStateOf(1)
        val first = Node { state.value }
        val second = Node { state.value }
        first.apply()
        second.apply()

        write(state, 2)
        observer.flush()

        assertEquals(2, first.applied)
        assertEquals(2, second.applied)
    }

    @Test
    fun `several changes between frames collapse into one re-apply`() {
        val state = mutableStateOf(1)
        val node = Node { state.value }
        node.apply()

        write(state, 2)
        write(state, 3)
        write(state, 4)
        observer.flush()

        assertEquals(2, node.applied)
    }
}
