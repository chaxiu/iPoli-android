package mypoli.android.repeatingquest.usecase

import mypoli.android.common.UseCase
import mypoli.android.common.datetime.datesUntil
import mypoli.android.common.datetime.isBetween
import mypoli.android.quest.Quest
import mypoli.android.quest.data.persistence.QuestRepository
import mypoli.android.repeatingquest.entity.RepeatingPattern
import mypoli.android.repeatingquest.entity.RepeatingQuest
import mypoli.android.repeatingquest.persistence.RepeatingQuestRepository
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.temporal.TemporalAdjusters
import org.threeten.bp.temporal.TemporalAdjusters.lastDayOfMonth

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 02/14/2018.
 */
class FindQuestsForRepeatingQuest(
    private val questRepository: QuestRepository,
    private val repeatingQuestRepository: RepeatingQuestRepository
) : UseCase<FindQuestsForRepeatingQuest.Params, FindQuestsForRepeatingQuest.Result> {

    override fun execute(parameters: Params): FindQuestsForRepeatingQuest.Result {
        val rq = parameters.repeatingQuest

        require(parameters.end.isAfter(parameters.start))

        if (parameters.end.isBefore(rq.start)) {
            return Result(listOf(), rq)
        }

        if (rq.end != null && parameters.start.isAfter(rq.end)) {
            return Result(listOf(), rq)
        }

        val start = if (parameters.start.isBefore(rq.start)) rq.start else parameters.start
        val end = if (rq.end != null && rq.end.isBefore(parameters.end)) rq.end else parameters.end

        val (scheduleDates, pattern) = when (rq.repeatingPattern) {

            RepeatingPattern.Daily -> {
                Pair(start.datesUntil(end).toSet(), rq.repeatingPattern)
            }

            is RepeatingPattern.Weekly -> Pair(
                weeklyDatesToScheduleInPeriod(
                rq.repeatingPattern,
                start,
                end
                ), rq.repeatingPattern
            )

            is RepeatingPattern.Monthly -> Pair(
                monthlyDatesToScheduleInPeriod(
                rq.repeatingPattern,
                start,
                end
                ), rq.repeatingPattern
            )

            is RepeatingPattern.Yearly -> Pair(
                yearlyDatesToScheduleInPeriod(
                rq.repeatingPattern,
                start,
                end
                ), rq.repeatingPattern
            )

            is RepeatingPattern.Flexible.Weekly -> {
                val scheduledPeriods = rq.repeatingPattern.scheduledPeriods.toMutableMap()
                val periods = findWeeklyPeriods2(start, end, parameters.lastDayOfWeek)
                periods.forEach {
                    if (!scheduledPeriods.containsKey(it.start)) {
                        scheduledPeriods[it.start] =
                            generateWeeklyFlexibleDates(rq.repeatingPattern, it)
                    }
                }
                val pattern = rq.repeatingPattern.copy(
                    scheduledPeriods = scheduledPeriods
                )

                Pair(
                    flexibleWeeklyToScheduleInPeriod(
                        pattern,
                        start,
                        end,
                        parameters.lastDayOfWeek
                    ), pattern
                )
            }


            is RepeatingPattern.Flexible.Monthly ->
                Pair(
                    flexibleMonthlyToScheduleInPeriod(rq.repeatingPattern, start, end),
                    rq.repeatingPattern
                )
        }


        if (scheduleDates.isEmpty()) {
            return Result(listOf(), rq)
        }

        val scheduledQuests = questRepository.findForRepeatingQuestBetween(rq.id, start, end)
        val (removed, existing) = scheduledQuests.partition { it.isRemoved }
        val schedule = existing.associateBy({ it.originalScheduledDate }, { it })
        val removedDates = removed.map { it.originalScheduledDate }
        val resultDates = scheduleDates - removedDates

        val quests = resultDates.map {

            if (schedule.containsKey(it)) {
                schedule[it]!!
            } else {
                createQuest(rq, it)
            }
        }
        return Result(
            quests,
            repeatingQuestRepository.save(
                rq.copy(
                    repeatingPattern = pattern
                )
            )
        )
    }

    private fun flexibleMonthlyToScheduleInPeriod(
        pattern: RepeatingPattern.Flexible.Monthly,
        start: LocalDate,
        end: LocalDate
    ): List<LocalDate> {
        val preferredDays = pattern.preferredDays
        val timerPerMonth = pattern.timesPerMonth
        require(timerPerMonth != preferredDays.size)
        require(timerPerMonth >= 1)
        require(timerPerMonth <= 31)

        val periods = findMonthlyPeriods(start, end)
        val result = mutableListOf<LocalDate>()

        for (p in periods) {
            val daysOfMonth =
                (1..p.start.lengthOfMonth()).map { it }.shuffled().take(timerPerMonth)

            val days = if (preferredDays.isNotEmpty()) {
                val scheduledMonthDays = preferredDays.shuffled().take(timerPerMonth)
                val remainingMonthDays =
                    (daysOfMonth - scheduledMonthDays).shuffled().take(timerPerMonth - scheduledMonthDays.size)
                scheduledMonthDays + remainingMonthDays
            } else {
                daysOfMonth.shuffled().take(timerPerMonth)
            }

            result.addAll(
                days
                    .map { p.start.withDayOfMonth(it) }
                    .filter { it.isBetween(p.start, p.end) }
            )
        }

        return result
    }

    private fun flexibleWeeklyToScheduleInPeriod(
        pattern: RepeatingPattern.Flexible.Weekly,
        start: LocalDate,
        end: LocalDate,
        lastDayOfWeek: DayOfWeek
    ): List<LocalDate> {

        val preferredDays = pattern.preferredDays
        val timesPerWeek = pattern.timesPerWeek
        require(timesPerWeek != preferredDays.size)
        require(timesPerWeek >= 1)
        require(timesPerWeek <= 7)

        val periods = findWeeklyPeriods2(start, end, lastDayOfWeek)

        val result = mutableListOf<LocalDate>()

        for (p in periods) {
            result.addAll(
                pattern.scheduledPeriods[p.start]!!
                    .filter { it.isBetween(start, end) }
            )
        }

        return result
    }

    private fun generateWeeklyFlexibleDates(
        pattern: RepeatingPattern.Flexible.Weekly,
        period: Period
    ): List<LocalDate> {
        val preferredDays = pattern.preferredDays
        val timesPerWeek = pattern.timesPerWeek

        val daysOfWeek = DayOfWeek.values().toList()
        val days = if (preferredDays.isNotEmpty()) {
            val scheduledWeekDays = preferredDays.shuffled().take(timesPerWeek)
            val remainingWeekDays =
                (daysOfWeek - scheduledWeekDays).shuffled()
                    .take(timesPerWeek - scheduledWeekDays.size)
            scheduledWeekDays + remainingWeekDays
        } else {
            daysOfWeek.shuffled().take(timesPerWeek)
        }
        return days.map { period.start.with(it) }

    }

    data class Period(val start: LocalDate, val end: LocalDate)

    private fun findWeeklyPeriods(
        start: LocalDate,
        end: LocalDate,
        lastDayOfWeek: DayOfWeek
    ): List<Period> {

        val periods = mutableListOf<Period>()

        var periodStart = start
        val dayAfterEnd = end.plusDays(1)
        while (periodStart.isBefore(dayAfterEnd)) {
            val periodEnd = periodStart.with(lastDayOfWeek)
            periods.add(Period(periodStart, if (periodEnd.isAfter(end)) end else periodEnd))
            periodStart = periodEnd.plusDays(1)
        }

        return periods
    }

    private fun findWeeklyPeriods2(
        start: LocalDate,
        end: LocalDate,
        lastDayOfWeek: DayOfWeek
    ): List<Period> {

        val periods = mutableListOf<Period>()
        val firstDayOfWeek = lastDayOfWeek.minus(6)


        var periodStart = start.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
        val dayAfterEnd = end.plusDays(1)
        while (periodStart.isBefore(dayAfterEnd)) {
            val periodEnd = periodStart.with(lastDayOfWeek)
            periods.add(Period(periodStart, periodEnd))
            periodStart = periodEnd.plusDays(1)
        }

        return periods
    }

    private fun findMonthlyPeriods(
        start: LocalDate,
        end: LocalDate
    ): List<Period> {
        val periods = mutableListOf<Period>()

        var periodStart = start
        val dayAfterEnd = end.plusDays(1)
        while (periodStart.isBefore(dayAfterEnd)) {

            val periodEnd = periodStart.with(lastDayOfMonth())
            periods.add(Period(periodStart, if (periodEnd.isAfter(end)) end else periodEnd))
            periodStart = periodEnd.plusDays(1)
        }

        return periods
    }

    private fun createQuest(
        rq: RepeatingQuest,
        it: LocalDate
    ): Quest {
        return Quest(
            name = rq.name,
            color = rq.color,
            icon = rq.icon,
            category = rq.category,
            startTime = rq.startTime,
            duration = rq.duration,
            scheduledDate = it,
            reminder = rq.reminder
        )
    }

    private fun monthlyDatesToScheduleInPeriod(
        repeatingPattern: RepeatingPattern.Monthly,
        start: LocalDate,
        end: LocalDate
    ): List<LocalDate> {

        var date = start
        val dates = mutableListOf<LocalDate>()
        while (date.isBefore(end.plusDays(1))) {
            if (date.dayOfMonth in repeatingPattern.daysOfMonth) {
                dates.add(date)
            }
            date = date.plusDays(1)
        }
        return dates

    }

    private fun weeklyDatesToScheduleInPeriod(
        repeatingPattern: RepeatingPattern.Weekly,
        start: LocalDate,
        end: LocalDate
    ): List<LocalDate> {

        var date = start
        val dates = mutableListOf<LocalDate>()
        while (date.isBefore(end.plusDays(1))) {
            if (date.dayOfWeek in repeatingPattern.daysOfWeek) {
                dates.add(date)
            }
            date = date.plusDays(1)
        }
        return dates

    }

    private fun yearlyDatesToScheduleInPeriod(
        repeatingPattern: RepeatingPattern.Yearly,
        start: LocalDate,
        end: LocalDate
    ): List<LocalDate> {
        if (start.year == end.year) {
            val date = LocalDate.of(
                start.year,
                repeatingPattern.month,
                repeatingPattern.dayOfMonth
            )
            return listOf(date).filter { it.isBetween(start, end) }
        }

        var startPeriodDate = start
        val dates = mutableListOf<LocalDate>()
        while (startPeriodDate <= end) {
            val lastDayOfYear = LocalDate.of(startPeriodDate.year, 12, 31)
            val date = LocalDate.of(
                startPeriodDate.year,
                repeatingPattern.month,
                repeatingPattern.dayOfMonth
            )
            val endPeriodDate = if (end.isBefore(lastDayOfYear)) end else lastDayOfYear
            if (date.isBetween(startPeriodDate, endPeriodDate)) {
                dates.add(date)
            }
            startPeriodDate = LocalDate.of(startPeriodDate.year + 1, 1, 1)
        }
        return dates

    }

    data class Result(val quests: List<Quest>, val repeatingQuest: RepeatingQuest)

    /**
     * @start inclusive
     * @end inclusive
     */
    data class Params(
        val repeatingQuest: RepeatingQuest,
        val start: LocalDate,
        val end: LocalDate,
        val firstDayOfWeek: DayOfWeek,
        val lastDayOfWeek: DayOfWeek
    )
}