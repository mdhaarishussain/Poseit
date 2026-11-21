# Poseit promo: storyboard v3 (clarity first)

Vertical 1080 x 1920, 30 fps, **816 frames (27.2 s)**, with sound, built in Remotion on the v2 project. Frame numbers
are global v3 frames at 30 fps, end exclusive. Positions are fractions of the frame (x right, y down); W = 1080 px,
H = 1920 px. All the numbers in this board are also data in `src/beats.ts` (v3 half) and `src/scenes/timing.ts` (v3
half); the footage plan is the `v3` block of `footage/manifest.json`; credits in `CREDITS.md`.

**Why v3.** The user ran the mom test on v2: "The problem is its still vague. I just did the mom test and she wasnt able
to make out the purpose of the app from the video". Two cold viewers who saw v2 as stills said the same (sheets in
`out/coldtest/r0/`): the parent guessed "a black-and-white fashion or perfume advert" for the first half and was 40%
sure what the app does; the scroller would have swiped during the silhouette montage and only understood at 11 s. v2
is well made but abstract. v3 keeps its real people, the app's real verdicts, the figure-on-person snaps, the colour
photo and the end, and makes every beat **say** something a non-technical viewer understands with the sound off.

**What a cold viewer must be able to say, sound off** (the acceptance test for this board):

| By | They can say | Where v3 says it |
|---|---|---|
| 3 s | "It's about posing for photos." | f0-96: a phone's camera on a woman standing stiffly, hands clasped; "Never know how to pose for photos?" (whole line up by 0.8 s) |
| 10 s | "It's a phone app that shows you how to pose and coaches you into it." | the Poseit icon on the phone (3.2 s); "Poseit shows you how." (3.3-5.2 s); the app's library, "Pick a pose to copy." (5.5-7.6 s); the pose page "Hands in pockets" (7.6-8.7 s); the camera opens (9.1 s), "Match the outline." (9.3-10.5 s); the app's card and "It tells you what to fix." (10.7-13.1 s) |
| the end | "When you hold the pose it takes the photo for you; it's called Poseit; it's free on Android." | "Hold still." / "It takes the photo for you." over 3-2-1 and the flash (13.3-16.9 s); "Here's your photo." (17.2-18.7 s); "99 poses for every kind of photo." (18.7-21.8 s); the icon, "Poseit", "Free for Android.", "Works offline.", "No account, no ads." (23.6-27.2 s) |

## 0. Diagnosis: why v2's purpose does not come across

Looked at the r0 sheets (every 0.5 s, `out/coldtest/r0/sheet-01..06.png`) next to the two viewers' reports.

1. **No phone and no app, ever.** 22 s without a device, a screen, a tap or a button. The live view fills the frame,
   so the app's skeleton, guide, ring and card read as video effects laid over footage ("a motion-capture demo", "video
   editing overlays"). The only phone in the film is a stock model's branded phone in c3, on screen for 0.5 s.
2. **Nothing explains anything in words.** The film was built to be carried by a voice-over, and the voice-over was
   never recorded (`vo.txt` is unvoiced). With the sound off the only words are the hook, one pose name, the app's own
   cards and the end card. Nobody says what the figures, the ring or the countdown are for.
3. **The hook does not say "photo".** "What do I do with my hands?" over a stick figure on black read as nerves, body
   language, interviews or public speaking (both viewers).
