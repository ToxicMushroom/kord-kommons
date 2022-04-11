package me.melijn.kordkommons.async

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SafeList<E>(private val lock: Mutex = Mutex()) {

    private val list = ArrayList<E>()

    val size: Int
        get() = list.size

    fun isEmpty(): Boolean = list.isEmpty()

    suspend fun get(index: Int): E = lock.withLock {
        return list[index] ?: throw IndexOutOfBoundsException()
    }

    suspend fun getAll(indexes: List<Int>): List<E> = lock.withLock {
        val elements = ArrayList<E>(indexes.size)
        for (i in indexes) elements[i] = list[i] ?: throw IndexOutOfBoundsException()
        return elements
    }

    suspend fun getOrNull(index: Int): E? = lock.withLock {
        return list[index]
    }

    suspend fun add(element: E): Boolean = lock.withLock {
        return list.add(element)
    }

    suspend fun add(index: Int, element: E) = lock.withLock {
        list.add(index, element)
    }

    suspend fun removeAt(index: Int): E = lock.withLock {
        return list.removeAt(index)
    }

    suspend fun removeAtOrNull(index: Int): E? = lock.withLock {
        return if (list.size > index) {
            list.removeAt(index)
        } else {
            null
        }
    }

    suspend fun remove(element: E) = lock.withLock {
        list.remove(element)
    }

    suspend fun shuffle() = lock.withLock {
        list.shuffle()
    }

    suspend fun clear() = lock.withLock {
        list.clear()
    }

    suspend fun take(amount: Int): List<E> = lock.withLock {
        return list.take(amount)
    }

    suspend fun removeFirstAndGetNextOrNull(amount: Int): E? = lock.withLock {
        for (i in 0 until (amount - 1)) {
            if (list.size < 1) break
            else list.removeAt(0)
        }
        return list.removeFirstOrNull()
    }

    suspend fun forEach(function: suspend (E) -> Unit) = lock.withLock {
        val size = list.size
        for (i in 0 until size) {
            function.invoke(list[i])
        }
    }

    suspend fun indexedForEach(function: (Int, E) -> Unit) = lock.withLock {
        val size = list.size
        for (i in 0 until size) {
            function.invoke(i, list[i])
        }
    }

    suspend fun indexOf(audioTrack: E): Int = lock.withLock {
        return list.indexOf(audioTrack)
    }

    suspend fun any(function: (E) -> Boolean): Boolean = lock.withLock {
        return list.any(function)
    }

    suspend fun firstOrNull(function: (E) -> Boolean): E? = lock.withLock {
        return list.firstOrNull(function)
    }

    suspend fun firstOrNull(): E? = lock.withLock {
        return list.firstOrNull()
    }

    suspend fun removeAll(toRemove: List<E>) = lock.withLock {
        list.removeAll(toRemove.toSet())
    }

    suspend fun addAll(elements: Collection<E>) = lock.withLock {
        list.addAll(elements)
    }
}