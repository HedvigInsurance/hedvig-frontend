package com.hedvig.app.feature.profile.ui.payment

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.SpannableString
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.hedvig.android.owldroid.type.DirectDebitStatus
import com.hedvig.app.R
import com.hedvig.app.feature.profile.ui.ProfileViewModel
import com.hedvig.app.feature.referrals.RedeemCodeDialog
import com.hedvig.app.feature.referrals.RefetchingRedeemCodeDialog
import com.hedvig.app.util.CustomTypefaceSpan
import com.hedvig.app.util.extensions.compatFont
import com.hedvig.app.util.extensions.concat
import com.hedvig.app.util.extensions.observe
import com.hedvig.app.util.extensions.proxyNavigate
import com.hedvig.app.util.extensions.setupLargeTitle
import com.hedvig.app.util.extensions.view.remove
import com.hedvig.app.util.extensions.view.setHapticClickListener
import com.hedvig.app.util.extensions.view.show
import com.hedvig.app.util.interpolateTextKey
import com.hedvig.app.viewmodel.DirectDebitViewModel
import kotlinx.android.synthetic.main.fragment_payment.*
import kotlinx.android.synthetic.main.loading_spinner.*
import org.koin.android.viewmodel.ext.android.sharedViewModel
import timber.log.Timber
import java.util.Calendar

class PaymentFragment : Fragment() {

    private val profileViewModel: ProfileViewModel by sharedViewModel()
    private val directDebitViewModel: DirectDebitViewModel by sharedViewModel()

    private val navController: NavController by lazy {
        requireActivity().findNavController(R.id.loggedNavigationHost)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_payment, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupLargeTitle(R.string.PROFILE_PAYMENT_TITLE, R.font.circular_bold, R.drawable.ic_back) {
            navController.popBackStack()
        }

        val today = Calendar.getInstance()
        val year = today.get(Calendar.YEAR).toString()
        val day = today.get(Calendar.DAY_OF_MONTH)
        val month = (today.get(Calendar.MONTH) + 1).let { month ->
            if (day > BILLING_DAY) {
                month + 1
            } else {
                month
            }
        }.let { String.format("%02d", it) }

        autogiroDate.text = interpolateTextKey(
            resources.getString(R.string.PROFILE_PAYMENT_NEXT_CHARGE_DATE),
            "YEAR" to year,
            "MONTH" to month,
            "DAY" to BILLING_DAY.toString()
        )

        changeBankAccount.setHapticClickListener {
            navController.proxyNavigate(R.id.action_paymentFragment_to_trustlyFragment)
        }

        connectBankAccount.setHapticClickListener {
            navController.proxyNavigate(R.id.action_paymentFragment_to_trustlyFragment)
        }

        redeemCode.setHapticClickListener {
            RefetchingRedeemCodeDialog
                .newInstance()
                .show(childFragmentManager, RefetchingRedeemCodeDialog.TAG)
        }

        loadData()
    }

    private fun loadData() {
        profileViewModel.data.observe(this, Observer { profileData ->
            loadingSpinner.remove()
            resetViews()
            sphereContainer.show()

            val monthlyCost = profileData?.insurance?.cost?.monthlyNet?.amount?.toBigDecimal()?.toInt()
            val amountPartOne = SpannableString("$monthlyCost\n")
            val perMonthLabel = resources.getString(R.string.PROFILE_PAYMENT_PER_MONTH_LABEL)
            val amountPartTwo = SpannableString(perMonthLabel)
            amountPartTwo.setSpan(
                CustomTypefaceSpan(requireContext().compatFont(R.font.circular_book)),
                0,
                perMonthLabel.length,
                Spanned.SPAN_EXCLUSIVE_INCLUSIVE
            )
            amountPartTwo.setSpan(
                AbsoluteSizeSpan(20, true),
                0,
                perMonthLabel.length,
                Spanned.SPAN_EXCLUSIVE_INCLUSIVE
            )
            profile_payment_amount.text = amountPartOne.concat(amountPartTwo)

            grossPremium.text = interpolateTextKey(
                resources.getString(R.string.PROFILE_PAYMENT_PRICE),
                "PRICE" to profileData?.insurance?.cost?.monthlyGross?.amount?.toBigDecimal()?.toInt()
            )

            discount.text = interpolateTextKey(
                resources.getString(R.string.PROFILE_PAYMENT_DISCOUNT),
                "DISCOUNT" to (profileData?.insurance?.cost?.monthlyDiscount?.amount?.toBigDecimal()?.toInt()?.unaryMinus())
            )

            netPremium.text = interpolateTextKey(
                resources.getString(R.string.PROFILE_PAYMENT_FINAL_COST),
                "FINAL_COST" to profileData?.insurance?.cost?.monthlyNet?.amount?.toBigDecimal()?.toInt()
            )

            bindBankAccountInformation()
        })
        directDebitViewModel.data.observe(this, Observer {
            bindBankAccountInformation()
        })
    }

    private fun resetViews() {
        connectBankAccountContainer.remove()
        changeBankAccount.remove()
        separator.remove()
        bankAccountUnderChangeParagraph.remove()
    }

    private fun bindBankAccountInformation() {
        val profileData = profileViewModel.data.value ?: return

        when (directDebitViewModel.data.value?.directDebitStatus ?: return) {
            DirectDebitStatus.ACTIVE -> {
                paymentDetailsContainer.show()
                bankName.text = profileData.bankAccount?.bankName ?: ""

                separator.show()
                accountNumber.text = profileData.bankAccount?.descriptor ?: ""
                changeBankAccount.show()
                if (profileData.insurance.cost?.monthlyDiscount?.amount?.toBigDecimal()?.toInt() == 0) {
                    redeemCode.show()
                }
            }
            DirectDebitStatus.PENDING -> {
                paymentDetailsContainer.show()
                bankName.text = profileData.bankAccount?.bankName

                accountNumber.text = resources.getString(R.string.PROFILE_PAYMENT_ACCOUNT_NUMBER_CHANGING)
                bankAccountUnderChangeParagraph.show()
            }
            DirectDebitStatus.NEEDS_SETUP -> connectBankAccountContainer.show()
            else -> {
                Timber.e("Payment fragment direct debit status UNKNOWN!")
            }
        }
    }

    companion object {
        const val BILLING_DAY = 27
    }
}
