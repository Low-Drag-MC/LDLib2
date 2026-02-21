package com.lowdragmc.lowdraglib2.gui.sync.bindings.impl

import kotlin.reflect.KProperty

operator fun <T> TrackData<T>.getValue(thisRef: Any?, property: KProperty<*>): T = value

operator fun <T> TrackData<T>.setValue(thisRef: Any?, property: KProperty<*>, newValue: T) {
    value = newValue
}

inline fun <A, B> TrackData<A>.map(
    crossinline get: (A) -> B,
    crossinline set: (B) -> A?
) = object : TrackData<B>(get(value)) {
    override fun getValue(): B {
        return get(this@map.value)
    }

    override fun setValue(value: B) {
        set(value)?.let { this@map.setValue(it) }
    }
}