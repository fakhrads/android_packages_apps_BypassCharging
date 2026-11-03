/*
 * SPDX-FileCopyrightText: 2026 Fakhri Adi Saputra
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.fakhrads.bypasscharging

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.service.quicksettings.TileService
import dev.fakhrads.bypasscharging.ui.charging.ChargingControl
import dev.fakhrads.bypasscharging.ui.charging.ChargingState

/**
 * Charging control in this app's process: the settings page and ControlProvider (which
 * ZeroEngine's charging page asks) both go through here, so they change the same state.
 */
class LocalChargingControl(context: Context) : ChargingControl {
    private val context = context.applicationContext
    private val bypass = BypassCharging(this.context)
    private val limit = ChargeLimit(this.context)
    private val cool = CoolCharging.get(this.context)

    override fun state(): ChargingState {
        val supported = bypass.isSupported()
        return ChargingState(
            installed = true,
            supported = supported,
            active = supported && bypass.isActive(),
            mode = bypass.getMode(),
            activeLimit = bypass.getActiveLimit(),
            smartLimit = BypassCharging.SMART_LIMIT,
            coolEnabled = cool.isEnabled(),
            limitSupported = limit.isSupported(),
            limitEnabled = limit.isEnabled(),
            watts = limit.getWatts(),
            minWatts = ChargeLimit.MIN_WATTS,
            maxWatts = ChargeLimit.MAX_WATTS,
            stepWatts = ChargeLimit.STEP_WATTS
        )
    }

    override fun setBypass(enabled: Boolean): String? {
        val ok = bypass.isSupported() && bypass.setActive(enabled)
        changed()
        return if (ok) null else details(bypass.getLastError())
    }

    override fun setMode(mode: Int): String? {
        val ok = bypass.setMode(mode)
        changed()
        return if (ok) null else details(bypass.getLastError())
    }

    override fun setCool(enabled: Boolean): String? {
        cool.setEnabled(enabled)
        return null
    }

    override fun setLimit(enabled: Boolean, watts: Int): String? =
        if (limit.set(enabled, watts)) null else context.getString(R.string.limit_failed)

    // The tile shows the bypass state, so it has to follow changes made here
    private fun changed() {
        TileService.requestListeningState(context, ComponentName(context, BypassTileService::class.java))
    }

    /** The reason and the grant state of each requested permission, to copy into a report. */
    private fun details(error: String?): String {
        val sb = StringBuilder(error ?: "unknown error")
        try {
            context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
                .requestedPermissions?.forEach { perm ->
                    sb.append('\n').append(perm).append(": ").append(
                        if (context.checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED) "granted"
                        else "DENIED"
                    )
                }
        } catch (e: PackageManager.NameNotFoundException) {
            // Not possible for our own package
        }
        return sb.toString()
    }
}
