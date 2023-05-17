package me.melijn.kordkommons.async

import kotlinx.coroutines.*
import java.util.concurrent.*
import kotlin.coroutines.CoroutineContext

public object TaskManager {

    private val threadFactory = { name: String ->
        var counter = 0
        { r: Runnable ->
            Thread(r, "[$name-Pool-%d]".replace("%d", "${counter++}"))
        }
    }

    public var scheduledExecutorService: ScheduledExecutorService =
        Executors.newScheduledThreadPool(15, threadFactory.invoke("Repeater"))

    public inline fun asyncAfter(afterMillis: Long, crossinline func: suspend () -> Unit): ScheduledFuture<*> {
        return scheduledExecutorService.schedule(RunnableTask { func() }, afterMillis, TimeUnit.MILLISECONDS)
    }
}

public object TaskScope : CoroutineScope {

    private val executorService: ExecutorService = ForkJoinPool()
    public var taskManagerSupervisor: CompletableJob = SupervisorJob()
    @Suppress("MemberVisibilityCanBePrivate")
    public val dispatcher: ExecutorCoroutineDispatcher = executorService.asCoroutineDispatcher()

    override val coroutineContext: CoroutineContext
        get() = (dispatcher + taskManagerSupervisor)
}