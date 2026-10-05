# Succubus sipping animation — v0.4

Mode: built-in `image_gen` (imagegen skill), generate, transparent background.
Original runtime asset (v0.4): `app/src/main/res/drawable-nodpi/succubus_sip_atlas.png`.
Archived source: `docs/art/succubus-sip-v0.4.png`. The current mode uses the USB-C revision
documented in `succubus-usb-sip.prompt.md`.
The selected 1143 × 1376 PNG is copied intact; `SuccubusAnimation.kt` registers four
runtime source rectangles and animates a bitmap mesh. It depicts an adult fantasy
character sipping battery energy through a straw.

## Final generation prompt

```text
Use case: stylized-concept
Asset type: transparent 2-by-2 animation sprite atlas for an Android battery-energy game.
Primary request: four consistent sequential poses of ONE unmistakably ADULT anime succubus woman (age 25+, mature face and adult proportions, five to six heads tall), playfully sipping glowing purple electricity from a small handheld battery via a short flexible magical drinking straw. The battery is a game prop, not a body part.
Subject: long violet hair, small black curved horns, small bat wings, heart-tip tail, confident teasing half-lidded eyes; fitted opaque plum corset dress with elegant sweetheart neckline, long gloves and thigh-high boots. Tasteful alluring fantasy fashion, all intimate parts fully covered, no nudity, no sexual activity, no childlike proportions.
Style: polished cute anime game illustration, clean dark outlines, rich plum and pink, soft cel shading, attractive expressive adult character. Consistent same costume, face, dimensions and prop across all four frames.
Composition: exact equal 2-by-2 grid, transparent gutters; each cell contains complete head-to-boots character, NO cropping, NO overlapping adjacent cells. Each character uses about 80% cell height and 65% cell width, same baseline, same scale and camera. Front three-quarter view with body facing slightly left. Character holds small glowing battery in left hand at waist height and gently touches straw to lips with right hand. Straw remains clearly visible leading only from battery to mouth.
Frame order: top-left relaxed smile with straw at lips, top-right gently sipping with slightly pursed lips and subtle cheeks, bottom-left eyes softly closed while sipping and wings raised a little, bottom-right satisfied wink and a little relaxed hand/wing motion, straw still at lips. Modest pose differences so loop reads as sipping, rather than four different characters.
Constraints: genuine transparent background, no floor, no shadows outside silhouettes, no letters, no panel borders, no checkerboard drawn in image, no numbers, no watermark, no extra characters, NO hearts or particles floating outside character (effects will be drawn by the app).
```
