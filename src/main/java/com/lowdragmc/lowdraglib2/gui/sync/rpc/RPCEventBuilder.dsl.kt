package com.lowdragmc.lowdraglib2.gui.sync.rpc

import com.lowdragmc.lowdraglib2.gui.ui.UIElement
import java.util.function.Supplier

/**
 * Kotlin-friendly [RPCEvent] factories with reified type inference.
 *
 * These are thin wrappers around [RPCEventBuilder.simple] overloads.
 *
 * Notes:
 * - For arity >= 3, [RPCEventBuilder] relies on its own functional interfaces (TriConsumer, Function3, Consumer4..Consumer12, Function4..Function12).
 *   This file assumes those interfaces are visible in the same package as [RPCEventBuilder].
 * - All functions are named [UIElement.rpcEvent] as requested.
 */
// 0 args
inline fun UIElement.rpcEvent(noinline executor: () -> Unit): RPCEmitter  =
    this.addRPCEvent(RPCEventBuilder.simple(executor))


inline fun <reified R> UIElement.rpcEventR(noinline executor: () -> R): RPCEmitter =
    this.addRPCEvent(RPCEventBuilder.simple(R::class.java, Supplier { executor() }))

// 1 args
inline fun <reified A1> UIElement.rpcEvent(noinline executor: (A1) -> Unit): RPCEmitter =
    this.addRPCEvent(RPCEventBuilder.simple(A1::class.java) { a1 -> executor(a1) })

inline fun <reified A1, reified R> UIElement.rpcEventR(noinline executor: (A1) -> R): RPCEmitter =
    this.addRPCEvent(RPCEventBuilder.simple(A1::class.java, R::class.java) { a1 -> executor(a1) })

// 2 args
inline fun <reified A1, reified A2> UIElement.rpcEvent(noinline executor: (A1, A2) -> Unit): RPCEmitter =
    this.addRPCEvent(RPCEventBuilder.simple(A1::class.java, A2::class.java) { a1, a2 -> executor(a1, a2) })

inline fun <reified A1, reified A2, reified R> UIElement.rpcEventR(noinline executor: (A1, A2) -> R): RPCEmitter =
    this.addRPCEvent(RPCEventBuilder.simple(A1::class.java, A2::class.java, R::class.java) { a1, a2 -> executor(a1, a2) })

// 3 args
inline fun <reified A1, reified A2, reified A3> UIElement.rpcEvent(noinline executor: (A1, A2, A3) -> Unit): RPCEmitter =
    this.addRPCEvent(RPCEventBuilder.simple(A1::class.java, A2::class.java, A3::class.java) { a1, a2, a3 -> executor(a1, a2, a3) })

inline fun <reified A1, reified A2, reified A3, reified R> UIElement.rpcEventR(noinline executor: (A1, A2, A3) -> R): RPCEmitter =
    this.addRPCEvent(RPCEventBuilder.simple(A1::class.java, A2::class.java, A3::class.java, R::class.java) { a1, a2, a3 -> executor(a1, a2, a3) })

// 4 args
inline fun <reified A1, reified A2, reified A3, reified A4> UIElement.rpcEvent(noinline executor: (A1, A2, A3, A4) -> Unit): RPCEmitter =
    this.addRPCEvent(RPCEventBuilder.simple(A1::class.java, A2::class.java, A3::class.java, A4::class.java) { a1, a2, a3, a4 ->
        executor(a1, a2, a3, a4)
    })

inline fun <reified A1, reified A2, reified A3, reified A4, reified R> UIElement.rpcEventR(noinline executor: (A1, A2, A3, A4) -> R): RPCEmitter =
    this.addRPCEvent(RPCEventBuilder.simple(A1::class.java, A2::class.java, A3::class.java, A4::class.java, R::class.java) { a1, a2, a3, a4 ->
        executor(a1, a2, a3, a4)
    })

// 5 args
inline fun <reified A1, reified A2, reified A3, reified A4, reified A5> UIElement.rpcEvent(noinline executor: (A1, A2, A3, A4, A5) -> Unit): RPCEmitter =
    this.addRPCEvent(RPCEventBuilder.simple(A1::class.java, A2::class.java, A3::class.java, A4::class.java, A5::class.java) { a1, a2, a3, a4, a5 ->
        executor(a1, a2, a3, a4, a5)
    })

