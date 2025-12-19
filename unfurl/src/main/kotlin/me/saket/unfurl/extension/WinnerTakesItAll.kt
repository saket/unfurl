package me.saket.unfurl.extension

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * An over-engineered way for running multiple concurrent jobs and
 * finding a winner _before_ it finishes its execution.
 *
 * ```kotlin
 * val result = winnerTakesItAll {
 *   for (url in urls) {
 *     attempt {
 *       val response = httpClient.get(url)
 *       if (response.isWinner) {
 *         claimVictory {
 *           // This block will only run for this winning url.
 *           // All other remaining attempts have been canceled.
 *           saveResponse(response)
 *         }
 *       }
 *     }
 *   }
 * }
 *
 * // Return value of the winning saveResponse()
 * // call or null if no victory was claimed.
 * println(result)
 * ```
 * */
internal suspend fun <T> winnerTakesItAll(action: suspend WinnerTakesItAll<T>.() -> Unit): T? {
  val race = WinnerTakesItAll<T>()
  coroutineScope {
    race.coroutineScope = this
    race.action()
  }
  return race.result
}

@OptIn(ExperimentalAtomicApi::class)
internal class WinnerTakesItAll<T> {
  var result: T? = null
    private set

  lateinit var coroutineScope: CoroutineScope
  private val winnerClaimed = AtomicBoolean(false)

  fun attempt(action: suspend AttemptScope<T>.() -> Unit) {
    coroutineScope.launch {
      val currentJob = this.coroutineContext[Job]!!
      if (!winnerClaimed.load()) {
        val attemptScope = object : AttemptScope<T> {
          override fun claimVictory(onVictory: () -> T) {
            if (winnerClaimed.compareAndSet(expectedValue = false, newValue = true)) {
              coroutineScope.coroutineContext[Job]?.children
                ?.filter { it != currentJob }
                ?.forEach { it.cancel() }
              result = onVictory()
            }
          }
        }
        action(attemptScope)
      }
    }
  }

  interface AttemptScope<T> {
    fun claimVictory(onVictory: () -> T)
  }
}
