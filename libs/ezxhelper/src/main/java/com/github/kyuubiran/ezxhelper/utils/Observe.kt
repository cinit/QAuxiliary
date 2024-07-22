package com.github.kyuubiran.ezxhelper.utils

/**
 * 监听一个对象，当值发生变化时调用 onValueChanged 中所有回调
 */
class Observe<T>(init: T, onValueChanged: ((T) -> Unit)? = null) {
    private var _value: T = init

    var value: T
        get() = _value
        set(newValue) = synchronized(this) {
            if (_value == newValue) return@synchronized
            _value = newValue
            if (onValueChanged.unsafeInvoke)
                onValueChanged.unsafeInvoke(newValue)
            else
                onValueChanged.invoke(newValue)
        }

    val onValueChanged = ValueChangedEvent<T>()

    init {
        if (onValueChanged != null) this.onValueChanged += onValueChanged
    }

    class ValueChangedEvent<T> {
        private val _listeners = mutableSetOf<(T) -> Unit>()

        var unsafeInvoke = false

        var onThrow: ((thr: Throwable) -> Unit)? = null

        fun add(listener: (T) -> Unit) {
            _listeners.add(listener)
        }

        fun remove(listener: (T) -> Unit) {
            _listeners.remove(listener)
        }

        fun addAll(listeners: Collection<(T) -> Unit>) {
            _listeners.addAll(listeners)
        }

        fun removeAll(listeners: Collection<(T) -> Unit>) {
            _listeners.removeAll(listeners.toSet())
        }

        fun addAll(listeners: Array<(T) -> Unit>) {
            _listeners.addAll(listeners)
        }

        fun removeAll(listeners: Array<(T) -> Unit>) {
            _listeners.removeAll(listeners.toSet())
        }

        fun clear() {
            _listeners.clear()
        }

        operator fun plusAssign(listener: (T) -> Unit) {
            add(listener)
        }

        operator fun minusAssign(listener: (T) -> Unit) {
            remove(listener)
        }

        operator fun plusAssign(listeners: Collection<(T) -> Unit>) {
            addAll(listeners)
        }

        operator fun minusAssign(listeners: Collection<(T) -> Unit>) {
            removeAll(listeners)
        }

        operator fun plusAssign(listeners: Array<(T) -> Unit>) {
            addAll(listeners)
        }

        operator fun minusAssign(listeners: Array<(T) -> Unit>) {
            removeAll(listeners)
        }

        fun unsafeInvoke(value: T) {
            _listeners.forEach { it(value) }
        }

        operator fun invoke(value: T) {
            _listeners.retainIf {
                try {
                    it(value)
                    true
                } catch (e: Throwable) {
                    onThrow?.invoke(e)
                    Log.e("Event invoke failed", e)
                    false
                }
            }
        }
    }
}