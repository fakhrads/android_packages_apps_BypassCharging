/*
 * SPDX-FileCopyrightText: 2026 Fakhri Adi Saputra
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.fakhrads.bypasscharging;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Turns bypass charging on and off through the MCA charge interface (charge_enable).
 *
 * A vote of 0 from this app to the charge enable votables (an AND of every voter) stops the
 * charger from charging the battery, and nothing else: the charger keeps its input current and
 * the adapter keeps its voltage, so the phone runs from USB and the battery current stays near
 * zero. Lineage Health charging control is not used for it: on onyx it stops charging through
 * the smart_chg SOC limit, which also drops the input to 5 V and 0.5 to 1 A, so a game draws
 * the rest from the battery.
 *
 * Two modes, both held by BypassHold in the app's persistent process:
 * - smart: charges to 80%, then the phone runs from the charger; charging resumes 3% lower
 * - bypass: holds the battery level it was turned on at, at any level
 *
 * The vote is gone after a reboot and BootReceiver turns bypass off. Versions before this one
 * used Lineage charging control; the settings they saved are put back once.
 *
 * The node is labeled vendor_sysfs_charge_limit and made writable by init.onyx.rc, in the
 * device tree, like ichg_limit.
 */
final class BypassCharging {
    private static final String TAG = "BypassCharging";

    static final int MODE_SMART = 0;
    static final int MODE_BYPASS = 1;

    static final int SMART_LIMIT = 80;
    // Charging resumes this many percent below the held level
    static final int RESUME_MARGIN = 3;

    private static final String NODE =
            "/sys/devices/platform/soc/soc:mca_charge_interface/charge_enable";
    // Voter name in the MCA votables, next to ChargeLimit's; "all" covers buck, quick and wireless
    private static final String VOTE_PREFIX = "bypasscharging all ";

    private static final String PREFS = "bypass_charging";
    private static final String KEY_MODE = "mode";
    // Present only while bypass is on: the level it holds
    private static final String KEY_ACTIVE_LIMIT = "active_limit";
    // Lineage charging control settings saved by earlier versions
    private static final String KEY_SAVED_ENABLED = "saved_enabled";
    private static final String KEY_SAVED_MODE = "saved_mode";
    private static final String KEY_SAVED_LIMIT = "saved_limit";

    private final Context mContext;
    private final ContentResolver mResolver;
    private final SharedPreferences mPrefs;
    private String mLastError;

    BypassCharging(Context context) {
        mContext = context;
        mResolver = context.getContentResolver();
        // Device protected storage: usable before the first unlock (tile, boot receiver)
        mPrefs = context.createDeviceProtectedStorageContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Why the last setActive/setMode failed, for the toast; null if unknown. */
    String getLastError() {
        return mLastError;
    }

    boolean isSupported() {
        return new File(NODE).canWrite();
    }

    int getMode() {
        return mPrefs.getInt(KEY_MODE, MODE_SMART);
    }

    /** Selects the mode; if bypass is on, it is applied again with the new mode. */
    boolean setMode(int mode) {
        mLastError = null;
        mPrefs.edit().putInt(KEY_MODE, mode).apply();
        return !isActive() || apply();
    }

    /** The level held while active, otherwise -1. */
    int getActiveLimit() {
        return mPrefs.getInt(KEY_ACTIVE_LIMIT, -1);
    }

    boolean isActive() {
        return mPrefs.contains(KEY_ACTIVE_LIMIT);
    }

    /** @return true if the new state was applied */
    boolean setActive(boolean active) {
        mLastError = null;
        if (!isSupported()) {
            mLastError = NODE + " is not writable";
            return false;
        }
        if (active == isActive()) {
            return true;
        }
        if (!active) {
            restore();
            return !isActive();
        }
        return apply();
    }

    /** Turns bypass off: charging is allowed again and the held level is forgotten. */
    void restore() {
        restoreLegacySettings();
        mPrefs.edit().remove(KEY_ACTIVE_LIMIT).apply();
        if (!setCharging(true)) {
            mLastError = "writing " + NODE + " failed";
        }
        BypassHold.get(mContext).update();
    }

    private boolean apply() {
        int limit = getMode() == MODE_BYPASS ? batteryLevel() : SMART_LIMIT;
        if (limit < 1) {
            Log.e(TAG, "Unknown battery level");
            mLastError = "battery level " + limit;
            return false;
        }
        restoreLegacySettings();
        mPrefs.edit().putInt(KEY_ACTIVE_LIMIT, Math.min(limit, 100)).apply();
        // BypassHold decides from the level whether the battery charges now
        if (!BypassHold.get(mContext).update()) {
            mLastError = "writing " + NODE + " failed";
            mPrefs.edit().remove(KEY_ACTIVE_LIMIT).apply();
            setCharging(true);
            return false;
        }
        return true;
    }

    /** Votes for charging the battery or not; the charger keeps running the phone either way. */
    static boolean setCharging(boolean charging) {
        // One vote per write: the driver parses a single "user type value" per store
        try (FileOutputStream out = new FileOutputStream(NODE)) {
            out.write((VOTE_PREFIX + (charging ? 1 : 0)).getBytes(StandardCharsets.US_ASCII));
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Failed to write charging " + charging + " to " + NODE, e);
            return false;
        }
    }

    private int batteryLevel() {
        BatteryManager bm = mContext.getSystemService(BatteryManager.class);
        int level = bm == null ? -1 : bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        return Math.min(level, 100);
    }

    /** Puts back the charging control settings an earlier version replaced, once. */
    private void restoreLegacySettings() {
        if (!mPrefs.contains(KEY_SAVED_ENABLED)) {
            return;
        }
        try {
            Integer mode = mPrefs.contains(KEY_SAVED_MODE) ? mPrefs.getInt(KEY_SAVED_MODE, 0) : null;
            RomPlatform.restoreChargingControl(mResolver, mode,
                    mPrefs.getInt(KEY_SAVED_LIMIT, SMART_LIMIT),
                    mPrefs.getBoolean(KEY_SAVED_ENABLED, false));
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to restore charging control", e);
        }
        mPrefs.edit()
                .remove(KEY_SAVED_ENABLED)
                .remove(KEY_SAVED_MODE)
                .remove(KEY_SAVED_LIMIT)
                .apply();
    }
}
