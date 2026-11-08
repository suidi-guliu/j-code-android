package dev.blamspot.jcode

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/** Debug logcat evidence for real-device cold/warm navigation; no timeout changes or UI work. */
internal class MainThreadHeartbeat : AutoCloseable {
    private val main=Handler(Looper.getMainLooper())
    private val pending=AtomicBoolean()
    @Volatile private var posted=0L
    private var lastReport=0L
    private val worker=Executors.newSingleThreadScheduledExecutor { runnable -> Thread(runnable,"sui-main-watchdog").apply { isDaemon=true } }
    init {
        worker.scheduleAtFixedRate({
            val now=SystemClock.uptimeMillis()
            if(pending.get() && now-posted>1000 && now-lastReport>2000) {
                lastReport=now
                Log.e("SuiPerformance","Main thread stalled ${now-posted}ms; pid=${android.os.Process.myPid()}\n"+Looper.getMainLooper().thread.stackTrace.joinToString("\n"))
            }
            if(pending.compareAndSet(false,true)) { posted=now;main.post { pending.set(false) } }
        },0,500,TimeUnit.MILLISECONDS)
    }
    override fun close() { worker.shutdownNow() }
}
