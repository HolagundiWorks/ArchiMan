package com.holagundi.archiman.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.holagundi.archiman.company.CompanyFileSelection

class CompanyFilePickerAccess(
    val revision: Int,
    val hasPending: () -> Boolean,
    val choose: (String) -> Unit,
    val take: () -> Pair<String, Uri>?,
)

val LocalCompanyFilePicker = staticCompositionLocalOf<CompanyFilePickerAccess> { error("Picker host missing") }

/** Registered outside AppLockGate so a normal lock/unlock cannot discard an active result. */
@Composable
fun CompanyFilePickerHost(content: @Composable () -> Unit) {
    val selection = remember { CompanyFileSelection<Uri>() }
    var revision by remember { mutableStateOf(0) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selection.complete(uri)
        revision++
    }
    val access = CompanyFilePickerAccess(revision, selection::hasPending, { origin ->
        selection.begin(origin)
        launcher.launch("*/*")
    }, selection::take)
    CompositionLocalProvider(LocalCompanyFilePicker provides access, content = content)
}