inline fun <reified A1, reified A2, reified A3, reified A4, reified A5, reified R> UIElement.rpcEventR(noinline executor: (A1, A2, A3, A4, A5) -> R): RPCEmitter =
    this.addRPCEvent(RPCEventBuilder.simple(A1::class.java, A2::class.java, A3::class.java, A4::class.java, A5::class.java, R::class.java) { a1, a2, a3, a4, a5 ->
        executor(a1, a2, a3, a4, a5)
    })

// 6 args
inline fun <reified A1, reified A2, reified A3, reified A4, reified A5, reified A6> UIElement.rpcEvent(noinline executor: (A1, A2, A3, A4, A5, A6) -> Unit): RPCEmitter =
    this.addRPCEvent(RPCEventBuilder.simple(A1::class.java, A2::class.java, A3::class.java, A4::class.java, A5::class.java, A6::class.java) { a1, a2, a3, a4, a5, a6 ->
        executor(a1, a2, a3, a4, a5, a6)
    })

inline fun <reified A1, reified A2, reified A3, reified A4, reified A5, reified A6, reified R> UIElement.rpcEventR(noinline executor: (A1, A2, A3, A4, A5, A6) -> R): RPCEmitter =
    this.addRPCEvent(RPCEventBuilder.simple(A1::class.java, A2::class.java, A3::class.java, A4::class.java, A5::class.java, A6::class.java, R::class.java) { a1, a2, a3, a4, a5, a6 ->
        executor(a1, a2, a3, a4, a5, a6)
    })

// 7 args
inline fun <reified A1, reified A2, reified A3, reified A4, reified A5, reified A6, reified A7> UIElement.rpcEvent(noinline executor: (A1, A2, A3, A4, A5, A6, A7) -> Unit): RPCEmitter =
    this.addRPCEvent(RPCEventBuilder.simple(A1::class.java, A2::class.java, A3::class.java, A4::class.java, A5::class.java, A6::class.java, A7::class.java) { a1, a2, a3, a4, a5, a6, a7 ->
        executor(a1, a2, a3, a4, a5, a6, a7)
    })

inline fun <reified A1, reified A2, reified A3, reified A4, reified A5, reified A6, reified A7, reified R> UIElement.rpcEventR(noinline executor: (A1, A2, A3, A4, A5, A6, A7) -> R): RPCEmitter =
    this.addRPCEvent(RPCEventBuilder.simple(A1::class.java, A2::class.java, A3::class.java, A4::class.java, A5::class.java, A6::class.java, A7::class.java, R::class.java) { a1, a2, a3, a4, a5, a6, a7 ->
        executor(a1, a2, a3, a4, a5, a6, a7)
    })

// 8 args
inline fun <reified A1, reified A2, reified A3, reified A4, reified A5, reified A6, reified A7, reified A8> UIElement.rpcEvent(noinline executor: (A1, A2, A3, A4, A5, A6, A7, A8) -> Unit): RPCEmitter =
    this.addRPCEvent(RPCEventBuilder.simple(A1::class.java, A2::class.java, A3::class.java, A4::class.java, A5::class.java, A6::class.java, A7::class.java, A8::class.java) { a1, a2, a3, a4, a5, a6, a7, a8 ->
        executor(a1, a2, a3, a4, a5, a6, a7, a8)
    })

inline fun <reified A1, reified A2, reified A3, reified A4, reified A5, reified A6, reified A7, reified A8, reified R> UIElement.rpcEventR(noinline executor: (A1, A2, A3, A4, A5, A6, A7, A8) -> R): RPCEmitter =
    this.addRPCEvent(RPCEventBuilder.simple(A1::class.java, A2::class.java, A3::class.java, A4::class.java, A5::class.java, A6::class.java, A7::class.java, A8::class.java, R::class.java) { a1, a2, a3, a4, a5, a6, a7, a8 ->
        executor(a1, a2, a3, a4, a5, a6, a7, a8)
    })

// 9 args
inline fun <reified A1, reified A2, reified A3, reified A4, reified A5, reified A6, reified A7, reified A8, reified A9> UIElement.rpcEvent(noinline executor: (A1, A2, A3, A4, A5, A6, A7, A8, A9) -> Unit): RPCEmitter =
    this.addRPCEvent(RPCEventBuilder.simple(A1::class.java, A2::class.java, A3::class.java, A4::class.java, A5::class.java, A6::class.java, A7::class.java, A8::class.java, A9::class.java) { a1, a2, a3, a4, a5, a6, a7, a8, a9 ->
        executor(a1, a2, a3, a4, a5, a6, a7, a8, a9)
    })

