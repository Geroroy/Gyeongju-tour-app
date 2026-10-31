package io.github.geroroy.gyeongjubadge.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import io.github.geroroy.gyeongjubadge.core.location.LocationFix
import io.github.geroroy.gyeongjubadge.core.location.LocationFixResult
import io.github.geroroy.gyeongjubadge.core.location.LocationProvider
import io.github.geroroy.gyeongjubadge.core.location.LocationRequestSpec
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/**
 * FusedLocationProviderClient 어댑터. docs/architecture.md §4
 *
 * - 정밀 위치 권한이 없으면 [LocationFixResult.PermissionDenied].
 * - `getCurrentLocation` 1회 요청. 요청 조건에 spec 의 timeout/maxAge 를 그대로 반영한다.
 * - 코루틴이 취소되면 FLP 요청도 취소한다.
 */
class FusedLocationProvider(
    context: Context,
) : LocationProvider {
    private val appContext = context.applicationContext
    private val client: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(appContext)
    }

    fun hasFineLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission") // hasFineLocationPermission() 으로 선검사
    override suspend fun currentFix(spec: LocationRequestSpec): LocationFixResult {
        if (!hasFineLocationPermission()) return LocationFixResult.PermissionDenied

        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setDurationMillis(spec.timeoutMillis)
            .setMaxUpdateAgeMillis(spec.maxAgeMillis)
            .build()
        val cancellation = CancellationTokenSource()

        val location = try {
            client.getCurrentLocation(request, cancellation.token).await(cancellation)
        } catch (e: CancellationException) {
            throw e
        } catch (e: SecurityException) {
            return LocationFixResult.PermissionDenied
        } catch (e: Exception) {
            return LocationFixResult.Unavailable(e.message ?: e.javaClass.simpleName)
        } ?: return LocationFixResult.Unavailable("no_location")

        return LocationFixResult.Success(
            LocationFix(
                latitude = location.latitude,
                longitude = location.longitude,
                accuracyMeters = if (location.hasAccuracy()) location.accuracy.toDouble() else null,
                elapsedRealtimeMillis = location.elapsedRealtimeNanos / 1_000_000L,
            ),
        )
    }
}
