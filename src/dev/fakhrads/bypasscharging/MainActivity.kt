/*
 * SPDX-FileCopyrightText: 2026 Fakhri Adi Saputra
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.fakhrads.bypasscharging

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dev.fakhrads.bypasscharging.ui.charging.ChargingErrorDialog
import dev.fakhrads.bypasscharging.ui.charging.chargingItems
import dev.fakhrads.bypasscharging.ui.charging.rememberChargingUi
import dev.fakhrads.bypasscharging.ui.components.SubScaffold
import dev.fakhrads.bypasscharging.ui.theme.ChargingTheme

/**
 * The settings page, opened from Settings > Battery and from a long press on the tile: bypass
 * on/off and its mode, then cool charging and the fast charging limit, with a live reading of
 * the power going into the battery. ZeroEngine's charging page shows the same.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChargingTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
                    val context = LocalContext.current
                    val ui = rememberChargingUi(remember { LocalChargingControl(context) })
                    SubScaffold(title = stringResource(R.string.app_name), onBack = ::finish) {
                        chargingItems(ui)
                    }
                    ChargingErrorDialog(ui)
                }
            }
        }
    }
}