inline fun <reified A1, reified A2, reified A3, reified A4, reified A5, reified A6, reified A7, reified A8, reified A9, reified R> UIElement.rpcEventR(noinline executor: (A1, A2, A3, A4, A5, A6, A7, A8, A9) -> R): RPCEmitter =
    this.addRPCEvent(RPCEventBuilder.simple(A1::class.java, A2::class.java, A3::class.java, A4::class.java, A5::class.java, A6::class.java, A7::class.java, A8::class.java, A9::class.java, R::class.java) { a1, a2, a3, a4, a5, a6, a7, a8, a9 ->
        executor(a1, a2, a3, a4, a5, a6, a7, a8, a9)
    })

// 10 args
inline fun <reified A1, reified A2, reified A3, reified A4, reified A5, reified A6, reified A7, reified A8, reified A9, reified A10> UIElement.rpcEvent(
    noinline executor: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10) -> Unit
): RPCEmitter =
    this.addRPCEvent(RPCEventBuilder.simple(A1::class.java, A2::class.java, A3::class.java, A4::class.java, A5::class.java, A6::class.java, A7::class.java, A8::class.java, A9::class.java, A10::class.java) { a1, a2, a3, a4, a5, a6, a7, a8, a9, a10 ->
        executor(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10)
    })

inline fun <reified A1, reified A2, reified A3, reified A4, reified A5, reified A6, reified A7, reified A8, reified A9, reified A10, reified R> UIElement.rpcEventR(
    noinline executor: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10) -> R
): RPCEmitter =
    this.addRPCEvent(RPCEventBuilder.simple(A1::class.java, A2::class.java, A3::class.java, A4::class.java, A5::class.java, A6::class.java, A7::class.java, A8::class.java, A9::class.java, A10::class.java, R::class.java) { a1, a2, a3, a4, a5, a6, a7, a8, a9, a10 ->
        executor(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10)
    })

// 11 args
inline fun <reified A1, reified A2, reified A3, reified A4, reified A5, reified A6, reified A7, reified A8, reified A9, reified A10, reified A11> UIElement.rpcEvent(
    noinline executor: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11) -> Unit
): RPCEmitter =
    this.addRPCEvent(RPCEventBuilder.simple(A1::class.java, A2::class.java, A3::class.java, A4::class.java, A5::class.java, A6::class.java, A7::class.java, A8::class.java, A9::class.java, A10::class.java, A11::class.java) { a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11 ->
        executor(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11)
    })

inline fun <reified A1, reified A2, reified A3, reified A4, reified A5, reified A6, reified A7, reified A8, reified A9, reified A10, reified A11, reified R> UIElement.rpcEventR(
    noinline executor: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11) -> R
): RPCEmitter =
    this.addRPCEvent(RPCEventBuilder.simple(A1::class.java, A2::class.java, A3::class.java, A4::class.java, A5::class.java, A6::class.java, A7::class.java, A8::class.java, A9::class.java, A10::class.java, A11::class.java, R::class.java) { a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11 ->
        executor(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11)
    })

// 12 args
inline fun <reified A1, reified A2, reified A3, reified A4, reified A5, reified A6, reified A7, reified A8, reified A9, reified A10, reified A11, reified A12> UIElement.rpcEvent(
    noinline executor: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12) -> Unit
): RPCEmitter =
    this.addRPCEvent(RPCEventBuilder.simple(A1::class.java, A2::class.java, A3::class.java, A4::class.java, A5::class.java, A6::class.java, A7::class.java, A8::class.java, A9::class.java, A10::class.java, A11::class.java, A12::class.java) { a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12 ->
        executor(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12)
    })

inline fun <reified A1, reified A2, reified A3, reified A4, reified A5, reified A6, reified A7, reified A8, reified A9, reified A10, reified A11, reified A12, reified R> UIElement.rpcEventR(
    noinline executor: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12) -> R
): RPCEmitter =
    this.addRPCEvent(RPCEventBuilder.simple(A1::class.java, A2::class.java, A3::class.java, A4::class.java, A5::class.java, A6::class.java, A7::class.java, A8::class.java, A9::class.java, A10::class.java, A11::class.java, A12::class.java, R::class.java) { a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12 ->
        executor(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12)
    })