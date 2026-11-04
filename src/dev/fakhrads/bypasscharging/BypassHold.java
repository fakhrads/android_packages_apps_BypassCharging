/*
 * SPDX-FileCopyrightText: 2026 Fakhri Adi Saputra
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.fakhrads.bypasscharging;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

/**
 * Holds the battery at the level BypassCharging set: at or above it the battery is not charged
 * and the phone runs from the charger; once the phone has drawn it RESUME_MARGIN lower (a load
 * above what the charger gives, or time unplugged) it charges back up to the level.
 *
 * It follows ACTION_BATTERY_CHANGED in the app's persistent process, like CoolCharging, and the
 * first update after the process starts always writes, so the kernel vote matches the saved
 * state even after the process died.
 */
final class BypassHold {
    private static final String TAG = "BypassCharging";

    private static final String PREFS = "bypass_charging";
    private static final String KEY_ACTIVE_LIMIT = "active_limit";

    private static BypassHold sInstance;

    private final Context mContext;
    private final SharedPreferences mPrefs;
    private final Handler mHandler;

    // Guarded by this; null until the first write
    private Boolean mCharging;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            update(intent);
        }
    };

    static synchronized BypassHold get(Context context) {
        if (sInstance == null) {
            sInstance = new BypassHold(context.getApplicationContext());
        }
        return sInstance;
    }

    private BypassHold(Context context) {
        mContext = context;
        // Device protected storage: the process starts before the first unlock
        mPrefs = context.createDeviceProtectedStorageContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        HandlerThread thread = new HandlerThread(TAG);
        thread.start();
        mHandler = new Handler(thread.getLooper());
    }

    void start() {
        mContext.registerReceiver(mReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED),
                null, mHandler);
    }

    /** Applies the saved state now; @return false if the kernel vote could not be written. */
    boolean update() {
        return update(mContext.registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED)));
    }

    private synchronized boolean update(Intent battery) {
        int limit = mPrefs.getInt(KEY_ACTIVE_LIMIT, -1);
        int level = levelOf(battery);
        boolean charging;
        if (limit < 0 || level < 0) {
            charging = true;
        } else if (level >= limit) {
            charging = false;
        } else if (level <= limit - BypassCharging.RESUME_MARGIN || mCharging == null) {
            charging = true;
        } else {
            // Between the resume level and the held one: keep going the way it was
            charging = mCharging;
        }
        if (mCharging != null && mCharging == charging) {
            return true;
        }
        if (!BypassCharging.setCharging(charging)) {
            return false;
        }
        Log.i(TAG, "Battery " + level + "%, hold " + limit + "%: "
                + (charging ? "charging" : "running from the charger"));
        mCharging = charging;
        return true;
    }

    private static int levelOf(Intent battery) {
        if (battery == null) {
            return -1;
        }
        int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        return level < 0 || scale <= 0 ? -1 : level * 100 / scale;
    }
}
