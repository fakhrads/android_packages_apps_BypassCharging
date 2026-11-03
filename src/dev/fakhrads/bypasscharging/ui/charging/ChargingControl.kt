/*
 * SPDX-FileCopyrightText: 2026 Fakhri Adi Saputra
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.fakhrads.bypasscharging.ui.charging

/** What the charging page shows; ZeroEngine gets the same through ControlProvider. */
data class ChargingState(
    val installed: Boolean = false,
    val supported: Boolean = false,
    val active: Boolean = false,
    val mode: Int = MODE_SMART,
    val activeLimit: Int = -1,
    val smartLimit: Int = 80,
    val coolEnabled: Boolean = false,
    val limitSupported: Boolean = false,
    val limitEnabled: Boolean = false,
    val watts: Int = 30,
    val minWatts: Int = 5,
    val maxWatts: Int = 60,
    val stepWatts: Int = 5
) {
    companion object {
        const val MODE_SMART = 0
        const val MODE_BYPASS = 1
    }
}

/** Charging control; each call blocks on the app that owns it, so call it off the main thread. */
interface ChargingControl {
    fun state(): ChargingState

    /** These return null when the change was applied, otherwise why it was not. */
    fun setBypass(enabled: Boolean): String?

    fun setMode(mode: Int): String?

    fun setCool(enabled: Boolean): String?

    fun setLimit(enabled: Boolean, watts: Int): String?
}
