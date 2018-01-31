package mypoli.android.home

import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.net.Uri
import android.os.Bundle
import android.view.*
import com.amplitude.api.Amplitude
import com.bluelinelabs.conductor.RestoreViewOnCreateController
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.FadeChangeHandler
import kotlinx.android.synthetic.main.controller_home.view.*
import mypoli.android.Constants
import mypoli.android.R
import mypoli.android.challenge.category.ChallengeCategoryListViewController
import mypoli.android.common.view.FeedbackDialogController
import mypoli.android.common.view.setToolbar
import mypoli.android.common.view.showShortToast
import mypoli.android.pet.PetViewController
import mypoli.android.quest.schedule.ScheduleViewController
import mypoli.android.store.theme.ThemeStoreViewController
import org.json.JSONObject

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 8/19/17.
 */
class HomeViewController(args: Bundle? = null) :
    RestoreViewOnCreateController(args) {

//    private var navigationItemSelected: MenuItem? = null

//    override fun onNavigationItemSelected(item: MenuItem): Boolean {
//        navigationItemSelected = item
//        view?.drawerLayout?.closeDrawer(GravityCompat.START)
//        return false
//    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?
    ): View {
        setHasOptionsMenu(true)

        val contentView = inflater.inflate(R.layout.controller_home, container, false)
        setToolbar(contentView.toolbar)

        return contentView
    }

    override fun onAttach(view: View) {
        super.onAttach(view)

        val handler = FadeChangeHandler()
        val childRouter = getChildRouter(view.controllerContainer, null)
        if (!childRouter.hasRootController()) {
            childRouter.setRoot(
//                RouterTransaction.with(PetViewController())
                RouterTransaction.with(ScheduleViewController())
//                RouterTransaction.with(TimerViewController())
//                RouterTransaction.with(ChallengeCategoryListViewController())
//                RouterTransaction.with(PersonalizeChallengeViewController())
//                RouterTransaction.with(ThemeStoreViewController())
                    .pushChangeHandler(handler)
                    .popChangeHandler(handler)
            )
        }
//        RatePopup().show(view.context)

//        DurationPickerDialogController().showDialog(childRouter, "hello")

//        router.pushController(RouterTransaction.with(TimerViewController()))
//        actionBarDrawerToggle.syncState()


    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.home_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        when (item.itemId) {

            R.id.actionPet -> {
                showPet()
                true
            }

            R.id.actionFeedback -> {
                showFeedbackDialog()
                true
            }
            R.id.actionThemes -> {
                showThemes()
                true
            }
            R.id.actionChallenges -> {
                showChallenges()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }


    private fun showPet() {
        val handler = FadeChangeHandler()
        router.pushController(
            RouterTransaction.with(PetViewController())
                .pushChangeHandler(handler)
                .popChangeHandler(handler)
        )
    }

    private fun showChallenges() {
        val handler = FadeChangeHandler()
        router.pushController(
            RouterTransaction.with(ChallengeCategoryListViewController())
                .pushChangeHandler(handler)
                .popChangeHandler(handler)
        )
    }

    private fun showThemes() {
        val handler = FadeChangeHandler()
        router.pushController(
            RouterTransaction.with(ThemeStoreViewController())
                .pushChangeHandler(handler)
                .popChangeHandler(handler)
        )
    }

    private fun showFeedbackDialog() {
        FeedbackDialogController(object : FeedbackDialogController.FeedbackListener {
            override fun onSendFeedback(feedback: String) {
                if (feedback.isNotEmpty()) {
                    Amplitude.getInstance().logEvent(
                        "feedback",
                        JSONObject().put("feedback", feedback)
                    )
                    showShortToast(R.string.feedback_response)
                }
            }

            override fun onChatWithUs() {
                Amplitude.getInstance().logEvent("feedback_chat")
                val myIntent = Intent(ACTION_VIEW, Uri.parse(Constants.DISCORD_CHAT_LINK))
                startActivity(myIntent)
            }
        }).showDialog(router, "feedback-dialog")
    }

}