/*
 * SPDX-FileCopyrightText: 2026 Fakhri Adi Saputra
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.fakhrads.bypasscharging;

import android.content.ContentResolver;

/**
 * What BypassCharging takes from the ROM, on one without Lineage charging control: nothing. The
 * same class as platform/evo; Android.bp picks one.
 */
final class RomPlatform {
    private RomPlatform() {}

    static void restoreChargingControl(ContentResolver resolver, Integer mode, int limit,
            boolean enabled) {}
}
