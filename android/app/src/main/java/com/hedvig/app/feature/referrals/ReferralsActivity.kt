package com.hedvig.app.feature.referrals

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.hedvig.android.owldroid.graphql.ProfileQuery
import com.hedvig.app.BuildConfig
import com.hedvig.app.LoggedInActivity
import com.hedvig.app.R
import com.hedvig.app.SplashActivity
import com.hedvig.app.feature.profile.service.ProfileTracker
import com.hedvig.app.feature.profile.ui.ProfileViewModel
import com.hedvig.app.feature.profile.ui.referral.InvitesAdapter
import com.hedvig.app.ui.decoration.BottomPaddingItemDecoration
import com.hedvig.app.util.extensions.observe
import com.hedvig.app.util.extensions.setupLargeTitle
import com.hedvig.app.util.extensions.showShareSheet
import com.hedvig.app.util.extensions.view.setHapticClickListener
import com.hedvig.app.util.extensions.view.updatePadding
import com.hedvig.app.util.interpolateTextKey
import com.hedvig.app.util.safeLet
import kotlinx.android.synthetic.main.app_bar.*
import kotlinx.android.synthetic.main.fragment_new_referral.*
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel
import timber.log.Timber

class ReferralsActivity : AppCompatActivity() {

    val tracker: ProfileTracker by inject()

    val profileViewModel: ProfileViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_new_referral)

        setupLargeTitle(R.string.PROFILE_REFERRAL_TITLE, R.font.circular_bold, R.drawable.ic_back) {
            onBackPressed()
        }
        invites.addItemDecoration(
            BottomPaddingItemDecoration(
                resources.getDimensionPixelSize(R.dimen.referral_extra_bottom_space)
            )
        )

        profileViewModel.data.observe(this) { data ->
            safeLet(
                data?.paymentWithDiscount?.grossPremium?.amount?.toBigDecimal()?.toInt(),
                data?.memberReferralCampaign
            ) { monthlyCost, referralCampaign ->
                bindData(monthlyCost, referralCampaign)
            } ?: Timber.e("No data")

            data?.memberReferralCampaign?.referralInformation?.let { referralInformation ->
                bindReferralsButton(
                    referralInformation.incentive.amount.toBigDecimal().toDouble(),
                    referralInformation.code,
                    BuildConfig.REFERRALS_LANDING_BASE_URL + referralInformation.code
                )
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.referral_more_info_menu, menu)
        toolbar.updatePadding(end = resources.getDimensionPixelSize(R.dimen.base_margin_double))
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.referralMoreInfo -> {
                profileViewModel.data.value?.memberReferralCampaign?.referralInformation?.incentive?.amount?.toBigDecimal()?.toInt()?.toString()?.let { incentive ->
                    ReferralBottomSheet.newInstance(incentive)
                        .show(supportFragmentManager, "moreInfoSheet")
                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun bindData(monthlyCost: Int, data: ProfileQuery.MemberReferralCampaign) {
        invites.adapter = InvitesAdapter(monthlyCost, data)
    }

    private fun bindReferralsButton(incentive: Double, code: String, referralLink: String) {
        referralButton.setHapticClickListener {
            tracker.clickReferral(incentive.toInt())
            showShareSheet("TODO Copy") { intent ->
                intent.apply {
                    putExtra(
                        Intent.EXTRA_TEXT,
                        interpolateTextKey(
                            resources.getString(R.string.REFERRAL_SMS_MESSAGE),
                            "REFERRAL_VALUE" to incentive.toString(),
                            "REFERRAL_CODE" to code,
                            "REFERRAL_LINK" to referralLink
                        )
                    )
                    type = "text/plain"
                }
            }
        }
    }

    companion object {
        const val EXTRA_IS_FROM_REFERRALS_NOTIFICATION = "extra_is_from_referrals_notification"
    }

    override fun onBackPressed() {
        if (intent.getBooleanExtra(EXTRA_IS_FROM_REFERRALS_NOTIFICATION, false)) {
            val intent = Intent(this, SplashActivity::class.java)
            intent.flags = intent.flags or Intent.FLAG_ACTIVITY_NO_HISTORY
            intent.putExtra(LoggedInActivity.EXTRA_NAVIGATE_TO_PROFILE_ON_START_UP, true)
            startActivity(intent)
        } else {
            super.onBackPressed()
        }
    }
}
