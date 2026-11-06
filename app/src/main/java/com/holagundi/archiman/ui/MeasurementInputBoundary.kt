@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@file:Suppress("DEPRECATION")

package com.holagundi.archiman.ui

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.*
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.awaitCancellation

/** Blocks IME sessions in this subtree only; other app screens keep their normal system keyboard. */
@Composable
fun MeasurementInputBoundary(content: @Composable () -> Unit) {
    val view = LocalView.current
    val context = LocalContext.current
    val visible = WindowInsets.isImeVisible
    fun hideExistingIme() {
        (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(view.windowToken, 0)
    }
    LaunchedEffect(visible) { hideExistingIme() }
    InterceptPlatformTextInput(interceptor = { _, _ -> awaitCancellation() }) {
        // Material's value-based TextField in Compose 1.7 also uses the legacy adapter.
        CompositionLocalProvider(LocalTextInputService provides null, content = content)
    }
}
