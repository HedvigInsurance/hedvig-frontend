package com.hedvig.app.feature.chat

import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import com.facebook.react.ReactApplication
import com.facebook.react.ReactInstanceManager
import com.facebook.react.ReactNativeHost
import com.facebook.react.ReactRootView
import com.facebook.react.common.LifecycleState
import com.hedvig.app.R
import com.hedvig.app.ReactBaseActivity
import com.hedvig.app.feature.marketing.ui.MarketingActivity
import com.hedvig.app.react.ActivityStarterModule
import com.hedvig.app.react.NativeRoutingModule
import com.hedvig.app.util.extensions.compatColor
import com.hedvig.app.util.extensions.localBroadcastManager
import com.hedvig.app.util.extensions.view.remove
import com.hedvig.app.util.extensions.view.show
import com.hedvig.app.util.newBroadcastReceiver
import com.hedvig.app.util.showRestartDialog
import kotlinx.android.synthetic.main.chat_activity.*
import org.koin.android.viewmodel.ext.android.viewModel


class ChatActivity : ReactBaseActivity() {
    val chatViewModel: ChatViewModel by viewModel()

    private var reactRootView: ReactRootView? = null

    private val reactNativeHost: ReactNativeHost
        get() = (application as ReactApplication).reactNativeHost

    private val reactInstanceManager: ReactInstanceManager
        get() = reactNativeHost.reactInstanceManager

    private var broadcastReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.chat_activity)
        val reactRootView = ReactRootView(this)
        this.reactRootView = reactRootView
        reactViewContainer.addView(this.reactRootView)
        val reactArgs = Bundle().also {
            it.putString(
                EXTRA_INTENT,
                intent.getStringExtra(EXTRA_INTENT)
                    ?: MarketingActivity.MarketingResult.ONBOARD.toString()
            )
        }
        reactRootView.startReactApplication(reactInstanceManager, "Chat", reactArgs)

        window.statusBarColor = compatColor(R.color.off_white)

        intent.getBooleanExtra(EXTRA_SHOW_RESTART, false).let { showRestart ->
            if (showRestart) {
                resetChatButton.show()
            }
        }
        intent.getBooleanExtra(EXTRA_SHOW_CLOSE, false).let { showClose ->
            if (showClose) {
                closeChatButton.show()
            }
        }

        resetChatButton.setOnClickListener {
            showRestartDialog {
                loadingSpinner.show()
                chatViewModel.logout { broadcastLogout() }
            }
        }

        closeChatButton.setOnClickListener {
            onBackPressed()
        }
    }

    private fun broadcastLogout() {
        broadcastReceiver = newBroadcastReceiver { _, _ ->
            loadingSpinner.remove()
        }.also { localBroadcastManager.registerReceiver(it, IntentFilter(ActivityStarterModule.BROADCAST_RELOAD_CHAT)) }

        localBroadcastManager.sendBroadcast(Intent(NativeRoutingModule.ON_BOARDING_INTENT_FILER).also {
            it.putExtra(NativeRoutingModule.NAVIGATE_ROUTING_EXTRA_NAME_ACTION, NativeRoutingModule.NAVIGATE_ROUTING_EXTRA_VALUE_RESTART_CHAT_ON_BOARDING)
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        if (reactRootView != null) {
            reactRootView!!.unmountReactApplication()
            reactRootView = null
        }
        if (reactInstanceManager.lifecycleState != LifecycleState.RESUMED) {
            reactInstanceManager.onHostDestroy(this)
            reactNativeHost.clear()
        }
    }

    override fun finish() {
        super.finish()
        if (intent.getBooleanExtra(EXTRA_SHOW_CLOSE, false)) {
            overridePendingTransition(R.anim.stay_in_place, R.anim.activity_slide_down_out)
        }
    }

    companion object {
        const val EXTRA_INTENT = "intent"
        const val EXTRA_SHOW_RESTART = "show_restart"
        const val EXTRA_SHOW_CLOSE = "show_close"
    }
}