4. **The first 10 s are abstract and come before any explanation.** A skeleton on a model in a desert (0:02.5),
   ghost silhouettes flashing on strangers every 0.27-0.53 s (0:05-0:08.5, "a glitchy transition effect", the
   scroller's swipe point), 99 identical mannequins ("a bit creepy"). The montage of outlines is shown before the
   viewer has seen what an outline is for; the explanation (camera, guide, hint, score) only starts at 11 s.
5. **Name, platform and "app" arrive in the last second** (21 s): a feed viewer never gets there.
6. **Who is it for?** The hook is first person (the one posing); the app's card is "Ask THEM to bend their right
   elbow more" (the one holding the phone). The parent could not tell whether it was for posing or for taking photos.
7. **The score reads backwards.** An unlabelled number in a ring that falls from 77 to 69 during the countdown, so
   the success moment looks like it is getting worse. "Perfect. Hold it" is on screen for 9 frames.
8. **The payoff is not framed as a photo.** No phone, no shutter on a screen, no "here's your picture": the switch
   from grey to colour reads as a grade change ("it just looked like the video switched to colour").
9. **Seventeen seconds of grey, full-bleed fashion footage with studio lights** read as a perfume advert and as
   "something for photographers or models", not for normal people.
10. **"Works offline"** arrives with no context ("filler").

**What v3 does about each:** a drawn phone carries the film from f0 to f516 and shows the app's real screens (1, 5);
captions in plain words carry the story, one sentence at a time, and the future voice-over reads the same words (2);
the first caption says "pose for photos" over a phone camera aimed at a person (3); the order is the app's own order,
problem, app, pick, coach, shoot, photo, and the figure montage moves after the camera act, where it is captioned
"99 poses for every kind of photo." (4); the name and icon arrive at 3.2 s (5); everything is "you", the woman is
alone with a propped-up phone and the app's hints use the real "Hints are for: I'm posing" setting, so the card says
"Bend your right elbow more" (6); the countdown is shown on the whole phone with "Hold still." and "It takes the
photo for you." (7); the flash happens on the phone, the photo opens from the app's gallery thumbnail and "Here's
your photo." names it (8); the grey footage now lives inside a phone screen next to the app's grey paper UI, and the
captions put it in everyday words (9); "Works offline." sits next to "Free for Android." and "No account, no ads.", as
reassurance (10).

## 1. PAGE and the one true thing

- **Problem:** someone points a phone at you and you freeze: you don't know how to stand or what to do with your
  hands, and the photo comes out stiff.
- **Audience:** friends and family on a phone, in a WhatsApp chat or a feed, often muted, not technical (the user's
  mum is the test). About 27 s.
- **Goal:** they understand what it is and install it (Android, free). The sentence they repeat: "It's a free app
  that shows you how to pose, tells you what to fix and takes the photo for you."
- **Emotion:** from "that's me" to "oh, that's easy".

**The one true thing (unchanged):** on the camera screen a body lines up with the see-through figure, its limbs turn
green, the ring closes, the countdown runs out and the photo is taken with nobody touching the phone. In v3 the viewer
sees that happen on a phone.

## 2. Brand inventory (v3 changes to v2's table)

| Look at | v3 |
|---|---|
| Core control | Shown where it lives: on a phone screen, in the app's own chrome (library, pose page, camera). |
| Voice | Two voices, never mixed: **captions** are the app talking to the viewer in second person, plain words, sentence case ("Never know how to pose for photos?"); the app's **own strings** stay the app's (the library, "Hands in pockets", "Try this pose", "Bend your right elbow more", "Perfect. Hold it", "Saved to Pictures/Poseit"). No first person anywhere. |
| Device | A generic drawn phone (Ink body, thin bezel, round corners, a punch-hole camera); no brand, no buttons, no tilt, no shadow. |
| Material | Unchanged: Studio paper and Ink, the white lay figures, the footage printed in the Studio ramp until the shutter, colour after it. |

## 3. Ideas considered, and the pick

| | Kind | The idea in one sentence |
|---|---|---|
| **A. The app's own order, captioned** | UI documentary of one task with a problem in front and a montage behind | A phone films a woman who doesn't know how to stand; Poseit opens on the phone; you pick a pose, match the outline, it tells you what to fix, you hold and it shoots; here's your photo; 99 poses for every kind of photo; the figure becomes the icon. |
| B. Before and after | Split comparison | The same person's stiff photo beside the posed one, then how. Needs one person with both moments in free footage (none: our coached woman is never stiff for long enough), and two panels per frame (G4). |
| C. v2 plus captions | Patch | Keep v2's order and add captions. The order is the problem (diagnosis 4): captions over abstract beats explain abstractions. |
| D. Talking-head demo | Testimonial | A person shows the app to camera. Would present a stock model as a user; not allowed. |

**Pick: A.** It is the only one that shows the app as an app, in the order a person uses it, with one consistent
"you", and it keeps v2's strongest real parts (the live act on real footage driven by the app's code, the colour
photo, the figure snaps, the end) in places where they now explain themselves.

**Tests.** Cover the logo: the lay figures, the app's grey paper UI and the per-limb signal colours are still only
Poseit. Competitor swap: another pose app has no library of drawn figures that become the icon. Mum test: every beat
has a sentence a non-technical viewer can read. Honesty: every screen is the app's own UI; the people are stock models
and never speak.

## 4. Film shape and system

