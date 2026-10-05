# USB-C sipping animation — v0.5

Mode: built-in `image_gen` (imagegen skill), edit, transparent background.
Reference: archived `docs/art/succubus-sip-v0.4.png`.
Runtime asset: `app/src/main/res/drawable-nodpi/succubus_usb_sip_atlas.png`.
The accepted 1143 × 1376 RGBA PNG is copied intact, with alpha ranging from 0 to 255.
The costume is a covered purple dress. Four expressions show the adult fantasy character
sipping glowing electricity from a recognizable USB-C connector, with a thick USB cable.

`SuccubusAnimation.kt` registers the feet, free USB plug and mouth-side USB tip in each pose.
The phone's port follows the free plug under the same mesh deformation as the character.
The generated artwork is preserved; row source rectangles exclude neighboring fragments.

## Final accepted edit prompt

```text
Use case: precise-object-edit
Asset type: transparent 2-by-2 game-animation sprite sheet.
Reference: preserve the adult fantasy woman's face, violet hair, horns, bat wings, tail, boots and polished anime rendering from the reference. Make her costume MORE MODEST: a fully opaque high-neck purple dress with long sleeves, knee-length skirt, opaque leggings, boots. Fully clothed adult fantasy character.
Replace the battery jar and drinking straw with an unmistakable USB-C charging cable. In each pose, one hand holds the black housing of a small silver USB-C connector a little in front of her mouth. She gently sips a tiny stream of glowing purple ELECTRICITY emerging from the metal connector, with a small air gap between plug and lips. The mouth makes a small rounded sipping shape. It must read as a charging-energy game animation, with no sexual action, suggestive gesture or bodily fluids.
The same clearly visible thick dark-purple USB cable runs down from the connector; her other hand loosely guides the cable near her waist. The cable hangs in an open loop to the left, and terminates at a visible free cable tail in the lower-left of each frame, where the app will draw the phone connection. Do not draw a battery jar, bottle, straw or phone in the sprite.
Four frames in reading order: eyes-open light sip, a slightly stronger puff-cheek sip, eyes-closed peaceful sip, pleased wink while sipping. Same adult character, scale, pose registration and costume across all four. Keep the connector's silver rounded-rectangle USB-C tip and black plastic housing clearly recognizable.
Exact 2-by-2 grid, each complete head-to-boot character and the entire cable fits inside its own cell with 8% transparent padding on every side, no crossing into other cells. Transparent background, no floor, frame borders, text, checkerboard pattern or watermark. Only a small electrical glow at the USB connector; no floating particles elsewhere.
```
