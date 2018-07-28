package io.ipoli.android.player.profile

import android.annotation.SuppressLint
import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.HorizontalChangeHandler
import com.bluelinelabs.conductor.support.RouterPagerAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import io.ipoli.android.Constants
import io.ipoli.android.R
import io.ipoli.android.common.datetime.startOfDayUTC
import io.ipoli.android.common.redux.android.ReduxViewController
import io.ipoli.android.common.text.LongFormatter
import io.ipoli.android.common.view.*
import io.ipoli.android.player.data.AndroidAvatar
import io.ipoli.android.player.profile.ProfileViewState.StateType.*
import kotlinx.android.synthetic.main.controller_profile.view.*
import kotlinx.android.synthetic.main.view_loader.view.*
import org.threeten.bp.LocalDate
import org.threeten.bp.Period

class ProfileViewController :
    ReduxViewController<ProfileAction, ProfileViewState, ProfileReducer> {

    override var reducer = ProfileReducer(ProfileReducer.PROFILE_KEY)

    private var isEdit: Boolean = false
    private var friendId: String? = null

    private val displayNameWatcher: TextWatcher = object : TextWatcher {

        override fun afterTextChanged(s: Editable) {
            renderDisplayNameLengthHint(s.length)
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

        }
    }

    private val bioWatcher: TextWatcher = object : TextWatcher {

        override fun afterTextChanged(s: Editable) {
            renderBioLengthHint(s.length)
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

        }

    }

    private val pageChangeListener = object : ViewPager.OnPageChangeListener {
        override fun onPageScrollStateChanged(state: Int) {

        }

        override fun onPageScrolled(
            position: Int,
            positionOffset: Float,
            positionOffsetPixels: Int
        ) {

        }

        override fun onPageSelected(position: Int) {
            view?.tabLayout?.getTabAt(position)?.select()
        }
    }

    private val tabListener = object : TabLayout.OnTabSelectedListener {
        override fun onTabReselected(tab: TabLayout.Tab?) {
        }

        override fun onTabUnselected(tab: TabLayout.Tab?) {
        }

        override fun onTabSelected(tab: TabLayout.Tab?) {
            view?.pager?.currentItem = tab?.position ?: 0
        }
    }

    constructor(friendId: String) : this() {
        this.friendId = friendId
        reducer = ProfileReducer(ProfileReducer.FRIEND_KEY)
    }

    constructor(args: Bundle? = null) : super(args = args) {
        reducer = ProfileReducer(ProfileReducer.PROFILE_KEY)
    }

    private fun renderDisplayNameLengthHint(length: Int) {
        @SuppressLint("SetTextI18n")
        view!!.displayNameLengthHint.text = "$length/${Constants.DISPLAY_NAME_MAX_LENGTH}"
    }

    private fun renderBioLengthHint(length: Int) {
        @SuppressLint("SetTextI18n")
        view!!.bioLengthHint.text = "$length/${Constants.BIO_MAX_LENGTH}"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?
    ): View {
        setHasOptionsMenu(true)
        val view = container.inflate(R.layout.controller_profile)
        setToolbar(view.toolbar)
        val collapsingToolbar = view.collapsingToolbarContainer
        collapsingToolbar.isTitleEnabled = false
        view.toolbar.title = stringRes(R.string.controller_profile_title)

        var coloredBackground = view.coloredBackground.background.mutate()
        coloredBackground = DrawableCompat.wrap(coloredBackground)
        DrawableCompat.setTint(coloredBackground, attrData(R.attr.colorPrimary))
        DrawableCompat.setTintMode(coloredBackground, PorterDuff.Mode.SRC_IN)

        if(friendId != null) {
            view.tabLayout.removeTabAt(2)
        }

        val avatarBackground = view.playerAvatarBackground.background as GradientDrawable
        avatarBackground.setColor(attrData(android.R.attr.colorBackground))

        view.post {
            view.requestFocus()
        }

        return view
    }

    override fun colorLayoutBars() {
        activity?.let {
            it.window.statusBarColor = attrData(android.R.attr.colorBackground)
            it.window.navigationBarColor = attrData(io.ipoli.android.R.attr.colorPrimary)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                it.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        showBackButton()
        if (friendId == null) {
            view.pager.adapter = ProfilePagerAdapter(this)
        } else {
            view.pager.adapter = ProfileFriendPagerAdapter(this)
        }
        view.displayNameEdit.addTextChangedListener(displayNameWatcher)
        view.bioEdit.addTextChangedListener(bioWatcher)
        view.tabLayout.getTabAt(0)!!.select()
        view.tabLayout.addOnTabSelectedListener(tabListener)
        view.pager.addOnPageChangeListener(pageChangeListener)
    }

    override fun onDetach(view: View) {
        view.displayNameEdit.removeTextChangedListener(displayNameWatcher)
        view.bioEdit.removeTextChangedListener(bioWatcher)
        view.tabLayout.removeOnTabSelectedListener(tabListener)
        view.pager.removeOnPageChangeListener(pageChangeListener)
        view.pager.adapter = object : PagerAdapter() {
            override fun isViewFromObject(view: View, `object`: Any) = false
            override fun getCount() = 0
        }
        resetDecorView()
        super.onDetach(view)
    }

    private fun resetDecorView() {
        activity?.let {
            it.window.decorView.systemUiVisibility = 0
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.profile_menu, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val editAction = menu.findItem(R.id.actionEdit)
        val saveAction = menu.findItem(R.id.actionSave)
        if (friendId != null) {
            saveAction.isVisible = false
            editAction.isVisible = false
        } else if (isEdit) {
            saveAction.isVisible = true
            editAction.isVisible = false
        } else {
            saveAction.isVisible = false
            editAction.isVisible = true
        }
        super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

            android.R.id.home ->
                return router.handleBack()

            R.id.actionEdit ->
                dispatch(ProfileAction.StartEdit)

            R.id.actionSave ->
                dispatch(
                    ProfileAction.Save(
                        view!!.displayNameEdit.text.toString(),
                        view!!.bioEdit.text.toString()
                    )
                )
        }
        return super.onOptionsItemSelected(item)
    }

    override fun handleBack(): Boolean {
        if (isEdit) {
            dispatch(ProfileAction.StopEdit)
            return true
        }
        return super.handleBack()
    }

    override fun onCreateLoadAction() = ProfileAction.Load(friendId)

    override fun render(state: ProfileViewState, view: View) {
        when (state.type) {

            LOADING -> {
                view.profileContainer.gone()
                view.loader.visible()
            }

            PROFILE_DATA_LOADED -> {
                isEdit = false
                activity!!.invalidateOptionsMenu()
                view.loader.gone()
                view.profileContainer.visible()
                view.editGroup.gone()
                view.displayName.visible()
                view.bio.visible()

                renderAvatar(state, view)
                renderInfo(state, view)
                renderLevelProgress(state, view)
                renderCoinsAndGems(state, view)
            }

            EDIT -> {
                isEdit = true
                activity!!.invalidateOptionsMenu()
                renderEdit(view, state)
            }

            EDIT_STOPPED -> {
                isEdit = false
                activity!!.invalidateOptionsMenu()
                view.editGroup.gone()
                view.displayName.visible()
                view.bio.visible()
            }

            else -> {
            }
        }
    }

    private fun renderCoinsAndGems(state: ProfileViewState, view: View) {
        view.coins.text = state.lifeCoinsText
        view.gems.text = state.gemsText
    }

    private fun renderEdit(view: View, state: ProfileViewState) {
        view.displayName.gone()
        view.bio.gone()
        view.editGroup.visible()
        val displayNameLength = state.displayNameText?.length ?: 0
        val bioLength = state.bioText?.length ?: 0
        view.displayNameEdit.setText(state.displayNameText ?: "")
        view.bioEdit.setText(state.bioText ?: "")
        view.displayNameEdit.setSelection(displayNameLength)
        view.bioEdit.setSelection(bioLength)

        renderDisplayNameLengthHint(displayNameLength)
        renderBioLengthHint(bioLength)
    }

    private fun renderInfo(state: ProfileViewState, view: View) {
        view.displayName.text = state.displayNameText
        if (state.username.isNullOrBlank()) {
            view.username.gone()
        } else {
            view.username.visible()
            @SuppressLint("SetTextI18n")
            view.username.text = "@${state.username}"
        }
        view.info.text = state.info
        view.bio.text = state.bioText
    }

    private fun renderAvatar(state: ProfileViewState, view: View) {
        Glide.with(view.context).load(state.avatarImage)
            .apply(RequestOptions.circleCropTransform())
            .into(view.playerAvatar)
        val background = view.playerAvatar.background as GradientDrawable
        background.setColor(colorRes(AndroidAvatar.valueOf(state.avatar.name).backgroundColor))

        if(friendId == null) {
            view.playerAvatar.onDebounceClick {
                navigateFromRoot().toAvatarStore(HorizontalChangeHandler())
            }
        }
    }

    private fun renderLevelProgress(state: ProfileViewState, view: View) {
        view.levelProgress.max = state.levelXpMaxProgress
        view.levelProgress.animateProgressFromZero(state.levelXpProgress)
        view.levelText.text = state.levelText
        view.levelProgressText.text = state.levelProgressText
    }

    private val ProfileViewState.avatarImage
        get() = AndroidAvatar.valueOf(avatar.name).image

    private val ProfileViewState.displayNameText
        get() = if (displayName.isNullOrBlank())
            stringRes(R.string.unknown_hero)
        else
            displayName

    private val ProfileViewState.info
        get() = "$titleText | Joined $createdAgoText"

    private val ProfileViewState.titleText: String
        get() {
            val titles = resources!!.getStringArray(R.array.player_titles)
            return titles[Math.min(titleIndex, titles.size - 1)]
        }

    private val ProfileViewState.createdAgoText: String
        get() {
            val p = Period.between(createdAgo.startOfDayUTC, LocalDate.now())
            return when {
                p.isZero || p.isNegative -> stringRes(R.string.today).toLowerCase()
                p.years > 0 -> "${p.years} years ago"
                p.months > 0 -> "${p.months} months ago"
                else -> "${p.days} days ago"
            }
        }

    private val ProfileViewState.bioText
        get() =
            if (bio.isNullOrBlank())
                stringRes(R.string.blank_bio)
            else
                bio

    private val ProfileViewState.levelText
        get() = "Level $level"

    private val ProfileViewState.levelProgressText
        get() = "$levelXpProgress / $levelXpMaxProgress"

    private val ProfileViewState.gemsText
        get() = LongFormatter.format(activity!!, gems.toLong())

    private val ProfileViewState.lifeCoinsText
        get() = LongFormatter.format(activity!!, coins.toLong())

    inner class ProfilePagerAdapter(controller: Controller) :
        RouterPagerAdapter(controller) {

        override fun configureRouter(router: Router, position: Int) {
            val page = when (position) {
                0 -> ProfileInfoViewController(reducer.stateKey)
                1 -> ProfilePostListViewController(reducer.stateKey, friendId)
                2 -> ProfileFriendListViewController(reducer.stateKey)
                3 -> ProfileChallengeListViewController(reducer.stateKey)
                else -> throw IllegalArgumentException("Unknown controller position $position")
            }
            router.setRoot(RouterTransaction.with(page))
        }

        override fun getItemPosition(`object`: Any): Int = PagerAdapter.POSITION_NONE

        override fun getCount() = 4
    }

    inner class ProfileFriendPagerAdapter(controller: Controller) :
        RouterPagerAdapter(controller) {

        override fun configureRouter(router: Router, position: Int) {
            val page = when (position) {
                0 -> ProfileInfoViewController(reducer.stateKey)
                1 -> ProfilePostListViewController(reducer.stateKey, friendId)
                2 -> ProfileChallengeListViewController(reducer.stateKey)
                else -> throw IllegalArgumentException("Unknown controller position $position")
            }
            router.setRoot(RouterTransaction.with(page))
        }

        override fun getItemPosition(`object`: Any): Int = PagerAdapter.POSITION_NONE

        override fun getCount() = 3
    }
}