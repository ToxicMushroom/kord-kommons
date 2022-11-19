package me.melijn.kordkommons.async

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

public class SafeList<E>(private val lock: Mutex = Mutex()) {

    private val list = ArrayList<E>()

    public val size: Int
        get() = list.size

    public fun isEmpty(): Boolean = list.isEmpty()

    public suspend fun get(index: Int): E = lock.withLock {
        return list[index] ?: throw IndexOutOfBoundsException()
    }

    public suspend fun getAll(indexes: Collection<Int>): List<E> = lock.withLock {
        val elements = ArrayList<E>(indexes.size)
        for (i in indexes) elements.add(list[i] ?: throw IndexOutOfBoundsException())
        return elements
    }

    public suspend fun getOrNull(index: Int): E? = lock.withLock {
        return list[index]
    }

    public suspend fun add(element: E): Boolean = lock.withLock {
        return list.add(element)
    }

    public suspend fun add(index: Int, element: E): Unit = lock.withLock {
        list.add(index, element)
    }

    public suspend fun removeAt(index: Int): E = lock.withLock {
        return list.removeAt(index)
    }

    public suspend fun removeAtOrNull(index: Int): E? = lock.withLock {
        return if (list.size > index) {
            list.removeAt(index)
        } else {
            null
        }
    }

    public suspend fun remove(element: E): Boolean = lock.withLock {
        list.remove(element)
    }

    public suspend fun shuffle(): Unit = lock.withLock {
        list.shuffle()
    }

    public suspend fun clear(): Unit = lock.withLock {
        list.clear()
    }

    public suspend fun take(amount: Int): List<E> = lock.withLock {
        return list.take(amount)
    }

    public suspend fun removeFirstAndGetNextOrNull(amount: Int): E? = lock.withLock {
        for (i in 0 until (amount - 1)) {
            if (list.size < 1) break
            else list.removeAt(0)
        }
        return list.removeFirstOrNull()
    }

    public suspend fun forEach(function: suspend (E) -> Unit): Unit = lock.withLock {
        val size = list.size
        for (i in 0 until size) {
            function.invoke(list[i])
        }
    }

    public suspend fun indexedForEach(function: (Int, E) -> Unit): Unit = lock.withLock {
        val size = list.size
        for (i in 0 until size) {
            function.invoke(i, list[i])
        }
    }

    public suspend fun indexOf(audioTrack: E): Int = lock.withLock {
        return list.indexOf(audioTrack)
    }

    public suspend fun any(function: (E) -> Boolean): Boolean = lock.withLock {
        return list.any(function)
    }

    public suspend fun firstOrNull(function: (E) -> Boolean): E? = lock.withLock {
        return list.firstOrNull(function)
    }

    public suspend fun firstOrNull(): E? = lock.withLock {
        return list.firstOrNull()
    }

    public suspend fun removeAll(toRemove: List<E>): Boolean = lock.withLock {
        list.removeAll(toRemove.toSet())
    }

    public suspend fun addAll(elements: Collection<E>): Boolean = lock.withLock {
        list.addAll(elements)
    }
}