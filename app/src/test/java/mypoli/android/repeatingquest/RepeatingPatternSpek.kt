package mypoli.android.repeatingquest

import mypoli.android.common.datetime.DateUtils
import mypoli.android.repeatingquest.entity.RepeatingPattern
import org.amshove.kluent.`should be null`
import org.amshove.kluent.`should be true`
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.temporal.TemporalAdjusters

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 02/25/2018.
 */

class RepeatingPatternSpek : Spek({

    describe("RepeatingPattern") {

        fun shouldHaveNextDate(nextDate: LocalDate?, date: LocalDate?) {

            if (date == null) {
                nextDate.`should be null`()
                return
            }

            date.isEqual(nextDate).`should be true`()
        }

        it("should not return before start") {
            val pattern = RepeatingPattern.Daily(DateUtils.today)
            val nextDate = pattern.nextDate(DateUtils.today.minusDays(1))
            shouldHaveNextDate(nextDate!!, DateUtils.today)
        }

        it("should not return after end") {
            val pattern = RepeatingPattern.Daily(DateUtils.today, DateUtils.today.plusDays(1))
            val nextDate = pattern.nextDate(DateUtils.today.plusDays(2))
            shouldHaveNextDate(nextDate, null)
        }

        describe("Daily") {

            it("should give today for next date") {
                val pattern = RepeatingPattern.Daily(DateUtils.today)
                val nextDate = pattern.nextDate(DateUtils.today)
                shouldHaveNextDate(nextDate!!, DateUtils.today)
            }
        }

        describe("Weekly") {

            describe("Fixed") {

                it("should give today when today is monday and monday is chosen day") {
                    val monday = DateUtils.today.with(DayOfWeek.MONDAY)
                    val pattern = RepeatingPattern.Weekly(
                        setOf(DayOfWeek.MONDAY),
                        monday
                    )
                    shouldHaveNextDate(pattern.nextDate(monday), monday)
                }

                it("should find date after 2 days when today is monday and wednesday is chosen day") {
                    val today =
                        DateUtils.today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    val wednesday = DateUtils.today.with(DayOfWeek.WEDNESDAY)
                    val pattern = RepeatingPattern.Weekly(
                        setOf(DayOfWeek.WEDNESDAY),
                        today
                    )
                    shouldHaveNextDate(pattern.nextDate(today), wednesday)
                }

                it("should find date considering day of week ordering") {
                    val today =
                        DateUtils.today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    val tuesday = DateUtils.today.with(DayOfWeek.TUESDAY)
                    val pattern = RepeatingPattern.Weekly(
                        setOf(DayOfWeek.WEDNESDAY, DayOfWeek.TUESDAY),
                        today
                    )
                    shouldHaveNextDate(pattern.nextDate(today), tuesday)
                }
            }

            describe("Flexible") {

                it("should give today when today is first day of week") {
                    val today =
                        DateUtils.today.with(TemporalAdjusters.previousOrSame(DateUtils.firstDayOfWeek))

                    val pattern = RepeatingPattern.Flexible.Weekly(
                        timesPerWeek = 2,
                        preferredDays = setOf(),
                        scheduledPeriods = mapOf(
                            today to listOf(today)
                        ),
                        start = today
                    )

                    shouldHaveNextDate(pattern.nextDate(today), today)
                }

                it("should give tomorrow when today is not scheduled") {

                    val today =
                        DateUtils.today.with(TemporalAdjusters.previousOrSame(DateUtils.firstDayOfWeek))

                    val tomorrow = today.plusDays(1)

                    val pattern = RepeatingPattern.Flexible.Weekly(
                        timesPerWeek = 2,
                        preferredDays = setOf(),
                        scheduledPeriods = mapOf(
                            today to listOf(tomorrow)
                        ),
                        start = today
                    )

                    shouldHaveNextDate(pattern.nextDate(today), tomorrow)
                }

                it("should give two days after tomorrow when from is after tomorrow") {

                    val today =
                        DateUtils.today.with(TemporalAdjusters.previousOrSame(DateUtils.firstDayOfWeek))

                    val tomorrow = today.plusDays(1)
                    val dayAfterTomorrow = tomorrow.plusDays(1)
                    val twoDaysAfterTomorrow = tomorrow.plusDays(2)

                    val pattern = RepeatingPattern.Flexible.Weekly(
                        timesPerWeek = 2,
                        preferredDays = setOf(),
                        scheduledPeriods = mapOf(
                            today to listOf(tomorrow, twoDaysAfterTomorrow)
                        ),
                        start = today
                    )

                    shouldHaveNextDate(pattern.nextDate(dayAfterTomorrow), twoDaysAfterTomorrow)
                }

                it("should get first date from next period") {

                    val today =
                        DateUtils.today.with(TemporalAdjusters.previousOrSame(DateUtils.firstDayOfWeek))

                    val tomorrow = today.plusDays(1)
                    val dayAfterTomorrow = tomorrow.plusDays(1)

                    val nextPeriodStart = today.plusWeeks(1)

                    val pattern = RepeatingPattern.Flexible.Weekly(
                        timesPerWeek = 2,
                        preferredDays = setOf(),
                        scheduledPeriods = mapOf(
                            today to listOf(tomorrow),
                            nextPeriodStart to listOf(nextPeriodStart)
                        ),
                        start = today
                    )

                    shouldHaveNextDate(pattern.nextDate(dayAfterTomorrow), nextPeriodStart)
                }
            }
        }

        describe("Monthly") {
            describe("Fixed") {
                it("should give today when today is 1st and 1st is chosen day") {
                    val first = DateUtils.today.withDayOfMonth(1)
                    val pattern = RepeatingPattern.Monthly(
                        daysOfMonth = setOf(1),
                        start = first
                    )
                    shouldHaveNextDate(pattern.nextDate(first), first)
                }

                it("should find first chosen date after today when today is not chosen") {
                    val today = DateUtils.today.withDayOfMonth(10)
                    val pattern = RepeatingPattern.Monthly(
                        setOf(1, 20, 15),
                        today.withDayOfMonth(1)
                    )
                    shouldHaveNextDate(pattern.nextDate(today), today.withDayOfMonth(15))
                }

                it("should find first chosen date from next month") {
                    val today = DateUtils.today.withDayOfMonth(10)
                    val first = today.withDayOfMonth(1)
                    val pattern = RepeatingPattern.Monthly(
                        setOf(1),
                        first
                    )
                    shouldHaveNextDate(pattern.nextDate(today), first.plusMonths(1))
                }
            }

            describe("Flexible") {
                it("should give today when today is first day of month") {
                    val today =
                        DateUtils.today.with(TemporalAdjusters.firstDayOfMonth())

                    val pattern = RepeatingPattern.Flexible.Monthly(
                        timesPerMonth = 2,
                        preferredDays = setOf(),
                        scheduledPeriods = mapOf(
                            today to listOf(today)
                        ),
                        start = today
                    )

                    shouldHaveNextDate(pattern.nextDate(today), today)
                }

                it("should give tomorrow when today is not scheduled") {

                    val today =
                        DateUtils.today.with(TemporalAdjusters.firstDayOfMonth())
                    val tomorrow = today.plusDays(1)

                    val pattern = RepeatingPattern.Flexible.Monthly(
                        timesPerMonth = 2,
                        preferredDays = setOf(),
                        scheduledPeriods = mapOf(
                            today to listOf(tomorrow)
                        ),
                        start = today
                    )

                    shouldHaveNextDate(pattern.nextDate(today), tomorrow)
                }

                it("should give two days after tomorrow when from is after tomorrow") {

                    val today =
                        DateUtils.today.with(TemporalAdjusters.firstDayOfMonth())

                    val tomorrow = today.plusDays(1)
                    val dayAfterTomorrow = tomorrow.plusDays(1)
                    val twoDaysAfterTomorrow = tomorrow.plusDays(2)

                    val pattern = RepeatingPattern.Flexible.Monthly(
                        timesPerMonth = 2,
                        preferredDays = setOf(),
                        scheduledPeriods = mapOf(
                            today to listOf(tomorrow, twoDaysAfterTomorrow)
                        ),
                        start = today
                    )

                    shouldHaveNextDate(pattern.nextDate(dayAfterTomorrow), twoDaysAfterTomorrow)
                }

                it("should get first date from next period") {

                    val today =
                        DateUtils.today.with(TemporalAdjusters.firstDayOfMonth())

                    val tomorrow = today.plusDays(1)
                    val dayAfterTomorrow = tomorrow.plusDays(1)

                    val nextPeriodStart = today.plusMonths(1)

                    val pattern = RepeatingPattern.Flexible.Monthly(
                        timesPerMonth = 2,
                        preferredDays = setOf(),
                        scheduledPeriods = mapOf(
                            today to listOf(tomorrow),
                            nextPeriodStart to listOf(nextPeriodStart)
                        ),
                        start = today
                    )

                    shouldHaveNextDate(pattern.nextDate(dayAfterTomorrow), nextPeriodStart)
                }

            }
        }
    }
})