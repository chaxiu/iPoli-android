package io.ipoli.android.common

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.facebook.share.model.ShareLinkContent
import com.facebook.share.widget.MessageDialog
import com.facebook.share.widget.ShareDialog
import com.google.android.gms.appinvite.AppInviteInvitation
import io.ipoli.android.Constants
import io.ipoli.android.Constants.Companion.FACEBOOK_PACKAGE
import io.ipoli.android.Constants.Companion.TWITTER_PACKAGE
import io.ipoli.android.R
import io.ipoli.android.common.di.Module
import io.ipoli.android.common.view.BaseDialogController
import io.ipoli.android.common.view.showLongToast
import io.ipoli.android.common.view.stringRes
import io.ipoli.android.myPoliApp
import kotlinx.android.synthetic.main.item_share.view.*
import kotlinx.android.synthetic.main.view_dialog_header.view.*
import space.traversal.kapsule.Injects
import space.traversal.kapsule.inject
import space.traversal.kapsule.required


/**
 * Created by Polina Zhelyazkova <polina@mypoli.fun>
 * on 3/26/18.
 */
class InviteFriendsDialogController : BaseDialogController(), Injects<Module> {

    private val eventLogger by required { eventLogger }

    companion object {
        const val INVITE_FRIEND_REQUEST_CODE = 876
    }

    override fun onCreateContentView(inflater: LayoutInflater, savedViewState: Bundle?): View {
        inject(myPoliApp.module(myPoliApp.instance))
        registerForActivityResult(INVITE_FRIEND_REQUEST_CODE)
        return inflater.inflate(R.layout.dialog_invite_friends, null)
    }

    override fun onHeaderViewCreated(headerView: View?) {
        headerView!!.dialogHeaderTitle.setText(R.string.invite_friends)
        val v = ViewUtils.dpToPx(8f, headerView.context).toInt()
        headerView.dialogHeaderIcon.setPadding(v, v, v, v)
        headerView.dialogHeaderIcon.setImageResource(R.drawable.ic_person_add_white_24dp)
    }

    override fun onCreateDialog(
        dialogBuilder: AlertDialog.Builder,
        contentView: View,
        savedViewState: Bundle?
    ): AlertDialog {

        val inviteIntent = Intent(Intent.ACTION_SEND);
        inviteIntent.type = "text/plain";
        inviteIntent.putExtra(Intent.EXTRA_TEXT, "");

        val adapter =
            ShareDialogAdapter(
                contentView.context,
                filterInviteProviders(contentView.context, inviteIntent)
            )
        dialogBuilder.setAdapter(adapter, { _, item ->
            val message = stringRes(R.string.invite_message)
            val packageName = adapter.getItem(item).packageName

            if (packageName == null) {
                onInviteWithFirebase(message)
            } else if (isFacebook(packageName)) {
                onInviteWithFacebook()
            } else {
                var text = message + " " + Constants.SHARE_URL
                if (isTwitter(packageName)) {
                    text += " via " + Constants.TWITTER_USERNAME
                }

                inviteIntent.putExtra(Intent.EXTRA_TEXT, text)
                inviteIntent.`package` = packageName
                activity!!.startActivity(inviteIntent)
            }
        })
        return dialogBuilder.create()
    }

    private fun onInviteWithFirebase(message: String) {
        val intent = AppInviteInvitation.IntentBuilder(stringRes(R.string.invite_title))
            .setMessage(message)
            .setCustomImage(Uri.parse(Constants.INVITE_IMAGE_URL))
            .setCallToActionText(stringRes(R.string.invite_call_to_action))
            .build()
        activity!!.startActivityForResult(intent, INVITE_FRIEND_REQUEST_CODE)
    }

    private fun onInviteWithFacebook() {
        val linkContent = ShareLinkContent.Builder()
            .setContentUrl(Uri.parse("https://play.google.com/store/apps/details?id=io.ipoli.android"))
            .build()
        if (MessageDialog.canShow(ShareLinkContent::class.java)) {
            MessageDialog.show(activity!!, linkContent)
        } else if (ShareDialog.canShow(ShareLinkContent::class.java)) {
            ShareDialog.show(activity!!, linkContent)
        } else {
            showLongToast(R.string.invite_request_update_facebook)
        }
    }

    private fun filterInviteProviders(context: Context, inviteIntent: Intent): List<ShareApp> {
        val shareApps = mutableListOf<ShareApp>()
        val apps = context.packageManager.queryIntentActivities(inviteIntent, 0)
        var twitter: ResolveInfo? = null
        for (info in apps) {
            val packageName = info.activityInfo.packageName
            val name = info.loadLabel(context.packageManager).toString()
            if (isTwitter(packageName)) {
                if (name == "Tweet") {
                    twitter = info
                }
                continue
            }
            if (isFacebook(packageName)) {
                continue
            }

            shareApps.add(ShareApp(packageName, name, info.loadIcon(context.packageManager)))
        }

        if (twitter != null) {
            shareApps.add(
                0,
                ShareApp(
                    twitter.activityInfo.packageName,
                    "Twitter",
                    twitter.loadIcon(context.packageManager)
                )
            )
        }

        shareApps.add(
            0,
            ShareApp(
                Constants.FACEBOOK_PACKAGE,
                "Facebook",
                ContextCompat.getDrawable(context, R.drawable.ic_facebook_blue_40dp)!!
            )
        )

        shareApps.add(
            0,
            ShareApp(
                null,
                "Email or SMS",
                ContextCompat.getDrawable(context, R.drawable.ic_email_red_40dp)!!
            )
        )
        return shareApps
    }

    private fun isFacebook(packageName: String) = packageName.startsWith(FACEBOOK_PACKAGE)

    private fun isTwitter(packageName: String) = packageName.startsWith(TWITTER_PACKAGE)

    data class ShareApp(val packageName: String?, val name: String, val icon: Drawable)

    inner class ShareDialogAdapter(context: Context, apps: List<ShareApp>) :
        ArrayAdapter<ShareApp>(context, R.layout.item_share, apps) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var view = convertView
            val app = getItem(position)
            if (view == null) {
                view =
                    LayoutInflater.from(context).inflate(R.layout.item_share, parent, false)
            }
            view!!.appName.text = app.name
            view.appIcon.setImageDrawable(app.icon)

            return view
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == INVITE_FRIEND_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                val inviteIds = AppInviteInvitation.getInvitationIds(resultCode, data!!)

                eventLogger.logEvent(
                    "firebase_invite_sent",
                    Bundle().apply { putInt("count", inviteIds.size) })
            } else {
                eventLogger.logEvent("firebase_invite_canceled")
            }
        }
    }
}