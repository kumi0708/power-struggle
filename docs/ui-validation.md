# Chibi UI validation — 2026-10-05

## Animation update — v0.3

The scene now uses separately registered character sprites: four pull poses for each girl,
idle/blink poses, and smiling/straining expressions. A continuous 8 × 12 bitmap mesh adds
torso/knee movement and trailing hair/ribbon motion. Shoe pivots stay on the ground, and the
rope endpoints use the same deformation as the characters' hand grips. Tap events speed up
the local pull; tap-sized changes received through the existing rope state animate the opponent.

Validation was repeated on API 36 with the emulator's Apple M1/Metal host GPU:

- `assembleDebug lintDebug --max-workers=2 --no-daemon` succeeded; no lint errors.
- Local preview taps change the advantage and trigger the new character animation.
- Returning to the disconnected screen restores live battery values and idle animation.
- Both split-screen players can reverse the same rope with their own controls.
- A 720 × 1600 MP4 of animated play was recorded and its sampled poses were visually checked.
- A six-second idle recording contained 187 distinct frames in each girl's head region,
  confirming continued animation without taps. Blink and expression poses are separate drawings.
- No fatal app exceptions were logged during these tests.

The local deliverables are `output/PowerStruggle-animated-v0.3-debug.apk` and
`output/PowerStruggle-animation-v0.3.mp4`. Device performance and actual USB power transfer
still need verification on physical phones.

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
