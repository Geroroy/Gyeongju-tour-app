package io.github.geroroy.gyeongjubadge.core.time

/**
 * 시계 추상화.
 *
 * - [elapsedRealtimeMillis]: 단조 증가 시계. 위치 신선도 비교에 사용(사용자가 바꿀 수 없음).
 * - [currentTimeMillis]: 벽시계(epoch). 배지 획득 시각 기록에만 사용.
 */
interface Clock {
    fun elapsedRealtimeMillis(): Long

    fun currentTimeMillis(): Long
}

/** JVM 기본 구현. Android 에서는 SystemClock.elapsedRealtime 기반 구현을 주입한다. */
object SystemClock : Clock {
    override fun elapsedRealtimeMillis(): Long = System.nanoTime() / 1_000_000

    override fun currentTimeMillis(): Long = System.currentTimeMillis()
}
