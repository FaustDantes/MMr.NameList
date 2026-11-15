package com.michalmarcin.namelist.platform

import com.google.crypto.tink.subtle.AesGcmHkdfStreaming
import java.io.DataInputStream
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** The backup is encrypted and needs a password ([Reason.REQUIRED]), or the password is wrong or the file is damaged ([Reason.WRONG]). */
class BackupPasswordException(val reason: Reason) : Exception("Backup password: $reason") {
    enum class Reason { REQUIRED, WRONG }
}

/**
 * Password encryption of a backup file (F17, ADR-028). Format: docs/backup-format.md.
 *
 * ```
 * "MMRNLENC" | version (1 byte) | PBKDF2 iterations (4 bytes) | salt (16 bytes) | AES-256-GCM stream
 * ```
 * The key comes from the password with PBKDF2-HMAC-SHA256. The stream is Tink's AES-GCM-HKDF streaming
 * AEAD in 64 KiB segments: large backups need no more memory than one segment, and a wrong password
 * or any changed byte fails the check. The header is bound to the stream as associated data.
 * Nothing here is secret except the password: the format is public.
 */
object BackupCrypto {
    val MAGIC: ByteArray = "MMRNLENC".toByteArray(Charsets.US_ASCII)
    const val FORMAT_VERSION = 1
    const val DEFAULT_ITERATIONS = 600_000
    const val MIN_ITERATIONS = 1_000
    const val MAX_ITERATIONS = 10_000_000
    const val SALT_BYTES = 16
    private const val KEY_BYTES = 32
    private const val SEGMENT_BYTES = 64 * 1024
    private const val HEADER_BYTES = 8 + 1 + 4 + SALT_BYTES
    private val random = SecureRandom()

    /** True when [head] (the first bytes of a file) starts with the encrypted-backup marker. */
    fun isEncrypted(head: ByteArray, length: Int = head.size): Boolean =
        length >= MAGIC.size && MAGIC.indices.all { head[it] == MAGIC[it] }

    /** Reads up to [buffer].size bytes; fewer only at the end of the stream. (`readNBytes` needs API 33.) */
    fun readUpTo(input: InputStream, buffer: ByteArray): Int {
        var total = 0
        while (total < buffer.size) {
            val n = input.read(buffer, total, buffer.size - total)
            if (n < 0) break
            total += n
        }
        return total
    }

    /** Writes the header to [sink] and returns a stream that encrypts everything written to it. Close it to finish. */
    fun encrypt(sink: OutputStream, password: CharArray, iterations: Int = DEFAULT_ITERATIONS): OutputStream {
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val header = ByteBuffer.allocate(HEADER_BYTES)
            .put(MAGIC)
            .put(FORMAT_VERSION.toByte())
            .putInt(iterations)
            .put(salt)
            .array()
        sink.write(header)
        return streaming(password, salt, iterations).newEncryptingStream(sink, header)
    }

    /**
     * Reads the header from [source] (positioned at the start of the file) and returns a stream of the
     * decrypted bytes. A wrong password shows when the stream is read, as an [IOException].
     */
    fun decrypt(source: InputStream, password: CharArray): InputStream {
        val header = ByteArray(HEADER_BYTES)
        try {
            DataInputStream(source).readFully(header)
        } catch (e: EOFException) {
            throw BackupException("Damaged encrypted backup", e)
        }
        val buffer = ByteBuffer.wrap(header)
        val magic = ByteArray(MAGIC.size).also(buffer::get)
        if (!magic.contentEquals(MAGIC)) throw BackupException("Not an encrypted backup")
        val version = buffer.get().toInt()
        if (version != FORMAT_VERSION) throw BackupException("Unsupported encrypted backup version $version")
        val iterations = buffer.getInt()
        // The file is not trusted: a huge count would only freeze the phone.
        if (iterations !in MIN_ITERATIONS..MAX_ITERATIONS) throw BackupException("Damaged encrypted backup")
        val salt = ByteArray(SALT_BYTES).also(buffer::get)
        return try {
            streaming(password, salt, iterations).newDecryptingStream(source, header)
        } catch (e: GeneralSecurityException) {
            throw BackupPasswordException(BackupPasswordException.Reason.WRONG)
        } catch (e: IOException) {
            throw BackupPasswordException(BackupPasswordException.Reason.WRONG)
        }
    }

    /** PBKDF2-HMAC-SHA256, 256 bits. Also used for the password check in settings. */
    fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, KEY_BYTES * 8)
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun streaming(password: CharArray, salt: ByteArray, iterations: Int) =
        AesGcmHkdfStreaming(deriveKey(password, salt, iterations), "HmacSha256", KEY_BYTES, SEGMENT_BYTES, 0)
}
