package mypoli.android.challenge.data

import android.support.annotation.ColorRes
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import mypoli.android.R
import mypoli.android.common.datetime.Time
import mypoli.android.quest.Color
import mypoli.android.quest.Icon
import org.threeten.bp.DayOfWeek

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 12/29/17.
 */
enum class Challenge(
    val category: Category,
    val gemPrice: Int,
    val quests: List<Challenge.Quest>,
    val durationDays: Int = 30
) {

    STRESS_FREE_MIND(
        Category.HEALTH_AND_FITNESS,
        5,
        listOf(
            Challenge.Quest.Repeating(
                "Meditate every day for 20 min",
                "Meditate",
                duration = 20,
                weekDays = DayOfWeek.values().toList(),
                startTime = Time.at(19, 0),
                color = Color.GREEN,
                icon = Icon.SUN
            ),
            Challenge.Quest.Repeating(
                "Read a book for 30 min 3 times a week",
                "Read a book",
                duration = 30,
                weekDays = listOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SUNDAY),
                color = Color.BLUE,
                icon = Icon.BOOK
            ),
            Challenge.Quest.OneTime(
                "Share your troubles with a friend",
                "Share your troubles with a friend",
                preferredDayOfWeek = DayOfWeek.SATURDAY,
                duration = 60,
                color = Color.PURPLE,
                icon = Icon.FRIENDS
            ),
            Challenge.Quest.Repeating(
                "Take a walk for 30 min 5 times a week",
                "Take a walk",
                duration = 30,
                weekDays = listOf(
                    DayOfWeek.MONDAY,
                    DayOfWeek.TUESDAY,
                    DayOfWeek.THURSDAY,
                    DayOfWeek.FRIDAY,
                    DayOfWeek.SUNDAY
                ),
                color = Color.GREEN,
                icon = Icon.TREE
            ),
            Challenge.Quest.Repeating(
                "Say 3 things that I am grateful for every morning",
                "Say 3 things that I am grateful for",
                duration = 15,
                weekDays = DayOfWeek.values().toList(),
                startTime = Time.at(10, 0),
                color = Color.RED,
                icon = Icon.LIGHT_BULB
            )
        )),
    WEIGHT_CUTTER(
        Category.HEALTH_AND_FITNESS,
        5,
        listOf(
            Challenge.Quest.OneTime(
                "Sign up for a gym club card",
                "Sign up for a gym club card",
                duration = 30,
                startAtDay = 1,
                color = Color.GREEN,
                icon = Icon.FITNESS
            ),
            Challenge.Quest.Repeating(
                "Run 2 times a week for 30 min",
                "Go for a run",
                duration = 30,
                weekDays = listOf(
                    DayOfWeek.TUESDAY,
                    DayOfWeek.SATURDAY
                ),
                color = Color.GREEN,
                icon = Icon.RUN
            ),
            Challenge.Quest.Repeating(
                "Workout at the gym 3 times a week for 1h",
                "Go for a run",
                duration = 60,
                startAtDay = 2,
                weekDays = listOf(
                    DayOfWeek.MONDAY,
                    DayOfWeek.WEDNESDAY,
                    DayOfWeek.FRIDAY
                ),
                color = Color.GREEN,
                icon = Icon.FITNESS
            ),
            Challenge.Quest.Repeating(
                "Measure & record my weight every morning",
                "Measure & record my weight",
                duration = 15,
                weekDays = DayOfWeek.values().toList(),
                startTime = Time.at(10, 0),
                color = Color.GREEN,
                icon = Icon.STAR
            ),
            Challenge.Quest.Repeating(
                "Prepare healthy dinner 6 times a week",
                "Prepare healthy dinner",
                duration = 45,
                weekDays = listOf(
                    DayOfWeek.MONDAY,
                    DayOfWeek.TUESDAY,
                    DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY,
                    DayOfWeek.FRIDAY,
                    DayOfWeek.SATURDAY
                ),
                startTime = Time.at(19, 0),
                color = Color.ORANGE,
                icon = Icon.RESTAURANT
            )
        )),
    HEALTHY_FIT(
        Category.HEALTH_AND_FITNESS,
        5,
        listOf(
            Quest.Repeating(
                "Drink 1 big bottle of water every day",
                "Drink 1 big bottle of water",
                20,
                null,
                Color.GREEN,
                Icon.STAR,
                null,
                DayOfWeek.values().toList(),
                true
            ),
            Quest.Repeating(
                "Eat healthy breakfast every day",
                "Eat healthy breakfast",
                30,
                Time.atHours(9),
                Color.GREEN,
                Icon.RESTAURANT,
                null
            ),
            Quest.Repeating(
                "Workout 3 times a week",
                "Workout",
                60,
                Time.atHours(19),
                Color.GREEN,
                Icon.FITNESS,
                null,
                listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.SATURDAY)
            ),
            Quest.Repeating(
                "Eat a fruit every day",
                "Eat a fruit",
                20,
                null,
                Color.GREEN,
                Icon.TREE,
                null
            ),
            Quest.Repeating(
                "Meditate 3 times a week",
                "Meditate",
                20,
                null,
                Color.GREEN,
                Icon.STAR,
                null,
                listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
            ),
            Quest.Repeating(
                "Cook healthy dinner 5 times a week",
                "Cook healthy dinner",
                60,
                null,
                Color.GREEN,
                Icon.RESTAURANT,
                null,
                listOf(
                    DayOfWeek.MONDAY,
                    DayOfWeek.TUESDAY,
                    DayOfWeek.WEDNESDAY,
                    DayOfWeek.FRIDAY,
                    DayOfWeek.SATURDAY
                )
            )
        )
    ),
    ENGLISH_JEDI(Category.BUILD_SKILL, 5, listOf()),
    CODING_NINJA(Category.BUILD_SKILL, 5, listOf()),
    FAMOUS_WRITER(Category.BUILD_SKILL, 5, listOf()),
    MASTER_COMMUNICATOR(Category.DEEP_WORK, 5, listOf()),
    FOCUSED_WORK(Category.DEEP_WORK, 5, listOf()),
    JOB_INTERVIEW(Category.DEEP_WORK, 5, listOf()),
    FRIENDS_TIME(Category.ME_TIME, 5, listOf()),
    ENJOY_MYSELF(Category.ME_TIME, 5, listOf()),
    FAMILY_TIME(Category.ME_TIME, 5, listOf());

    enum class Category {
        BUILD_SKILL,
        ME_TIME,
        DEEP_WORK,
        ORGANIZE_MY_LIFE,
        HEALTH_AND_FITNESS
    }

    sealed class Quest {
        data class OneTime(
            val text: String,
            val name: String,
            val duration: Int,
            val startTime: Time? = null,
            val color: Color = Color.GREEN,
            val icon: Icon = Icon.STAR,
            val startAtDay: Int? = null,
            val preferredDayOfWeek: DayOfWeek? = null,
            val selected: Boolean = true
        ) : Quest()

        data class Repeating(
            val text: String,
            val name: String,
            val duration: Int,
            val startTime: Time? = null,
            val color: Color = Color.GREEN,
            val icon: Icon = Icon.STAR,
            val startAtDay: Int? = null,
            val weekDays: List<DayOfWeek> = DayOfWeek.values().toList(),
            val selected: Boolean = true
        ) : Quest()
    }
}

