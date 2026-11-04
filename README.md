# Bypass charging

Quick Settings tile for Evolution X on the POCO F7 (onyx): while the charger is plugged in, the battery stops
charging and the charger powers the phone. No root.

Two modes, chosen on the page that opens with a long press on the tile:

- **Smart**: charge up to 80%, then run from the charger; charging resumes at 77%.
- **Bypass**: hold the battery at the level it had when the tile was turned on. The battery only tops up again
  once it is 3% lower (when the phone draws more than the charger gives), at any level.

Bypass turns itself off after every reboot (`BootReceiver`, on `LOCKED_BOOT_COMPLETED`).

## Cool charging

On by default (switch under Fast charging). The charger driver's JEITA tables keep full current up to 48 °C,
so a phone charging at 90 W reaches about 47 °C before it slows down. Cool charging steps the battery current
down instead, with 0.5 °C of hysteresis:

| battery | limit |
|---|---|
| below 40 °C | none |
| 40 °C | 10 A |
| 41 °C | 7 A |
| 42 °C | 4.5 A |
| 43 °C and up | 2.5 A |

It is its own voter (`coolcharging`) on the MCA `ichg_limit` node, next to the power limit (`bypasscharging`).
The driver takes the lowest vote, so neither can make charging faster than the other or than the thermal limits.
The app's process is persistent and follows `ACTION_BATTERY_CHANGED`; the vote is dropped on unplug.

## How it works

The app votes on the MCA charge interface, `charge_enable`: `bypasscharging all 0` stops charging the battery
and `bypasscharging all 1` allows it again. The charge enable votables (buck, quick, wireless) are an AND of every
voter, so the vote only ever stops charging, and it changes nothing else: the charger keeps its input current
limit and the adapter its voltage, so the phone runs straight from USB and the battery current stays near zero.
`BypassHold` decides the vote from the held level in the persistent process, on `ACTION_BATTERY_CHANGED`.

Lineage charging control is not used. On onyx its HAL stops charging through the MCA `smart_chg` SOC limit
(`65553`: `endurance_pro` with `soc_limit` 1), and the buck strategy then also asks the adapter for 5 V and limits
the input to 500 mA (1000 mA for some batteries), so a game draws the rest from the battery. Versions that used it
saved the charging control settings; they are put back once.

The node is labeled `vendor_sysfs_charge_limit` like `ichg_limit` and made writable by `init.onyx.rc`, in the
device tree.

## Build

In the device tree:

```
PRODUCT_PACKAGES += BypassCharging
```

Platform-signed, installed to `system_ext`. Needs the MCA `charge_interface` label and permissions from
`fakhrads/android_device_xiaomi_onyx` (branch `evo-cnb`): `sepolicy/vendor/genfs_contexts`, `platform_app.te`
and `init.onyx.rc`.
