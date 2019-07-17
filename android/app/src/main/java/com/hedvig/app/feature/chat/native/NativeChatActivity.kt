package com.hedvig.app.feature.chat.native

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.hedvig.android.owldroid.graphql.ChatMessagesQuery
import com.hedvig.app.util.extensions.view.setHapticClickListener
import com.hedvig.app.util.showRestartDialog
import kotlinx.android.synthetic.main.activity_chat.*
import org.koin.android.viewmodel.ext.android.viewModel
import android.provider.MediaStore
import android.provider.MediaStore.MediaColumns
import android.net.Uri
import com.hedvig.app.R
import com.hedvig.app.util.extensions.getAuthenticationToken
import com.hedvig.app.util.extensions.handleSingleSelectLink
import com.hedvig.app.util.extensions.observe
import com.hedvig.app.util.extensions.setAuthenticationToken
import com.hedvig.app.util.extensions.showAlert
import com.hedvig.app.util.extensions.triggerRestartActivity
import com.hedvig.app.util.extensions.calculateNonFullscreenHeightDiff
import com.hedvig.app.util.extensions.hasPermissions
import com.hedvig.app.util.extensions.askForPermissions
import android.content.Intent
import android.app.Activity
import android.os.Handler
import com.hedvig.app.feature.chat.UploadBottomSheet
import com.hedvig.app.util.extensions.view.updatePadding
import kotlinx.coroutines.*
import timber.log.Timber
import android.os.Environment
import java.io.File
import android.support.v4.content.FileProvider
import java.io.IOException

class NativeChatActivity : AppCompatActivity() {

    private val chatViewModel: ChatViewModel by viewModel()
    private val userViewModel: UserViewModel by viewModel()

    private var keyboardHeight = 0
    private var systemNavHeight = 0
    private var navHeightDiff = 0
    private var isKeyboardBreakPoint = 0

    private var isKeyboardShown = false
    private var preventOpenAttachFile = false
    private var preventOpenAttachFileHandler = Handler()

    private val resetPreventOpenAttachFile = { preventOpenAttachFile = false }

    private var attachPickerDialog: AttachPickerDialog? = null

    private var currentPhotoPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        keyboardHeight = resources.getDimensionPixelSize(R.dimen.default_attach_file_height)
        isKeyboardBreakPoint = resources.getDimensionPixelSize(R.dimen.is_keyboard_brake_point_height)
        navHeightDiff = resources.getDimensionPixelSize(R.dimen.nav_height_div)

        setContentView(R.layout.activity_chat)

