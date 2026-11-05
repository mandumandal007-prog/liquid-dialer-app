package com.mandu.liquiddialer.telecom

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.telecom.DisconnectCause
import com.mandu.liquiddialer.data.Repo
import kotlinx.coroutines.flow.MutableStateFlow

/** Auto-redial: user khud shuru karta hai, busy/error pe app khud dobara try karta hai.
 *  Hard cap MAX_ATTEMPTS, har retry se pehle GAP_SEC ka countdown notification ke saath,
 *  aur har waqt "Rok do" se cancel. Call lag gayi ya cap poora to session khatam.
 *  Ye kabhi bina user ke start kiye call nahi karta. */
object AutoRedial {
    const val MAX_ATTEMPTS = 5
    const val GAP_SEC = 10

    /** waitingUntil > 0 = countdown chal raha (us waqt next dial), 0 = abhi dial ho raha/raha. */
    data class Session(val number: String, val name: String, val attempt: Int, val waitingUntil: Long)

    val session = MutableStateFlow<Session?>(null)

    /** Tests hook. */
    var placeCall: (Context, String) -> Boolean = { ctx, num -> CallManager.place(ctx, num, null) }
    var now: () -> Long = { System.currentTimeMillis() }
    /** Tests hook: delay ke bina turant "tick" chalane ke liye scheduler override. */
    var scheduler: ((Long, () -> Unit) -> Unit)? = null

    private val handler = Handler(Looper.getMainLooper())
    private var pending: Runnable? = null

    fun shouldRetry(answered: Boolean, cause: Int?): Boolean =
        !answered && (cause == DisconnectCause.BUSY || cause == DisconnectCause.ERROR)

    /** User ka explicit tap. Turant pehli koshish dial hoti hai. */
    fun start(ctx: Context, number: String, name: String) {
        if (number.isBlank()) return
        cancelPending()
        session.value = Session(number, name, attempt = 1, waitingUntil = 0)
        placeCall(ctx, number)
        CallNotifier.autoRedialProgress(ctx, session.value!!, GAP_SEC)
    }

    /** LdInCallService.onCallRemoved se: session ki call khatam hui to decide karo. */
    fun onCallRemoved(ctx: Context, number: String, answered: Boolean, cause: Int?) {
        val s = session.value ?: return
        if (Repo.key(s.number) != Repo.key(number)) return
        when {
            answered -> finish(ctx, "Call lag gayi")
            !shouldRetry(answered, cause) -> finish(ctx, "Call nahi lagi")
            s.attempt >= MAX_ATTEMPTS -> finish(ctx, "Call nahi lag paayi - $MAX_ATTEMPTS koshishein ho gayin")
            else -> scheduleNext(ctx, s)
        }
    }

    private fun scheduleNext(ctx: Context, s: Session) {
        val until = now() + GAP_SEC * 1000L
        val next = s.copy(attempt = s.attempt + 1, waitingUntil = until)
        session.value = next
        CallNotifier.autoRedialProgress(ctx, next, GAP_SEC)
        val tick: () -> Unit = {
            if (session.value?.waitingUntil == until) {
                session.value = next.copy(waitingUntil = 0)
                placeCall(ctx, next.number)
                CallNotifier.autoRedialProgress(ctx, session.value!!, GAP_SEC)
            }
        }
        val sched = scheduler
        if (sched != null) sched(GAP_SEC * 1000L, tick)
        else {
            val r = Runnable(tick); pending = r
            handler.postDelayed(r, GAP_SEC * 1000L)
        }
    }

    /** "Rok do" (notification ya in-app button) - user ka explicit cancel. */
    fun cancel(ctx: Context) = finish(ctx, "Auto-redial rok diya")

    private fun finish(ctx: Context, msg: String) {
        cancelPending()
        val s = session.value ?: return
        session.value = null
        CallNotifier.cancelAutoRedial(ctx)
        CallNotifier.autoRedialDone(ctx, s.name.ifBlank { Repo.pretty(s.number) }, msg)
    }

    private fun cancelPending() { pending?.let { handler.removeCallbacks(it) }; pending = null }
}
