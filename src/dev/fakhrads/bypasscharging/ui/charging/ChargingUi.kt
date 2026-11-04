/*
 * SPDX-FileCopyrightText: 2026 Fakhri Adi Saputra
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.fakhrads.bypasscharging.ui.charging

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import dev.fakhrads.bypasscharging.R
import dev.fakhrads.bypasscharging.ui.components.Choice
import dev.fakhrads.bypasscharging.ui.components.ChoiceList
import dev.fakhrads.bypasscharging.ui.components.FooterText
import dev.fakhrads.bypasscharging.ui.components.HeroCard
import dev.fakhrads.bypasscharging.ui.components.HeroStat
import dev.fakhrads.bypasscharging.ui.components.SettingSlider
import dev.fakhrads.bypasscharging.ui.components.SettingSwitch
import dev.fakhrads.bypasscharging.ui.components.SettingsCard
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Locale

private const val READING_INTERVAL_MS = 2000L

// The USB node is in tenths of a degree, the board node in thousandths
private const val USB_TEMP = "/sys/class/power_supply/usb/temp"
private const val BOARD_TEMP = "/sys/devices/virtual/thermal/thermal_message/board_sensor_temp"

/** A live reading of the battery and the charger. */
data class ChargingReading(
    val charging: Boolean = false,
    val volts: Double = 0.0,
    val amps: Double = 0.0,
    val batteryTemp: Double = Double.NaN,
    val usbTemp: Double = Double.NaN,
    val boardTemp: Double = Double.NaN
) {
    val watts get() = volts * amps
}

private fun read(context: Context): ChargingReading {
    val battery: Intent? = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val status = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
    val tempTenths = battery?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
    val microAmps = context.getSystemService(BatteryManager::class.java)
        ?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) ?: 0
    return ChargingReading(
        charging = status == BatteryManager.BATTERY_STATUS_CHARGING,
        volts = (battery?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0) / 1e3,
        // The sign of current_now differs between drivers
        amps = if (microAmps == Int.MIN_VALUE) 0.0 else Math.abs(microAmps) / 1e6,
        batteryTemp = if (tempTenths > 0) tempTenths / 10.0 else Double.NaN,
        usbTemp = readTemp(USB_TEMP, 10.0),
        boardTemp = readTemp(BOARD_TEMP, 1000.0)
    )
}

/** A temperature from a sysfs node, in degrees; NaN if the node cannot be read. */
private fun readTemp(path: String, divisor: Double): Double =
    try {
        File(path).readText().trim().toInt() / divisor
    } catch (e: Exception) {
        Double.NaN
    }

/** The charging page's state, refreshed while it is on screen, and its actions. */
class ChargingUi(
    val state: ChargingState?,
    val reading: ChargingReading,
    val error: String?,
    val act: (ChargingControl.() -> String?) -> Unit,
    val dismissError: () -> Unit
)

@Composable
fun rememberChargingUi(control: ChargingControl): ChargingUi {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<ChargingState?>(null) }
    var reading by remember { mutableStateOf(ChargingReading()) }
    var error by remember { mutableStateOf<String?>(null) }
    // One change at a time, in order: a slider drag sends several and the last one must win
    val changes = remember { Mutex() }
    LaunchedEffect(owner, control) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                val (s, r) = withContext(Dispatchers.IO) { control.state() to read(context) }
                state = s
                reading = r
                delay(READING_INTERVAL_MS)
            }
        }
    }
    return ChargingUi(
        state = state,
        reading = reading,
        error = error,
        act = { block ->
            scope.launch {
                val (failure, s) = changes.withLock {
                    withContext(Dispatchers.IO) { control.block() to control.state() }
                }
                state = s
                if (failure != null) error = failure
            }
        },
        dismissError = { error = null }
    )
}

@Composable
private fun temp(celsius: Double) =
    if (celsius > 0 && celsius < 150) stringResource(R.string.hero_temp, celsius)
    else stringResource(R.string.hero_temp_unknown)

/**
 * The charging page: live hero with the bypass switch, bypass mode and fast charging.
 * [notInstalled] is the title and text shown when the app that does the work is missing.
 */
