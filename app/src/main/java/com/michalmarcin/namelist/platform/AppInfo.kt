package com.michalmarcin.namelist.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import java.util.Locale

/** The build that runs now, for the About section and for bug reports (F24). */
data class AppInfo(
    val versionName: String,
    val buildNumber: Long,
    val androidRelease: String,
    val sdk: Int,
    val device: String,
    val language: String,
) {
    /** "1.2.0 (build 7)" without the words, for the subject line. */
    val shortLine: String get() = "$versionName ($buildNumber)"

    fun reportSubject(): String = "Name List $shortLine: problem report"

    /**
     * The body of the report mail. Only the build and the phone are named; nothing from the user's list.
     * [intro] is the localized "Describe the problem here:" line. The technical lines stay in English.
     */
    fun reportBody(intro: String): String = buildString {
        appendLine(intro)
        appendLine()
        appendLine()
        appendLine("---")
        appendLine("App: Name List $versionName")
        appendLine("Build: $buildNumber")
        appendLine("Android: $androidRelease (API $sdk)")
        appendLine("Device: $device")
        append("Language: $language")
    }

    companion object {
        const val CONTACT_EMAIL = "info@michalmarcin.com"

        fun read(context: Context): AppInfo {
            val info = runCatching { context.packageManager.getPackageInfo(context.packageName, 0) }.getOrNull()
            return AppInfo(
                versionName = info?.versionName ?: "?",
                buildNumber = info?.let(PackageInfoCompat::getLongVersionCode) ?: 0L,
                androidRelease = Build.VERSION.RELEASE.orEmpty(),
                sdk = Build.VERSION.SDK_INT,
                device = listOf(Build.MANUFACTURER, Build.MODEL).filter { !it.isNullOrBlank() }.joinToString(" "),
                language = Locale.getDefault().toLanguageTag(),
            )
        }

        /** Opens the mail app addressed to [CONTACT_EMAIL]. Returns false when there is no mail app. */
        fun composeMail(context: Context, subject: String? = null, body: String? = null): Boolean {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
                putExtra(Intent.EXTRA_EMAIL, arrayOf(CONTACT_EMAIL))
                subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
                body?.let { putExtra(Intent.EXTRA_TEXT, it) }
            }
            return try {
                context.startActivity(intent)
                true
            } catch (_: ActivityNotFoundException) {
                false
            }
        }
    }
}
