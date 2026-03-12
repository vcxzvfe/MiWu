package com.github.miwu.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.miwu.MainApplication
import com.github.miwu.R
import com.github.miwu.logic.datastore.MiotUserDataStore
import com.github.miwu.utils.Logger
import com.github.miwu.logic.repository.AppRepository
import com.github.miwu.logic.setting.AppSetting
import com.github.miwu.ui.main.MainActivity
import kndroidx.extension.start
import kndroidx.extension.toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import miwu.miot.model.MiotUser
import miwu.miot.provider.MiotLoginProvider
import java.net.SocketTimeoutException
import java.util.concurrent.TimeoutException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class LoginViewModel(
    val loginProvider: MiotLoginProvider,
    val appRepository: AppRepository,
    val miotUserDataStore: MiotUserDataStore
) : ViewModel() {
    private val logger = Logger()
    private val loginJob = Job()
    private val scope = CoroutineScope(loginJob)
    private val _qrcode = MutableStateFlow("")
    private val _event = MutableSharedFlow<Event>()
    private val _qrExpired = MutableStateFlow(false)
    private val _qrSecondsRemaining = MutableStateFlow(0)
    private var pollRetryCount = 0

    val user = MutableStateFlow("")
    val password = MutableStateFlow("")

    val qrcode = _qrcode.asStateFlow()
    val event = _event.asSharedFlow()
    val qrExpired = _qrExpired.asStateFlow()
    val qrSecondsRemaining = _qrSecondsRemaining.asStateFlow()

    companion object {
        private const val QR_TIMEOUT_SECONDS = 300 // 5 minutes
        private const val MAX_POLL_RETRIES = 60
    }

    fun requestClassicLogin() {
        val user = user.value
        val pwd = password.value
        viewModelScope.launch(Dispatchers.IO) {
            event(Event.ShowLoading(true))
            runCatching {
                loginProvider.login(user, pwd).getOrThrow()
            }.onFailure { e ->
                loginFailure(e)
            }.onSuccess { user ->
                loginSuccess(user)
                event(Event.ShowLoading(false))
            }
        }
    }

    fun requestQRCodeLogin() {
        logger.info("Request for a login qrcode")
        loginJob.cancelChildren()
        pollRetryCount = 0
        _qrExpired.value = false
        scope.launch(Dispatchers.IO) {
            runCatching {
                _qrcode.emit("")
                logger.info("Generating QR code...")
                val response = loginProvider
                    .generateLoginQrCode()
                    .getOrElse { e ->
                        logger.error("QR code generation failed: {}", e.message)
                        throw e
                    }
                logger.info("QR code API response: code={}, desc={}", response.code, response.desc)
                val qrcode = response.toQrCode()
                if (qrcode == null) {
                    logger.error(
                        "QR code data missing: loginUrl={}, lp={}",
                        response.loginUrl,
                        response.lp
                    )
                    error("generate login qrcode failure, response=${response}")
                }
                logger.info("QR code generated, polling login url: {}", qrcode.loginUrl)
                _lastLoginUrl = qrcode.loginUrl
                _qrcode.emit(qrcode.data)
                startQrCountdown()
                loginProvider
                    .loginByQrCode(qrcode.loginUrl)
                    .getOrThrow()
            }.onFailure { e ->
                if (e is SocketTimeoutException || e is TimeoutException) {
                    pollRetryCount++
                    if (pollRetryCount >= MAX_POLL_RETRIES) {
                        logger.error("QR code login exceeded max retries ({})", MAX_POLL_RETRIES)
                        _qrExpired.value = true
                        _qrSecondsRemaining.value = 0
                        loginFailure(IllegalStateException("QR code expired, tap to refresh"))
                    } else {
                        logger.info("QR code login poll timed out, retry {}/{}", pollRetryCount, MAX_POLL_RETRIES)
                        pollQRCodeLogin()
                    }
                } else {
                    logger.error("QR code login failed: {}", e.message)
                    loginFailure(e)
                }
            }.onSuccess { user ->
                loginSuccess(user)
            }
        }
    }

    private fun pollQRCodeLogin() {
        scope.launch(Dispatchers.IO) {
            runCatching {
                val qrcodeData = _qrcode.value
                if (qrcodeData.isEmpty()) return@launch
                loginProvider
                    .loginByQrCode(_lastLoginUrl ?: return@launch)
                    .getOrThrow()
            }.onFailure { e ->
                if (e is SocketTimeoutException || e is TimeoutException) {
                    pollRetryCount++
                    if (pollRetryCount >= MAX_POLL_RETRIES) {
                        logger.error("QR code login exceeded max retries ({})", MAX_POLL_RETRIES)
                        _qrExpired.value = true
                        _qrSecondsRemaining.value = 0
                        loginFailure(IllegalStateException("QR code expired, tap to refresh"))
                    } else {
                        logger.info("QR code login poll timed out, retry {}/{}", pollRetryCount, MAX_POLL_RETRIES)
                        pollQRCodeLogin()
                    }
                } else {
                    logger.error("QR code login failed: {}", e.message)
                    loginFailure(e)
                }
            }.onSuccess { user ->
                loginSuccess(user)
            }
        }
    }

    private var _lastLoginUrl: String? = null

    private fun startQrCountdown() {
        _qrSecondsRemaining.value = QR_TIMEOUT_SECONDS
        scope.launch(Dispatchers.Main) {
            for (i in QR_TIMEOUT_SECONDS downTo 0) {
                _qrSecondsRemaining.value = i
                if (i == 0) {
                    _qrExpired.value = true
                    loginJob.cancelChildren()
                }
                delay(1.seconds)
            }
        }
    }

    private suspend fun loginSuccess(user: MiotUser) {
        logger.info("login successfully")
        val user = user.copy(deviceId = MainApplication.androidId)
        miotUserDataStore.updateData { user }
        event(Event.LoginSuccess(user))
    }

    suspend fun loginFailure(e: Throwable) {
        logger.warn("login failure, cause by {}", e.stackTraceToString())
        event(Event.LoginFailure(e))
    }

    fun cancelLogin() {
        loginJob.cancelChildren()
        _qrcode.value = ""
    }

    override fun onCleared() {
        loginJob.cancelChildren()
        super.onCleared()
    }

    private suspend fun event(event: Event) = _event.emit(event)

    sealed interface Event {
        data class LoginSuccess(val user: MiotUser) : Event
        data class LoginFailure(val e: Throwable) : Event
        data class ShowLoading(val show: Boolean) : Event
    }
}