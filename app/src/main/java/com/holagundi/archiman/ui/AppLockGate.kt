package com.holagundi.archiman.ui

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.ProcessLifecycleOwner
import com.holagundi.archiman.R
import com.holagundi.archiman.ui.theme.CarbonButton as Button

/**
 * Requires the device screen lock when the whole app process leaves the foreground.
 * Uses [ProcessLifecycleOwner] so in-app activities (pickers hosted in-process) do not
 * spuriously lock; leaving to another app still re-locks. ArchiMan has no separate password.
 */
@Composable
fun AppLockGate(enabled: Boolean, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val keyguard = remember { context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager }
    val processLifecycle = remember { ProcessLifecycleOwner.get() }
    var locked by remember { mutableStateOf(false) }
    var unlockError by remember { mutableStateOf<String?>(null) }
    val unlock = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            locked = false
            unlockError = null
        } else {
            unlockError = context.getString(R.string.app_lock_failed)
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP, lifecycleOwner = processLifecycle) {
        if (enabled && keyguard.isDeviceSecure) locked = true
    }

    if (!enabled || !locked) {
        content()
        return
    }

    fun promptUnlock() {
        unlockError = null
        if (!keyguard.isDeviceSecure) {
            locked = false
            return
        }
        val intent = if (Build.VERSION.SDK_INT >= 26) {
            keyguard.createConfirmDeviceCredentialIntent(
                context.getString(R.string.app_lock_title),
                context.getString(R.string.app_lock_body),
            )
        } else {
            null
        }
        if (intent != null) unlock.launch(intent) else locked = false
    }

    LaunchedEffect(locked) {
        if (locked) promptUnlock()
    }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.app_lock_title), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.app_lock_body), color = MaterialTheme.colorScheme.onSurfaceVariant)
        unlockError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = ::promptUnlock, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.app_lock_unlock))
        }
    }
}
