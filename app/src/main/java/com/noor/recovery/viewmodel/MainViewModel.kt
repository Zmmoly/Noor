package com.noor.recovery.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.noor.recovery.data.Milestones
import com.noor.recovery.data.RecoveryRepository
import com.noor.recovery.data.RecoverySession
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class TimerState(
    val days: Long = 0,
    val hours: Long = 0,
    val minutes: Long = 0,
    val seconds: Long = 0,
    val totalHours: Double = 0.0,
    val ringProgress: Float = 0f    // 0..1 over 30 days
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = RecoveryRepository(application)

    private val _session = MutableStateFlow<RecoverySession?>(null)
    val session: StateFlow<RecoverySession?> = _session.asStateFlow()

    private val _timer = MutableStateFlow(TimerState())
    val timer: StateFlow<TimerState> = _timer.asStateFlow()

    private val _completedMilestones = MutableStateFlow<Set<String>>(emptySet())
    val completedMilestones: StateFlow<Set<String>> = _completedMilestones.asStateFlow()

    private var tickJob: Job? = null

    init {
        viewModelScope.launch {
            val s = repo.sessionFlow.first()
            _session.value = s
            if (s != null) startTicking()
        }
    }

    fun startSession(addictionId: String) {
        viewModelScope.launch {
            val session = RecoverySession(addictionId, System.currentTimeMillis())
            repo.saveSession(session)
            _session.value = session
            startTicking()
        }
    }

    fun resetSession() {
        viewModelScope.launch {
            tickJob?.cancel()
            repo.clearSession()
            _session.value = null
            _timer.value = TimerState()
            _completedMilestones.value = emptySet()
        }
    }

    private fun startTicking() {
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (true) {
                val session = _session.value ?: break
                val elapsed = System.currentTimeMillis() - session.startTimeMs
                val totalSec = elapsed / 1000L
                val days    = totalSec / 86400
                val hours   = (totalSec % 86400) / 3600
                val minutes = (totalSec % 3600) / 60
                val seconds = totalSec % 60
                val totalHours = elapsed / 3_600_000.0
                val progress = (days / 30f).coerceIn(0f, 1f)

                _timer.value = TimerState(days, hours, minutes, seconds, totalHours, progress)

                // Check milestones
                val done = Milestones.all
                    .filter { totalHours >= it.requiredHours }
                    .map { it.id }
                    .toSet()
                _completedMilestones.value = done

                delay(1000)
            }
        }
    }
}
