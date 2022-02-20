package com.droidbaza.locationfilter

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.core.content.PermissionChecker
import java.math.RoundingMode

object Ext {
    fun Float.round(count: Int = 1): Float {
        return this.toBigDecimal().setScale(count, RoundingMode.DOWN).toFloat()
    }

    fun Context.locationSettingsDialog(
        title: String,
        message: String,
        negative: String,
        positive: String
    ) {
        AlertDialog.Builder(this)
            .setCancelable(true)
            .setTitle(title)
            .setMessage(message)
            .setNegativeButton(negative) { dialogInterface: DialogInterface, _: Int -> dialogInterface.dismiss() }
            .setPositiveButton(positive) { _: DialogInterface?, _: Int ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .create()
            .show()
    }

    fun <T : Any> List<T>.getMiddleOrNull(): T? {
        return getOrNull(size / 2)
    }

    inline fun Context?.hasGeoPermissions(needToRequest: (permission: String) -> Unit): Boolean {
        val permission = Manifest.permission.ACCESS_FINE_LOCATION
        val result = this?.let {
            (PermissionChecker.checkSelfPermission(
                it, permission
            ) == PermissionChecker.PERMISSION_GRANTED)
        } ?: false
        if (!result) {
            needToRequest(permission)
        }
        return result
    }
}