package com.michalmarcin.namelist.platform

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.util.Random

/** F17: the encryption layer on its own. Few iterations keep the tests fast; the header carries the count. */
class BackupCryptoTest {

    private val password = "correct horse".toCharArray()
    private val fast = 1_000

    private fun encrypt(data: ByteArray, pw: CharArray = password): ByteArray {
        val out = ByteArrayOutputStream()
        BackupCrypto.encrypt(out, pw, fast).use { it.write(data) }
        return out.toByteArray()
    }

    private fun decrypt(bytes: ByteArray, pw: CharArray = password): ByteArray =
        BackupCrypto.decrypt(ByteArrayInputStream(bytes), pw).use { it.readBytes() }

    /** Decrypting must fail in a way the app turns into "wrong password or damaged file". */
    private fun assertRejected(bytes: ByteArray, pw: CharArray = password) {
        try {
            decrypt(bytes, pw)
            fail("Expected the file to be rejected")
        } catch (_: BackupPasswordException) {
        } catch (_: BackupException) {
        } catch (_: IOException) {
        }
    }

    private fun sample(size: Int) = ByteArray(size).also { Random(42).nextBytes(it) }

    @Test
    fun roundTripSmallAndLargerThanOneSegment() {
        listOf(0, 1, 100, 70_000, 200_000).forEach { size ->
            val data = sample(size)
            assertArrayEquals("size $size", data, decrypt(encrypt(data)))
        }
    }

    @Test
    fun theFileStartsWithTheMarkerAndHidesTheContent() {
        val secret = "Anna Nováková, Brno".toByteArray()
        val file = encrypt(secret)
        assertTrue(BackupCrypto.isEncrypted(file))
        assertFalse(String(file, Charsets.ISO_8859_1).contains("Anna"))
    }

    @Test
    fun aPlainZipIsNotEncrypted() {
        assertFalse(BackupCrypto.isEncrypted(byteArrayOf('P'.code.toByte(), 'K'.code.toByte(), 3, 4)))
        assertFalse(BackupCrypto.isEncrypted(ByteArray(0)))
    }

    @Test
    fun theSameDataGivesADifferentFileEachTime() {
        val data = sample(1_000)
        val a = encrypt(data)
        val b = encrypt(data)
        assertFalse(a.contentEquals(b))
        assertArrayEquals(data, decrypt(b))
    }

    @Test
    fun aWrongPasswordIsRejected() {
        assertRejected(encrypt(sample(5_000)), "correct horsE".toCharArray())
    }

    @Test
    fun aChangedByteAnywhereIsRejected() {
        val file = encrypt(sample(150_000))
        listOf(9, 14, 25, 40, file.size / 2, file.size - 1).forEach { position ->
            val bad = file.copyOf().also { it[position] = (it[position].toInt() xor 1).toByte() }
            assertRejected(bad)
        }
    }

    @Test
    fun aTruncatedFileIsRejected() {
        val file = encrypt(sample(150_000))
        assertRejected(file.copyOf(file.size - 5))
        assertRejected(file.copyOf(file.size / 2))
        assertRejected(file.copyOf(10))
    }

    @Test
    fun absurdIterationCountsInAFileAreRefused() {
        val file = encrypt(sample(10))
        listOf(0, 1, Int.MAX_VALUE, -5).forEach { count ->
            val bad = file.copyOf()
            ByteBuffer.wrap(bad).position(9).putInt(count)
            try {
                decrypt(bad)
                fail("Expected $count to be refused")
            } catch (e: BackupException) {
                assertTrue(e.message!!.isNotEmpty())
            }
        }
    }

    @Test
    fun anotherFileTypeIsNotTreatedAsEncrypted() {
        try {
            decrypt("not a backup file at all, just text".toByteArray())
            fail("Expected an error")
        } catch (_: BackupException) {
        }
        assertEquals(8, BackupCrypto.MAGIC.size)
    }
}
