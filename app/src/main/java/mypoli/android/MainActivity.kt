package mypoli.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.amplitude.api.Amplitude
import com.bluelinelabs.conductor.Conductor
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import mypoli.android.common.LoadDataAction
import mypoli.android.common.di.Module
import mypoli.android.common.view.playerTheme
import mypoli.android.home.HomeViewController
import mypoli.android.player.AuthProvider
import mypoli.android.player.Player
import mypoli.android.player.persistence.model.ProviderType
import mypoli.android.timer.TimerViewController
import space.traversal.kapsule.Injects
import space.traversal.kapsule.inject
import space.traversal.kapsule.required

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 7/6/17.
 */
class MainActivity : AppCompatActivity(), Injects<Module> {

    lateinit var router: Router

    private val database by required { database }

    private val playerRepository by required { playerRepository }
    private val petStatsChangeScheduler by required { lowerPetStatsScheduler }
    private val stateStore by required { stateStore }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(playerTheme)
        setContentView(R.layout.activity_main)

        val amplitudeClient =
            Amplitude.getInstance().initialize(this, AnalyticsConstants.AMPLITUDE_KEY)
        amplitudeClient.enableForegroundTracking(application)
        if (BuildConfig.DEBUG) {
            Amplitude.getInstance().setLogLevel(Log.VERBOSE)
            amplitudeClient.setOptOut(true)

            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + packageName)
                )
                startActivityForResult(intent, 0)
            }
        }

        incrementAppRun()

        router =
            Conductor.attachRouter(this, findViewById(R.id.controllerContainer), savedInstanceState)
        router.setPopsLastView(true)
        inject(myPoliApp.module(this))

        if (!playerRepository.hasPlayer()) {
            val player = Player(
                authProvider = AuthProvider(provider = ProviderType.ANONYMOUS.name),
                schemaVersion = Constants.SCHEMA_VERSION
            )
            playerRepository.save(player)
            petStatsChangeScheduler.schedule()
        } else {
            migrateIfNeeded()
        }

        val startIntent = intent
        if (startIntent != null && startIntent.action == ACTION_SHOW_TIMER) {
            val questId = intent.getStringExtra(Constants.QUEST_ID_EXTRA_KEY)
            router.setRoot(RouterTransaction.with(TimerViewController(questId)))
        } else if (!router.hasRootController()) {
            router.setRoot(RouterTransaction.with(HomeViewController()))
//            router.setRoot(RouterTransaction.with(TestViewController()))
//            router.setRoot(RouterTransaction.with(ChallengeCategoryListViewController()))
//            router.setRoot(RouterTransaction.with(PersonalizeChallengeViewController()))
        }
        stateStore.dispatch(LoadDataAction.All)
    }

    private fun migrateIfNeeded() {
        val playerSchema = playerRepository.findSchemaVersion()
        if (playerSchema == null || playerSchema != Constants.SCHEMA_VERSION) {
            Migration(database).run()
        }
    }

    private fun incrementAppRun() {
        val pm = PreferenceManager.getDefaultSharedPreferences(this)
        val run = pm.getInt(Constants.KEY_APP_RUN_COUNT, 0)
        pm.edit().putInt(Constants.KEY_APP_RUN_COUNT, run + 1).apply()
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

    val rootRouter get() = router

    fun enterFullScreen() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
    }

    fun exitFullScreen() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
    }

    companion object {
        const val ACTION_SHOW_TIMER = "mypoli.android.intent.action.SHOW_TIMER"
    }
}