package com.holagundi.archiman.ui

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.holagundi.archiman.R
import com.holagundi.archiman.portal.PortalServer
import com.holagundi.archiman.portal.PortalService
import com.holagundi.archiman.portal.WifiAddresses
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class PortalViewModel @Inject constructor(
    private val portal: PortalServer,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    val state = portal.state

    fun start() {
        val address = WifiAddresses.current()
        if (address == null) {
            portal.fail(context.getString(R.string.portal_no_wifi))
            return
        }
        ContextCompat.startForegroundService(
            context,
            Intent(context, PortalService::class.java).putExtra(PortalService.ADDRESS, address),
        )
    }

    fun stop() {
        context.startService(Intent(context, PortalService::class.java).setAction(PortalService.STOP))
    }
}
