package com.hedvig.app.feature.chat.native

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.net.Uri
import com.apollographql.apollo.api.Response
import com.hedvig.android.owldroid.graphql.ChatMessagesQuery
import com.hedvig.android.owldroid.graphql.UploadFileMutation
import com.hedvig.app.util.LiveEvent
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.concurrent.TimeUnit

class ChatViewModel(
    private val chatRepository: ChatRepository
) : ViewModel() {

    val messages = MutableLiveData<ChatMessagesQuery.Data>()
    val sendMessageResponse = MutableLiveData<Boolean>()
    val sendSingleSelectResponse = MutableLiveData<Boolean>()
    val sendFileResponse = MutableLiveData<Boolean>()
    val isUploading = LiveEvent<Boolean>()
    val uploadBottomSheetResponse = LiveEvent<UploadFileMutation.Data>()
    val fileUploadOutcome = LiveEvent<FileUploadOutcome>()
    val takePictureUploadOutcome = LiveEvent<FileUploadOutcome>()

    private val disposables = CompositeDisposable()

    fun loadAndSubscribe() {
        load()

        disposables += chatRepository.subscribeToChatMessages()
            .subscribe({ response ->
                Timber.e("onNext")
                response.data()?.message?.let {
                    Timber.e("Incoming message on subscription: %s", it.toString())
                    chatRepository
                        .writeNewMessage(
                            it.fragments.chatMessageFragment
                        )
                }
            }, {
                Timber.e(it)
            }, {
                //TODO: handle in UI
                Timber.i("subscribeToChatMessages was completed")
            })
    }

    fun load() {
        disposables += chatRepository
            .fetchChatMessages()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ response ->
                if (shouldBeDelayed(response)) {
                    postWithDelay(response)
                } else {
                    postResponseValue(response)
                }
            }, { Timber.e(it) })
    }

    fun uploadFile(uri: Uri) {
        uploadFile(uri) { data ->
            fileUploadOutcome.postValue(FileUploadOutcome(uri, !data.hasErrors()))
        }
    }

    fun uploadTakenPicture(uri: Uri) {
        uploadFile(uri) { data ->
            takePictureUploadOutcome.postValue(FileUploadOutcome(uri, !data.hasErrors()))
        }
    }

    private fun uploadFile(uri: Uri, onNext: (Response<UploadFileMutation.Data>) -> Unit) {
        isUploading.value = true
        disposables += chatRepository
            .uploadFile(uri)
            .subscribe( { data ->
                data.data()?.let {
                    respondWithFile(it.uploadFile.key, uri)
                }
                onNext(data)
            }, { Timber.e(it) })
    }

    fun uploadFileFromProvider(uri: Uri) {
        isUploading.value = true
        disposables += chatRepository
            .uploadFileFromProvider(uri)
            .subscribe({ data ->
                data.data()?.let {
                    respondWithFile(it.uploadFile.key, uri)
                    uploadBottomSheetResponse.postValue(data.data())
                }
            }, { Timber.e(it) })
    }

    private fun calculateDelay(response: Response<ChatMessagesQuery.Data>): Long =
        response.data()?.messages?.firstOrNull()?.fragments?.chatMessageFragment?.body?.text?.length?.times(
            PARAGRAPH_DELAY_MULTIPLIER_MS
        )?.toLong()
            ?: 0L

    //Todo: I will need help with the logic of this
    private fun shouldBeDelayed(response: Response<ChatMessagesQuery.Data>) =
        response.data()?.messages?.firstOrNull()?.fragments?.chatMessageFragment?.body?.type == "paragraph" ||
            response.data()?.messages?.getOrNull(1)?.fragments?.chatMessageFragment?.body?.type == "paragraph" ||
            response.data()?.messages?.getOrNull(2)?.fragments?.chatMessageFragment?.body?.type == "paragraph"

    private fun postWithDelay(response: Response<ChatMessagesQuery.Data>) {
        val delay = calculateDelay(response)
        disposables += Observable
            .timer(delay, TimeUnit.MILLISECONDS, Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                postResponseValue(response)
            }, {
                Timber.e(it, "Timer failed!")
                postResponseValue(response)
            })
    }

    private fun postResponseValue(response: Response<ChatMessagesQuery.Data>) {
        val data = response.data()
        messages.postValue(data)
    }

    fun respondToLastMessage(message: String) {
        disposables += chatRepository
            .sendChatMessage(getLastId(), message)
            .subscribe({ sendMessageResponse.postValue(it.data()?.isSendChatTextResponse) }, { Timber.e(it) })
    }

    private fun respondWithFile(key: String, uri: Uri) {
        disposables += chatRepository
            .sendFileResponse(getLastId(), key, uri)
            .subscribe({
                it.data()?.let { data ->
                    sendFileResponse.postValue(data.isSendChatFileResponse)
                }
            }, {
                Timber.e(it)
            })
    }

    fun respondWithSingleSelect(value: String) {
        disposables += chatRepository
            .sendSingleSelect(getLastId(), value)
            .subscribe(
                { sendSingleSelectResponse.postValue(it.data()?.isSendChatSingleSelectResponse) },
                { Timber.e(it) })
    }

    private fun getLastId(): String =
        messages.value?.messages?.firstOrNull()?.fragments?.chatMessageFragment?.globalId
            ?: throw RuntimeException("Messages is not initialized!")

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }

    fun uploadClaim(path: String) {
        disposables += chatRepository
            .uploadClaim(getLastId(), path)
            .subscribe({ response ->
                if (response.hasErrors()) {
                    Timber.e(response.errors().toString())
                    return@subscribe
                }

                load()

            }, { Timber.e(it) })
    }

    companion object {
        private const val PARAGRAPH_DELAY_MULTIPLIER_MS = 30
    }
}

