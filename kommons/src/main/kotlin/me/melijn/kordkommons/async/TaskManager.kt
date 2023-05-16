package me.melijn.kordkommons.async

import kotlinx.coroutines.*
import java.util.concurrent.*

public object TaskManager {

    private val threadFactory = { name: String ->
        var counter = 0
        { r: Runnable ->
            Thread(r, "[$name-Pool-%d]".replace("%d", "${counter++}"))
        }
    }

    private val executorService: ExecutorService = ForkJoinPool()
    @Suppress("MemberVisibilityCanBePrivate")
    public val dispatcher: ExecutorCoroutineDispatcher = executorService.asCoroutineDispatcher()
    public var scheduledExecutorService: ScheduledExecutorService =
        Executors.newScheduledThreadPool(15, threadFactory.invoke("Repeater"))
    public var coroutineScope: CoroutineScope = CoroutineScope(dispatcher)

    public fun async(block: suspend CoroutineScope.() -> Unit): Job {
        return coroutineScope.launch {
            Task {
                block.invoke(this)
            }.run()
        }
    }

    public fun asyncIgnoreEx(block: suspend CoroutineScope.() -> Unit): Job = coroutineScope.launch {
        try {
            block.invoke(this)
        } catch (ignored: Throwable) {
            // ignored by design
        }
    }

    public fun <T> taskValueAsync(block: suspend CoroutineScope.() -> T): Deferred<T> = coroutineScope.async {
        DeferredTask { block.invoke(this) }.run()
    }

    public fun <T> taskValueNAsync(block: suspend CoroutineScope.() -> T?): Deferred<T?> = coroutineScope.async {
        DeferredNTask {
            block.invoke(this)
        }.run()
    }

    public fun <T> evalTaskValueNAsync(block: suspend CoroutineScope.() -> T?): Deferred<T?> = coroutineScope.async {
        EvalDeferredNTask {
            block.invoke(this)
        }.run()
    }

    public inline fun asyncInline(crossinline block: CoroutineScope.() -> Unit): Job = coroutineScope.launch {
        TaskInline {
            block.invoke(this)
        }.run()
    }

    public inline fun asyncAfter(afterMillis: Long, crossinline func: suspend () -> Unit): ScheduledFuture<*> {
        return scheduledExecutorService.schedule(RunnableTask { func() }, afterMillis, TimeUnit.MILLISECONDS)
    }
}
