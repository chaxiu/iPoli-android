package io.ipoli.android.common.view.widget.chart

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.support.annotation.ColorRes
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import io.ipoli.android.R
import io.ipoli.android.common.ViewUtils
import io.ipoli.android.common.datetime.DateUtils
import io.ipoli.android.common.datetime.DateUtils.DAYS_IN_A_WEEK
import io.ipoli.android.common.datetime.daysUntil
import io.ipoli.android.common.datetime.weekOfMonth
import io.ipoli.android.repeatingquest.usecase.CreateRepeatingQuestHistoryUseCase
import io.ipoli.android.repeatingquest.usecase.CreateRepeatingQuestHistoryUseCase.DateHistory.*
import org.threeten.bp.LocalDate
import org.threeten.bp.Month
import org.threeten.bp.YearMonth
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.TextStyle
import org.threeten.bp.temporal.TemporalAdjusters

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 02/23/2018.
 */
class HistoryChart @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val completedPaint = Paint()
    private val skippedPaint = Paint()
    private val failedPaint = Paint()
    private val nonePaint = Paint()
    private val todoPaint = Paint()
    private val todayPaint = Paint()
    private val dayPaintLight: Paint
    private val dayPaintDark: Paint
    private val monthPaint = Paint()
    private val dayOfWeekPaint = Paint()

    private val textBounds = Rect()

    private val cellRadius = ViewUtils.dpToPx(16f, context)
    private val rowPadding = ViewUtils.dpToPx(8f, context)

    private val rowData: MutableList<RowData> = mutableListOf()

    private val monthFormatter = DateTimeFormatter.ofPattern("MMMM")

    private val dayTexts = DateUtils.daysOfWeekText(TextStyle.SHORT_STANDALONE)

    class CellDataCreator(history: CreateRepeatingQuestHistoryUseCase.History) {

        private var currentDate: LocalDate = history.currentDate
        private var start: LocalDate = history.start
        private var end: LocalDate = history.end
        private var data: Map<LocalDate, CreateRepeatingQuestHistoryUseCase.DateHistory> =
            history.data

        fun create(): List<RowData> {
            val result = mutableListOf<RowData>()

            val shouldSplit =
                start.monthValue != end.monthValue

//            val shouldSplit =
//                start.monthValue != end.with(TemporalAdjusters.previousOrSame(DateUtils.firstDayOfWeek)).monthValue

            val lastTopDay = start.plusWeeks(3).minusDays(1)

            val shouldSplitTop = shouldSplit && lastTopDay.monthValue != start.monthValue
            val shouldSplitBottom = shouldSplit && !shouldSplitTop

            when {
                shouldSplitTop -> result.addAll(createForTopSplit())
                shouldSplitBottom -> result.addAll(createForBottomSplit())
                else -> result.addAll(createWithNoSplit())
            }

            return result
        }


        private fun createForBottomSplit(): List<RowData> {
            val result = mutableListOf<RowData>()

            val startMonthLastDay = start.with(TemporalAdjusters.lastDayOfMonth())

            result.addAll(
                createCellsForWeeks(
                    weeksToAdd = 3,
                    firstWeekStart = start
                )
            )

            val currentWeekStart =
                currentDate.with(TemporalAdjusters.previousOrSame(DateUtils.firstDayOfWeek))
            val currentWeekEnd = currentWeekStart.plusWeeks(1).minusDays(1)

            if (currentWeekStart.month == currentWeekEnd.month) {

                if (currentWeekStart.month == start.month) {
                    result.add(RowData.CellRow(createCellsForWeek(currentWeekStart)))
                }

                if (currentWeekStart.weekOfMonth != startMonthLastDay.weekOfMonth) {
                    result.add(
                        RowData.CellRow(
                            createWeekWithNoneCellsAtEnd(
                                startMonthLastDay
                            )
                        )
                    )
                }


                val nextMonthFirst = startMonthLastDay.plusDays(1)

                result.addAll(createMonthWithWeekDaysRows(end.month))

                if (nextMonthFirst <= currentDate) {
                    result.addAll(
                        createCellsForWeeks(weeksToAdd = 2, firstWeekStart = nextMonthFirst)
                    )
                } else {
                    result.add(
                        RowData.CellRow(
                            createWeekWithNoneCellsAtStart(
                                nextMonthFirst
                            )
                        )
                    )
                }

            } else {

                result.add(
                    RowData.CellRow(
                        createWeekWithNoneCellsAtEnd(
                            startMonthLastDay
                        )
                    )
                )
                val nextMonthFirst = startMonthLastDay.plusDays(1)

                result.addAll(createMonthWithWeekDaysRows(end.month))

                result.add(
                    RowData.CellRow(
                        createWeekWithNoneCellsAtStart(
                            nextMonthFirst
                        )
                    )
                )

                if (nextMonthFirst.dayOfWeek != DateUtils.firstDayOfWeek) {
                    val lastWeekStart = nextMonthFirst.plusWeeks(1).with(
                        TemporalAdjusters.previousOrSame(DateUtils.firstDayOfWeek)
                    )

                    result.add(RowData.CellRow(createCellsForWeek(lastWeekStart)))
                }
            }

            return result
        }

        private fun createForTopSplit(): List<RowData> {
            val result = mutableListOf<RowData>()

            val firstWeekLast =
                start.with(TemporalAdjusters.nextOrSame(DateUtils.lastDayOfWeek))

            val firstOfMonth = currentDate.with(TemporalAdjusters.firstDayOfMonth())

            val firstWeekFirst =
                firstWeekLast.with(TemporalAdjusters.previousOrSame(DateUtils.firstDayOfWeek))

            val firstWeekMonthLast = firstWeekFirst.with(TemporalAdjusters.lastDayOfMonth())

            var thisWeekStart = firstWeekFirst

            while (true) {
                val thisWeekEnd = thisWeekStart.plusWeeks(1)

                if (thisWeekStart.monthValue != thisWeekEnd.monthValue) {
                    result.add(
                        RowData.CellRow(
                            createWeekWithNoneCellsAtEnd(
                                firstWeekMonthLast
                            )
                        )
                    )
                    break
                } else {
                    result.add(RowData.CellRow(createCellsForWeek(thisWeekStart)))
                    thisWeekStart = thisWeekStart.plusWeeks(1)
                }
            }

            result.addAll(createMonthWithWeekDaysRows(end.month))

            if (firstOfMonth.dayOfWeek != DateUtils.firstDayOfWeek) {
                result.add(
                    RowData.CellRow(
                        createWeekWithNoneCellsAtStart(
                            firstOfMonth
                        )
                    )
                )
            }

            val weekStart =
                firstOfMonth.with(TemporalAdjusters.previousOrSame(DateUtils.firstDayOfWeek))
            val firstDayOfWeekIsFirstOfMonth =
                weekStart.dayOfMonth == 1

            val fullWeeksToAdd =
                if (firstDayOfWeekIsFirstOfMonth)
                    ROW_COUNT_WITH_SPLIT - result.size - 1
                else
                    ROW_COUNT_WITH_SPLIT - result.size

            val firstWeekStart =
                if (firstDayOfWeekIsFirstOfMonth)
                    weekStart
                else
                    weekStart.plusWeeks(1)

            result.addAll(
                createCellsForWeeks(
                    weeksToAdd = fullWeeksToAdd,
                    firstWeekStart = firstWeekStart
                )
            )

            return result
        }

        private fun createWithNoSplit(): List<RowData> {
            val result = mutableListOf<RowData>()
            result.addAll(createMonthWithWeekDaysRows(currentDate.month))

            result.add(
                RowData.CellRow(
                    createWeekWithNoneCellsAtStart(
                        start
                    )
                )
            )

            result.addAll(
                createCellsForWeeks(
                    weeksToAdd = 3,
                    firstWeekStart = start.plusWeeks(1).with(
                        TemporalAdjusters.previousOrSame(
                            DateUtils.firstDayOfWeek
                        )
                    )
                )
            )

            val lastOfMonth = currentDate.with(TemporalAdjusters.lastDayOfMonth())
            result.add(RowData.CellRow(createWeekWithNoneCellsAtEnd(lastOfMonth)))

            return result
        }

        private fun createMonthWithWeekDaysRows(month : Month = currentDate.month, year : Int = currentDate.year) =
            listOf(
                RowData.MonthRow(YearMonth.of(year, month)),
                RowData.WeekDaysRow
            )

        private fun createCellsForWeeks(
            weeksToAdd: Int,
            firstWeekStart: LocalDate
        ): List<RowData> {

            val result = mutableListOf<RowData>()

            var currentWeekStart = firstWeekStart
            (1..weeksToAdd).forEach {
                result.add(RowData.CellRow(createCellsForWeek(currentWeekStart)))
                currentWeekStart = currentWeekStart.plusWeeks(1)
            }

            return result
        }

        private fun createWeekWithNoneCellsAtStart(
            firstOfMonth: LocalDate
        ): List<Cell> {
            val firstOfWeek =
                firstOfMonth.with(TemporalAdjusters.previousOrSame(DateUtils.firstDayOfWeek))

            val noneCellCount = firstOfWeek.daysUntil(firstOfMonth).toInt()

            val cells = mutableListOf<Cell>()

            val firstOfWeekDayOfMonth = firstOfWeek.dayOfMonth

            (0 until noneCellCount).forEach {
                cells.add(Cell(firstOfWeekDayOfMonth + it, null))
            }

            val lastOfWeek =
                firstOfMonth.with(TemporalAdjusters.nextOrSame(DateUtils.lastDayOfWeek))

            val cellCount = (firstOfMonth.daysUntil(lastOfWeek) + 1).toInt()

            (1..cellCount).forEach {
                val date = firstOfWeek.plusDays(noneCellCount + it.toLong() - 1)
                cells.add(
                    createCellFor(
                        data[date]!!,
                        date.dayOfMonth
                    )
                )
            }
            return cells
        }

        private fun createWeekWithNoneCellsAtEnd(
            lastOfMonth: LocalDate
        ): List<Cell> {

            val thisWeekStart =
                lastOfMonth.with(TemporalAdjusters.previousOrSame(DateUtils.firstDayOfWeek))

            val daysBetweenEnd = (thisWeekStart.daysUntil(lastOfMonth) + 1).toInt()

            val cells = mutableListOf<Cell>()

            val startDayOfMonth = thisWeekStart.dayOfMonth

            (0 until daysBetweenEnd).forEach {
                val date = thisWeekStart.plusDays(it.toLong())
                cells.add(
                    createCellFor(
                        data[date]!!,
                        startDayOfMonth + it
                    )
                )
            }

            val noneCellCount = DAYS_IN_A_WEEK - daysBetweenEnd

            (1..noneCellCount).forEach {
                cells.add(Cell(it, null))
            }
            return cells
        }

        private fun createCellsForWeek(
            weekStart: LocalDate
        ) = (0 until DAYS_IN_A_WEEK).map {
            val date = weekStart.plusDays(it.toLong())
            val state = data[date]!!
            val dayOfMonth = weekStart.dayOfMonth + it
            createCellFor(state, dayOfMonth)
        }

        private fun createCellFor(
            state: CreateRepeatingQuestHistoryUseCase.DateHistory,
            dayOfMonth: Int
        ) = Cell(dayOfMonth, state)
    }

    data class Cell(
        val dayOfMonth: Int,
        val state: CreateRepeatingQuestHistoryUseCase.DateHistory?
    )

    sealed class RowData {
        data class MonthRow(val month: YearMonth) : RowData()
        object WeekDaysRow : RowData()
        data class CellRow(val cells: List<Cell>) : RowData()
    }

    companion object {
        const val ROW_COUNT_WITH_SPLIT = 8
    }

    init {
        completedPaint.color = ContextCompat.getColor(context, R.color.md_green_500)
        completedPaint.isAntiAlias = true

        skippedPaint.color = ContextCompat.getColor(context, R.color.md_green_300)
        skippedPaint.isAntiAlias = true

        failedPaint.color = ContextCompat.getColor(context, R.color.md_red_500)
        failedPaint.isAntiAlias = true

        nonePaint.color = ContextCompat.getColor(context, R.color.md_dark_text_12)
        nonePaint.isAntiAlias = true

        todoPaint.color = ContextCompat.getColor(context, R.color.md_green_700)
        todoPaint.isAntiAlias = true
        todoPaint.strokeWidth = ViewUtils.dpToPx(1.5f, context)
        todoPaint.style = Paint.Style.STROKE

        val accentColor = TypedValue().let {
            context.theme.resolveAttribute(R.attr.colorAccent, it, true)
            it.data
        }

        todayPaint.color = accentColor
        todayPaint.isAntiAlias = true
        todayPaint.strokeWidth = ViewUtils.dpToPx(1.5f, context)
        todayPaint.style = Paint.Style.STROKE

        monthPaint.color = ContextCompat.getColor(context, R.color.md_dark_text_87)
        monthPaint.isAntiAlias = true
        monthPaint.textAlign = Paint.Align.CENTER
        monthPaint.textSize = ViewUtils.spToPx(18, context).toFloat()

        dayPaintLight = createTextPaint(context, R.color.md_light_text_100, 14)
        dayPaintDark = createTextPaint(context, R.color.md_dark_text_87, 14)

        dayOfWeekPaint.color = ContextCompat.getColor(context, R.color.md_dark_text_87)
        dayOfWeekPaint.isAntiAlias = true
        dayOfWeekPaint.textAlign = Paint.Align.CENTER
        dayOfWeekPaint.textSize = ViewUtils.spToPx(12, context).toFloat()
    }

    private fun createTextPaint(context: Context, @ColorRes color: Int, textSize: Int): Paint {
        val paint = Paint()
        paint.color = ContextCompat.getColor(context, color)
        paint.isAntiAlias = true
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = ViewUtils.spToPx(textSize, context).toFloat()
        return paint
    }

    fun updateData(history: CreateRepeatingQuestHistoryUseCase.History) {
        rowData.clear()
        rowData.addAll(CellDataCreator(history).create())
        postInvalidate()
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = View.MeasureSpec.getSize(widthMeasureSpec)
        val height = View.MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val totalWidth = measuredWidth

        val cxs = (1..DAYS_IN_A_WEEK).map { it * (totalWidth / 8) }

        rowData.forEachIndexed { i, rowData ->

            val y = (i + 1) * rowPadding + ((i + 1) * cellRadius * 2)

            when (rowData) {
                is RowData.MonthRow -> {

                    val monthText = monthFormatter.format(rowData.month)
                    monthPaint.getTextBounds(monthText, 0, monthText.length, textBounds)

                    canvas.drawText(
                        monthText,
                        (totalWidth / 2).toFloat(),
                        y - (textBounds.exactCenterY() * 2),
                        monthPaint
                    )
                }

                is RowData.WeekDaysRow -> {
                    dayTexts.forEachIndexed { j, dayText ->
                        val x = cxs[j].toFloat()
                        dayOfWeekPaint.getTextBounds(dayText, 0, dayText.length, textBounds)
                        canvas.drawText(dayText, x, y - textBounds.exactCenterY(), dayOfWeekPaint)
                    }
                }

                is RowData.CellRow -> {
                    drawCellRow(rowData, canvas, cxs, y)
                }
            }
        }

    }

    private fun drawCellRow(
        rowData: RowData.CellRow,
        canvas: Canvas,
        xs: List<Int>,
        y: Float
    ) {
        rowData.cells.forEachIndexed { i, cell ->

            val x = xs[i].toFloat()

            val dayOfMonth = cell.dayOfMonth.toString()

            when (cell.state) {

                DONE_ON_SCHEDULE -> {
                    canvas.drawCell(completedPaint, x, y)
                    canvas.drawDayOfMonth(dayOfMonth, dayPaintLight, x, y)
                }

                DONE_NOT_ON_SCHEDULE -> {
                    canvas.drawCell(completedPaint, x, y)
                    canvas.drawDayOfMonth(dayOfMonth, dayPaintLight, x, y)
                }

                SKIPPED -> {
                    canvas.drawCell(skippedPaint, x, y)
                    canvas.drawDayOfMonth(dayOfMonth, dayPaintLight, x, y)
                }

                FAILED -> {
                    canvas.drawCell(failedPaint, x, y)
                    canvas.drawDayOfMonth(dayOfMonth, dayPaintLight, x, y)
                }

                TODAY -> {
                    canvas.drawCell(todayPaint, x, y)
                    canvas.drawDayOfMonth(dayOfMonth, dayPaintDark, x, y)
                }

                BEFORE_START, AFTER_END, EMPTY -> {
                    canvas.drawDayOfMonth(dayOfMonth, dayPaintDark, x, y)
                }

                TODO -> {
                    canvas.drawCell(todoPaint, x, y)
                    canvas.drawDayOfMonth(dayOfMonth, dayPaintDark, x, y)
                }

                else -> canvas.drawCell(nonePaint, x, y)
            }
        }
    }

    private fun Canvas.drawCell(
        paint: Paint,
        x: Float,
        y: Float
    ) = drawCircle(x, y, cellRadius, paint)

    private fun Canvas.drawDayOfMonth(
        dayOfMonth: String,
        paint: Paint,
        x: Float,
        y: Float
    ) {
        paint.getTextBounds(dayOfMonth, 0, dayOfMonth.length, textBounds)
        drawText(dayOfMonth, x, y - textBounds.exactCenterY(), paint)
    }
}