package com.goldsilver.livecalc

import com.goldsilver.livecalc.ui.viewmodel.SplashViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SplashViewModelTest {

    @Test
    fun testMaxSplashHoldCap() {
        // Must be capped at 1500 ms and never wait indefinitely
        assertEquals(1500L, SplashViewModel.MAX_SPLASH_HOLD_MS)
        assertTrue(SplashViewModel.MAX_SPLASH_HOLD_MS <= 1500L)
    }
}
