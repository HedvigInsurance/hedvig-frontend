package com.hedvig.app.feature.chat.native

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.*
import com.hedvig.app.R
import com.hedvig.app.util.whenApiVersion
import kotlinx.android.synthetic.main.attach_picker_dialog.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils.loadAnimation

class AttachPicker(context: Context) : Dialog(context, R.style.TransparentDialog) {

    var pickerHeight = 0
    lateinit var images: List<String>

    private var preventDismiss = false
    private var runningDismissAnimation = false

    private lateinit var takePhotoCallback: () -> Unit
    private lateinit var uploadFileCallback: () -> Unit

    init {
        window?.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window?.setWindowAnimations(R.style.DialogNoAnimation)
        setContentView(R.layout.attach_picker_dialog)
        setupDialogTouchEvents()
        setupWindowsParams()
        setupBottomSheetParams()
        setupRecyclerView()
    }

    fun initialize(takePhotoCallback: () -> Unit, uploadFileCallback: () -> Unit) {
        this.takePhotoCallback = takePhotoCallback
        this.uploadFileCallback = uploadFileCallback
    }

    override fun show() {
        super.show()
        animatePickerSheet(true)
    }

    override fun dismiss() {
        if (!runningDismissAnimation) {
            preventDismiss = true
            runningDismissAnimation = true
            animatePickerSheet(false)
        }
        if (!preventDismiss) {
            super.dismiss()
        }
    }

    private fun animatePickerSheet(show: Boolean) {
        //maybe we should create a better animation but this is something
        val animation = loadAnimation(context, if (show) R.anim.slide_in_up else R.anim.slide_out_down)
        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationEnd(animation: Animation?) {
                if (!show) {
                    preventDismiss = false
                    dismiss()
                    runningDismissAnimation = false
                }
            }

            override fun onAnimationRepeat(animation: Animation?) = Unit
            override fun onAnimationStart(animation: Animation?) = Unit
        })
        attachPickerBottomSheet.startAnimation(animation)
    }

    private fun setupWindowsParams() = window?.let { window ->
        val params = window.attributes
        params.width = ViewGroup.LayoutParams.MATCH_PARENT
        params.height = ViewGroup.LayoutParams.MATCH_PARENT

        params.dimAmount = 0f
        params.flags = params.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND.inv()

        params.flags = params.flags or
            WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR or
            WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS

        whenApiVersion(Build.VERSION_CODES.P) {
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        window.attributes = params
    }

    private fun setupBottomSheetParams() {
        val params = attachPickerBottomSheet.layoutParams
        params.height = pickerHeight
        attachPickerBottomSheet.layoutParams = params
    }

    private fun setupRecyclerView() {
        attachFileRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        attachFileRecyclerView.adapter = AttachFileAdapter(
            images,
            pickerHeight,
            takePhotoCallback,
            uploadFileCallback
        )
    }

    private fun setupDialogTouchEvents() {
        attachPickerRoot.setOnTouchListener { _, _ ->
            dismiss()
            false
        }
        //prevent dismiss in this area
        attachPickerBottomSheet.setOnTouchListener { _, _ -> true }
    }

    companion object {
        private const val ANIMATION_DURATION_MS = 1000L
    }
}
