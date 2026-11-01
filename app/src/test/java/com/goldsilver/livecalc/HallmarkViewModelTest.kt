package com.goldsilver.livecalc

import com.goldsilver.livecalc.data.model.PurityTable
import com.goldsilver.livecalc.ui.viewmodel.HallmarkViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HallmarkViewModelTest {

    private lateinit var viewModel: HallmarkViewModel

    @Before
    fun setUp() {
        viewModel = HallmarkViewModel()
    }

    @Test
    fun testHuidRegexValidation() {
        val regex = HallmarkViewModel.HUID_REGEX

        // Valid 6-character alphanumeric
        assertTrue(regex.matches("AB12C3"))
        assertTrue(regex.matches("123456"))
        assertTrue(regex.matches("ABCDEF"))
        assertTrue(regex.matches("9XYZ80"))

        // Invalid: too short
        assertFalse(regex.matches(""))
        assertFalse(regex.matches("A"))
        assertFalse(regex.matches("AB12C"))

        // Invalid: too long
        assertFalse(regex.matches("AB12C3D"))
        assertFalse(regex.matches("1234567"))

        // Invalid: special characters
        assertFalse(regex.matches("AB-12C"))
        assertFalse(regex.matches("AB 12C"))
        assertFalse(regex.matches("AB_12C"))
        assertFalse(regex.matches("AB12C!"))

        // Invalid: lowercase (regex is uppercase only)
        assertFalse(regex.matches("ab12c3"))
    }

    @Test
    fun testOnHuidChangeSanitizesAndLimitsLength() {
        // Lowercase input should be converted to uppercase
        viewModel.onHuidChange("ab12c3")
        assertEquals("AB12C3", viewModel.huidInput.value)
        assertTrue(viewModel.isHuidValid.value)
        assertFalse(viewModel.isError.value)

        // Longer input should be clamped to 6 characters
        viewModel.onHuidChange("abcdefgh123")
        assertEquals("ABCDEF", viewModel.huidInput.value)
        assertTrue(viewModel.isHuidValid.value)

        // Special characters and symbols should be stripped
        viewModel.onHuidChange("A-B#12!C")
        assertEquals("AB12C", viewModel.huidInput.value)
        assertFalse(viewModel.isHuidValid.value)
        assertTrue(viewModel.isError.value)
    }

    @Test
    fun testPasteHuidSanitization() {
        // Spaces should be stripped
        viewModel.pasteHuid(" A B 1 2 C 3 ")
        assertEquals("AB12C3", viewModel.huidInput.value)
        assertTrue(viewModel.isHuidValid.value)

        // Newlines, tabs and special characters should be stripped and uppercased
        viewModel.pasteHuid("ab\n12\tc3!extra")
        assertEquals("AB12C3", viewModel.huidInput.value)
        assertTrue(viewModel.isHuidValid.value)

        // Null or blank clipboard should not crash or change input
        viewModel.clearHuid()
        viewModel.pasteHuid(null)
        assertEquals("", viewModel.huidInput.value)

        viewModel.pasteHuid("   ")
        assertEquals("", viewModel.huidInput.value)
    }

    @Test
    fun testPurityTableConstants() {
        val items = PurityTable.ALL_ITEMS
        assertEquals(6, items.size)

        // 24K Gold
        val gold24k = PurityTable.GOLD_24K
        assertEquals("24K", gold24k.carat)
        assertEquals("999", gold24k.fineness)
        assertEquals(1.0, gold24k.factor, 0.0001)
        assertTrue(gold24k.isGold)

        // 22K Gold
        val gold22k = PurityTable.GOLD_22K
        assertEquals("22K", gold22k.carat)
        assertEquals("916", gold22k.fineness)
        assertEquals(0.9167, gold22k.factor, 0.0001)
        assertTrue(gold22k.isGold)

        // 18K Gold
        val gold18k = PurityTable.GOLD_18K
        assertEquals("18K", gold18k.carat)
        assertEquals("750", gold18k.fineness)
        assertEquals(0.75, gold18k.factor, 0.0001)
        assertTrue(gold18k.isGold)

        // 14K Gold
        val gold14k = PurityTable.GOLD_14K
        assertEquals("14K", gold14k.carat)
        assertEquals("585", gold14k.fineness)
        assertEquals(0.5833, gold14k.factor, 0.0001)
        assertTrue(gold14k.isGold)

        // Silver 999
        val silver999 = PurityTable.SILVER_999
        assertEquals("999", silver999.carat)
        assertEquals("999", silver999.fineness)
        assertEquals(1.0, silver999.factor, 0.0001)
        assertFalse(silver999.isGold)

        // Silver 925
        val silver925 = PurityTable.SILVER_925
        assertEquals("925", silver925.carat)
        assertEquals("925", silver925.fineness)
        assertEquals(0.925, silver925.factor, 0.0001)
        assertFalse(silver925.isGold)
    }
}
