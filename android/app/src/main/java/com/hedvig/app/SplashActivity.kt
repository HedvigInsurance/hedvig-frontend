package com.hedvig.app

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Animatable2
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.ActivityOptionsCompat
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.hedvig.app.feature.marketing.ui.MarketingActivity
import com.hedvig.app.feature.offer.OfferActivity
import com.hedvig.app.feature.referrals.ReferralsReceiverActivity
import com.hedvig.app.service.LoginStatus
import com.hedvig.app.service.LoginStatusService
import com.hedvig.app.util.extensions.compatColor
import com.hedvig.app.util.extensions.startClosableChat
import com.hedvig.app.util.safeLet
import com.hedvig.app.util.whenApiVersion
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.util.concurrent.TimeUnit

class SplashActivity : BaseActivity() {

    private val loggedInService: LoginStatusService by inject()

    private var referralCode: String? = null
    private var referralIncentive: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        whenApiVersion(Build.VERSION_CODES.M) {
            window.statusBarColor = compatColor(R.color.off_white)
        }

        val avd = ((window.decorView.background as LayerDrawable).findDrawableByLayerId(R.id.drawable) as AnimatedVectorDrawable)
        avd.start()

        handleIntent(intent)
    }

    override fun onStart() {
        super.onStart()

        FirebaseDynamicLinks.getInstance().getDynamicLink(intent).addOnSuccessListener { pendingDynamicLinkData ->
            if (pendingDynamicLinkData != null && pendingDynamicLinkData.link != null) {
                val link = pendingDynamicLinkData.link
                val referee = link.getQueryParameter("memberId")
                val incentive = link.getQueryParameter("incentive")
                if (referee != null && incentive != null) {
                    getSharedPreferences("referrals", Context.MODE_PRIVATE).edit().putString("referee", referee)
                        .putString("incentive", incentive).apply()

                    val b = Bundle()
                    b.putString("invitedByMemberId", referee)
                    b.putString("incentive", incentive)

                    FirebaseAnalytics.getInstance(this).logEvent("referrals_open", b)
                }

                handleNewReferralLink(link)
            }
        }

        disposables += Observable
            .timer(900, TimeUnit.MILLISECONDS)
            .flatMap { loggedInService.getLoginStatus() }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                navigateToActivity(it)
            }, { Timber.e(it) })
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val appLinkAction = intent.action
        if (Intent.ACTION_VIEW == appLinkAction) {
            Uri.parse(intent.data?.getQueryParameter("link"))?.let { handleNewReferralLink(it) }
        }
    }

    private fun handleNewReferralLink(link: Uri) {
        referralCode = link.getQueryParameter("code")
        referralIncentive = "10" //FIXME: this must be changed when we hav an other incentive then 10
    }

    private fun navigateToActivity(loginStatus: LoginStatus) = when (loginStatus) {
        LoginStatus.ONBOARDING -> {
            safeLet(referralCode, referralIncentive) { referralCode, incentive ->
                startActivity(ReferralsReceiverActivity.newInstance(this, referralCode, incentive))
            } ?: startActivity(Intent(this, MarketingActivity::class.java))
        }
        LoginStatus.IN_OFFER -> {
            val intent = Intent(this, OfferActivity::class.java)
            referralCode?.let { intent.putExtra(OfferActivity.EXTRA_REFERRAL_CODE, it) }
            startActivity(intent)
        }
        LoginStatus.LOGGED_IN -> {

            val options =
                ActivityOptionsCompat.makeCustomAnimation(this, R.anim.stay_in_place, R.anim.fade_out)

            ActivityCompat.startActivity(this, Intent(this, LoggedInActivity::class.java), options.toBundle())
        }
        LoginStatus.LOGGED_IN_TERMINATED -> startActivity(Intent(this, LoggedInTerminatedActivity::class.java))
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.stay_in_place, R.anim.fade_out)
    }
}
