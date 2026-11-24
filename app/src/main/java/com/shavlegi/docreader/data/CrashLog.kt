package com.shavlegi.docreader.data

import android.content.Context
import java.io.File

private const val CRASH_FILE = "last-crash.txt"

/**
 * Catches uncaught exceptions (crashes) and saves the stack trace to the
 * app's private storage, so the next launch can show it. The system crash
 * dialog is still shown afterwards (we chain to the default handler).
 * Note: native crashes and hard OOM kills do not reach this handler — if no
 * log appears after a crash, that itself points to a native/OOM cause.
 */
fun installCrashHandler(context: Context) {
    val appContext = context.applicationContext
    val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        runCatching {
            val text = buildString {
                appendLine("Time: ${java.util.Date()}")
                appendLine("Thread: ${thread.name}")
                appendLine(throwable.stackTraceToString())
                var cause = throwable.cause
                while (cause != null) {
                    appendLine("Caused by: ${cause.stackTraceToString()}")
                    cause = cause.cause
                }
            }
            File(appContext.filesDir, CRASH_FILE).writeText(text)
        }
        defaultHandler?.uncaughtException(thread, throwable)
    }
}

fun readCrashLog(context: Context): String? {
    val file = File(context.filesDir, CRASH_FILE)
    if (!file.exists()) return null
    return runCatching { file.readText() }.getOrNull()
}

fun clearCrashLog(context: Context) {
    runCatching { File(context.filesDir, CRASH_FILE).delete() }
}
