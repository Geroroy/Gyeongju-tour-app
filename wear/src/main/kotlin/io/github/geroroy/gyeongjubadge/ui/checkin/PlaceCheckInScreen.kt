package io.github.geroroy.gyeongjubadge.ui.checkin

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import io.github.geroroy.gyeongjubadge.R
import io.github.geroroy.gyeongjubadge.core.checkin.CheckInResult
import io.github.geroroy.gyeongjubadge.core.model.PlaceCategory
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

/**
 * ViewModel 과 권한 흐름을 연결하는 라우트. 권한은 버튼을 누른 시점에 요청한다(docs/check-in-rules.md §6).
 */
@Composable
fun PlaceCheckInRoute(viewModel: CheckInViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        // "대략적인 위치"만 허용된 경우 FINE 이 false → 정확도 요건을 만족할 수 없으므로 거부로 처리
        if (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            viewModel.checkIn()
        } else {
            viewModel.onPermissionDenied()
        }
    }

    PlaceCheckInScreen(
        state = state,
        onCheckInClick = {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
            if (granted) {
                viewModel.checkIn()
            } else {
                permissionLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                )
            }
        },
    )
}

@Composable
fun PlaceCheckInScreen(
    state: CheckInUiState,
    onCheckInClick: () -> Unit,
) {
    val columnState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    ScreenScaffold(
        scrollState = columnState,
        edgeButton = {
            EdgeButton(
                onClick = { if (state.canCheckIn) onCheckInClick() },
                enabled = state.canCheckIn,
            ) {
                Text(
                    text = stringResource(
                        when {
                            state.isEarned -> R.string.action_earned
                            state.isChecking -> R.string.action_checking
                            else -> R.string.action_check_in
                        },
                    ),
                )
            }
        },
    ) { contentPadding ->
        TransformingLazyColumn(
            state = columnState,
            contentPadding = contentPadding,
        ) {
            item {
                ListHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                ) {
                    Text(text = state.place.name)
                }
            }
            item {
                Text(
                    text = stringResource(
                        when (state.place.category) {
                            PlaceCategory.LANDMARK -> R.string.category_landmark
                            PlaceCategory.FOOD -> R.string.category_food
                        },
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            item {
                Text(
                    text = state.place.description,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            item {
                Text(
                    text = statusText(state),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (state.isChecking) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

/** 상태 → 사용자 문구 (docs/product-spec.md §4.2). */
@Composable
private fun statusText(state: CheckInUiState): String {
    val phase = state.phase
    return when {
        phase is CheckInPhase.Checking -> stringResource(R.string.status_checking)
        phase is CheckInPhase.Completed -> resultText(phase.result)
        state.earnedBadge != null -> {
            val earnedAt = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(state.earnedBadge.earnedAtEpochMillis))
            stringResource(R.string.status_earned_at, earnedAt)
        }
        else -> stringResource(R.string.status_idle)
    }
}

@Composable
private fun resultText(result: CheckInResult): String =
    when (result) {
        is CheckInResult.Granted -> stringResource(R.string.result_granted)
        is CheckInResult.AlreadyEarned -> stringResource(R.string.result_already_earned)
        CheckInResult.PermissionDenied -> stringResource(R.string.result_permission_denied)
        is CheckInResult.LocationUnavailable -> stringResource(R.string.result_unavailable)
        is CheckInResult.Timeout -> stringResource(R.string.result_timeout, (result.timeoutMillis / 1000L).toInt())
        is CheckInResult.LowAccuracy ->
            result.accuracyMeters?.let { stringResource(R.string.result_low_accuracy, it.roundToInt()) }
                ?: stringResource(R.string.result_low_accuracy_unknown)
        is CheckInResult.StaleLocation -> stringResource(R.string.result_stale_location)
        is CheckInResult.OutOfRange -> stringResource(R.string.result_out_of_range, result.distanceMeters.roundToInt())
    }
