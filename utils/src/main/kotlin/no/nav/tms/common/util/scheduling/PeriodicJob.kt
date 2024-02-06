package no.nav.tms.common.util.scheduling

import kotlinx.coroutines.*
import kotlinx.coroutines.CoroutineStart.LAZY
import java.time.Duration

/**
 * Extend class to schedule a coroutine which executes a given task as defined in initializeJob every given interval
 */
abstract class PeriodicJob(private val interval: Duration) {

    private val scope = CoroutineScope(Dispatchers.Default + Job())

    abstract val job: Job

    protected fun initializeJob(periodicProcess: suspend () -> Unit) = scope.launch(start = LAZY) {
        while (job.isActive) {
            periodicProcess()
            delay(interval.toMillis())
        }
    }

    fun start() {
        job.start()
    }

    suspend fun stop() {
        job.cancelAndJoin()
    }
}