fun LazyListScope.chargingItems(ui: ChargingUi, notInstalled: Pair<String, String>? = null) {
    val state = ui.state
    item(key = "charging_hero") { ChargingHero(ui) }
    if (state == null) return
    if (!state.installed) {
        if (notInstalled != null) {
            item(key = "charging_missing") {
                SettingsCard(title = notInstalled.first, icon = Icons.Default.Error) {
                    FooterText(notInstalled.second)
                }
            }
        }
        return
    }

    item(key = "charging_mode") {
        SettingsCard(title = stringResource(R.string.category_bypass_mode), icon = Icons.Default.Tune) {
            ChoiceList(
                choices = listOf(
                    Choice(
                        ChargingState.MODE_SMART,
                        stringResource(R.string.mode_smart),
                        stringResource(R.string.summary_smart, state.smartLimit, state.smartLimit - 3),
                        Icons.Default.BatteryChargingFull
                    ),
                    Choice(
                        ChargingState.MODE_BYPASS,
                        stringResource(R.string.mode_bypass),
                        stringResource(R.string.summary_bypass),
                        Icons.Default.Power
                    ),
                ),
                selected = state.mode,
                enabled = state.supported,
                onSelect = { mode -> ui.act { setMode(mode) } }
            )
        }
    }

    item(key = "charging_speed") {
        SettingsCard(title = stringResource(R.string.category_fast_charging), icon = Icons.Default.Speed) {
            SettingSwitch(
                title = stringResource(R.string.cool_title),
                subtitle = stringResource(R.string.cool_summary),
                checked = state.coolEnabled,
                enabled = state.limitSupported,
                icon = Icons.Default.AcUnit,
                onCheckedChange = { enabled -> ui.act { setCool(enabled) } }
            )
            ChoiceList(
                choices = listOf(
                    Choice(
                        false,
                        stringResource(R.string.limit_off_title),
                        stringResource(R.string.limit_off_summary),
                        Icons.Default.ElectricBolt
                    ),
                    Choice(
                        true,
                        stringResource(R.string.limit_on_title),
                        if (state.limitSupported) {
                            stringResource(R.string.limit_on_summary, state.watts)
                        } else {
                            stringResource(R.string.limit_not_supported)
                        },
                        Icons.Default.Bolt
                    ),
                ),
                selected = state.limitEnabled,
                enabled = state.limitSupported,
                onSelect = { enabled -> ui.act { setLimit(enabled, state.watts) } }
            )
            AnimatedVisibility(visible = state.limitEnabled) {
                SettingSlider(
                    title = stringResource(R.string.limit_watts_title),
                    value = state.watts,
                    min = state.minWatts,
                    max = state.maxWatts,
                    step = state.stepWatts,
                    enabled = state.limitSupported,
                    valueLabel = { "$it W" },
                    onValueChange = { watts -> ui.act { setLimit(true, watts) } }
                )
            }
        }
    }

    item(key = "charging_footer") { FooterText(stringResource(R.string.footer)) }
}

@Composable
private fun ChargingHero(ui: ChargingUi) {
    val state = ui.state
    val reading = ui.reading
    val active = state?.active == true
    // The battery is taking power: before the limit is reached this is true while active too
    val charging = reading.charging && reading.watts > 0.1
    val bypass = state?.mode == ChargingState.MODE_BYPASS
    val limit = state?.activeLimit ?: 0
    val title: String
    val subtitle: String
    when {
        active && charging -> {
            title = stringResource(R.string.hero_charging_title, reading.watts)
            subtitle = stringResource(
                if (bypass) R.string.subtitle_bypass_from else R.string.subtitle_smart_on, limit
            )
        }
        active -> {
            title = stringResource(R.string.tile_label)
            // Held at the limit the charger runs the phone; what the battery still gives shows
            // when the charger is not enough
            subtitle = stringResource(
                R.string.hero_battery_power,
                stringResource(
                    if (bypass) R.string.subtitle_bypass_on else R.string.subtitle_smart_on, limit
                ),
                String.format(Locale.getDefault(), "%.1f", if (reading.watts > 0.05) -reading.watts else 0.0)
            )
        }
        charging -> {
            title = stringResource(R.string.hero_charging_title, reading.watts)
            subtitle = stringResource(R.string.hero_charging_detail, reading.volts, reading.amps)
        }
        else -> {
            title = stringResource(R.string.app_name)
            subtitle = stringResource(
                if (state != null && state.installed && !state.supported) R.string.not_supported
                else R.string.reading_idle
            )
        }
    }
    val modeLabel = when {
        state == null || !state.installed -> null
        active -> stringResource(R.string.mode_bypass)
        state.coolEnabled -> stringResource(R.string.cool_title)
        state.limitEnabled -> "${state.watts} W"
        else -> stringResource(R.string.limit_off_title)
    }
    HeroCard(
        title = title,
        subtitle = subtitle,
        icon = if (active) Icons.Default.Power else if (charging) Icons.Default.BatteryChargingFull
        else Icons.Default.PowerOff,
        active = active || charging,
        stats = listOf(
            HeroStat(stringResource(R.string.hero_stat_battery), temp(reading.batteryTemp)),
            HeroStat(stringResource(R.string.hero_stat_usb), temp(reading.usbTemp)),
            HeroStat(stringResource(R.string.hero_stat_board), temp(reading.boardTemp)),
        ),
        chips = listOfNotNull(modeLabel?.let { Icons.Default.Bolt to it }),
        checked = if (state?.installed == true) active else null,
        switchEnabled = state?.supported == true,
        onCheckedChange = { enabled -> ui.act { setBypass(enabled) } }
    )
}

/** Shows why the last change failed, with a button to copy it into a report. */
@Composable
fun ChargingErrorDialog(ui: ChargingUi) {
    val error = ui.error ?: return
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = ui.dismissError,
        icon = { Icon(Icons.Default.Error, contentDescription = null) },
        title = { Text(stringResource(R.string.toggle_failed)) },
        text = { Text(error, style = MaterialTheme.typography.bodySmall) },
        confirmButton = {
            TextButton(onClick = ui.dismissError) { Text(stringResource(android.R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = {
                context.getSystemService(ClipboardManager::class.java)
                    ?.setPrimaryClip(ClipData.newPlainText("charging", error))
                ui.dismissError()
            }) { Text(stringResource(android.R.string.copy)) }
        },
        shape = MaterialTheme.shapes.extraLarge
    )
}
