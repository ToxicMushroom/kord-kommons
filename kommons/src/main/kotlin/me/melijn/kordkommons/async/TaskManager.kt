package me.melijn.kordkommons.async

import kotlinx.coroutines.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

public object TaskScope : CoroutineScope {

    private val executorService: ExecutorService = Executors.newVirtualThreadPerTaskExecutor()
    public var taskManagerSupervisor: CompletableJob = SupervisorJob()

    @Suppress("MemberVisibilityCanBePrivate")
    public val dispatcher: ExecutorCoroutineDispatcher = executorService.asCoroutineDispatcher()

    override val coroutineContext: CoroutineContext
        get() = (dispatcher + taskManagerSupervisor)
}