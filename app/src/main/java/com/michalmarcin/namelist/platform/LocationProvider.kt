package com.michalmarcin.namelist.platform

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

/** A point on Earth. Stays on the device (N3); the app never looks up a city for it (ADR-019). */
data class Coordinates(val latitude: Double, val longitude: Double) {
    /** "49.1951, 16.6068": 4 decimals is about 11 m, enough to find a place again. */
    fun format(): String = String.format(Locale.ROOT, "%.4f, %.4f", latitude, longitude)
}

/** Reads the current location once, on user request (F10). */
class LocationProvider(private val context: Context) {

    fun hasPermission(): Boolean = hasFine() || hasCoarse()

    /** The current location, or null if it is off, not allowed, or takes longer than [timeoutMs]. */
    suspend fun current(timeoutMs: Long = 20_000): Coordinates? {
        if (!hasPermission()) return null
        val manager = context.getSystemService(LocationManager::class.java) ?: return null
        val provider = providers().firstOrNull { manager.isProviderEnabled(it) } ?: return null
        return withTimeoutOrNull(timeoutMs) { request(manager, provider) }
    }

    @SuppressLint("MissingPermission") // Checked in current().
    private suspend fun request(manager: LocationManager, provider: String): Coordinates? =
        suspendCancellableCoroutine { continuation ->
            val signal = CancellationSignal()
            continuation.invokeOnCancellation { signal.cancel() }
            try {
                LocationManagerCompat.getCurrentLocation(
                    manager,
                    provider,
                    signal,
                    ContextCompat.getMainExecutor(context),
                ) { location ->
                    if (continuation.isActive) {
                        continuation.resume(location?.let { Coordinates(it.latitude, it.longitude) })
                    }
                }
            } catch (_: SecurityException) {
                if (continuation.isActive) continuation.resume(null)
            }
        }

    /** Best provider first. GPS needs fine location; with "approximate" only, it is skipped. */
    private fun providers(): List<String> = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(LocationManager.FUSED_PROVIDER)
        if (hasFine()) add(LocationManager.GPS_PROVIDER)
        add(LocationManager.NETWORK_PROVIDER)
    }

    private fun hasFine() = granted(Manifest.permission.ACCESS_FINE_LOCATION)

    private fun hasCoarse() = granted(Manifest.permission.ACCESS_COARSE_LOCATION)

    private fun granted(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    companion object {
        val PERMISSIONS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    }
}
