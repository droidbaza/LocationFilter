/*
 *  Created by droidbaza on 20.2.2022
 *
 *  Copyright (c) 2022 . All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

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