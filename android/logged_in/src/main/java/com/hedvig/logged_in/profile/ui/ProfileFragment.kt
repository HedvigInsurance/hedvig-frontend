package com.hedvig.logged_in.profile.ui

import android.arch.lifecycle.Observer
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.hedvig.app.AsyncStorageNative
import com.hedvig.app.util.localBroadcastManager
import com.hedvig.common.owldroid.ProfileQuery
import com.hedvig.logged_in.R
import com.hedvig.logged_in.loggedin.BaseTabFragment
import com.hedvig.common.util.interpolateTextKey
import com.hedvig.app.viewmodel.DirectDebitViewModel
import com.hedvig.common.util.extensions.proxyNavigate
import com.hedvig.common.util.extensions.setIsLoggedIn
import com.hedvig.common.util.extensions.triggerRestartCurrentActivity
import com.hedvig.common.util.extensions.view.remove
import com.hedvig.common.util.extensions.view.show
import com.hedvig.logged_in.util.setupLargeTitle
import kotlinx.android.synthetic.main.fragment_profile.*
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.sharedViewModel
import com.hedvig.app.R as appR

class ProfileFragment : BaseTabFragment() {
    val asyncStorageNative: AsyncStorageNative by inject()

    val profileViewModel: ProfileViewModel by sharedViewModel()
    val directDebitViewModel: DirectDebitViewModel by sharedViewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        populateData()
        loadReferralFeature()
    }

    private fun loadReferralFeature() {
        profileViewModel.remoteConfigData.observe(this, Observer { remoteConfigData ->
            remoteConfigData?.let { rcd ->
                if (!rcd.referralsEnabled) {
                    return@Observer
                }
                profileReferralRow.setHighlighted()
                profileReferralRow.name = interpolateTextKey(
                    resources.getString(appR.string.PROFILE_ROW_REFERRAL_TITLE),
                    "INCENTIVE" to "${rcd.referralsIncentiveAmount}"
                )
                profileReferralRow.setOnClickListener {
                    navController.proxyNavigate(R.id.action_loggedInFragment_to_referralFragment)
                }
                profileReferralRow.show()
            }
        })
    }

    private fun populateData() {
        profileViewModel.data.observe(this, Observer { profileData ->
            loadingSpinner?.remove()
            rowContainer.show()
            logout.show()

            setupLargeTitle(appR.string.PROFILE_TITLE, appR.font.circular_bold)

            profileData?.let { data ->
                setupMyInfoRow(data)
                setupMyHomeRow(data)
                setupCoinsured(data)
                setupCharity(data)
                setupPayment(data)
                setupCertificateUrl(data)
            }

            feedbackRow.setOnClickListener {
                navController.proxyNavigate(R.id.action_loggedInFragment_to_feedbackFragment)
            }
            aboutAppRow.setOnClickListener {
                navController.proxyNavigate(R.id.action_loggedInFragment_to_aboutAppFragment)
            }
            logout.setOnClickListener {
                profileViewModel.logout {
                    requireContext().applicationContext.setIsLoggedIn(false)
                    localBroadcastManager.sendBroadcast(Intent(PROFILE_NAVIGATION_BROADCAST).apply {
                        putExtra("action", "logout")
                    })
                    asyncStorageNative.deleteKey("@hedvig:token")
                    requireActivity().triggerRestartCurrentActivity()
                }
            }
        })
    }

    private fun setupMyInfoRow(profileData: ProfileQuery.Data) {
        val firstName = profileData.member().firstName() ?: ""
        val lastName = profileData.member().lastName() ?: ""
        myInfoRow.description = "$firstName $lastName"
        myInfoRow.setOnClickListener {
            navController.proxyNavigate(R.id.action_loggedInFragment_to_myInfoFragment)
        }
    }

    private fun setupMyHomeRow(profileData: ProfileQuery.Data) {
        myHomeRow.description = profileData.insurance().address()
        myHomeRow.setOnClickListener {
            navController.proxyNavigate(R.id.action_loggedInFragment_to_myHomeFragment)
        }
    }

    private fun setupCoinsured(profileData: ProfileQuery.Data) {
        val personsInHousehold = profileData.insurance().personsInHousehold() ?: 1
        coinsuredRow.description = interpolateTextKey(
            resources.getString(appR.string.PROFILE_ROW_COINSURED_DESCRIPTION),
            "NUMBER" to "$personsInHousehold"
        )
        coinsuredRow.setOnClickListener {
            navController.proxyNavigate(R.id.action_loggedInFragment_to_coinsuredFragment)
        }
    }

    private fun setupCharity(profileData: ProfileQuery.Data) {
        charityRow.description = profileData.cashback()?.name()
        charityRow.setOnClickListener {
            navController.proxyNavigate(R.id.action_loggedInFragment_to_charityFragment)
        }
    }

    private fun setupPayment(profileData: ProfileQuery.Data) {
        paymentRow.description = interpolateTextKey(
            resources.getString(appR.string.PROFILE_ROW_PAYMENT_DESCRIPTION),
            "COST" to profileData.insurance().monthlyCost()?.toString()
        )
        paymentRow.setOnClickListener {
            navController.proxyNavigate(R.id.action_loggedInFragment_to_paymentFragment)
        }
    }

    private fun setupCertificateUrl(profileData: ProfileQuery.Data) {
        profileData.insurance().certificateUrl()?.let { policyUrl ->
            insuranceCertificateRow.show()
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(policyUrl))
            insuranceCertificateRow.setOnClickListener {
                startActivity(intent)
            }
        }
    }

    companion object {
        const val PROFILE_NAVIGATION_BROADCAST = "profileNavigation"
    }
}
