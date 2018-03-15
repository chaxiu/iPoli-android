package mypoli.android.challenge.predefined.entity

import android.support.annotation.ColorRes
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import mypoli.android.R
import mypoli.android.challenge.entity.Challenge
import mypoli.android.challenge.predefined.entity.PredefinedChallengeData.Quest.OneTime
import mypoli.android.challenge.predefined.entity.PredefinedChallengeData.Quest.Repeating
import mypoli.android.common.datetime.Time
import mypoli.android.quest.Color
import mypoli.android.quest.Icon
import org.threeten.bp.DayOfWeek

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 12/29/17.
 */

data class PredefinedChallengeData(
    val category: Category,
    val quests: List<Quest>,
    val durationDays: Int = 30
) {

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

enum class PredefinedChallenge(
    val title: String,
    val color: Color,
    val icon: Icon?,
    val difficulty: Challenge.Difficulty,
    val motivations: List<String>,
    val category: PredefinedChallengeData.Category,
    val gemPrice: Int,
    val quests: List<PredefinedChallengeData.Quest>,
    val durationDays: Int = 30
) {

    STRESS_FREE_MIND(
        title = "Stress-Free Mind",
        color = Color.GREEN,
        icon = Icon.TREE,
        difficulty = Challenge.Difficulty.NORMAL,
        motivations = listOf("Be more focused", "Be relaxed", "Be healthy"),
        category = PredefinedChallengeData.Category.HEALTH_AND_FITNESS,
        gemPrice = 0,
        quests = listOf(
            Repeating(
                "Meditate every day for 20 min",
                "Meditate",
                duration = 20,
                weekDays = DayOfWeek.values().toList(),
                startTime = Time.at(19, 0),
                color = Color.GREEN,
                icon = Icon.SUN
            ),
            Repeating(
                "Read a book for 30 min 3 times a week",
                "Read a book",
                duration = 30,
                weekDays = listOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SUNDAY),
                color = Color.BLUE,
                icon = Icon.BOOK
            ),
            OneTime(
                "Share my troubles with a friend",
                "Share my troubles with a friend",
                preferredDayOfWeek = DayOfWeek.SATURDAY,
                duration = 60,
                color = Color.PURPLE,
                icon = Icon.FRIENDS
            ),
            Repeating(
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
            Repeating(
                "Say 3 things that I am grateful for every morning",
                "Say 3 things that I am grateful for",
                duration = 15,
                weekDays = DayOfWeek.values().toList(),
                startTime = Time.at(10, 0),
                color = Color.RED,
                icon = Icon.LIGHT_BULB
            )
        )
    ),
    WEIGHT_CUTTER(
        title = "Weight Cutter",
        color = Color.GREEN,
        icon = Icon.FITNESS,
        difficulty = Challenge.Difficulty.HARD,
        motivations = listOf("Feel great", "Become more confident", "Become healthier"),
        category = PredefinedChallengeData.Category.HEALTH_AND_FITNESS,
        gemPrice = 5,
        quests = listOf(
            OneTime(
                "Sign up for a gym club card",
                "Sign up for a gym club card",
                duration = 30,
                startAtDay = 1,
                color = Color.GREEN,
                icon = Icon.FITNESS
            ),
            Repeating(
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
            Repeating(
                "Workout at the gym 3 times a week for 1h",
                "Workout at the gym",
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
            Repeating(
                "Measure & record my weight every morning",
                "Measure & record my weight",
                duration = 15,
                weekDays = DayOfWeek.values().toList(),
                startTime = Time.at(10, 0),
                color = Color.GREEN,
                icon = Icon.STAR
            ),
            Repeating(
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
        )
    ),
    HEALTHY_FIT(
        title = "Healthy & Fit",
        color = Color.GREEN,
        icon = Icon.RUN,
        difficulty = Challenge.Difficulty.NORMAL,
        motivations = listOf("Be healthier", "Stay fit", "Feel great"),
        category = PredefinedChallengeData.Category.HEALTH_AND_FITNESS,
        gemPrice = 5,
        quests = listOf(
            Repeating(
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
            Repeating(
                "Eat healthy breakfast every day",
                "Eat healthy breakfast",
                30,
                Time.atHours(9),
                Color.GREEN,
                Icon.RESTAURANT,
                null
            ),
            Repeating(
                "Workout 3 times a week",
                "Workout",
                60,
                Time.atHours(19),
                Color.GREEN,
                Icon.FITNESS,
                null,
                listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.SATURDAY)
            ),
            Repeating(
                "Eat a fruit every day",
                "Eat a fruit",
                20,
                null,
                Color.GREEN,
                Icon.TREE,
                null
            ),
            Repeating(
                "Meditate 3 times a week",
                "Meditate",
                20,
                null,
                Color.GREEN,
                Icon.STAR,
                null,
                listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
            ),
            Repeating(
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
    ENGLISH_JEDI(
        title = "English Jedi",
        color = Color.BLUE,
        icon = Icon.BOOK,
        difficulty = Challenge.Difficulty.HARD,
        motivations = listOf("Learn to read great books", "Participate in conversations", "Meet & speak with new people"),
        category = PredefinedChallengeData.Category.BUILD_SKILL,
        gemPrice = 5,
        quests = listOf(
            OneTime(
                "Sign up at Duolingo",
                "Sign up at Duolingo",
                20,
                Time.atHours(11),
                Color.BLUE,
                Icon.BOOK,
                1
            ),
            OneTime(
                "Sign up at a local English course",
                "Sign up at a local English course",
                30,
                null,
                Color.BLUE,
                Icon.BOOK,
                1
            ),
            OneTime(
                "Subscribe to the Misterduncan YouTube channel",
                "Subscribe to the Misterduncan YouTube channel",
                30,
                Time.at(11, 30),
                Color.BLUE,
                Icon.BOOK,
                1
            ),
            Repeating(
                "Learn using Duolingo for 20 min every day",
                "Learn using Duolingo",
                20,
                null,
                Color.BLUE,
                Icon.ACADEMIC,
                2
            ),
            Repeating(
                "Watch e movie with English subtitles 5 times a week",
                "Watch e movie with English subtitles",
                60,
                Time.atHours(20),
                Color.PURPLE,
                Icon.CAMERA,
                null,
                listOf(
                    DayOfWeek.MONDAY,
                    DayOfWeek.TUESDAY,
                    DayOfWeek.THURSDAY,
                    DayOfWeek.SATURDAY,
                    DayOfWeek.SUNDAY
                )
            ),
            Repeating(
                "Read Alice in Wonderland 4 times a week",
                "Read Alice in Wonderland",
                20,
                Time.atHours(21),
                Color.BLUE,
                Icon.BOOK,
                null,
                listOf(
                    DayOfWeek.MONDAY,
                    DayOfWeek.WEDNESDAY,
                    DayOfWeek.FRIDAY,
                    DayOfWeek.SATURDAY
                )
            )
        )
    ),
    CODING_NINJA(
        title = "Coding Ninja",
        color = Color.BLUE,
        icon = Icon.LIGHT_BULB,
        difficulty = Challenge.Difficulty.HARD,
        motivations = listOf("Learn to command my computer", "Understand technologies better", "Find new job"),
        category = PredefinedChallengeData.Category.BUILD_SKILL,
        gemPrice = 5,
        quests = listOf(
            OneTime(
                "Sign up at freeCodeCamp",
                "Sign up at freeCodeCamp",
                20,
                Time.atHours(11),
                Color.BLUE,
                Icon.STAR,
                1
            ),
            Repeating(
                "Read JavaScript For Cats 3 times a week",
                "Read JavaScript For Cats",
                30,
                null,
                Color.BLUE,
                Icon.BOOK,
                null,
                listOf(
                    DayOfWeek.TUESDAY,
                    DayOfWeek.THURSDAY,
                    DayOfWeek.SATURDAY
                )
            ),
            Repeating(
                "Conquer freeCodeCamp challenges 5 times a week",
                "Conquer freeCodeCamp challenges",
                45,
                null,
                Color.BLUE,
                Icon.LIGHT_BULB,
                null,
                listOf(
                    DayOfWeek.MONDAY,
                    DayOfWeek.TUESDAY,
                    DayOfWeek.WEDNESDAY,
                    DayOfWeek.FRIDAY,
                    DayOfWeek.SATURDAY
                )
            ),
            Repeating(
                "Watch CS50x Programming Course 2 times a week",
                "Watch CS50x Programming Course",
                60,
                null,
                Color.BLUE,
                Icon.LIGHT_BULB,
                null,
                listOf(
                    DayOfWeek.THURSDAY,
                    DayOfWeek.SUNDAY
                )
            )
        )
    ),
    FAMOUS_WRITER(
        title = "Famous writer",
        color = Color.BLUE,
        icon = Icon.BOOK,
        difficulty = Challenge.Difficulty.HARD,
        motivations = listOf("Better present my ideas", "Become more confident", "Meet new people"),
        category = PredefinedChallengeData.Category.BUILD_SKILL,
        gemPrice = 0,
        quests = listOf(
            OneTime(
                "Create blog on Medium.com",
                "Create blog on Medium.com",
                30,
                Time.atHours(11),
                Color.BLUE,
                Icon.LIGHT_BULB,
                2
            ),
            OneTime(
                "Choose what I am going to write about",
                "Choose what I am going to write about",
                30,
                Time.at(11, 30),
                Color.BLUE,
                Icon.LIGHT_BULB,
                2
            ),
            OneTime(
                "Pick 5 bloggers who inspire me and read most of their posts",
                "Pick 5 bloggers who inspire me and read most of their posts",
                120,
                null,
                Color.BLUE,
                Icon.ACADEMIC,
                3
            ),
            OneTime(
                "Research & write my first blog post",
                "Research & write my first blog post",
                120,
                null,
                Color.BLUE,
                Icon.LIGHT_BULB,
                4
            ),
            Repeating(
                "Write a blog post once every week",
                "Write a blog post once",
                90,
                null,
                Color.BLUE,
                Icon.LIGHT_BULB,
                7,
                listOf(
                    DayOfWeek.SATURDAY
                )
            )
        )
    ),
    MASTER_COMMUNICATOR(
        title = "Master Presenter",
        color = Color.RED,
        icon = Icon.STAR,
        difficulty = Challenge.Difficulty.NORMAL,
        motivations = listOf("Better present my ideas", "Become more confident", "Explain better"),
        category = PredefinedChallengeData.Category.DEEP_WORK,
        gemPrice = 0,
        quests = listOf(
            OneTime(
                "Research how to give great presentation",
                "Research how to give great presentation",
                60,
                null,
                Color.BLUE,
                Icon.LIGHT_BULB,
                1
            ),
            OneTime(
                "Sign up at Canva.com",
                "Sign up at Canva.com",
                20,
                null,
                Color.BLUE,
                Icon.STAR,
                1
            ),
            OneTime(
                "Create my presentation at Canva.com",
                "Create my presentation at Canva.com",
                120,
                null,
                Color.BLUE,
                Icon.LIGHT_BULB,
                2
            ),
            Repeating(
                "Practice presenting alone every day",
                "Practice presenting alone",
                30,
                null,
                Color.BLUE,
                Icon.STAR,
                3
            ),
            OneTime(
                "Practice presenting to a friend",
                "Practice presenting to a friend",
                60,
                null,
                Color.BLUE,
                Icon.FRIENDS,
                8
            ),
            OneTime(
                "Upload my presentation to SlideShare.net",
                "Upload my presentation to SlideShare.net",
                60,
                null,
                Color.BLUE,
                Icon.CLOUD,
                10
            )
        ),
        durationDays = 10
    ),
    FOCUSED_WORK(
        title = "Focused work",
        color = Color.RED,
        icon = Icon.BRIEFCASE,
        difficulty = Challenge.Difficulty.HARD,
        motivations = listOf("Be more productive", "Achieve more"),
        category = PredefinedChallengeData.Category.DEEP_WORK,
        gemPrice = 5,
        quests = listOf(
            OneTime(
                "Prepare a special place for doing my work for 1 hour",
                "Prepare a special place for doing my work",
                60,
                null,
                Color.RED,
                Icon.LIGHT_BULB,
                1
            ),
            OneTime(
                "Decide on what I want to accomplish",
                "Decide on what I want to accomplish",
                30,
                null,
                Color.RED,
                Icon.STAR,
                2
            ),
            Repeating(
                "Focus on my work & do it every weekday",
                "Focus on my work & do it",
                180,
                null,
                Color.RED,
                Icon.HEART,
                startAtDay = 3,
                weekDays = listOf(
                    DayOfWeek.MONDAY,
                    DayOfWeek.TUESDAY,
                    DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY,
                    DayOfWeek.FRIDAY
                )
            ),
            Repeating(
                "Evaluate how well I did every Saturday for 30 min",
                "Evaluate how well I did",
                30,
                null,
                Color.RED,
                Icon.LIGHT_BULB,
                startAtDay = 6,
                weekDays = listOf(DayOfWeek.SATURDAY)
            )
        )
    ),
    JOB_INTERVIEW(
        title = "Job interview",
        color = Color.RED,
        icon = Icon.MONEY,
        difficulty = Challenge.Difficulty.NORMAL,
        motivations = listOf("Find better job", "Work something more interesting"),
        category = PredefinedChallengeData.Category.DEEP_WORK,
        gemPrice = 5,
        quests = listOf(
            OneTime(
                "Compare the job requirements with my skill set",
                "Compare the job requirements with my skill set",
                60,
                null,
                Color.RED,
                Icon.BRIEFCASE,
                1
            ),
            Repeating(
                "Research the organization & what it does for 30 min once a week",
                "Research the organization & what it does",
                30,
                null,
                Color.RED,
                Icon.BRIEFCASE,
                3,
                listOf(DayOfWeek.MONDAY)
            ),
            OneTime(
                "Plan what to wear for the Interview",
                "Plan what to wear for the Interview",
                60,
                null,
                Color.RED,
                Icon.STAR,
                startAtDay = 10
            ),
            OneTime(
                "Read my CV and practice talking about previous experience for 1 hour",
                "Read my CV and practice talking about previous experience",
                60,
                null,
                Color.RED,
                Icon.BRIEFCASE,
                startAtDay = 11
            ),
            OneTime(
                "Practice responding to possible questions for 30 minutes",
                "Practice responding to possible questions",
                30,
                null,
                Color.RED,
                Icon.STAR,
                startAtDay = 12
            )
        ),
        durationDays = 14
    ),
    FRIENDS_BLAST(
        title = "Friends blast",
        color = Color.PURPLE,
        icon = Icon.FRIENDS,
        difficulty = Challenge.Difficulty.NORMAL,
        motivations = listOf("Feel more connected to others", "Have more fun"),
        category = PredefinedChallengeData.Category.ME_TIME,
        gemPrice = 4,
        quests = listOf(
            Repeating(
                "Call a friend 2 times a week",
                "Call a friend",
                30,
                null,
                Color.PURPLE,
                Icon.PHONE,
                null,
                listOf(
                    DayOfWeek.WEDNESDAY,
                    DayOfWeek.SATURDAY
                )
            ),
            Repeating(
                "Go out with friends 2 times a week",
                "Go out with friends",
                90,
                null,
                Color.PURPLE,
                Icon.FRIENDS,
                null,
                listOf(
                    DayOfWeek.TUESDAY,
                    DayOfWeek.SATURDAY
                )
            ),
            OneTime(
                "Connect with a forgotten friend",
                "Connect with a forgotten friend",
                30,
                null,
                Color.PURPLE,
                Icon.PHONE,
                3
            ),
            OneTime(
                "Plan a vacation with friends",
                "Plan a vacation with friends",
                120,
                null,
                Color.PURPLE,
                Icon.FRIENDS,
                null,
                DayOfWeek.SUNDAY
            )
        )
    ),
    JUST_HAVE_FUN(
        title = "Just have fun",
        color = Color.PURPLE,
        icon = Icon.GAME_CONTROLLER,
        difficulty = Challenge.Difficulty.EASY,
        motivations = listOf("Be healthier and happier"),
        category = PredefinedChallengeData.Category.ME_TIME,
        gemPrice = 4,
        quests = listOf(
            Repeating(
                "Play my favorite game 3 times a week for 1 hour",
                "Play my favorite game",
                60,
                Time.atHours(20),
                Color.PURPLE,
                Icon.GAME_CONTROLLER,
                null,
                listOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY, DayOfWeek.SUNDAY)
            ),
            Repeating(
                "Enjoy my hobby once a week for 2 hours",
                "Enjoy my hobby",
                120,
                Time.atHours(10),
                Color.PURPLE,
                Icon.WRENCH,
                null,
                listOf(DayOfWeek.SUNDAY)
            ),
            Repeating(
                "Watch my favorite TV series for 45 minutes twice a week",
                "Watch my favorite TV series",
                45,
                null,
                Color.PURPLE,
                Icon.STAR,
                null,
                listOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY)
            ),
            OneTime(
                "Talk with a friend about starting a hobby together",
                "Talk with a friend about starting a hobby together",
                30,
                null,
                Color.PURPLE,
                Icon.STAR,
                startAtDay = 7,
                preferredDayOfWeek = DayOfWeek.SATURDAY
            )
        )
    ),
    FAMILY_TIME(
        title = "Family time",
        color = Color.PURPLE,
        icon = Icon.FRIENDS,
        difficulty = Challenge.Difficulty.NORMAL,
        motivations = listOf("Stay close with family"),
        category = PredefinedChallengeData.Category.ME_TIME,
        gemPrice = 0,
        quests = listOf(
            Repeating(
                "Call my parents every week",
                "Call my parents",
                20,
                Time.atHours(20),
                Color.PURPLE,
                Icon.PHONE,
                null,
                listOf(
                    DayOfWeek.SUNDAY
                )
            ),
            OneTime(
                "Visit my parents every month",
                "Visit my parents",
                120,
                Time.atHours(10),
                Color.PURPLE,
                Icon.FRIENDS,
                null,
                DayOfWeek.SUNDAY
            ),
            Repeating(
                "Have family dinner 3 times a week",
                "Have family dinner",
                45,
                Time.atHours(20),
                Color.PINK,
                Icon.PIZZA,
                null,
                listOf(
                    DayOfWeek.MONDAY,
                    DayOfWeek.THURSDAY,
                    DayOfWeek.SUNDAY
                )
            ),
            Repeating(
                "Play with my children every weekend",
                "Play with my children",
                60,
                Time.atHours(11),
                Color.PINK,
                Icon.FOOTBALL,
                null,
                listOf(
                    DayOfWeek.SATURDAY,
                    DayOfWeek.SUNDAY
                )
            ),
            OneTime(
                "Plan family vacation",
                "Plan family vacation",
                90,
                Time.atHours(10),
                Color.PURPLE,
                Icon.FRIENDS,
                null,
                DayOfWeek.SUNDAY
            )
        )
    ),
    KEEP_THINGS_TIDY(
        title = "Keep things tidy",
        color = Color.TEAL,
        icon = Icon.HOME,
        difficulty = Challenge.Difficulty.NORMAL,
        motivations = listOf("Live healthier", "Be more organized"),
        category = PredefinedChallengeData.Category.ORGANIZE_MY_LIFE,
        gemPrice = 3,
        quests = listOf(
            Repeating(
                "Use the vacuum cleaner every weekend",
                "Use the vacuum cleaner",
                60,
                Time.atHours(11),
                Color.BROWN,
                Icon.HOME,
                null,
                listOf(
                    DayOfWeek.SATURDAY
                )
            ),
            Repeating(
                "Do the laundry twice a week",
                "Do the laundry",
                30,
                Time.atHours(11),
                Color.BROWN,
                Icon.HOME,
                null,
                listOf(
                    DayOfWeek.WEDNESDAY,
                    DayOfWeek.SATURDAY
                )
            ),
            Repeating(
                "Do the dishes every day",
                "Do the dishes",
                20,
                Time.atHours(21),
                Color.BROWN,
                Icon.HOME,
                null
            ),
            Repeating(
                "Clean after the pet 3 times a week",
                "Clean after the pet",
                20,
                Time.at(21, 30),
                Color.BROWN,
                Icon.PAW,
                null,
                listOf(
                    DayOfWeek.TUESDAY,
                    DayOfWeek.THURSDAY,
                    DayOfWeek.SATURDAY
                ),
                false
            ),
            Repeating(
                "Clean the bathroom once a week",
                "Clean the bathroom",
                40,
                null,
                Color.BROWN,
                Icon.HOME,
                null,
                listOf(
                    DayOfWeek.SATURDAY
                )
            ),
            Repeating(
                "Tidy all rooms twice a week",
                "Tidy all rooms",
                20,
                null,
                Color.BROWN,
                Icon.HOME,
                null,
                listOf(
                    DayOfWeek.TUESDAY,
                    DayOfWeek.FRIDAY
                )
            )
        )
    ),
    ORGANIZE_MY_DAY(
        title = "Organize my day",
        color = Color.TEAL,
        icon = Icon.STAR,
        difficulty = Challenge.Difficulty.NORMAL,
        motivations = listOf("Be more organized", "Achieve more in less time"),
        category = PredefinedChallengeData.Category.ORGANIZE_MY_LIFE,
        gemPrice = 3,
        quests = listOf(
            Repeating(
                "Plan my schedule for the day every morning",
                "Plan my schedule for the day",
                20,
                Time.atHours(10),
                Color.TEAL,
                Icon.LIGHT_BULB,
                null
            ),
            Repeating(
                "Check the weather forecast and choose my clothes for the next day",
                "Check the weather forecast and choose my clothes for the next day",
                20,
                Time.atHours(22),
                Color.PINK,
                Icon.HOME,
                null
            ),
            Repeating(
                "Review how productive my day was every evening",
                "Review how productive my day was",
                20,
                Time.at(22, 30),
                Color.TEAL,
                Icon.STAR,
                null
            ),
            Repeating(
                "Review how productive my week was",
                "Review how productive my week was",
                20,
                Time.at(22, 30),
                Color.TEAL,
                Icon.STAR,
                null,
                listOf(
                    DayOfWeek.SUNDAY
                )
            ),
            Repeating(
                "Add new healthy snack in my work drawer 2 times a week",
                "Add new healthy snack in my work drawer",
                20,
                null,
                Color.TEAL,
                Icon.RESTAURANT,
                null,
                listOf(
                    DayOfWeek.MONDAY,
                    DayOfWeek.THURSDAY
                )
            )
        )
    ),
    STAY_ON_TOP_OF_THINGS(
        title = "Stay on top of things",
        color = Color.TEAL,
        icon = Icon.HOME,
        difficulty = Challenge.Difficulty.NORMAL,
        motivations = listOf("Be more organized", "Do not forget important things"),
        category = PredefinedChallengeData.Category.ORGANIZE_MY_LIFE,
        gemPrice = 3,
        quests = listOf(
            Repeating(
                "Shop for food and household cleaning products every week",
                "Shop for food and household cleaning products",
                90,
                null,
                Color.TEAL,
                Icon.SHOPPING_CART,
                null,
                listOf(
                    DayOfWeek.SATURDAY
                )
            ),
            OneTime(
                "Pay the bills and review my savings and investments every month",
                "Pay the bills",
                30,
                null,
                Color.TEAL,
                Icon.MONEY,
                15
            ),
            Repeating(
                "Prepare dinner 3 times a week",
                "Prepare dinner",
                90,
                null,
                Color.TEAL,
                Icon.RESTAURANT,
                null,
                listOf(
                    DayOfWeek.TUESDAY,
                    DayOfWeek.THURSDAY,
                    DayOfWeek.SATURDAY
                )
            ),
            Repeating(
                "Walk the dog every day",
                "Walk the dog",
                30,
                null,
                Color.TEAL,
                Icon.PAW,
                null,
                selected = false
            ),
            Repeating(
                "Prepare school lunch for the kids every morning",
                "Prepare school lunch for the kids",
                30,
                Time.atHours(8),
                Color.TEAL,
                Icon.PIZZA,
                null,
                listOf(
                    DayOfWeek.MONDAY,
                    DayOfWeek.TUESDAY,
                    DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY,
                    DayOfWeek.FRIDAY
                ),
                selected = false
            )
        )
    );
}

enum class AndroidPredefinedChallenge(
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
    ),
    FRIENDS_BLAST(
        R.string.challenge_friends_blast,
        R.string.challenge_friends_blast_description,
        R.drawable.challenge_friends_blast,
        R.drawable.challenge_friends_blast_background,
        Category.ME_TIME
    ),
    JUST_HAVE_FUN(
        R.string.challenge_just_have_fun,
        R.string.challenge_just_have_fun_description,
        R.drawable.challenge_just_have_fun,
        R.drawable.challenge_just_have_fun_background,
        Category.ME_TIME
    ),
    FAMILY_TIME(
        R.string.challenge_family_time,
        R.string.challenge_family_time_description,
        R.drawable.challenge_family_time,
        R.drawable.challenge_family_time_background,
        Category.ME_TIME
    ),
    KEEP_THINGS_TIDY(
        R.string.challenge_keep_things_tidy,
        R.string.challenge_keep_things_tidy_description,
        R.drawable.challenge_keep_things_tidy,
        R.drawable.challenge_keep_things_tidy_background,
        Category.ORGANIZE_MY_LIFE
    ),
    ORGANIZE_MY_DAY(
        R.string.challenge_organize_my_day,
        R.string.challenge_organize_my_day_description,
        R.drawable.challenge_organize_my_day,
        R.drawable.challenge_organize_my_day_background,
        Category.ORGANIZE_MY_LIFE
    ),
    STAY_ON_TOP_OF_THINGS(
        R.string.challenge_stay_on_top_of_things,
        R.string.challenge_stay_on_top_of_things_description,
        R.drawable.challenge_stay_on_top_of_things,
        R.drawable.challenge_stay_on_top_of_things_background,
        Category.ORGANIZE_MY_LIFE
    );

    enum class Category(@StringRes val title: Int, @ColorRes val color: Int) {
        BUILD_SKILL(R.string.challenge_category_build_skill_name, R.color.md_blue_700),
        ME_TIME(R.string.challenge_category_me_time_name, R.color.md_purple_600),
        DEEP_WORK(R.string.challenge_category_deep_work_name, R.color.md_red_600),
        ORGANIZE_MY_LIFE(R.string.challenge_category_organize_my_life_name, R.color.md_teal_500),
        HEALTH_AND_FITNESS(R.string.challenge_category_health_fitness_name, R.color.md_green_600)
    }
}