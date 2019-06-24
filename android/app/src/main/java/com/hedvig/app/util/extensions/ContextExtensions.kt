package com.hedvig.app.util.extensions

import android.app.Activity
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.support.annotation.ColorRes
import android.support.annotation.DrawableRes
import android.support.annotation.FontRes
import android.support.annotation.StringRes
import android.support.v4.content.ContextCompat
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.content.res.AppCompatResources
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.hedvig.app.SplashActivity

private const val SHARED_PREFERENCE_NAME = "hedvig_shared_preference"
private const val SHARED_PREFERENCE_IS_LOGGED_IN = "shared_preference_is_logged_in"

fun Context.compatColor(@ColorRes color: Int) = ContextCompat.getColor(this, color)

fun Context.compatFont(@FontRes font: Int) = ResourcesCompat.getFont(this, font)

fun Context.compatDrawable(@DrawableRes drawable: Int) = AppCompatResources.getDrawable(this, drawable)

fun Context.hideKeyboard(view: View) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}

fun Context.triggerRestartActivity(activity: Class<*> = SplashActivity::class.java) {
    val startActivity = Intent(this, activity)
    val pendingIntentId = 56665 // Randomly chosen identifier, this number has no significance.
    val pendingIntent =
        PendingIntent.getActivity(this, pendingIntentId, startActivity, PendingIntent.FLAG_CANCEL_CURRENT)
    val mgr = this.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, pendingIntent)
    System.exit(0)
}

fun Context.setIsLoggedIn(isLoggedIn: Boolean) =
    getSharedPreferences().edit().putBoolean(SHARED_PREFERENCE_IS_LOGGED_IN, isLoggedIn).commit()

fun Context.isLoggedIn(): Boolean =
    getSharedPreferences().getBoolean(SHARED_PREFERENCE_IS_LOGGED_IN, false)

private fun Context.getSharedPreferences() = this.getSharedPreferences(SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE)

fun Context.showShareSheet(title: String, configureClosure: ((Intent) -> Unit)?) {
    val intent = Intent().apply {
        action = Intent.ACTION_SEND
    }

    configureClosure?.let { it(intent) }
    startActivity(
        Intent.createChooser(intent, title)
    )
}

/**
 * Note: This extension will not accept an Application Context
 */
fun Context.showAlert(
    @StringRes title: Int,
    @StringRes message: Int,
    @StringRes positiveLabel: Int = android.R.string.ok,
    @StringRes negativeLabel: Int = android.R.string.cancel,
    positiveAction: () -> Unit,
    negativeAction: (() -> Unit)? = null
): AlertDialog =
    AlertDialog
        .Builder(this)
        .setTitle(resources.getString(title))
        .setMessage(resources.getString(message))
        .setPositiveButton(resources.getString(positiveLabel)) { _, _ ->
            positiveAction()
        }
        .setNegativeButton(resources.getString(negativeLabel)) { _, _ ->
            negativeAction?.let { it() }
        }
        .show()
        .apply {
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

fun Context.copyToClipboard(
    text: String
) {
    (getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)
        ?.primaryClip = ClipData.newPlainText(null, text)
}

fun Context.makeToast(
    @StringRes text: Int,
    length: Int = Toast.LENGTH_LONG
) = makeToast(resources.getString(text), length)

fun Context.makeToast(
    text: String,
    length: Int = Toast.LENGTH_LONG
) = Toast.makeText(this, text, length).show()
