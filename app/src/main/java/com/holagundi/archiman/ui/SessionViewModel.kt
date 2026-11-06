package com.holagundi.archiman.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.holagundi.archiman.data.DemoCompanyPackage
import com.holagundi.archiman.data.DemoCompanyStore
import com.holagundi.archiman.data.SessionStore
import com.holagundi.archiman.data.SuiteRepository
import com.holagundi.archiman.data.local.PracticeDao
import com.holagundi.archiman.data.local.entity.LocalUserEntity
import com.holagundi.archiman.portal.PortalServer
import com.holagundi.archiman.portal.PortalService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class SessionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionStore: SessionStore,
    private val practiceDao: PracticeDao,
    private val suiteRepository: SuiteRepository,
    private val demoCompanyStore: DemoCompanyStore,
    private val portal: PortalServer,
) : ViewModel() {
    val company = practiceDao.observeCompany().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val users = suiteRepository.observeUsers().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _catalog = MutableStateFlow<DemoCompanyPackage?>(null)
    val catalog = _catalog.asStateFlow()
    private val _initialized = MutableStateFlow(false)
    val initialized = _initialized.asStateFlow()
    private val _signedIn = MutableStateFlow<LocalUserEntity?>(null)
    val signedIn = _signedIn.asStateFlow()
    private val _notice = MutableStateFlow<String?>(null)
    val notice = _notice.asStateFlow()
    private val _opening = MutableStateFlow(false)
    val opening = _opening.asStateFlow()

    init {
        viewModelScope.launch {
            _catalog.value = withContext(Dispatchers.IO) { runCatching { demoCompanyStore.readPackage() }.getOrNull() }
            val restored = withContext(Dispatchers.IO) {
                if (practiceDao.companyCount() > 0) suiteRepository.ensureDefaults()
                val id = sessionStore.signedInUserId.first()
                id?.let { practiceDao.activeUsers().firstOrNull() }
            }
            if (restored != null) {
                _signedIn.value = restored
            }
            _initialized.value = true
        }
    }

    fun openBuiltIn() = open(OPENED) { demoCompanyStore.openBuiltIn() }

    fun openFromUri(uri: Uri) = open(OPENED) { demoCompanyStore.openFromUri(uri) }

    fun createCompany(practiceName: String, principalName: String, city: String) =
        open(CREATED) { demoCompanyStore.createBlank(practiceName, principalName, city) }

    fun signIn(user: LocalUserEntity) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val now = System.currentTimeMillis()
                practiceDao.touchLogin(user.id, now)
                sessionStore.signIn(user.id)
            }
            _signedIn.value = user
            _notice.value = null
        }
    }

    fun signOut() {
        viewModelScope.launch {
            if (portal.isRunning()) {
                context.startService(Intent(context, PortalService::class.java).setAction(PortalService.STOP))
            }
            sessionStore.signOut()
            _signedIn.value = null
        }
    }

    fun clearNotice() {
        _notice.value = null
    }

    private fun open(success: String, block: suspend () -> Unit) {
        if (_opening.value) return
        viewModelScope.launch {
            _opening.value = true
            _notice.value = null
            val error = runCatching {
                withContext(Dispatchers.IO) {
                    block()
                    suiteRepository.ensureDefaults()
                    sessionStore.markCompanyReady()
                }
            }.exceptionOrNull()?.message
            if (error == null) practiceDao.activeUsers().firstOrNull()?.let { signIn(it) }
            _notice.value = error ?: success
            _opening.value = false
        }
    }

    companion object {
        const val OPENED = "Practice opened."
        const val CREATED = "Practice created."
    }
}
