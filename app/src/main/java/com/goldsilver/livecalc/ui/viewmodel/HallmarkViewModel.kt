package com.goldsilver.livecalc.ui.viewmodel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goldsilver.livecalc.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Locale

class HallmarkViewModel : ViewModel() {

    companion object {
        const val BIS_CARE_PACKAGE_NAME = "com.bis.bisapp"
        const val BIS_GOV_URL = "https://www.bis.gov.in"
        val HUID_REGEX = Regex("^[A-Z0-9]{6}$")
    }

    private val _huidInput = MutableStateFlow("")
    val huidInput: StateFlow<String> = _huidInput.asStateFlow()

    private val _isHuidValid = MutableStateFlow(false)
    val isHuidValid: StateFlow<Boolean> = _isHuidValid.asStateFlow()

    private val _isError = MutableStateFlow(false)
    val isError: StateFlow<Boolean> = _isError.asStateFlow()

    private fun updateState(sanitized: String) {
        _huidInput.value = sanitized
        val valid = HUID_REGEX.matches(sanitized)
        _isHuidValid.value = valid
        _isError.value = sanitized.isNotEmpty() && !valid
    }

    /**
     * Sanitizes user typed input: uppercase only, letters & digits, max 6 characters.
     */
    fun onHuidChange(input: String) {
        val sanitized = input
            .filter { it.isLetterOrDigit() }
            .uppercase(Locale.US)
            .take(6)
        updateState(sanitized)
    }

    /**
     * Pastes text from clipboard: strips spaces, filters alphanumeric, uppercases, max 6 characters.
     */
    fun pasteHuid(clipboardText: String?) {
        if (clipboardText.isNullOrBlank()) return
        val sanitized = clipboardText
            .replace("\\s+".toRegex(), "")
            .filter { it.isLetterOrDigit() }
            .uppercase(Locale.US)
            .take(6)
        updateState(sanitized)
    }

    fun clearHuid() {
        updateState("")
    }

    /**
     * Copies HUID to clipboard, triggers snackbar, and launches the BIS CARE app or Play Store.
     */
    fun verifyInBisCare(
        context: Context,
        onShowSnackbar: (message: String, actionLabel: String?, onAction: (() -> Unit)?) -> Unit
    ) {
        val huid = _huidInput.value.trim()
        if (!HUID_REGEX.matches(huid)) return

        // 1. Copy HUID to system clipboard
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (clipboard != null) {
                val clip = ClipData.newPlainText("HUID", huid)
                clipboard.setPrimaryClip(clip)
            }
        } catch (_: Exception) {}

        // 2. Notify user that HUID was copied
        val copiedMsg = context.getString(R.string.snackbar_huid_copied)
        onShowSnackbar(copiedMsg, null, null)

        // 3. Launch BIS CARE app if installed
        val packageManager = context.packageManager
        val launchIntent = packageManager.getLaunchIntentForPackage(BIS_CARE_PACKAGE_NAME)
        if (launchIntent != null) {
            try {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return
            } catch (_: Exception) {}
        }

        // 4. Fallback to Play Store
        openPlayStore(context, BIS_CARE_PACKAGE_NAME, onShowSnackbar)
    }

    /**
     * Opens BIS CARE on Google Play Store with market:// URI and web fallback.
     */
    fun openPlayStore(
        context: Context,
        packageName: String = BIS_CARE_PACKAGE_NAME,
        onShowSnackbar: (message: String, actionLabel: String?, onAction: (() -> Unit)?) -> Unit
    ) {
        try {
            val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(marketIntent)
        } catch (_: Exception) {
            try {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            } catch (_: Exception) {
                // Show Snackbar on failure
                val errorMsg = context.getString(R.string.snackbar_bis_care_error)
                val openLabel = context.getString(R.string.action_open)
                onShowSnackbar(errorMsg, openLabel) {
                    try {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(browserIntent)
                    } catch (_: Exception) {}
                }
            }
        }
    }

    /**
     * Opens official BIS website in browser.
     */
    fun openBisWebsite(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(BIS_GOV_URL)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}
