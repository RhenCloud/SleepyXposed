package com.rhencloud.sleepyxposed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LogLevel {
    VERBOSE, DEBUG, INFO, WARN, ERROR
}

object LogRepository {
    private val _logs = MutableSharedFlow<String>(replay = 30)
    val logs = _logs.asSharedFlow()
    var LogLv = LogLevel.INFO
        private set

    fun setLogLevel(level: String) {
        LogLv = when (level.uppercase()) {
            "VERBOSE" -> LogLevel.VERBOSE
            "DEBUG" -> LogLevel.DEBUG
            "WARN" -> LogLevel.WARN
            "ERROR" -> LogLevel.ERROR
            else -> LogLevel.INFO
        }
    }

    fun addLog(level: LogLevel, message: String) {
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val currentTime = timeFormat.format(Date())

        if (level >= LogLv) _logs.tryEmit("[$level]-[$currentTime] $message")
    }
}

class LogViewModel : ViewModel() {
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs

    init {
        viewModelScope.launch {
            LogRepository.logs.collectLatest { newLog ->
                _logs.value = (_logs.value + newLog).takeLast(100)
            }
        }
    }
}
