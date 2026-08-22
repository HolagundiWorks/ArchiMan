package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.SiteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanServerScreen(
    viewModel: SiteViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isServerRunning by viewModel.isServerRunning.collectAsStateWithLifecycle()
    val serverUrl by viewModel.serverUrl.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = SleekBgLight,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SleekOutline)
                        .clickable { viewModel.navigateTo(AppScreen.HOME) }
                        .testTag("btn_back_home"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = SleekTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "Local LAN Web Server",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SleekTextPrimary,
                    letterSpacing = (-0.5).sp
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Status Card
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = if (isServerRunning) SleekPrimaryContainer.copy(alpha = 0.6f) else SleekSurfaceLight,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isServerRunning) SleekPrimaryBlue else SleekOutline
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(if (isServerRunning) SleekPrimaryBlue else SleekOutline),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isServerRunning) Icons.Default.Wifi else Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = if (isServerRunning) Color.White else SleekTextSecondary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Text(
                        text = if (isServerRunning) "Web Server Running on Local Wi-Fi" else "Local Web Server Stopped",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = SleekTextPrimary
                    )

                    if (isServerRunning && serverUrl.isNotBlank()) {
                        Surface(
                            color = SleekSurfaceLight,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.5.dp, SleekPrimaryBlue),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Open in any laptop / PC browser:", fontSize = 11.sp, color = SleekTextSecondary)
                                    Text(
                                        text = serverUrl,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 17.sp,
                                        color = SleekPrimaryBlue
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Server URL", serverUrl))
                                        Toast.makeText(context, "URL copied to clipboard", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.testTag("btn_copy_server_url")
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy URL", tint = SleekPrimaryBlue)
                                }
                            }
                        }
                    }

                    // Server Toggle Button
                    Button(
                        onClick = {
                            if (isServerRunning) viewModel.stopLanServer() else viewModel.startLanServer()
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isServerRunning) MaterialTheme.colorScheme.error else SleekPrimaryBlue
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_toggle_lan_server")
                    ) {
                        Icon(
                            imageVector = if (isServerRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isServerRunning) "Stop Local Web Server" else "Start Local Web Server",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // How it works guide
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = SleekSurfaceLight,
                border = BorderStroke(1.dp, SleekOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "HOW LAPTOP WEB ACCESS WORKS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextSecondary,
                        letterSpacing = 0.5.sp
                    )

                    GuidelineItem(
                        step = "1",
                        title = "Connect to Same Wi-Fi or Hotspot",
                        description = "Connect your laptop/PC to the same Wi-Fi network as this Android device, or turn on your Android portable hotspot and connect your laptop to it."
                    )

                    GuidelineItem(
                        step = "2",
                        title = "Open Laptop Web Browser",
                        description = "Open Google Chrome, Firefox, or Safari on your laptop and type the URL shown above (e.g. $serverUrl)."
                    )

                    GuidelineItem(
                        step = "3",
                        title = "Instant Measurement Dashboard & Excel",
                        description = "The built-in web application allows project managers or billing engineers to review site entries, add measurements directly, download CSV spreadsheets, and print bill abstracts without installing any software or requiring internet connectivity."
                    )
                }
            }
        }
    }
}

@Composable
fun GuidelineItem(step: String, title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(SleekPrimaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(text = step, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SleekOnPrimaryContainer)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SleekTextPrimary)
            Text(text = description, fontSize = 12.sp, color = SleekTextSecondary)
        }
    }
}
