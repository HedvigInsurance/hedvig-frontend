package com.hedvig.app.feature.offer

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.hedvig.android.owldroid.graphql.OfferQuery
import com.hedvig.android.owldroid.graphql.RedeemReferralCodeMutation
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import timber.log.Timber

class OfferViewModel(
    private val offerRepository: OfferRepository
) : ViewModel() {
    val data = MutableLiveData<OfferQuery.Data>()

    private val disposables = CompositeDisposable()

    init {
        load()
    }

    fun load() {
        disposables += offerRepository
            .loadOffer()
            .subscribe({ response ->
                if (response.hasErrors()) {
                    Timber.e(response.errors().toString())
                    return@subscribe
                }

                data.postValue(response.data())
            }, { Timber.e(it) })
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }

    fun removeDiscount() {
        disposables += offerRepository
            .removeDiscount()
            .subscribe({ response ->
                if (response.hasErrors()) {
                    Timber.e(response.errors().toString())
                    return@subscribe
                }

                removeDiscountFromCache()
            }, { Timber.e(it)})
    }

    private fun removeDiscountFromCache() {
        offerRepository.removeDiscountFromCache()
    }

    fun writeDiscountToCache(data: RedeemReferralCodeMutation.Data) = offerRepository.writeDiscountToCache(data)
}
