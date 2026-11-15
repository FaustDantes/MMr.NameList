package com.michalmarcin.namelist.platform

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * The backup password set in settings (F17, ADR-028). The app keeps only a check value (PBKDF2 hash with
 * its own salt), never the password. The check lets the user change the password with the old one and
 * stops a typing error from locking a backup. Each backup is still encrypted with a fresh salt.
 */
class BackupPasswordStore(
    context: Context,
    private val iterations: Int = BackupCrypto.DEFAULT_ITERATIONS,
) {
    private val prefs = context.getSharedPreferences("backup_password", Context.MODE_PRIVATE)
    private val random = SecureRandom()
    private val state = MutableStateFlow(prefs.contains(KEY_HASH))

    /** True when a password is set. */
    val isSet: StateFlow<Boolean> = state.asStateFlow()

    suspend fun set(password: CharArray) = withContext(Dispatchers.Default) {
        require(password.size >= MIN_LENGTH) { "Password too short" }
        val salt = ByteArray(BackupCrypto.SALT_BYTES).also(random::nextBytes)
        val hash = BackupCrypto.deriveKey(password, salt, iterations)
        prefs.edit()
            .putString(KEY_SALT, encode(salt))
            .putString(KEY_HASH, encode(hash))
            .putInt(KEY_ITERATIONS, iterations)
            .apply()
        state.value = true
    }

    suspend fun verify(password: CharArray): Boolean = withContext(Dispatchers.Default) {
        val salt = prefs.getString(KEY_SALT, null)?.let(::decode) ?: return@withContext false
        val expected = prefs.getString(KEY_HASH, null)?.let(::decode) ?: return@withContext false
        val rounds = prefs.getInt(KEY_ITERATIONS, iterations)
        // Constant-time compare.
        MessageDigest.isEqual(expected, BackupCrypto.deriveKey(password, salt, rounds))
    }

    /** Sets [new] only when [old] is right. Returns false when [old] is wrong. */
    suspend fun change(old: CharArray, new: CharArray): Boolean {
        if (!verify(old)) return false
        set(new)
        return true
    }

    /** Removes the password only when [old] is right. Backups made before stay locked with it. */
    suspend fun remove(old: CharArray): Boolean {
        if (!verify(old)) return false
        reset()
        return true
    }

    /** For a forgotten password. Old encrypted backups can still only be opened with the old password. */
    fun reset() {
        prefs.edit().clear().apply()
        state.value = false
    }

    private fun encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

    private fun decode(text: String): ByteArray = Base64.getDecoder().decode(text)

    companion object {
        const val MIN_LENGTH = 8
        private const val KEY_SALT = "salt"
        private const val KEY_HASH = "hash"
        private const val KEY_ITERATIONS = "iterations"
    }
}
