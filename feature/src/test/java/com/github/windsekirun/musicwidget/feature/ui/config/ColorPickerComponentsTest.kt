package com.github.windsekirun.musicwidget.feature.ui.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorPickerComponentsTest {

    @Test
    fun presetColors_containsRequiredColors() {
        val presetHexes = PRESET_COLORS.map { it.hex.uppercase() }
        val expectedHexes = listOf(
            "#FFFFFF", // White
            "#5CB3FF", // Shirakami Fubuki Cosmic Blue
            "#FAD02C", // Star Gold
            "#A855F7", // Neon Purple
            "#10B981", // Mint
            "#F43F5E", // Coral
            "#F59E0B"  // Amber
        )

        assertEquals("Should contain 7 preset colors", 7, PRESET_COLORS.size)
        expectedHexes.forEach { hex ->
            assertTrue("Should contain preset $hex", presetHexes.contains(hex))
        }
    }

    @Test
    fun normalizeHex_formatsCorrectly() {
        assertEquals("#FFFFFF", normalizeHex("ffffff"))
        assertEquals("#FFFFFF", normalizeHex("#ffffff"))
        assertEquals("#5CB3FF", normalizeHex("5cb3ff"))
        assertEquals("#5CB3FF", normalizeHex("#5cb3ff"))
        assertEquals("#A855F7", normalizeHex("  #a855f7  "))
        assertEquals("#10B981", normalizeHex("10b981"))
    }

    @Test
    fun isValidHexColor_validatesHexStrings() {
        assertTrue(isValidHexColor("#FFFFFF"))
        assertTrue(isValidHexColor("#ffffff"))
        assertTrue(isValidHexColor("ffffff"))
        assertTrue(isValidHexColor("#5CB3FF"))
        assertTrue(isValidHexColor("#A855F7"))
        assertTrue(isValidHexColor("#FF10B981")) // 8-char ARGB

        assertFalse(isValidHexColor(""))
        assertFalse(isValidHexColor("#"))
        assertFalse(isValidHexColor("#FFF")) // 3-char shorthand not supported for standard android parseColor
        assertFalse(isValidHexColor("#FFFFF")) // 5-char
        assertFalse(isValidHexColor("#GGGGGG")) // Invalid characters
        assertFalse(isValidHexColor("12345Z")) // Invalid character
    }

    @Test
    fun parseColorOrNull_parsesValidAndHandlesInvalid() {
        val validColor = parseColorOrNull("#5CB3FF")
        assertNotNull("Valid hex should parse", validColor)

        val validNoHash = parseColorOrNull("5CB3FF")
        assertNotNull("Valid hex without hash should parse", validNoHash)

        val invalidColor = parseColorOrNull("invalid_hex")
        assertNull("Invalid hex should return null without throwing", invalidColor)
    }
}
