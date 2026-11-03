/*
 * SPDX-FileCopyrightText: 2026 Fakhri Adi Saputra
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.fakhrads.bypasscharging

import android.content.ContentProvider
import android.content.ContentValues
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import android.os.Process

/**
 * Lets ZeroEngine show and change the charging settings on its own charging page. Everything
 * still runs here, in this app's persistent process, which holds the charging votes and has
 * the SELinux access to the charger nodes.
 *
 * Only call() is used: "state" returns the settings, the "set_*" methods change one and return
 * "ok" and, when it failed, "error". Callers must be the system uid or signed with this app's
 * key; call() is not covered by the provider's read and write permissions, so it checks itself.
 */
class ControlProvider : ContentProvider() {

    private val control by lazy { LocalChargingControl(requireNotNull(context)) }

    override fun onCreate() = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        checkCaller()
        val args = extras ?: Bundle()
        // The caller's identity is only needed for the check; the work is this app's own
        val token = Binder.clearCallingIdentity()
        try {
            return when (method) {
                METHOD_STATE -> state()
                METHOD_SET_BYPASS -> result(control.setBypass(args.getBoolean(KEY_ENABLED)))
                METHOD_SET_MODE -> result(control.setMode(args.getInt(KEY_MODE)))
                METHOD_SET_COOL -> result(control.setCool(args.getBoolean(KEY_ENABLED)))
                METHOD_SET_LIMIT ->
                    result(control.setLimit(args.getBoolean(KEY_ENABLED), args.getInt(KEY_WATTS)))
                else -> throw IllegalArgumentException("Unknown method $method")
            }
        } finally {
            Binder.restoreCallingIdentity(token)
        }
    }

    private fun checkCaller() {
        val uid = Binder.getCallingUid()
        if (uid == Process.SYSTEM_UID || uid == Process.myUid()) return
        val pm = requireNotNull(context).packageManager
        if (pm.checkSignatures(uid, Process.myUid()) != PackageManager.SIGNATURE_MATCH) {
            throw SecurityException("uid $uid may not control charging")
        }
    }

    private fun state(): Bundle {
        val s = control.state()
        return Bundle().apply {
            putBoolean(KEY_SUPPORTED, s.supported)
            putBoolean(KEY_ACTIVE, s.active)
            putInt(KEY_MODE, s.mode)
            putInt(KEY_ACTIVE_LIMIT, s.activeLimit)
            putInt(KEY_SMART_LIMIT, s.smartLimit)
            putBoolean(KEY_COOL_ENABLED, s.coolEnabled)
            putBoolean(KEY_LIMIT_SUPPORTED, s.limitSupported)
            putBoolean(KEY_LIMIT_ENABLED, s.limitEnabled)
            putInt(KEY_WATTS, s.watts)
            putInt(KEY_MIN_WATTS, s.minWatts)
            putInt(KEY_MAX_WATTS, s.maxWatts)
            putInt(KEY_STEP_WATTS, s.stepWatts)
        }
    }

    private fun result(error: String?) = Bundle().apply {
        putBoolean(KEY_OK, error == null)
        if (error != null) putString(KEY_ERROR, error)
    }

    // Only call() is offered
    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ) = 0

    private companion object {
        const val METHOD_STATE = "state"
        const val METHOD_SET_BYPASS = "set_bypass"
        const val METHOD_SET_MODE = "set_mode"
        const val METHOD_SET_COOL = "set_cool"
        const val METHOD_SET_LIMIT = "set_limit"
        const val KEY_OK = "ok"
        const val KEY_ERROR = "error"
        const val KEY_ENABLED = "enabled"
        const val KEY_SUPPORTED = "supported"
        const val KEY_ACTIVE = "active"
        const val KEY_MODE = "mode"
        const val KEY_ACTIVE_LIMIT = "active_limit"
        const val KEY_SMART_LIMIT = "smart_limit"
        const val KEY_COOL_ENABLED = "cool_enabled"
        const val KEY_LIMIT_SUPPORTED = "limit_supported"
        const val KEY_LIMIT_ENABLED = "limit_enabled"
        const val KEY_WATTS = "watts"
        const val KEY_MIN_WATTS = "min_watts"
        const val KEY_MAX_WATTS = "max_watts"
        const val KEY_STEP_WATTS = "step_watts"
    }
}
