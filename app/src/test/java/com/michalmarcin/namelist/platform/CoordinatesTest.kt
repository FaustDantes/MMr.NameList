package com.michalmarcin.namelist.platform

import org.junit.Assert.assertEquals
import org.junit.Test

class CoordinatesTest {

    @Test
    fun formatsFourDecimalsWithDotsInAnyLocale() {
        val default = java.util.Locale.getDefault()
        try {
            java.util.Locale.setDefault(java.util.Locale.forLanguageTag("cs-CZ")) // decimal comma locale
            assertEquals("49.1951, 16.6068", Coordinates(49.195061, 16.606836).format())
            assertEquals("-33.8688, 151.2093", Coordinates(-33.86882, 151.20929).format())
        } finally {
            java.util.Locale.setDefault(default)
        }
    }
}
