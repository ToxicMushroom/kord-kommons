package me.melijn.kordkommons.async


public class Task(private val func: suspend () -> Unit) : KTRunnable {

    override suspend fun run() {
        try {
            func()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}

public class DeferredNTask<T>(private val func: suspend () -> T?) : DeferredNKTRunnable<T> {

    override suspend fun run(): T? {
        return try {
            func()
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }
}

public class EvalDeferredNTask<T>(private val func: suspend () -> T?) : DeferredNKTRunnable<T> {

    override suspend fun run(): T? {
        return func()
    }
}

public class DeferredTask<T>(private val func: suspend () -> T) : DeferredKTRunnable<T> {

    override suspend fun run(): T {
        return func()
    }
}

public class RunnableTask(private val func: suspend () -> Unit) : Runnable {

    override fun run() {
        TaskManager.async {
            func()
        }
    }
}

public class TaskInline(private inline val func: () -> Unit) : Runnable {

    override fun run() {
        try {
            func()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}

@FunctionalInterface
public interface KTRunnable {
    public suspend fun run()
}

@FunctionalInterface
public interface DeferredNKTRunnable<T> {
    public suspend fun run(): T?
}

@FunctionalInterface
public interface DeferredKTRunnable<T> {
    public suspend fun run(): T
}