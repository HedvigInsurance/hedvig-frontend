package com.hedvig.app.feature.profile.ui.referral

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.hedvig.android.owldroid.graphql.ProfileQuery
import com.hedvig.app.R
import com.hedvig.app.util.LightClass
import com.hedvig.app.util.extensions.*
import com.hedvig.app.util.extensions.view.remove
import com.hedvig.app.util.extensions.view.show
import com.hedvig.app.util.getLightness
import com.hedvig.app.util.hashColor
import com.hedvig.app.util.interpolateTextKey
import kotlinx.android.synthetic.main.referral_header.view.*
import kotlinx.android.synthetic.main.referral_invite_row.view.*
import kotlin.math.ceil
import kotlin.math.min

class InvitesAdapter(
    private val monthlyCost: Int,
    private val data: ProfileQuery.ReferralInformation
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, position: Int): RecyclerView.ViewHolder = when (position) {
        HEADER -> {
            HeaderViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.referral_header, parent, false)
            )
        }
        else -> ItemViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.referral_invite_row, parent, false)
        )
    }

    override fun getItemViewType(position: Int) = if (position == 0) {
        HEADER
    } else {
        ITEM
    }

    override fun getItemCount(): Int {
        var count = 1 //start of with header
        count += data.invitations?.size ?: 0
        data.referredBy?.let { count += 1 }

        return count
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        when (viewHolder.itemViewType) {
            HEADER -> (viewHolder as? HeaderViewHolder)?.apply {
                val incentive =
                    data.campaign?.monthlyCostDeductionIncentive()?.amount?.amount?.toBigDecimal()?.toInt()
                        ?: return@apply
                if (monthlyCost / incentive <= PROGRESS_TANK_MAX_SEGMENTS) {
                    progressTankView.initialize(
                        monthlyCost,
                        calculateDiscount(),
                        incentive
                    )
                    progressTankView.show()
                    referralProgressHighPremiumContainer.remove()
                } else {
                    referralProgressHighPremiumContainer.show()
                    referralProgressHighPremiumDiscount.text = interpolateTextKey(
                        referralProgressHighPremiumDiscount.resources.getString(R.string.REFERRAL_PROGRESS_HIGH_PREMIUM_DISCOUNT),
                        "DISCOUNT_VALUE" to calculateDiscount().toString()
                    )
                    referralProgressHighPremiumCurrentPrice.text = interpolateTextKey(
                        referralProgressHighPremiumDiscount.resources.getString(R.string.REFERRAL_PROGRESS_HIGH_PREMIUM_DESCRIPTION),
                        "MONTHLY_COST" to monthlyCost.toString()
                    )
                    progressTankView.remove()
                }
                val campaignCode = data.campaign.code
                code.text = campaignCode
                code.setOnLongClickListener {
                    code.context.copyToClipboard(campaignCode)
                    code.context.makeToast(R.string.REFERRAL_INVITE_CODE_COPIED_MESSAGE)
                    true
                }
                subtitle.text = interpolateTextKey(
                    subtitle.resources.getString(R.string.REFERRAL_PROGRESS_HEADLINE),
                    "NUMBER_OF_FRIENDS_LEFT" to calculateInvitesLeftToFree().toString()
                )
                explainer.text = interpolateTextKey(
                    explainer.resources.getString(R.string.REFERRAL_PROGRESS_BODY),
                    "REFERRAL_VALUE" to incentive.toString()
                )
            }
            ITEM -> (viewHolder as? ItemViewHolder)?.apply {
                when (val referral = getReferralFromPosition(position)) {
                    is ProfileQuery.AsActiveReferral -> bindActiveRow(
                        this,
                        referral.name,
                        referral.discount.amount
                    )
                    is ProfileQuery.AsActiveReferral1 -> bindActiveRow(
                        this,
                        referral.name,
                        referral.discount.amount
                    )
                    is ProfileQuery.AsInProgressReferral -> bindInProgress(this, referral.name)
                    is ProfileQuery.AsInProgressReferral1 -> bindInProgress(this, referral.name)
                    is ProfileQuery.AsNotInitiatedReferral,
                    is ProfileQuery.AsNotInitiatedReferral1 -> bindNotInitiated(this)
                    is ProfileQuery.AsTerminatedReferral -> bindTerminated(this, referral.name)
                    is ProfileQuery.AsTerminatedReferral1 -> bindTerminated(this, referral.name)
                }
            }
        }
    }

    private fun bindActiveRow(viewHolder: ItemViewHolder, nameString: String?, discountString: String?) =
        viewHolder.apply {
            setupAvatarWithLetter(this, nameString)

            name.text = nameString
            statusText.text = statusText.resources.getString(R.string.REFERRAL_INVITE_NEWSTATE)

            statusIconContainer.setBackgroundResource(R.drawable.background_rounded_corners)
            statusIconContainer.background.setTint(
                statusIconContainer.context.compatColor(
                    R.color.light_gray
                )
            )
            discount.text = interpolateTextKey(
                discount.resources.getString(R.string.REFERRAL_INVITE_ACTIVE_VALUE),
                "REFERRAL_VALUE" to discountString
            )
            statusIcon.setImageDrawable(statusIcon.context.compatDrawable(R.drawable.ic_filled_checkmark))
        }

    private fun bindInProgress(viewHolder: ItemViewHolder, nameString: String?) = viewHolder.apply {
        setupAvatarWithLetter(this, nameString)

        name.text = nameString
        statusText.text = statusText.resources.getString(R.string.REFERRAL_INVITE_STARTEDSTATE)

        statusIcon.setImageDrawable(statusIcon.context.compatDrawable(R.drawable.ic_clock))
    }

    private fun bindNotInitiated(viewHolder: ItemViewHolder) = viewHolder.apply {
        avatar.setImageDrawable(avatar.context.compatDrawable(R.drawable.ic_ghost))
        avatar.scaleType = ImageView.ScaleType.CENTER
        name.text = name.resources.getString(R.string.REFERRAL_INVITE_ANON)
        statusText.text = statusText.resources.getString(R.string.REFERRAL_INVITE_OPENEDSTATE)
        statusIcon.setImageDrawable(statusIcon.context.compatDrawable(R.drawable.ic_clock))
    }

    private fun bindTerminated(viewHolder: ItemViewHolder, nameString: String?) = viewHolder.apply {
        setupAvatarWithLetter(this, nameString)
        name.text = nameString
        statusText.text = statusText.resources.getString(R.string.REFERRAL_INVITE_QUITSTATE)
        statusIcon.setImageDrawable(statusIcon.context.compatDrawable(R.drawable.ic_cross))
    }

    private fun setupAvatarWithLetter(viewHolder: ItemViewHolder, name: String?) {
        viewHolder.apply {
            if (!name.isNullOrBlank()) {
                avatar.setImageDrawable(avatar.context.compatDrawable(R.drawable.sphere))
                val hashedColor = avatar.context.compatColor(hashColor(name))
                avatar.drawable.mutate().setTint(hashedColor)
                avatarLetter.text = name[0].toString().capitalize()
                avatarLetter.setTextColor(
                    avatarLetter.context.compatColor(
                        when (getLightness(hashedColor)) {
                            LightClass.DARK -> R.color.off_white
                            LightClass.LIGHT -> R.color.off_black_dark
                        }
                    )
                )
            }
        }
    }

    private fun getReferralFromPosition(position: Int): Any? =
        data.invitations?.getOrNull(position - 1)
            ?: data.referredBy

    //TODO: Let's get the data from backend
    private fun calculateDiscount(): Int {
        var totalDiscount = 0
        (data.referredBy as? ProfileQuery.AsActiveReferral?)?.let {
            totalDiscount += it.discount.amount.toBigDecimal().toInt()
        }
        data.invitations?.filterIsInstance(ProfileQuery.AsActiveReferral1::class.java)
            ?.forEach { receiver -> totalDiscount += receiver.discount.amount.toBigDecimal().toInt() }
        return min(totalDiscount, monthlyCost)
    }

    //TODO: Let's get the data from backend
    private fun calculateInvitesLeftToFree(): Int {
        val amount = monthlyCost - calculateDiscount()
        val incentive =
            (data.campaign.incentive as? ProfileQuery.AsMonthlyCostDeduction)?.amount?.amount?.toBigDecimal()?.toDouble()
                ?: 0.0
        return ceil((amount / incentive)).toInt()
    }

    companion object {
        private const val HEADER = 0
        private const val ITEM = 1

        private const val PROGRESS_TANK_MAX_SEGMENTS = 20
    }

    inner class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val progressTankView: ProgressTankView = view.discountView
        val referralProgressHighPremiumContainer: LinearLayout = view.referralProgressHighPremiumContainer
        val referralProgressHighPremiumDiscount: TextView = view.referralProgressHighPremiumDiscount
        val referralProgressHighPremiumCurrentPrice: TextView = view.referralProgressHighPremiumCurrentPrice
        val subtitle: TextView = view.subtitle
        val explainer: TextView = view.explainer
        val code: TextView = view.code
    }

    inner class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val avatar: ImageView = view.avatar
        val avatarLetter: TextView = view.avatarLetter
        val name: TextView = view.name
        val statusText: TextView = view.statusText
        val statusIconContainer: LinearLayout = view.statusIconContainer
        val discount: TextView = view.discount
        val statusIcon: ImageView = view.statusIcon
    }
}
