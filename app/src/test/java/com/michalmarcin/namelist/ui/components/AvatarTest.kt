package com.michalmarcin.namelist.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class AvatarTest {

    @Test
    fun initialsUseFirstTwoWords() {
        assertEquals("AN", initials("Anna Nováková"))
        assertEquals("JM", initials("  jan   maria  novák "))
        assertEquals("E", initials("eva"))
        assertEquals("ŠD", initials("šimon dvořák"))
        assertEquals("?", initials("   "))
    }

    @Test
    fun colorDependsOnlyOnName() {
        assertEquals(AvatarColors.index("Anna"), AvatarColors.index(" anna "))
        assertEquals(AvatarColors.forName("Peter", dark = false), AvatarColors.forName("Peter", dark = false))
    }
}
