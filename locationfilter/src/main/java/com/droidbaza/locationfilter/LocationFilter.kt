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

import android.location.Location
import com.droidbaza.locationfilter.Ext.round
import java.lang.Math.toDegrees
import java.lang.Math.toRadians
import kotlin.math.*


object LocationFilter {

    private const val SPEED_VARIANCE = 14f
    private const val VALID_ACCURACY = 19f
    private const val MIN_ACCURACY = 0.2f
    
    const val NORMAL_SPEED = 1.4f
    const val ACCELERATION_SPEED = 1.2f
    const val MEDIUM_SPEED = 2f
    const val HIGH_SPEED = 4f
    const val STOP_SPEED = 0.15f
    const val LOW_SPEED = 0.4f

    fun filteredBy(
        old: Location?,
        new: Location,
        timeInSec: Long,
    ): Location {
        val maxDistance = old?.distanceTo(new) ?: 0f
        var maxSpeed = maxDistance / timeInSec
        if (maxSpeed > 20) {
            maxSpeed = 20f
        }
        val accuracyFactor = accuracyFactor(new.accuracy)


        val oldSpeed = old?.speed ?: STOP_SPEED
        val locationSpeed = new.speed

        var speedVariance = (oldSpeed - maxSpeed).absoluteValue / NORMAL_SPEED

        if (speedVariance > 1) {
            speedVariance = 1 - speedVariance / SPEED_VARIANCE
        }
        if (speedVariance <= 0f) {
            speedVariance = 0.005f
        }

        var mayBeAuto = false
        var finalSpeed = if (locationSpeed in LOW_SPEED..3f) {
            locationSpeed
        } else {
            mayBeAuto = true
            maxSpeed * speedVariance
        }

        val minSpeed = minOf(oldSpeed, maxSpeed)
        val isRunOrDrive = maxSpeed > HIGH_SPEED && mayBeAuto && minSpeed > MEDIUM_SPEED

        var finalDistance = timeInSec * finalSpeed

        if (finalDistance > maxDistance) {
            finalDistance = maxDistance
        }

        if (isRunOrDrive) {
            finalDistance /= speedVariance
            finalSpeed = finalDistance / timeInSec
        } else {
            if (finalSpeed > NORMAL_SPEED) {
                finalDistance *= speedVariance
            }
        }

        if (finalSpeed < 0.05) {
            finalSpeed = LOW_SPEED
        }

        val min = minOf(finalSpeed, oldSpeed)
        val max = maxOf(finalSpeed, oldSpeed)

        val speedFactor = if (min != 0f) {
            max / min
        } else 1f

        if (speedFactor > ACCELERATION_SPEED && !isRunOrDrive) {
            finalSpeed = min * ACCELERATION_SPEED
            if (finalSpeed >= MEDIUM_SPEED) {
                finalSpeed /= NORMAL_SPEED
            }
        }
        if (speedFactor > HIGH_SPEED) {
            finalSpeed = min * HIGH_SPEED
        }

        if (old == null || accuracyFactor == -1f) {
            return new.also {
                it.speed = finalSpeed
            }
        }
        val averageBearing = (old.bearing + new.bearing) / 2

        val finalLocation = if (accuracyFactor < MIN_ACCURACY) {
            locationByBearing(
                old.latitude,
                old.longitude,
                averageBearing,
                finalDistance
            ).also {
                it.speed = finalSpeed
                it.bearing = new.bearing
            }

        } else {
            locationByDistance(old, new, maxDistance, finalDistance).also {
                it.speed = finalSpeed
                it.bearing = new.bearing
            }
        }
        return finalLocation
    }

    private fun accuracyFactor(
        newAccuracy: Float,
    ): Float {
        if (newAccuracy in 0.1f..VALID_ACCURACY) return -1f
        if (newAccuracy <= 0) return 1f
        return VALID_ACCURACY / newAccuracy
    }

