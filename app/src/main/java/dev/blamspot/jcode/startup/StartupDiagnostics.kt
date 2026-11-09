package dev.blamspot.jcode.startup

import android.content.Context
import android.util.Log
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/** Private bounded diagnostic journal. Never rewrites preferences, package state, projects or rootfs. */
object StartupDiagnostics {
    private const val TAG="SuiStartup"
    private val installed=AtomicBoolean(false)
    private val reported=ConcurrentHashMap.newKeySet<String>()
    private val writer=Executors.newSingleThreadExecutor { task -> Thread(task,"sui-startup-journal").apply { isDaemon=true } }
    fun install(context: Context) {
        if(!installed.compareAndSet(false,true)) return
        val app=context.applicationContext
        val previous=Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread,error ->
            Log.e(TAG,"Uncaught failure on ${thread.name}",error)
            write(app,"FATAL ${thread.name}",error.stackTraceToString())
            previous?.uncaughtException(thread,error)
        }
        event(app,"activity.create Android=${android.os.Build.VERSION.SDK_INT} ABI=${android.os.Build.SUPPORTED_ABIS.joinToString()} version=${dev.blamspot.jcode.BuildConfig.VERSION_NAME}")
    }
    fun event(context: Context,stage: String) {
        Log.i(TAG,stage)
        writer.execute { write(context.applicationContext,stage,"") }
    }
    fun report(context: Context,stage: String,error: Exception) {
        // At most one full copy for the same optional failure per process; no automatic retry loop.
        val key="$stage:${error.javaClass.name}:${error.message}"
        if(!reported.add(key)) return
        Log.e(TAG,stage,error)
        writer.execute { write(context.applicationContext,stage,error.stackTraceToString()) }
    }
    @Synchronized private fun write(context: Context,stage: String,trace: String) {
        try {
            val root=File(context.filesDir,"sui-code/diagnostics").apply { mkdirs() }
            val log=File(root,"startup.log")
            if(log.length()>128*1024) log.renameTo(File(root,"startup.previous.log"))
            java.io.FileOutputStream(log,log.length()<=128*1024).bufferedWriter().use { it.write("${java.time.Instant.now()} $stage\n${trace.take(128*1024)}\n") }
        } catch(e: Exception) { Log.e(TAG,"Could not persist startup journal",e) }
    }
}