enum class AndroidChallenge(
    @StringRes val title: Int,
    @StringRes val description: Int,
    @DrawableRes val smallImage: Int,
    @DrawableRes val backgroundImage: Int,
    val category: Category
) {
    STRESS_FREE_MIND(
        R.string.challenge_stress_free_mind,
        R.string.challenge_stress_free_mind_description,
        R.drawable.challenge_stress_free_mind,
        R.drawable.challenge_stress_free_mind_background,
        Category.HEALTH_AND_FITNESS
    ),
    WEIGHT_CUTTER(
        R.string.challenge_weight_cutter,
        R.string.challenge_weight_cutter_description,
        R.drawable.challenge_weight_cutter,
        R.drawable.challenge_weight_cutter_background,
        Category.HEALTH_AND_FITNESS
    ),
    HEALTHY_FIT(
        R.string.challenge_healthy_and_fit,
        R.string.challenge_healthy_and_fit_description,
        R.drawable.challenge_healthy_and_fit,
        R.drawable.challenge_healthy_and_fit_background,
        Category.HEALTH_AND_FITNESS
    ),
    ENGLISH_JEDI(
        R.string.challenge_english_jedi,
        R.string.challenge_english_jedi_description,
        R.drawable.challenge_english_jedi,
        R.drawable.challenge_english_jedi_background,
        Category.BUILD_SKILL
    ),
    CODING_NINJA(
        R.string.challenge_coding_ninja,
        R.string.challenge_coding_ninja_description,
        R.drawable.challenge_coding_ninja,
        R.drawable.challenge_coding_ninja_background,
        Category.BUILD_SKILL
    ),
    FAMOUS_WRITER(
        R.string.challenge_famous_writer,
        R.string.challenge_famous_writer_description,
        R.drawable.challenge_famous_writer,
        R.drawable.challenge_famous_writer_background,
        Category.BUILD_SKILL
    ),
    MASTER_COMMUNICATOR(
        R.string.challenge_master_communicator,
        R.string.challenge_master_communicator_description,
        R.drawable.challenge_communication_master,
        R.drawable.challenge_communication_master_background,
        Category.DEEP_WORK
    ),
    FOCUSED_WORK(
        R.string.challenge_focused_work,
        R.string.challenge_focused_work_description,
        R.drawable.challenge_focus_on_work,
        R.drawable.challenge_focus_on_work_background,
        Category.DEEP_WORK
    ),
    JOB_INTERVIEW(
        R.string.challenge_job_interview,
        R.string.challenge_job_interview_description,
        R.drawable.challenge_job_interview,
        R.drawable.challenge_job_interview_background,
        Category.DEEP_WORK
    );

    enum class Category(@StringRes val title: Int, @ColorRes val color: Int) {
        BUILD_SKILL(R.string.challenge_category_build_skill_name, R.color.md_blue_500),
        ME_TIME(R.string.challenge_category_me_time_name, R.color.md_purple_500),
        DEEP_WORK(R.string.challenge_category_deep_work_name, R.color.md_red_500),
        ORGANIZE_MY_LIFE(R.string.challenge_category_organize_my_life_name, R.color.md_teal_500),
        HEALTH_AND_FITNESS(R.string.challenge_category_health_fitness_name, R.color.md_green_500)
    }
}