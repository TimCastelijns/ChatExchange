import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

internal class Scheduler {

    internal val executor = Executors.newSingleThreadScheduledExecutor()
    private val eventExecutor = Executors.newCachedThreadPool()

    fun scheduleHourlyTask(andExecute: Boolean = true, task: () -> Unit) =
            schedule(Runnable { task.invoke() }, 1, TimeUnit.HOURS, andExecute)

    fun scheduleDailyTask(andExecute: Boolean = true, task: () -> Unit) =
            schedule(Runnable { task.invoke() }, 24, TimeUnit.HOURS, andExecute)

    fun scheduleTaskWithCustomInterval(rate: Long, timeUnit: TimeUnit,
                                       andExecute: Boolean = true, task: () -> Unit) =
            schedule(Runnable { task.invoke() }, rate, timeUnit, andExecute)

    private fun schedule(action: Runnable, rate: Long, timeUnit: TimeUnit, andExecute: Boolean) {
        if (andExecute) {
            action.run()
            executor.scheduleAtFixedRate(action, rate, rate, timeUnit)
        } else {
            executor.scheduleAtFixedRate(action, rate, rate, timeUnit)
        }
    }

    fun shutDown() {
        executor.shutdown()
        eventExecutor.shutdown()
    }
}