    fun locationByOffset(
        old: Location,
        new: Location,
        offset: Float,
    ): Location {
        val lat = coordinateByOffset(old.latitude, new.latitude, offset)
        val lon = coordinateByOffset(old.longitude, new.longitude, offset)

        return Location(new.provider).also {
            it.latitude = lat
            it.longitude = lon
        }
    }

    private fun coordinateByOffset(old: Double, new: Double, offset: Float): Double {
        if (offset == -1f) return new
        val diffFactor = (new - old) * offset
        return new - diffFactor
    }

    private fun bearingByAccuracy(old: Float, new: Float, offset: Float): Float {
        val diffFactor = (new - old) * offset
        return old + diffFactor
    }

    fun deviationByPath(path: List<Location>, location: Location): Float {
        val distanceList = mutableListOf<Float>()
        path.forEach {
            val currentDistance = location.distanceTo(it)
            distanceList.add(currentDistance)
        }
        return distanceList.minOrNull() ?: -1f
    }

    fun locationByDistance(
        old: Location,
        new: Location,
        realDistance: Float,
        targetDistance: Float,
    ): Location {
        if (realDistance == targetDistance) return new
        val lat =
            coordinateByDistance(old.latitude, new.latitude, realDistance, targetDistance)
        val lon = coordinateByDistance(
            old.longitude,
            new.longitude,
            realDistance,
            targetDistance
        )
        return Location(new.provider).also {
            it.latitude = lat
            it.longitude = lon
        }

    }

    fun coordinateByDistance(
        old: Double,
        new: Double,
        realDistance: Float,
        targetDistance: Float,
    ): Double {
        return old + (new - old) * targetDistance / realDistance
    }

    fun locationByBearing(
        latitude: Double,
        longitude: Double,
        bearing: Float,
        distanceInMetres: Float,
    ): Location {
        val earthRadius = 6371000
        val minDegree = -180
        val maxDegree = 180

        val bearRad: Double = toRadians(bearing.toDouble())
        val latRad: Double = toRadians(latitude)
        val lonRad: Double = toRadians(longitude)
        val delta = distanceInMetres / earthRadius

        val targetLatRad = asin(
            sin(latRad) * cos(delta) + cos(latRad) * sin(delta) * cos(bearRad)
        )

        var lambda = lonRad + atan2(
            sin(bearRad) * sin(delta) * cos(latRad),
            cos(delta) - sin(latRad) * sin(targetLatRad)
        )

        var targetLon = toDegrees(lambda)
        if (targetLon < minDegree || targetLon > maxDegree) {
            lambda = ((lambda + 3 * Math.PI) % (2 * Math.PI)) - Math.PI
            targetLon = toDegrees(lambda)
        }
        val targetLat = toDegrees(targetLatRad)

        return Location("fused").also {
            it.latitude = targetLat
            it.longitude = targetLon
        }
    }

    fun bearingByLocations(
        lat1: Double?,
        long1: Double?,
        lat2: Double,
        long2: Double,
    ): Float {
        if (lat1 == null || long1 == null) return 0f
        val dLon = long2 - long1
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - (sin(lat1) * cos(lat2) * cos(dLon))
        var brng = atan2(y, x)
        brng = toDegrees(brng)
        brng = (brng + 359.99999) % 360
        return brng.toInt().toFloat()
    }

    fun burnedCalories(
        speed: Float,
        time: Long,
        humanHeight: Float = 1.7f,
        humanWeight: Float = 70f
    ): Float {

        /* Energy consumption (kcal / min) = 0.035 * M + (V2 / H) * 0.029 * M
         * where M is the weight of the human body (70 kg),
         * H - human height (170 cm),
         * V - walking speed (m / s).
         * */

        val result = 0.035f * humanWeight + (speed.pow(2) / humanHeight) * 0.029f * humanWeight
        return (result * time / 60).round()
    }

}
