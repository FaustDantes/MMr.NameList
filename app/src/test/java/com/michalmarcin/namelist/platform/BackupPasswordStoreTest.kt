package com.michalmarcin.namelist.platform

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** F17: the password in settings. The app keeps a check value, never the password. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class BackupPasswordStoreTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val store = BackupPasswordStore(context, iterations = 1_000)

    private fun chars(text: String) = text.toCharArray()

    @Test
    fun startsWithoutAPassword() {
        assertFalse(store.isSet.value)
        assertFalse(runBlocking { store.verify(chars("anything goes")) })
    }

    @Test
    fun setThenVerify() = runBlocking {
        store.set(chars("my secret 1"))
        assertTrue(store.isSet.value)
        assertTrue(store.verify(chars("my secret 1")))
        assertFalse(store.verify(chars("my secret 2")))
        assertFalse(store.verify(chars("")))
    }

    @Test
    fun theSavedValueSurvivesARestartAndDoesNotContainThePassword() = runBlocking {
        store.set(chars("my secret 1"))
        val again = BackupPasswordStore(context, iterations = 1_000)
        assertTrue(again.isSet.value)
        assertTrue(again.verify(chars("my secret 1")))
        val saved = context.getSharedPreferences("backup_password", Context.MODE_PRIVATE).all.values.joinToString()
        assertFalse(saved.contains("my secret"))
    }

    @Test
    fun aShortPasswordIsRefused() {
        try {
            runBlocking { store.set(chars("short")) }
            fail("Expected a refusal")
        } catch (_: IllegalArgumentException) {
        }
        assertFalse(store.isSet.value)
    }

    @Test
    fun changeNeedsTheOldPassword() = runBlocking {
        store.set(chars("first password"))
        assertFalse(store.change(chars("wrong old one"), chars("second password")))
        assertTrue("the old password still works", store.verify(chars("first password")))

        assertTrue(store.change(chars("first password"), chars("second password")))
        assertTrue(store.verify(chars("second password")))
        assertFalse(store.verify(chars("first password")))
    }

    @Test
    fun removeNeedsTheOldPassword() = runBlocking {
        store.set(chars("first password"))
        assertFalse(store.remove(chars("nope nope nope")))
        assertTrue(store.isSet.value)
        assertTrue(store.remove(chars("first password")))
        assertFalse(store.isSet.value)
    }

    @Test
    fun resetRemovesAForgottenPassword() = runBlocking {
        store.set(chars("first password"))
        store.reset()
        assertFalse(store.isSet.value)
        assertFalse(BackupPasswordStore(context, iterations = 1_000).isSet.value)
        store.set(chars("brand new one"))
        assertEquals(true, store.verify(chars("brand new one")))
    }
}