        input.initialize(
            sendTextMessage = { message -> chatViewModel.respondToLastMessage(message) },
            sendSingleSelect = { value -> chatViewModel.respondWithSingleSelect(value) },
            sendSingleSelectLink = { value -> handleSingleSelectLink(value) },
            paragraphPullMessages = { chatViewModel.load() },
            openAttachFile = {
                if (!preventOpenAttachFile) {
                    if (hasPermissions(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        openAttachPicker()
                    } else {
                        askForPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_WRITE_PERMISSION)
                    }
                }
            },
            requestAudioPermission = {
                askForPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_AUDIO_PERMISSION)
            },
            uploadRecording = { path ->
                chatViewModel.uploadClaim(path)
            }
        )

        messages.adapter = ChatAdapter(this, onPressEdit = {
            showAlert(
                R.string.CHAT_EDIT_MESSAGE_TITLE,
                positiveLabel = R.string.CHAT_EDIT_MESSAGE_SUBMIT,
                negativeLabel = R.string.CHAT_EDIT_MESSAGE_CANCEL,
                positiveAction = {
                    chatViewModel.editLastResponse()
                }
            )
        })

        chatViewModel.messages.observe(lifecycleOwner = this) { data ->
            data?.let { bindData(it) }
        }
        // Maybe we should move the loading into the chatViewModel instead
        chatViewModel.sendMessageResponse.observe(lifecycleOwner = this) { response ->
            if (response == true) {
                input.clearInput()
                chatViewModel.load()
            }
        }
        chatViewModel.sendSingleSelectResponse.observe(lifecycleOwner = this) { response ->
            if (response == true) {
                chatViewModel.load()
            }
        }
        chatViewModel.sendFileResponse.observe(lifecycleOwner = this) { response ->
            if (response == true) {
                chatViewModel.load()
            }
        }
        chatViewModel.takePictureUploadOutcome.observe(lifecycleOwner = this) {
            attachPickerDialog?.uploadingTakenPicture(false)
            currentPhotoPath?.let { File(it).delete() }
        }

        resetChatButton.setOnClickListener {
            showRestartDialog {
                setAuthenticationToken(null)
                userViewModel.logout { triggerRestartActivity(NativeChatActivity::class.java) }
            }
        }

        reload.setHapticClickListener {
            chatViewModel.load()
        }

        getAuthenticationToken()?.let {
            chatViewModel.loadAndSubscribe()
        } ?: run {
            userViewModel.newSessionInformation.observe(lifecycleOwner = this@NativeChatActivity) { data ->
                data?.createSessionV2?.token?.let {
                    setAuthenticationToken(it)

                    chatViewModel.loadAndSubscribe()
                }
            }
            userViewModel.newSession()
        }

        chatRoot.viewTreeObserver.addOnGlobalLayoutListener {
            val heightDiff = chatRoot.calculateNonFullscreenHeightDiff()
            if (heightDiff > isKeyboardBreakPoint) {
                if (systemNavHeight > 0) systemNavHeight -= navHeightDiff
                this.keyboardHeight = heightDiff - systemNavHeight
                isKeyboardShown = true
            } else {
                systemNavHeight = heightDiff
                isKeyboardShown = false
            }
        }
    }

    private fun bindData(data: ChatMessagesQuery.Data) {
        (messages.adapter as? ChatAdapter)?.messages = data.messages
        input.message = data.messages.firstOrNull()?.let { ChatInputType.from(it) }
    }

    private fun openAttachPicker() {
        val attachPickerDialog = AttachPickerDialog(this)
        attachPickerDialog.initialize(
            takePhotoCallback = {
                if (hasPermissions(Manifest.permission.CAMERA)) {
                    startTakePicture()
                } else {
                    askForPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
                }
            },
            showUploadBottomSheetCallback = {
                UploadBottomSheet().show(supportFragmentManager, "FileUploadOverlay")
            },
            dismissCallback = { motionEvent ->

                motionEvent?.let {
                    preventOpenAttachFile = true
                    this.dispatchTouchEvent(motionEvent)
                    preventOpenAttachFileHandler.removeCallbacks(resetPreventOpenAttachFile)
                    // unfortunately the best way I found to prevent reopening :(
                    preventOpenAttachFileHandler.postDelayed(resetPreventOpenAttachFile, 100)
                }

                input.rotateFileUploadIcon(false)
                if (!isKeyboardShown) {
                    input.updatePadding(bottom = 0)
                }
                this.attachPickerDialog = null
            },
            uploadFileCallback = { uri ->
                chatViewModel.uploadFile(uri)
            }
        )
        chatViewModel.fileUploadOutcome.observe(lifecycleOwner = this) { data ->
            data?.uri?.path?.let { path ->
                attachPickerDialog.imageWasUploaded(path)
            }
        }
        attachPickerDialog.pickerHeight = keyboardHeight
        if (!isKeyboardShown) {
            input.updatePadding(bottom = keyboardHeight)
        }
        attachPickerDialog.show()

        GlobalScope.launch(Dispatchers.IO) {
            val images = getImagesPath()
            GlobalScope.launch(Dispatchers.Main) {
                attachPickerDialog.setImages(images)
            }
        }

        input.rotateFileUploadIcon(true)
        this.attachPickerDialog = attachPickerDialog
    }

    private fun startTakePicture() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: run {
                Timber.e("Could not getExternalFilesDir")
                return
            }

        val tempTakenPhotoFile = try {
            File.createTempFile(
                "JPEG_${System.currentTimeMillis()}_",
                ".jpg",
                storageDir
            ).apply {
                currentPhotoPath = absolutePath
            }
        } catch (ex: IOException) {
            Timber.e("Error occurred while creating the photo file")
            null
        }

        tempTakenPhotoFile?.also { file ->
            val photoURI: Uri = FileProvider.getUriForFile(
                this,
                "com.hedvig.android.file.provider",
                file
            )
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            startActivityForResult(takePictureIntent, TAKE_PICTURE_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            TAKE_PICTURE_REQUEST_CODE -> if (resultCode == Activity.RESULT_OK) {
                currentPhotoPath?.let { tempFile ->
                    attachPickerDialog?.uploadingTakenPicture(true)

                    chatViewModel.uploadTakenPicture(Uri.fromFile(File(tempFile)))
                }
            }
        }
    }

    private suspend fun getImagesPath(): List<String> = withContext(Dispatchers.IO) {
        val uri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val listOfAllImages = ArrayList<String>()
        val columnIndexData: Int

        val projection = arrayOf(MediaColumns.DATA, MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        val cursor = this@NativeChatActivity.contentResolver.query(uri, projection, null, null, null)

        cursor?.let {
            columnIndexData = cursor.getColumnIndexOrThrow(MediaColumns.DATA)
            while (it.moveToNext()) {
                listOfAllImages.add(it.getString(columnIndexData))
            }
        }
        cursor?.close()

        listOfAllImages
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_WRITE_PERMISSION ->
                if ((grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }))
                    openAttachPicker()
            REQUEST_CAMERA_PERMISSION ->
                if ((grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }))
                    startTakePicture()
            REQUEST_AUDIO_PERMISSION ->
                if ((grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }))
                    input.audioRecorderPermissionGranted()
            else -> { // Ignore all other requests.
            }
        }
    }

    override fun onDestroy() {
        preventOpenAttachFileHandler.removeCallbacks(resetPreventOpenAttachFile)
        super.onDestroy()
    }

    companion object {
        private const val REQUEST_WRITE_PERMISSION = 35134
        private const val REQUEST_CAMERA_PERMISSION = 54332
        private const val REQUEST_AUDIO_PERMISSION = 12994

        private const val TAKE_PICTURE_REQUEST_CODE = 2371
    }
}
