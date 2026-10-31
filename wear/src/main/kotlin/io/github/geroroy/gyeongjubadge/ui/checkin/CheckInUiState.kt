package io.github.geroroy.gyeongjubadge.ui.checkin

import io.github.geroroy.gyeongjubadge.core.checkin.CheckInResult
import io.github.geroroy.gyeongjubadge.core.model.EarnedBadge
import io.github.geroroy.gyeongjubadge.core.model.Place

/** 인증 진행 단계. */
sealed interface CheckInPhase {
    data object Idle : CheckInPhase

    data object Checking : CheckInPhase

    data class Completed(val result: CheckInResult) : CheckInPhase
}

/**
 * 장소 상세 겸 인증 화면 상태. docs/architecture.md §3.3
 *
 * @property earnedBadge Room Flow 에서 구독한 획득 기록. null 이면 미획득.
 */
data class CheckInUiState(
    val place: Place,
    val earnedBadge: EarnedBadge? = null,
    val phase: CheckInPhase = CheckInPhase.Idle,
) {
    val isEarned: Boolean get() = earnedBadge != null
    val isChecking: Boolean get() = phase is CheckInPhase.Checking
    val canCheckIn: Boolean get() = !isEarned && !isChecking
}
