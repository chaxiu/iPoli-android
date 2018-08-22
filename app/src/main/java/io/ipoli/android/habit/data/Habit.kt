package io.ipoli.android.habit.data

import io.ipoli.android.common.datetime.Time
import io.ipoli.android.common.persistence.EntityWithTags
import io.ipoli.android.player.data.Player
import io.ipoli.android.quest.Color
import io.ipoli.android.quest.Icon
import io.ipoli.android.tag.Tag
import org.threeten.bp.DayOfWeek
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime

/**
 * Created by Polina Zhelyazkova <polina@mypoli.fun>
 * on 6/16/18.
 */
data class Habit(
    override val id: String = "",
    val name: String,
    val color: Color,
    val icon: Icon,
    override val tags: List<Tag> = listOf(),
    val days: Set<DayOfWeek>,
    val isGood: Boolean,
    val timesADay: Int = 1,
    val challengeId: String? = null,
    val note: String = "",
    val history: Map<LocalDate, CompletedEntry> = emptyMap(),
    val currentStreak: Int = 0,
    val prevStreak: Int = 0,
    val bestStreak: Int = 0,
    val isRemoved: Boolean = false,
    override val createdAt: Instant = Instant.now(),
    override val updatedAt: Instant = Instant.now(),
    val removedAt: Instant? = null
) : EntityWithTags {

    fun isCompletedFor(date: LocalDateTime = LocalDateTime.now(), resetDayTime: Time): Boolean {
        if (!shouldBeDoneOn(date, resetDayTime)) return true
        return completedCountForDate(date, resetDayTime) >= timesADay
    }

    fun completedCountForDate(date: LocalDateTime = LocalDateTime.now(), resetDayTime: Time): Int {

        val (startDate, endDate) = Player.datesSpan(date, resetDayTime)

        var completedCount = 0

        history[startDate]?.let {
            completedCount += it.completedAtTimes.count { t -> t >= resetDayTime }
        }

        endDate?.let {
            history[it]?.let { ce ->
                completedCount += ce.completedAtTimes.count { t -> t < resetDayTime }
            }
        }


        return completedCount
    }

    fun shouldBeDoneOn(date: LocalDateTime = LocalDateTime.now(), resetDayTime: Time): Boolean {
        val currentDate = Player.currentDate(date, resetDayTime)
        return days.contains(currentDate.dayOfWeek)
    }

}

data class CompletedEntry(
    val completedAtTimes: List<Time> = emptyList(),
    val experience: Int? = null,
    val coins: Int? = null
) {
    val completedCount = completedAtTimes.size

    fun complete(time: Time = Time.now()) =
        copy(
            completedAtTimes = completedAtTimes + time
        )

    fun undoLastComplete() =
        copy(
            completedAtTimes = completedAtTimes.dropLast(1)
        )
}