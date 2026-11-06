package com.mandu.liquiddialer.data

import android.content.Context
import com.mandu.liquiddialer.LdApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Khud backup har hafte: settings me on ho aur pichli koshish 7+ din purani ho, to app khulne par
 * FullBackup.export ka JSON phone ke app-private folder (filesDir/backups) me likhta hai.
 * Sirf device-local: koi network, permission, alarm ya WorkManager nahi. Aakhri KEEP files rakhta hai.
 * Koi bhi failure app start ko kabhi block nahi karta (maybe false return karke chup-chaap skip).
 */
object AutoBackup {
    const val KEEP = 3
    const val GAP_MS: Long = 7L * 24 * 60 * 60 * 1000

    fun dir(ctx: Context): File = File(ctx.filesDir, "backups")

    /** Aakhri auto-backup files, nayi se purani order me (naam me sortable timestamp hai). */
    fun files(ctx: Context): List<File> =
        (dir(ctx).listFiles { f -> f.isFile && f.name.startsWith("auto-") && f.name.endsWith(".json") }
            ?: emptyArray()).sortedByDescending { it.name }

    fun due(now: Long, lastTryAt: Long): Boolean = lastTryAt <= 0L || now - lastTryAt >= GAP_MS

    /** Turant backup likhta hai aur KEEP se purani files hata deta hai. Exception throw kar sakta hai. */
    @Synchronized
    fun runNow(ctx: Context, now: Long = System.currentTimeMillis()): File {
        val json = FullBackup.export(ctx)
        dir(ctx).mkdirs()
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date(now))
        val f = File(dir(ctx), "auto-$stamp.json")
        f.writeText(json)
        files(ctx).drop(KEEP).forEach { it.delete() }
        return f
    }

    /**
     * App-open hook: on ho aur due ho to backup banata hai. Koshish ka time pehle likhta hai taaki
     * failure pe har app-open par retry-storm na lage. Return: sach me backup bana ya nahi.
     */
    suspend fun maybe(ctx: Context, now: Long = System.currentTimeMillis()): Boolean {
        val s = LdApp.instance.repo.flow.first()
        if (!s.autoBackup || !due(now, s.lastAutoBackupTry)) return false
        LdApp.instance.repo.set(K.lastAutoBackupTry, now)
        return withContext(Dispatchers.IO) {
            try {
                runNow(ctx, now)
                LdApp.instance.repo.set(K.lastAutoBackupAt, now)
                true
            } catch (e: Exception) {
                false
            }
        }
    }
}