| Decision | Choice and reason |
|---|---|
| Shape | Problem, app, three steps, result, breadth, name: B1 problem, B2 Poseit, B3 pick, B4 match and fix, B5 hold and shoot, B6 your photo, B7 99 poses, B8 the name. |
| Length | 816 f, 27.2 s. Caption reading time sets it (0.4 s a word, at least 1.2 s each); the live act keeps v2's real verdict timing, moved 40 f earlier. |
| Carrier | **The phone** (f0-516): in the woman's room, on its own screens, in her studio, then the photo grows out of it. It changes framing at every beat (W1, W2, M, D1, D2, W3, C, W3, PHOTO), so it is never pinned. After the photo the white figure takes over (B7-B8), as in v2. |
| Captions | The story's voice: one sentence at a time, Archivo SemiBold 0.051 H (cap height 0.035 H, line 0.056 H) on a Sheet cue card, centred, in a top band (card top 0.135 H) or a bottom band (card bottom 0.865 H), never in the outer 12.5%, never over the HUD, never two at once. End lines: Ink on Paper. Full spec in section 9 and `CAPTION_SYSTEM`. |
| Clock | As v2: posed things step, live things are smooth. Footage at its own cadence (frame repetition to 30 fps); the app's HUD and the film camera smooth; the UI moves like the app (navigation slides, a scroll with a real fling curve). |
| Music grid | 112.5 BPM, beat 16 f, bars at f16 + 64n. Cuts on beats: f96, 160, 272, 560, 576, 592, 608, 624, 656, 816. The live act runs on the app's clock. |
| Type | Archivo only. Captions SemiBold 0.051 H; "Poseit" in C2 and the end wordmark in Archivo Expanded SemiBold; the app's UI at its own sizes, magnified by the camera when it must be read. |
| Colour | Grey until the shutter: the footage on the phone and in the room behind it is printed in the Studio ramp (Ink to Sheet), the app's UI is its own grey paper and ink; signal colours only for match feedback (the guide's limbs, the ring). **The photo is the first colour** (f480, on the phone, then full frame); the chorus after it is in its own colour; the end is Studio paper. |
| Camera (in the film) | One camera group per move: device, screen and everything on it scale together; captions and the room do not. The room behind the phone moves at 0.4x (depth). |
| Camera (the app's) | Back camera, not mirrored, guide "Hands in pockets", strictness 0.75 ("Balanced"), and the real quick setting "Hints are for: I'm posing" (AudienceMode.SUBJECT), so the hints talk to her: "Bend your right elbow more". |
| Overshoot | None. Every graphic lands decelerating; the touch disc squashes 2 f on a press. |
| Remotion rules | One theme, every curve eased and clamped, OffthreadVideo for every plate (muted), a slow push on every still, render and look at frames. |

## 5. Worlds per act

| Act | Frames | Ground | Light | Depth | Texture | Sound |
|---|---|---|---|---|---|---|
| I. The problem and the app | 0-160 | Her white photo studio (o1, mk-34412), the plate itself scaled 1.6x, blurred 28 px, printed grey, lifted 25% toward Sheet; the phone in front, its screen sharp. | The studio's soft white light. | Room far, phone near, caption frontmost. | Grain. | Room tone; a soft swell into the icon (hit 1, f96) and the groove's first kick. |
| II. The app | 160-272 | The same room behind; the app's grey paper UI on the screen (library, pose page). The room swaps to her studio while the magnified pose page covers the frame (f240). | Flat UI light; the room's light behind. | The room, the phone, the touch disc. | Grain. | The groove light; a soft scroll texture; a dry tap on each touch (f220, f260). |
| III. The camera | 272-472 | Her photo studio (b5-live, mk-39676) behind the phone, blurred and grey; on the screen the app's camera: the live view printed grey, the see-through guide in per-limb colour, her skeleton, the ring, the card, the countdown. | The studio's softbox. | Room, phone, the app's HUD on the screen, caption frontmost. | Grain. | The coaching bed 6 dB down; card tick f308; the lock chime (hit 2, f361); beeps f382, 412, 442; 8 f of silence; the shutter (hit 3, f472, the loudest sound). |
| IV. The photo | 472-560 | The photo in colour: on the phone, then filling the frame (pink paper, her clothes, the softbox). | The flash decays into it. | A still photo, a slow push. | Grain. | The warm chord blooms with the colour (f488). |
| V. 99 poses | 560-656 | Five real people in their own colour (c7, c2, c5, c6, c9), each with the library figure the app's matcher fitted to them. | Each clip's own light. | The figure on the person; the film camera's push across the cuts. | Footage grain, the film's grain. | The groove returns; a shutter-click per figure; the drop under c9 (f624-656). |
| VI. The name | 656-816 | Studio paper, one figure; the cove on the pull back. | v2's softbox key. | The figure (then the icon) and its contact shadow. | Paper tooth, grain. | A soft low re-strike on the cut; wooden ticks on twos for the relax; room tone; the chord rings out. |

**Picture sources.** o1 and b5-live and the chorus: Mixkit stock (manifest), b5-live and the chorus detected with the
app's own lite model. The phone's screens: the app's own Compose screens rendered by its Roborazzi tests at 1200 x
2580. The figures, the paper, the device and the touch disc: drawn in code from the Studio tokens and the 99 real poses.

## 6. Hero moment and energy

- **The screenshot frame: f380, the end of "Perfect. Hold it"** (v2's strongest still, f420, moved by -40): the
  phone pushed in (C), her hands in her pockets inside the all-green figure, the ring green with its hold arc at 0.91,
  "Perfect. Hold it" on the app's card, "It tells you what to fix." below. The hardest work: the app's matcher,
  stabiliser and coach frame by frame on the real footage, inside the app's real camera chrome on a phone.
- **The second: f220-240**, the finger on "Hands in pockets" in the real library and the pose page opening.
- **The emotional peak: f488-520**, the photo opening in colour on the phone and growing to fill the frame.

**Energy curve.** Quiet problem; the icon lands (hit 1, 12%); light UI rhythm; the coaching (quiet, the bed down); the
lock (hit 2, 44%); the pull back and the countdown build; 8 f of silence; the shutter (hit 3, 58%, the loudest) and
the colour; the groove returns for the five people; the drop under c9; smaller: the figure, the icon, the name, a
living hold.

## 7. Importance budget and beat table

Importance 3 = the point: B4 and B5 (2 of 8 beats, 25%).

| # | Frames | Length | Role | Imp. | Buys | Caption(s) | The app's own text on screen | Picture with the text covered |
|---|---|---|---|---|---|---|---|---|
| B1 | 0-96 | 3.2 s | hook | 2 | Buys recognition: the viewer sees someone freeze in front of a phone camera and reads that this is about posing for photos. | C1 "Never know how to pose for photos?" | none (a plain camera: a shutter ring) | A dark phone standing in a bright white studio; on its screen a woman stands stiffly, hands clasped, glancing about; a white shutter ring under her. |
| B2 | 96-160 | 2.1 s | turn | 2 | Buys the name: the viewer sees an app called Poseit open on that phone. | C2 "Poseit / shows you how." | the launcher icon | The phone's screen turns to grey paper and the icon (a white figure, hand on hip, on a grey disc) lands in the middle. |
| B3 | 160-272 | 3.7 s | proof | 2 | Buys step 1: the viewer watches someone pick a pose in the app. | C3 "Pick a pose to copy." | the library ("Poseit", "99 poses", "Hand on hip", "Hands in pockets"), the pose page ("Hands in pockets", "Try this pose") | The app's library, a grid of white figures on grey tiles; it scrolls; a fingertip disc taps the figure with its hands in its pockets; its page opens, magnified: the figure in frame marks, its name; the fingertip taps a black button. |
| B4 | 272-382 | 3.7 s | proof | **3** | Buys step 2: the viewer sees the outline on the camera and the app telling her what to fix. | C4 "Match the outline."; C5 "It tells you / what to fix." | "Bend your right elbow more", "Perfect. Hold it", the score | The phone in a real photo studio; on its screen a woman in grey, a see-through figure beside her coloured green with an amber arm, a skeleton on her, a number in a ring; the camera pushes in: a card above her head; she slides into the figure; every limb turns green. |
| B5 | 382-472 | 3.0 s | proof | **3** | Buys hands-free: the viewer sees a countdown on a phone nobody touches, then the flash. | C6 "Hold still."; C7 "It takes the photo / for you." | 3, 2, 1, the score | The camera pulls back to the whole phone; a big 3, 2, 1 on its screen; she holds still; a white flash on the screen. |
| B6 | 472-560 | 2.9 s | proof | 2 | Buys the result: the viewer sees the photo it took, saved, and calls it a photo. | C8 "Here's your photo." | "Saved to Pictures/Poseit" | A colour photo appears on the phone and grows until it fills the frame: the woman, hands in her pockets, on pink paper in a studio. |
| B7 | 560-656 | 3.2 s | proof | 1 | Buys breadth: the viewer sees poses for all kinds of people and photos. | C9 "99 poses for / every kind of photo." | none | Five people in colour (walking, sitting cross-legged, framing her face, a hand in a blazer pocket, an older man smiling with arms crossed), a white see-through figure landing on each in their pose, cut on the beat. |
| B8 | 656-816 | 5.3 s | cta | 1 | Buys the name and the offer: the viewer connects the figure with the icon and reads Poseit, free for Android. | C10 "Free for Android."; C11 "Works offline."; C12 "No account, no ads." | the wordmark "Poseit" | The last figure on grey paper, solid, relaxes into hand on hip; a grey disc grows behind it: the app icon; the camera pulls back; the name under it. |

## 8. Caption script

`CAPTIONS` in `src/beats.ts` is the source; `vo.txt` holds the same sentences, one per line, so a later voice-over
reads exactly these words (about 2.5 words a second, the captions' pace). Need = 12 f a word, at least 36 f.

| # | Text (line breaks as on screen) | Frames | On screen | Words | Need | Band | Beat | Over |
|---|---|---|---|---|---|---|---|---|
| C1 | Never know how / to pose for photos? | 0-96 | 96 f, 3.20 s | 7 | 84 f | top | B1 | the room above the phone; first word already rising on f0, whole line by f24 |
| C2 | **Poseit** / shows you how. | 100-156 | 56 f, 1.87 s | 4 | 48 f | top | B2 | the room; "Poseit" in the wordmark face; the icon lands on the screen at f96 |
| C3 | Pick a pose to copy. | 164-228 | 64 f, 2.13 s | 5 | 60 f | top | B3 | the room above the library (the app's own onboarding title) |
| C4 | Match the outline. | 278-314 | 36 f, 1.20 s | 3 | 36 f | top | B4 | her studio above the phone; the outline stands beside her |
| C5 | It tells you / what to fix. | 322-394 | 72 f, 2.40 s | 6 | 72 f | bottom | B4 | the app's bottom chrome (pose strip) with the phone pushed in; the app's card reads at the top |
| C6 | Hold still. | 398-434 | 36 f, 1.20 s | 2 | 36 f | top | B5 | her studio above the whole phone; the "3" and "2" on the screen |
| C7 | It takes the photo / for you. | 436-508 | 72 f, 2.40 s | 6 | 72 f | top | B5 | the same; the capture (f472) under it |
| C8 | Here's your photo. | 516-560 | 44 f, 1.47 s | 3 | 36 f | top | B6 | the photo's headroom (backdrop roll, softbox) once it fills the frame |
| C9 | 99 poses for / every kind of photo. | 562-654 | 92 f, 3.07 s | 7 | 84 f | bottom | B7 | the people's legs and the floor (faces are in the upper half) |
| C10 | Free for Android. | 724-816 | 92 f, 3.07 s | 3 | 36 f | end | B8 | Ink on Paper under the wordmark |
| C11 | Works offline. | 740-816 | 76 f, 2.53 s | 2 | 36 f | end | B8 | Ink on Paper |
| C12 | No account, no ads. | 752-816 | 64 f, 2.13 s | 4 | 48 f | end | B8 | Ink on Paper |

Widths at 0.051 H (98 px) Archivo SemiBold, measured: the widest lines are "Pick a pose to copy." 0.82 W, "No account,
no ads." 0.83 W, "every kind of photo." 0.82 W, "to pose for photos?" 0.81 W; every line fits the 0.82-0.83 W measure.
Text not read as a caption but planned for reading: "Hands in pockets" on the pose page (f236-250, magnified to at
least 0.035 H em); the app's card "Bend your right elbow more" (on the phone from f308, readable at C from f330 to
f372); "Perfect. Hold it" (f373-381, the app's 9 frames); the digits (0.06 H at W3).

Copy rules (DESIGN.md): sentence case, British spelling, no exclamation marks, no middle dots, no arrows, no emoji,
no ALL CAPS, numerals as the app writes them ("99 poses").

## 9. Beat by beat

### B1. The problem (f0-96), STORY

- **Phone:** framing W1 (screen 0.50 W wide, top 0.30 H, centred), drifting to 0.515 W with the top fixed. The film
  opens already moving.
- **Screen:** a plain camera (no brand, no Poseit chrome yet): o1's plate full width on the screen, a black band at
  the top (0.05 of the screen height) and at the bottom (0.14) with a white shutter ring (0.18 of the screen width).
  She stands stiffly, hands clasped, glancing to one side (source 0.0-3.2 s, real time). Nobody presses the shutter.
- **Room:** o1's plate behind the phone (ROOM_LOOK: 1.6x, 28 px blur, Studio ramp, 25% toward Sheet).
- **Caption:** C1, top band. Words rise 3 f apart from f-2 (the first already rising on f0).
- **Exit:** on the f96 beat the screen cuts to the app's launch screen. Phone, room and framing carry across.
- **Kind and honest:** she is a stock model standing for a camera, shown full length, never mocked, never captioned
  as speaking; the caption is a question to the viewer.

### B2. Poseit (f96-160), STORY

- **f96:** the screen cuts to the Android launch screen: Studio Paper with the launcher icon (`docs/images/icon.png`,
  0.40 of the screen width, centred), landing from 2.6x (softLand 8 f). The camera steps W1 -> W2 (0.56 W, softLand
  10 f), then drifts to 0.575 W.
- **Caption:** C2 "Poseit" (Archivo Expanded SemiBold) / "shows you how." from f100.
- **Exit (the seam to APP, f160):** the launch screen, icon settled, at W2's `to` rect, in o1's room. APP's B3 starts
  from exactly that frame.

### B3. Pick a pose (f160-272), APP

- **f160-172:** on the screen, the launch screen gives way to the library (the app's real library render). The camera
  pushes W2 -> M (screen 0.66 W, top 0.29 H, the screen running off the bottom of the frame), then drifts.
- **f176-214:** one scroll of the library with a real fling curve: the header (search, Today's pose, tabs, chips)
  rises and the first row, "Hand on hip" and **"Hands in pockets"** (the second tile in the real render), settles at
  the middle of the screen with "99 poses" above it and the next rows below.
- **f216-221:** the touch disc (PHONE.touch: an Ink disc at 22%, ring at 45%) parks on "Hands in pockets" for 4 f,
  squashes 2 f, and the tile answers a frame later (the app's ripple).
- **f222-232:** the app's navigation to the pose page for `stand_hands_in_pockets` (a real render of
  PoseDetailScreen for that pose). **f228-240:** the camera pushes in (D1) until the title "Hands in pockets" reads at
  an em of at least 0.035 H, with the figure in its frame marks above it. The screen covers the whole frame: **the
  room swaps here (f240)** from o1's white studio to her pink-paper studio.
- **f246-258:** the camera pulls back (D2) so "Try this pose" is in view (about 0.82 H). **f256-261:** the disc parks
  and taps it. **f262-272:** the app's navigation to the camera; **f264-284:** the camera pulls back to W3 and the
  room behind the phone is her studio.
- **Caption:** C3 over the library (f164-228). No caption on the pose page: its title is the words.
- **Answer to B1:** the opener's woman had her hands clasped; the pose picked is "Hands in pockets".

### B4. Match the outline, it tells you what to fix (f272-382), APP

- **Screen:** the app's camera chrome (real render: top bar, the pose strip with "Hands in pockets" selected, the
  shutter, the gallery thumbnail) around the live view: the b5-live plate at v3 frames (v2 frame + LIVE_OFFSET,
  -40), printed grey, with v2's overlay (the see-through guide in per-limb `forScore` colour, the live skeleton, the
  ring, the card) in the preview. Her shoes stay visible above the pose strip.
- **f272-296:** the camera is open but has not found her yet: plate frame 0 (source 0.0) held, the guide white and
  uncoloured, no skeleton, no ring (illustrative, declared). **f296:** the app finds her (LIVE_V3.found): the guide's
  limbs take their colours, the skeleton and the ring 65 appear.
- **f278-314:** C4 "Match the outline." (top band, W3). The outline stands beside her; she will slide into it.
- **f308:** the app's card "Bend your right elbow more" (SUBJECT phrasing after the rerun), small on the phone.
- **f312-330:** the camera pushes W3 -> C: the preview 0.70 W wide, its top near 0.035 H, the card's top at or
  below 0.125 H, her feet at or above 0.70 H. The card reads from f330 (target 0.035 H text, at least 0.030 H: v2's
  0.04 H card is 0.028 H at this scale, so the HUD gets a declared in-phone magnification of about 1.1-1.25x).
- **f322-394:** C5 "It tells you / what to fix." in the bottom band, over the app's pose strip.
- **f326-356:** her hair flick (real time), then hands into her pockets as she slides into the guide; the amber arm
  turns green as the score climbs.
- **f361 (hit 2):** the lock: the ring turns green and its hold arc runs; the film camera steps 1.5% (v2). Every
  limb green from f364. **f373-381:** "Perfect. Hold it" (the app's 9 frames).
- **Exit:** the "3" lands at f382.

### B5. Hold still, it takes the photo for you (f382-472), APP

- **f382-402:** on the "3" the camera pulls back C -> W3: the whole phone, standing in her studio, nobody near it.
- **Countdown:** 3 / 2 / 1 at f382 / 412 / 442 on the screen (the app's clock; v2's HUD layout, glyph about 0.06 H at
  W3), her real hold at 0.5x (v2's remap), the score in the ring (small at W3).
- **Captions:** C6 "Hold still." f398-434, then C7 "It takes the photo / for you." f436-508, top band.
- **f464-472:** nothing moves but the footage. **f472 (hit 3):** the app's capture flash on the preview (85% white,
  280 ms decay).

### B6. Here's your photo (f472-560), APP

- **f480:** the app's gallery thumbnail (bottom-left of the camera chrome) takes the photo with its white ring, and
  the app's snackbar "Saved to Pictures/Poseit" rises (real string, small).
- **f484-494:** the photo opens from the thumbnail to the full screen (the app's photo viewer chrome if rendered,
  else the photo alone), **in colour**: the capture frame (source 4.45 s) without guide, skeleton or HUD, as the app
  saves it. The first colour of the film.
- **f490-516:** the camera pushes into the photo (PHOTO framing) until it fills the frame and the device has left
  past the frame edges; then a slow push 1.00 -> 1.06 about her mid-hip to f560, still moving on the cut.
- **Caption:** C8 "Here's your photo." f516-560 over the photo's headroom.
- **Exit:** hard cut on the f560 beat.

### B7. 99 poses for every kind of photo (f560-656), STORY

- **Shots** (CHORUS_V3: v2 plates and `chorus.json` picks at v3 frame - offset, now in each clip's own colour):

| Shot | Frames | Who | The figure (the app's pick) |
|---|---|---|---|
| c7 | 560-576 | a woman backstage at a shoot, full length | Walk toward the camera (flipped) |
| c2 | 576-592 | a woman sitting cross-legged on a warm set | Cross-legged |
| c5 | 592-608 | a woman in a white dress, hands by her face | Face frame (flipped) |
| c6 | 608-624 | a man against a magenta wall | Blazer pocket |
| c9 | 624-656 | an older man smiling, arms crossed (the couple clip, re-cropped in v2 so his partner's face is out of frame) | Crossed-arm smile (head anchor) |

- **What moves:** v2's landing on every cut (the veil from 1.25x, softLand 5 f, settling 0.70 -> 0.34, the white edge
  a frame later from 1.32x), one shutter-click per landing. The film camera pushes 1.00 -> 1.06 across c7-c6 about
  each person's mid-hip, carrying the photo's push across f560, then drifts 0.3% on c9 (the drop).
- **Check:** c7, c5 and c6 are now 16 f (v2 showed 8): their plates have 23-29 f after the landing; look at the figure
  for the extra frames (the app's hold score covered only v2's 8). Re-check the veil reads on colour footage.
- **Caption:** C9 in the bottom band.
- **Never "Hands in pockets" here** (it is the pick in B3; the manifest's snap rule).

### B8. The name (f656-816), STORY

- **f656:** hard cut on the beat to Studio paper: a solid lay figure in "Crossed-arm smile", exactly where c9's guide
  was on f655 (half body, legs past the bottom edge), with its contact shadow; the chorus push carried across and
  settling (f656-672).
- **f672-684:** on twos it relaxes into "Hand on hip" (`stand_hand_on_hip`, the icon's pose).
- **f684-700:** the app icon forms around it: a Set-grey disc grows behind the figure from its centre (softLand) and
  its Ink contour thickens to the icon's weight, so the figure on paper becomes `docs/images/icon.png`.
- **f688-712:** the camera pulls back; the icon settles (about 0.30 W, centred near 0.33 H).
- **f708:** the wordmark "Poseit" (Archivo Expanded, about 0.50 W) lands under it (v2's lockup motion).
- **f724 / 740 / 752:** C10 "Free for Android.", C11 "Works offline.", C12 "No account, no ads." resolve a word every
  3 f, Ink on Paper, 0.051 H, centred, stacked under the wordmark (about 0.60-0.77 H).
- **f768-816:** a living hold (slow drift), so the static end hold measures 0.6-1.4 s.
- **No QR code:** the film is watched on the phone the QR would have to scan; the link travels in the message.

## 10. Seams

| Seam | Frame | Last frame of A | First frame of B | Carried | Device |
|---|---|---|---|---|---|
| B1 -> B2 | 96 | The plain camera on her, phone at W1 | The launch screen, icon landing from 2.6x; the camera stepping to W2 | Phone, room, framing | Screen cut on the beat |
| B2 -> B3 (STORY -> APP) | 160 | Launch screen, icon settled, W2 `to` | The same frame; the library takes the screen | Phone, room, framing (PHONE_FRAMING.W2.to = M's start of move) | In-screen transition + push |
| library -> pose page | 221 | The tile answering the tap | The app's navigation to the pose page | The figure | The tap causes it |
| B3 -> B4 | 262-272 | "Try this pose" pressed | The camera screen, the guide in the same pose | The pose (page figure -> guide) | The tap causes it; pull back to W3 |
| W3 -> C | 312-330 | Whole phone | Phone pushed in, card readable | Everything on the screen | One camera group |
| C -> W3 | 382-402 | The lock, "Perfect. Hold it" | The "3", the whole phone | Everything on the screen | The digit causes it |
| B5 -> B6 | 472 | 1 fading, stillness | The app's flash | Her | The app's capture |
| photo -> full frame | 490-516 | The photo on the phone | The photo filling the frame | The photo | One camera group, the device exits |
| B6 -> B7 | 560 | The photo, push still moving | c7 with its figure landing | The push's velocity | Hard cut on the beat |
| B7 -> B8 | 656 | c9 with his figure | The solid figure on paper at the same frame | The figure | Carried object (v2's device), colour -> paper |

## 11. Layout

**Phone framings** (`PHONE_FRAMING`; the screen rectangle; screen height = 1.209 x screen width in these units).
STORY follows W1 and W2 exactly; APP may refine M to PHOTO within the constraints.

| Framing | Frames | Screen cx, top, w | Constraint |
|---|---|---|---|
| W1 | 0-96 | 0.50, 0.30 H, 0.50 -> 0.515 W | caption band above it clear; the shutter ring above 0.875 H |
| W2 | 96-160 | 0.50, 0.29 H, 0.56 -> 0.575 W | the icon about 0.22 W |
| M | 160-226 | 0.50, 0.29 H, 0.66 -> 0.68 W | the screen runs off the bottom; the tapped tile in the frame's middle third |
| D1 | 228-246 | about 0.50, -0.38 H, 1.45 W | title em >= 0.035 H for >= 14 f; the screen covers the frame (the room swaps) |
| D2 | 246-264 | 0.50, 0.08 H, 0.66 W | "Try this pose" visible (about 0.82 H) |
| W3 | 264-312 and 382-490 | 0.50, 0.30 H, 0.48 -> 0.49 W | the countdown glyph about 0.06 H; caption band above it clear |
| C | 312-382 | 0.50, about -0.01 H, 0.70 W | preview top near 0.035 H; the card's top >= 0.125 H and its text >= 0.030 H (target 0.035, by the HUD's in-phone magnification, declared); her feet at or above 0.70 H; the two-line bottom caption (0.721-0.865 H) over the app's chrome, not over her |
| PHOTO | 490-516 | to about 0.50, -0.10 H, 1.08 W | the photo (the preview area) covers the whole frame by f516 |

**Caption bands.** Top: the card's top at 0.135 H (a two-line card ends at 0.279 H). Bottom: the card's bottom at
0.865 H (a two-line card starts at 0.721 H). Card: Sheet, padding 0.045 W by 0.016 H, radius 0.028 W.
Nothing typed in the outer 12.5% at the top or bottom. In the wide framings the phone's top is at 0.29-0.30 H, below
the top band; in C the HUD owns the top and the caption takes the bottom.

**The HUD on the phone.** v2's composition (the plate, guide, skeleton, ring at (0.845, 0.58), card at 0.13, countdown
at 0.215 of the preview) scaled into the preview, then magnified by the camera. The app's chrome stays at the app's
proportions. One card and one digit at a time, as in v2.

## 12. Gates planned here

- **G1 proof readable:** captions cap 0.035 H; the pose title at least 0.035 H em at D1; the app's card at least
  0.030 H at C (target 0.035); the digits about 0.06 H at W3; the library is read as a picture (tiles of figures) plus
  the caption, its labels are not asked to be read. By eye on the sheet.
- **G2 no empty frame:** no flat run over 3 frames (the launch screen holds 54 f with the icon settled and the camera
  drifting; the held plate frame f272-296 is inside a moving pull back).
- **G3 end card short:** final shot f656-816 = 160 f = 19.6% of the film; static end hold 0.6-1.4 s (a slow drift
  after f768).
- **G4 one hero per frame:** the phone with its screen (the room is out of focus, captions are words not panels);
  the photo; one person with their figure; the figure, then the lockup.
- **G5 no page chrome:** no corner labels, counters, timecodes, progress bars or kickers; captions are centred
  sentences on one card, never pinned (they change band with the framing and are absent in D1 and B8's lockup until
  the end lines). By eye (lint.mjs reads HTML films).
- **G0** on the render with `look.py --expect 27.2 --expect-audio`.
- **The cold test again:** stills every 0.5 s to sheets, the same two context-free viewers, against the table at the
  top. v3 passes when both answer all three rows without guessing.

## 13. Build split

Two builders work in parallel in the same tree. Ownership is by file; shared files take small, local edits, and each
section of a shared file has one owner. Every scene receives the GLOBAL v3 frame, as in v2.

**BUILDER APP: the phone, the app's screens, the camera act, the HUD and the live pipeline (B3-B6, f160-560).**
- Owns (new): `src/phone/Device.tsx` (the device from `PHONE`: `<Device rect={ScreenRect}>` with the screen content
  laid out in a 1200 x 2580 box), `src/phone/framing.ts` (`screenRectAt(f)` from `PHONE_FRAMING`), `src/phone/Room.tsx`
  (the blurred plate behind the phone from `ROOMS_V3` and `ROOM_LOOK`), `src/phone/screens/*` (the plain camera for
  B1, the launch screen with the icon for B2, the library, the pose page, the camera chrome, the photo viewer, the
  touch disc), `src/scenes/PhoneDemo.tsx` and `src/scenes/demo/*`.
- Owns (reworked): `src/scenes/Viewfinder.tsx` (the live view and HUD rendered into the phone's preview at v3 frames),
  `src/scenes/viewfinder/*` (`match.ts`: LIVE_OFFSET or the rebuilt v3 window), `src/ui/Hud.tsx` (in-phone scale).
- Owns (app and scripts): new `app/src/test/java/com/poseit/app/promo/PromoScreens.kt` (Roborazzi renders at 1200 x
  2580 of LibraryScreen, PoseDetailScreen for `stand_hands_in_pockets`, the camera chrome with "Hands in pockets"
  selected and an empty preview, the photo viewer chrome, the launch icon), `app/src/test/java/com/poseit/app/promo/PromoFootageMatch.kt`
  (read `camera.audience` from the manifest instead of hard-coding PHOTOGRAPHER), `scripts/assets.sh` (copy the new
  renders into `public/screens/`, gitignored), `scripts/check-live.ts` (SUBJECT, `CAPTURE_WINDOW_V3`, assert
  `LIVE_V3` = the verdicts), `scripts/footage-match.mjs` if the rerun needs it.
- Owns in shared files: `footage/manifest.json` `b5-live` and its line in the `v3` block (set `camera.audience` to
  SUBJECT, optionally move `film` and the remap keys by -40 and rerun `npm run footage -- --only b5-live`,
  `npm run landmarks -- --only b5-live`, `npm run footage-match`); `public/footage/b5-live*.json`; `beats.ts`
  `LIVE_V3`, `CAPTURE_WINDOW_V3` and `PHONE_FRAMING` from M on; `timing.ts` `DEMO_V3`; `theme.ts` a `phone` section if
  needed.
- **Lands first, in its own commit:** `Device`, `framing.ts`, `Room`, the plain camera and the launch screen, so STORY
  can build B1-B2 on them (STORY stubs them from `PHONE` until then).
- Acceptance: the capture in `CAPTURE_WINDOW_V3`; the card text "Bend your right elbow more"; every screen is a render
  of the app's own Compose code; the C framing constraints; `npm run check` passes.

**BUILDER STORY: the problem, the captions, the people, the end, assembly and sound (B1-B2 f0-160, B7-B8 f560-816).**
- Owns (new): `src/captions/Captions.tsx` and `src/captions/layout.ts` (the caption layer from `CAPTIONS` and
  `CAPTION_SYSTEM`, frontmost over the whole film), `src/scenes/Opening.tsx` (B1-B2: Room, Device, the plain camera
  with the o1 plate, the launch screen and the icon landing; APP's components used read-only).
- Owns (reworked): `src/scenes/People.tsx` and `src/scenes/people/*` (`CHORUS_V3`, colour, offsets),
  `src/scenes/TheName.tsx` (from c9's figure, the icon forming, the wordmark, the end lines), `src/scenes/studio/*`
  as TheName needs it.
- Owns (assembly and sound): `src/Promo.tsx` (`ACTS_V3` + the caption layer), `src/Root.tsx` (816 f), `src/events.ts`
  (v3 cues), `scripts/gen-audio.mjs`, `src/audio/voiceover.ts` (VO_LINES = the CAPTIONS windows, vo.txt order),
  `sound-plan.md` ("v3 as built"), `README.md`.
- Owns in shared files: `footage/manifest.json` `o1` and the chorus lines of the `v3` block; `beats.ts` `CAPTIONS`,
  `CAPTION_SYSTEM`, `CHORUS_V3`, `PHONE_FRAMING` W1-W2; `timing.ts` `OPENING_V3`, `CHORUS_V3_TIMING`, `END_V3`;
  `theme.ts` a `caption` section.
- Retires v2 last, after APP's demo renders: `src/scenes/Hook.tsx`, `src/scenes/StudioShot.tsx` (keep as Dev
  compositions or delete), the v2 exports in `beats.ts` and `timing.ts`, `CAPTURE_WINDOW` (with APP's check-live
  switch).

**Shared files** (small local edits, owner per section as above): `src/beats.ts`, `src/scenes/timing.ts`,
`src/theme.ts`, `footage/manifest.json`, `public/footage/plates.json` (written by `fetch-footage.mjs`; whoever runs it
commits it), `NOTE.md` (each appends its own section), `CREDITS.md` (whoever changes which clips are used),
`src/scenes/shots.ts` (v2's seams; v3 adds none across builders: the only cross-builder seam is f160, defined by
`PHONE_FRAMING` and the launch screen). Read-only for both: `src/footage/*`, `docs/DESIGN.md`, the app's code outside
the promo tests.

**Rules for both:** a caption window moves only if a verdict frame moves (and then with it); never a caption over a
face; never a person's words; the app's strings exactly as the app writes them; no phone brand; no video or audio
renders committed; `npm run check` passes at every commit.

## 14. Real, rebuilt, illustrative (v3, to record in NOTE.md)

- **Real:** the footage (Mixkit stock models, who never used the app); the app's screens (its own Compose code
  rendered by its Roborazzi tests); every skeleton, guide colour, score, card, hold, countdown and capture frame in
  the camera act (the app's model, PoseMatcher, HintStabilizer and CaptureCoach on the plate, with the real "Hints are
  for: I'm posing" setting); the chorus poses (the app's matcher); the 99 poses, the figures, the icon, the strings,
  Archivo.
- **Rebuilt:** the HUD (v2, magnified), the live view's place inside the camera chrome, the navigation and scroll
  motion between screens (close to the app's, not captured).
- **Illustrative:** the phone (a generic drawn device), the touch disc (a finger), the blurred rooms behind the phone,
  the grey print until the shutter, the held first plate frame before the app finds her (f272-296), the photo opening
  from the thumbnail with the capture frame as the photo, the 0.5x stretch of her hold (v2), the figure becoming the
  icon, and the captions (the film's voice, not the app's UI).

## 15. Left out on purpose

- **The stick-figure hook and the desert skeleton (v2 B1-B2):** abstract, and the hook never said "photo".
- **The 99-figure studio (v2 B4):** "a bit creepy" without context; the app's own library on the phone shows the 99
  poses, and C9 says "99 poses".
- **v2's chorus before the case:** moved after it (B7), five shots instead of nine, captioned.
- **c3 (the balcony selfie):** a recognisable branded phone. **c1, c8:** the same man three times. **c4:** a second
  crossed-arm pose. **b2-alive:** no longer needed.
- **The friend-photographer case ("Ask them to ..."):** one "you" per film; the app's Auto setting does it on the back
  camera, and the film uses the real "I'm posing" setting instead.
- **A stock phone in a real hand:** every free clip with a phone shows a branded phone and another camera app (the
  manifest's notUsed); the drawn phone stands in a real scene instead.
- **A label on the score ring:** the app has none; the green limbs and "Perfect. Hold it" carry it, and the countdown
  is shown at W3 where the ring is small.
- **The QR code:** watched on the phone it would have to scan.
- **Feature tour** (pose from a photo, the editor, photoshoot mode, voice coaching, settings): one problem, one pose,
  one photo.

## 16. Data and assets for the build

- **Footage:** `npm run footage` (now also fetches o1, mk-34412, 1920 x 1080, crop x 660 w 608: the plate is in
  `public/footage/plates.json`). o1 needs no detection. b5-live: the SUBJECT rerun (section 13). The chorus: v2's
  plates and `chorus.json`, unchanged.
- **App screens:** `PromoScreens.kt` renders (section 13), copied by `npm run assets`. The existing renders in
  `public/screens/` (library, detail of "Hand on hip", camera-matched) are the fallback for layout work only: the pose
  page must be "Hands in pockets".
- **Icon:** `docs/images/icon.png` (256 px) for the launch screen; B8 draws the icon from the figure and the Set disc
  so it can form, matching icon.png at the end.
- **Fonts:** Archivo SemiBold and Archivo Expanded SemiBold in `public/fonts/` (already there).
- **Sound:** `npm run audio` from `src/events.ts`; the v3 cues are in `sound-plan.md` ("v3 deltas").

The v2 board this replaces is in git history (commit 1bdc824 and its review notes in NOTE.md).
