/*
 * SPDX-FileCopyrightText: 2026 Fakhri Adi Saputra
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.fakhrads.bypasscharging;

import android.content.ContentResolver;

import lineageos.health.HealthInterface;
import lineageos.providers.LineageSettings;

/**
 * What BypassCharging takes from the ROM, on Evolution X: Lineage charging control. platform/aosp
 * has the same class for a ROM without it; Android.bp picks one.
 */
final class RomPlatform {
    private RomPlatform() {}

    /**
     * Puts back Lineage charging control as versions before the MCA vote left it.
     *
     * @param mode the saved charging control mode, null if none was saved
     */
    static void restoreChargingControl(ContentResolver resolver, Integer mode, int limit,
            boolean enabled) {
        if (mode != null && mode != HealthInterface.MODE_NONE) {
            LineageSettings.System.putInt(resolver,
                    LineageSettings.System.CHARGING_CONTROL_MODE, mode);
        }
        // LineageSettings.System.CHARGING_CONTROL_LIMIT_VALIDATOR takes 70 to 100
        LineageSettings.System.putInt(resolver,
                LineageSettings.System.CHARGING_CONTROL_LIMIT,
                Math.max(70, Math.min(limit, 100)));
        LineageSettings.System.putInt(resolver,
                LineageSettings.System.CHARGING_CONTROL_ENABLED, enabled ? 1 : 0);
    }
}
