# Chibi UI validation — 2026-10-05

## Build

`./gradlew assembleDebug lintDebug --max-workers=2` succeeded with JDK 17 and the Android SDK.
Lint reported no errors. Its three warnings concern the upstream target SDK and fixed portrait
orientation (`OldTargetApi`, `LockedOrientationActivity`, and `DiscouragedApi`).

## Android emulator

Tested on Android API 36 at 1080 × 2400:

- The disconnected screen shows the approved transparent character art, the real local battery
  level, an unknown opponent level, connection instructions, and the demo button.
- Entering the demo shows the explicit no-power-transfer/sample-battery label.
- Repeated taps change the preview from a losing position to the player's advantage and move
  the rope marker.
- Exiting the demo restores the disconnected screen and the live battery display.
- Split-screen preview shows two controls. Tapping the upper control pulls toward the upper
  player; tapping the lower control reverses the same rope toward the lower player.
- Screenshots were visually checked for readable text, visible character feet/hands, and no
  overlaps. The pink player's illustration is mirrored to align with the local tug meter.
- No app fatal exceptions appeared in AndroidRuntime logs during these checks.

`chibi-waiting.png` and `chibi-demo.png` use the debug-only face-up capture option.
`chibi-split.png` retains the opposing orientations used for face-to-face play.

## Physical-device validation still needed

USB communication, Shizuku permissions, actual battery-current readings, and USB power-role
swaps were not tested with two physical phones. The upstream game/control code is unchanged;
the UI preview never calls its tap or swap methods.
