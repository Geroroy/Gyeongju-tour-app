package io.github.geroroy.gyeongjubadge.ui.checkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.geroroy.gyeongjubadge.core.checkin.CheckInResult
import io.github.geroroy.gyeongjubadge.core.checkin.CheckInService
import io.github.geroroy.gyeongjubadge.core.model.Place
import io.github.geroroy.gyeongjubadge.core.repository.BadgeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 단일 장소 인증 화면의 상태 보유자. 판정은 전부 [CheckInService] 에 위임한다.
 */
class CheckInViewModel(
    private val place: Place,
    private val checkInService: CheckInService,
    repository: BadgeRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(CheckInUiState(place = place))
    val state: StateFlow<CheckInUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeAll().collect { badges ->
                val mine = badges.firstOrNull { it.placeId == place.id }
                _state.update { it.copy(earnedBadge = mine) }
            }
        }
    }

    /** 권한이 이미 허용된 상태에서 호출한다. 진행 중이면 무시. */
    fun checkIn() {
        if (_state.value.isChecking) return
        _state.update { it.copy(phase = CheckInPhase.Checking) }
        viewModelScope.launch {
            val result = checkInService.checkIn(place)
            _state.update { it.copy(phase = CheckInPhase.Completed(result)) }
        }
    }

    /** 런타임 권한 요청을 사용자가 거부했을 때. */
    fun onPermissionDenied() {
        _state.update { it.copy(phase = CheckInPhase.Completed(CheckInResult.PermissionDenied)) }
    }

    fun dismissResult() {
        _state.update { it.copy(phase = CheckInPhase.Idle) }
    }

    companion object {
        fun factory(
            place: Place,
            checkInService: CheckInService,
            repository: BadgeRepository,
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { CheckInViewModel(place, checkInService, repository) }
            }
    }
}
