# Tug animation assets

Generated with the built-in image_gen tool, using `power-tug-girls-v1.png` as the approved identity reference.

Runtime assets:
- `app/src/main/res/drawable-nodpi/tug_emotes_atlas.png`: blue pull poses and both characters' idle/blink/emotion poses.
- `app/src/main/res/drawable-nodpi/tug_pink_pull_atlas.png`: four pink pull poses.

Source rectangles, planted-foot pivots and hand grips are registered in `TugAnimation.kt`. Sprites are cropped in memory at runtime; the generated PNGs are preserved intact. Both PNGs have genuine alpha transparency. The pink pull cycle uses the dedicated four-pose atlas for consistent orientation.

## Character and emote atlas

Use case: identity-preserve.
Asset type: production 2D character animation sprite atlas for an Android tug-of-war game.
Input image: the reference shows the TWO APPROVED characters. Preserve their exact identity and appealing rendering: brunette with blue eyes and blue hair bow, white blue-trim battery-emblem sporty shirt, navy shorts and blue sneakers; blonde with pink eyes and pink bow, white pink-trim battery-emblem sporty shirt, pink shorts and pink sneakers. Keep their approximately two-head-tall chibi proportions and glossy fluffy hairstyles.

Create ONE SQUARE PNG sprite sheet on a truly transparent background, a strictly regular 4-column by 4-row grid of 16 equal square cells. No visible grid, no lettering, no borders. Each cell contains exactly ONE full-body girl, same character scale and fixed baseline, with transparent padding on every side. Feet always at approximately 92% of the cell height and hair top around 7%. Characters never cross cell boundaries. All sprites face three-quarter toward the rope and have legs firmly apart. Brunette faces RIGHT in the first two rows; blonde faces LEFT in the last two rows. Girls grip an invisible horizontal rope with BOTH hands in front of the waist; hands should be adjacent and clearly gripping, at approximately 62% of cell height. Show only a tiny short white rope grip between the fists if needed; NO long rope, NO USB plug, NO sparks, NO props outside the character. The connecting rope will be drawn by the game. These are real animation keyframes: CHANGE elbow bending, torso posture, knees, hairstyle sway and facial expression; do NOT simply translate the same drawing.

Cell assignment, left-to-right in each row:
Row 1 (BRUNETTE): frame 0 ready stance with open eyes; frame 1 same ready stance with eyes closed in a cute blink; frame 2 anticipation with arms more extended and knees braced; frame 3 active pull with bent elbows, torso leaning backward, flowing hair trailing a little.
Row 2 (BRUNETTE): frame 4 strongest pull with deeper knees and determined happy face, ribbons/hair swinging; frame 5 recovery returning toward the ready stance, knees straightening and hair settling; frame 6 winning pull with a delighted smile and bright open eyes; frame 7 losing strain, body pulled a little toward the rope, worried but cute expression and one small sweat drop.
Row 3 (BLONDE): same ordered frames 0,1,2,3, naturally facing LEFT.
Row 4 (BLONDE): same ordered frames 4,5,6,7, naturally facing LEFT.

Very important animation alignment: constant feet baseline, consistent head size and body volume, outfits unchanged. Pose changes are moderate and physically coherent so frames 2→3→4→5→2 make a smooth looping tugging motion. Preserve the reference's beautiful cute anime illustration quality and facial detail. Transparent background with alpha, clean cutout edges, no background glow or scene, no text or watermark. High resolution square sheet, ideally 2048x2048.

## Atlas layout refinement

Use case: precise-object-edit.
Image 1 is the animation atlas edit target. Image 2 is the approved original identity reference.
Fix ONLY the atlas layout so it can be sliced reliably in software. Preserve the same 16 poses, two character identities, facial expressions and costume details from image 1, with the original approved face quality of image 2. Keep the 4 columns × 4 rows and the row ordering (brunette in rows 1-2, blonde in rows 3-4).

CRITICAL: each of the 16 sprites must fit entirely INSIDE its own equal square cell with AT LEAST 12% cell-width transparent margin on EVERY side. Shrink every character uniformly to approximately 75% of the cell height. Hair, bows, hands and shoes must never reach or cross a cell boundary. The sprite sheet needs clear transparent gutters. All feet share a baseline at EXACTLY 86% of each cell's height; head tops about 12%. Maintain the same head size and feet height across all frames, including the deeply crouched pose (its head can move down naturally but feet remain planted). These are individual separate cutout sprites for runtime source-rectangle cropping. Do not add grid lines, labels, panels, background effects, or a backdrop. Actual transparent background with alpha. Deliver a square 4×4 atlas, ideally 2048×2048. Preserve the genuinely different arm/knee/torso/hair poses and the blink, smile and losing-strain expressions. No long rope and no USB plugs.

## Pink character pull cycle

Use case: identity-preserve.
Make a clean 2-by-2 animation sprite sheet containing FOUR full-body cutout drawings of ONLY the blonde girl with the pink ribbon from the approved reference image. Preserve her exact adorable face, pink eyes, blonde fluffy hair, large pink bow, white pink-trim battery-emblem top, pink shorts and pink sneakers. Same two-head-tall chibi proportions and beautiful detailed kawaii anime painting.
All FOUR poses MUST FACE LEFT and lean backward toward the RIGHT when pulling. BOTH clenched hands grip an imaginary horizontal rope on the LEFT side of the body, never on the right. Keep the hand grip level near the waist. No long rope, no plugs, no electricity effects: the app draws the rope dynamically.
Layout: SQUARE sheet, exactly 2 columns × 2 rows. Four equally sized separate sprites, one centered per quadrant, 15% transparent padding on ALL FOUR sides of EVERY sprite. No overlap and no visible grids. Constant head size and shoe baseline within each quadrant.
Frames left-to-right, top-to-bottom:
0: anticipation, arms reaching toward the left, knees slightly bent;
1: active pull, elbows bend, torso leans toward the right, hair and pink bow trail toward the left;
2: strongest pull, knees bend more deeply with feet planted, hands pulled closer toward the left waist, determined cute smile;
3: recovery, torso rises slightly, arms release partway back toward the left, hair/bow settle.
This is a continuous frame animation cycle 0→1→2→3→0: clear moderate changes of knees, elbows, torso posture and swinging hair, not translations of one unchanged pose. Every character faces LEFT, never mirror or turn her around. Genuine alpha transparency, no background, no labels, no watermarks, crisp edges.

