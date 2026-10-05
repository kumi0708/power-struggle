# Chibi UI validation — 2026-10-05

## Alternate viewing mode — v0.4

The mode selector adds a self-running adult succubus sipping battery energy through a
straw. Four expression/sipping drawings combine with continuous head/hand, hair, wing and
breathing movement. Planted-foot pivots keep the poses aligned. Runtime source rectangles
include the full lower-left wing and exclude its tip from the adjacent lower-right pose.
The original 1143 × 1376 generated PNG is preserved, with alpha ranging from 0 to 255.

API 36 emulator checks with the Apple M1/Metal host GPU verified:

- The disconnected mode shows live battery readings and its own preview button.
- The labeled preview runs without input, keeps sample battery values stable, and has no
  tug button. Tapping the illustration does not perform a game tap.
- Switching to tug restores the original tap controls, and taps still change the advantage.
- Selecting succubus persists across process restarts. Android Back returns to tug and saves
  that choice. Exiting the preview restores the live battery display.
- The draining preview labels the opposite recipient; particles reverse accordingly.
- Both opposing halves show the selected mode. Either selector updates both halves.
- No fatal app exceptions were logged. Character and UI screenshots were visually checked.
- A native 720 × 1600 recording of approximately nine seconds contains uninterrupted
  animation without tapping (501 encoded frames, about 56 recorded frames per second).

`assembleDebug lintDebug --max-workers=2 --no-daemon` succeeded with no errors. The three
remaining lint warnings are the existing SDK/orientation warnings documented below.
The APK and recording are `output/PowerStruggle-succubus-v0.4-debug.apk` and
`output/PowerStruggle-succubus-v0.4.mp4`.

The new renderer and selector do not request USB power swaps or send game taps. Connected
visuals use the existing battery state. Actual USB-C transfer still needs two physical phones.

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
