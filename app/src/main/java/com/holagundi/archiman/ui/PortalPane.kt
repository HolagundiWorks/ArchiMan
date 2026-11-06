package com.holagundi.archiman.ui

import com.holagundi.archiman.ui.theme.CarbonButton as Button
import com.holagundi.archiman.ui.theme.CarbonOutlinedButton as OutlinedButton

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.holagundi.archiman.R

@Composable
fun PortalPane(viewModel: PortalViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.start()
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.portal_heading), style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        Text(stringResource(R.string.portal_body), color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (state.running) {
            Text(stringResource(R.string.portal_on), color = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.portal_address), style = MaterialTheme.typography.labelLarge)
            SelectionContainer {
                Text(state.address.orEmpty(), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.titleMedium)
            }
            Text("Pair this browser", style = MaterialTheme.typography.titleMedium)
            Text("Before entering the access code, open the browser certificate details and compare its SHA-256 fingerprint with this phone. Accept the local certificate only when every group matches. Repeat after the Wi-Fi address changes. Never bypass a mismatched certificate.")
            SelectionContainer { Text(state.fingerprint.orEmpty(), fontFamily = FontFamily.Monospace) }
            Text(stringResource(R.string.portal_code), style = MaterialTheme.typography.labelLarge)
            SelectionContainer {
                Text(state.code.orEmpty(), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            }
            Text(stringResource(R.string.portal_readonly), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.portal_hour), color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(
                onClick = viewModel::stop,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) { Text(stringResource(R.string.portal_stop)) }
        } else {
            Text(state.detail ?: stringResource(R.string.portal_off), color = if (state.detail == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error)
            Button(
                onClick = {
                    val needsNotice = Build.VERSION.SDK_INT >= 33 &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    if (needsNotice) permission.launch(Manifest.permission.POST_NOTIFICATIONS) else viewModel.start()
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) { Text(stringResource(R.string.portal_start)) }
        }
    }
}
