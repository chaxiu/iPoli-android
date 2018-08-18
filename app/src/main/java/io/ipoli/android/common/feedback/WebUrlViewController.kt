package io.ipoli.android.common.feedback

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import com.bluelinelabs.conductor.RestoreViewOnCreateController
import io.ipoli.android.MyPoliApp
import io.ipoli.android.R
import io.ipoli.android.common.di.UIModule
import io.ipoli.android.common.view.inflate
import space.traversal.kapsule.Injects
import space.traversal.kapsule.inject
import space.traversal.kapsule.required

class WebUrlViewController(args: Bundle? = null) :
    RestoreViewOnCreateController(
        args
    ), Injects<UIModule> {

    private val eventLogger by required { eventLogger }
    private var url = ""
    private var screenName = ""

    constructor(
        url: String,
        screenName: String
    ) : this() {
        this.url = url
        this.screenName = screenName
    }

    override fun onContextAvailable(context: Context) {
        inject(MyPoliApp.uiModule(context))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?
    ): View {
        val view = container.inflate(R.layout.controller_feedback) as WebView
        view.webChromeClient = WebChromeClient()
        @SuppressLint("SetJavaScriptEnabled")
        view.settings.javaScriptEnabled = true
        view.settings.domStorageEnabled = true
        view.loadUrl(url)
        return view
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        eventLogger.logCurrentScreen(
            activity!!,
            screenName
        )
    }
}