package io.ipoli.android

import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.bluelinelabs.conductor.Conductor
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.FadeChangeHandler
import com.google.firebase.auth.FirebaseAuth
import io.ipoli.android.achievement.usecase.UnlockAchievementsUseCase
import io.ipoli.android.common.AppState
import io.ipoli.android.common.DataLoadedAction
import io.ipoli.android.common.ErrorLogger
import io.ipoli.android.common.LoadDataAction
import io.ipoli.android.common.di.UIModule
import io.ipoli.android.common.home.HomeAction
import io.ipoli.android.common.navigation.Navigator
import io.ipoli.android.common.privacy.PrivacyPolicyViewController
import io.ipoli.android.common.redux.Action
import io.ipoli.android.common.redux.Dispatcher
import io.ipoli.android.common.redux.SideEffectHandler
import io.ipoli.android.common.view.Debounce
import io.ipoli.android.common.view.playerTheme
import io.ipoli.android.player.auth.AuthAction
import io.ipoli.android.player.data.Membership
import io.ipoli.android.store.powerup.AndroidPowerUp
import io.ipoli.android.store.powerup.buy.BuyPowerUpDialogController
import io.ipoli.android.store.powerup.middleware.ShowBuyPowerUpAction
import io.ipoli.android.tag.show.TagAction
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import org.threeten.bp.LocalDate
import space.traversal.kapsule.Injects
import space.traversal.kapsule.inject
import space.traversal.kapsule.required
import java.util.*


/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 7/6/17.
 */
class MainActivity : AppCompatActivity(), Injects<UIModule>, SideEffectHandler<AppState> {

    lateinit var router: Router

    private val playerRepository by required { playerRepository }
    private val sharedPreferences by required { sharedPreferences }
    private val unlockAchievementsUseCase by required { unlockAchievementsUseCase }
    private val resetDayScheduler by required { resetDayScheduler }

    private val stateStore by required { stateStore }
    private val dataExporter by required { dataExporter }
    private val eventLogger by required { eventLogger }

    val rootRouter get() = router

