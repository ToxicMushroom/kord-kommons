package me.melijn.kordkommons.async

import kotlinx.coroutines.launch

public class RunnableTask(private val func: suspend () -> Unit) : Runnable {

    override fun run() {
        TaskScope.launch {
            func()
        }
    }
}