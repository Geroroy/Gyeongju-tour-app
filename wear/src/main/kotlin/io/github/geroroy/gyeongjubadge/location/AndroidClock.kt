package io.github.geroroy.gyeongjubadge.location

import android.os.SystemClock
import io.github.geroroy.gyeongjubadge.core.time.Clock

/**
 * Android 시계. `elapsedRealtime` 은 `Location.elapsedRealtimeNanos` 와 같은 기준이라
 * 위치 신선도 비교에 그대로 쓸 수 있다.
 */
object AndroidClock : Clock {
    override fun elapsedRealtimeMillis(): Long = SystemClock.elapsedRealtime()

    override fun currentTimeMillis(): Long = System.currentTimeMillis()
}