    override fun onCreate(savedInstanceState: Bundle?) {
        super<AppCompatActivity>.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.O) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        setTheme(playerTheme)
        setContentView(R.layout.activity_main)


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent =
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            startActivityForResult(intent, 0)
            Toast.makeText(this, R.string.allow_overlay_request, Toast.LENGTH_LONG).show()
        }

        inject(MyPoliApp.uiModule(this))

        router =
            Conductor.attachRouter(
                this,
                findViewById(R.id.controllerContainer),
                savedInstanceState
            )
        router.setPopsLastView(true)

        val schemaVer =
            sharedPreferences.getInt(Constants.KEY_SCHEMA_VERSION, Constants.SCHEMA_VERSION)

        val playerId = sharedPreferences.getString(Constants.KEY_PLAYER_ID, null)

        if (playerId != null && schemaVer == Constants.SCHEMA_VERSION) {
            startApp()
            stateStore.dispatch(LoadDataAction.All)
            checkForFriendInvite(intent)
            return
        }

        if (sharedPreferences.getInt(
                Constants.KEY_PRIVACY_ACCEPTED_VERSION,
                -1
            ) != Constants.PRIVACY_POLICY_VERSION
        ) {

            getInvitePlayerId(intent)?.let {
                sharedPreferences.edit().putString(Constants.KEY_INVITE_PLAYER_ID, it).apply()
            }

            router.setRoot(RouterTransaction.with(PrivacyPolicyViewController()))
            return
        }

        if (schemaVer < 200 || !sharedPreferences.getBoolean(
                Constants.KEY_PLAYER_DATA_IMPORTED,
                true
            )
        ) {
            Navigator(router).setMigration(
                sharedPreferences.getString(
                    Constants.KEY_PLAYER_ID,
                    ""
                ), schemaVer
            )
            return
        }

        Navigator(router).setOnboard()
    }

    override fun onNewIntent(intent: Intent) {
        val navigator = Navigator(router)
        when (intent.action) {

            ACTION_SHOW_QUICK_ADD ->
                navigator.setAddQuest(
                    closeListener = {
                        finish()
                    },
                    currentDate = LocalDate.now(),
                    isFullscreen = true
                )

            ACTION_SHOW_TIMER -> {
                val questId = intent.getStringExtra(Constants.QUEST_ID_EXTRA_KEY)
                navigator.setQuest(questId)
            }

            ACTION_SHOW_PET ->
                navigator.setPet(showBackButton = false)

            ACTION_PLAN_DAY ->
                navigator.setPlanDay()

            else -> navigator.setHome()
        }

        checkForFriendInvite(intent)
    }

    private fun getInvitePlayerId(intent: Intent) =
        intent.data?.getQueryParameter("playerId")

    private fun checkForFriendInvite(intent: Intent) {
        getInvitePlayerId(intent)?.let {
            val firestorePlayerId = FirebaseAuth.getInstance().currentUser?.uid
            if (firestorePlayerId == null) {
                eventLogger.logEvent("invite_request_login", mapOf("invitePlayerId" to it))
                sharedPreferences.edit().putString(Constants.KEY_INVITE_PLAYER_ID, it).apply()
                Toast.makeText(
                    this,
                    getString(R.string.sign_in_to_accept_friendship),
                    Toast.LENGTH_LONG
                ).show()
                return
            }
            if (it == firestorePlayerId) {
                Toast.makeText(this, getString(R.string.cant_friend_yourself), Toast.LENGTH_LONG)
                    .show()
                return
            }
            Navigator(router).toAcceptFriendship(it)
        }
    }

    private fun startApp() {
        val navigator = Navigator(router)
        if (intent.action == ACTION_SHOW_TIMER) {
            val questId = intent.getStringExtra(Constants.QUEST_ID_EXTRA_KEY)
            navigator.setQuest(questId)
        } else if (shouldShowQuickAdd(intent)) {
            navigator.setAddQuest(
                closeListener = {
                    finish()
                },
                currentDate = LocalDate.now(),
                isFullscreen = true
            )
        } else if (intent.action == ACTION_SHOW_PET) {
            navigator.setPet(showBackButton = false)
        } else if (intent.action == ACTION_PLAN_DAY) {
            navigator.setPlanDay()
        } else if (!router.hasRootController()) {
            navigator.setHome()
//            router.setRoot(RouterTransaction.with(TodayViewController()))

            launch(CommonPool) {
                val p = playerRepository.find()!!
                launch(UI) {
                    if (p.isLoggedIn() && p.username.isNullOrEmpty()) {
                        Navigator(router).setAuth()
                    } else if (Random().nextInt(10) == 1 && p.membership == Membership.NONE) {
                        showPremiumSnackbar()
                    }
                }
                unlockAchievementsUseCase.execute(UnlockAchievementsUseCase.Params(p))
                resetDayScheduler.schedule()
                if (p.isLoggedIn()) {
                    try {
                        dataExporter.exportNewData()
                    } catch (e: Throwable) {
                        ErrorLogger.log(e)
                    }
                }

            }
        }

        incrementAppRun()
    }

    private fun shouldShowQuickAdd(startIntent: Intent) =
        startIntent.action == ACTION_SHOW_QUICK_ADD

    private fun incrementAppRun() {
        val run = sharedPreferences.getInt(Constants.KEY_APP_RUN_COUNT, 0)
        sharedPreferences.edit().putInt(Constants.KEY_APP_RUN_COUNT, run + 1).apply()
    }

    override fun onResume() {
        super.onResume()
        stateStore.addSideEffectHandler(this)
    }

    override fun onPause() {
        stateStore.removeSideEffectHandler(this)
        super.onPause()
    }

    override fun onBackPressed() {
        if (!router.handleBack()) {
            super.onBackPressed()
        }
        if (!router.hasRootController()) {
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        router.onActivityResult(requestCode, resultCode, data)
    }

    fun showBackButton() {
        val actionBar = supportActionBar!!
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setDisplayShowHomeEnabled(true)
    }

    fun pushWithRootRouter(transaction: RouterTransaction) {
        router.pushController(transaction)
    }

    private fun showPremiumSnackbar() {
        Snackbar.make(
            findViewById(R.id.activityContainer),
            getString(
                R.string.trial_membership_message,
                Constants.POWER_UPS_TRIAL_PERIOD_DAYS
            ),
            Snackbar.LENGTH_INDEFINITE
        ).setAction(R.string.go_premium, Debounce.clickListener { _ ->
            Navigator(router).toMembership()
        }).show()
    }

    override suspend fun execute(action: Action, state: AppState, dispatcher: Dispatcher) {
        withContext(UI) {
            when (action) {
                is ShowBuyPowerUpAction ->
                    showPowerUpDialog(action)

                is TagAction.TagCountLimitReached ->
                    Toast.makeText(
                        this@MainActivity,
                        R.string.max_tag_count_reached,
                        Toast.LENGTH_LONG
                    ).show()

                is HomeAction.ShowPlayerSetup ->
                    Navigator(router).setAuth()

                is DataLoadedAction.PlayerChanged -> {
                    val editor = sharedPreferences.edit()
                    val player = action.player
                    editor
                        .putString(Constants.KEY_TIME_FORMAT, player.preferences.timeFormat.name)
                        .putInt(Constants.KEY_SCHEMA_VERSION, player.schemaVersion)
                        .apply()
                }
            }
        }
    }

    private fun showPowerUpDialog(action: ShowBuyPowerUpAction) {
        Navigator(router).toBuyPowerUp(action.powerUp) { result ->
            when (result) {
                BuyPowerUpDialogController.Result.TooExpensive ->
                    Toast.makeText(
                        this,
                        R.string.power_up_too_expensive,
                        Toast.LENGTH_SHORT
                    ).show()

                is BuyPowerUpDialogController.Result.Bought -> {
                    Toast.makeText(
                        this,
                        getString(
                            R.string.power_up_bought,
                            getString(AndroidPowerUp.valueOf(result.powerUp.name).title)
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                }

                is BuyPowerUpDialogController.Result.UnlockAll ->
                    Navigator(router).toMembership(FadeChangeHandler())
            }
        }
    }

    override fun canHandle(action: Action) =
        action is ShowBuyPowerUpAction
            || action === TagAction.TagCountLimitReached
            || action === HomeAction.ShowPlayerSetup
            || action === AuthAction.PlayerSetupCompleted
            || action === AuthAction.GuestCreated
            || action is DataLoadedAction.PlayerChanged

    companion object {
        const val ACTION_SHOW_TIMER = "io.ipoli.android.intent.action.SHOW_TIMER"
        const val ACTION_SHOW_QUICK_ADD = "io.ipoli.android.intent.action.SHOW_QUICK_ADD"
        const val ACTION_SHOW_PET = "io.ipoli.android.intent.action.SHOW_PET"
        const val ACTION_PLAN_DAY = "io.ipoli.android.intent.action.PLAN_DAY"
    }
}