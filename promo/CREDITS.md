# Promo film: footage credits

The v2 film uses stock video of real people from Mixkit. The clips are not stored in this repository; they are fetched
from the links below when the film is built (`footage/manifest.json`).

**The people shown are stock models.** They appear in stock footage licensed for reuse. They are not users of Poseit,
they did not review or endorse it, and nothing in the film speaks for them. The app's graphics drawn over them (the
skeleton, the lay figure, the colours, the cards and the score) show what the app's code computes for those frames.

## Licence

Every clip below is offered on its own page under the **Mixkit Stock Video Free License**
(https://mixkit.co/license/#videoFree), checked on 2026-11-21. As Mixkit states it: "Items under the Mixkit Stock
Video Free License can be used in your commercial and non-commercial projects, for free. You're permitted to download,
copy, modify, distribute, publicly perform and broadcast the Items. [...] Attribution is not required, however, we
would appreciate it if you credit Mixkit where reasonably possible." Use is also subject to the Mixkit User Terms
(https://mixkit.co/terms/). We credit Mixkit here.

No footage from Coverr, Pexels, Pixabay or Wikimedia Commons is used.

## Clips

| Used for | Clip (title as published) | Page |
|---|---|---|
| B2, the first real person | Woman posing for the camera in the middle of a desert (34404) | https://mixkit.co/free-stock-video/woman-posing-for-the-camera-in-the-middle-of-a-34404/ |
| B5-B7, the live coaching and the photo | Girl in a photographic set modeling on a pink background (39676) | https://mixkit.co/free-stock-video/girl-in-a-photographic-set-modeling-on-a-pink-background-39676/ |
| Chorus c1, c6, c8 | Urban trendy man posing with pink filter (1236) | https://mixkit.co/free-stock-video/urban-trendy-man-posing-with-pink-filter-1236/ |
| Chorus c2 | Confident Indian Model in Full Frame (101459) | https://mixkit.co/free-stock-video/confident-indian-model-in-full-frame-101459/ |
| Chorus c3 | Women taking photos on a balcony (3191) | https://mixkit.co/free-stock-video/women-taking-photos-on-a-balcony-3191/ |
| Chorus c4 (and the c9 fallback) | Many poses from a model who is in a photo studio (50649) | https://mixkit.co/free-stock-video/many-poses-from-a-model-who-is-in-a-photo-50649/ |
| Chorus c5 | Young female model posing in a photo studio (41665) | https://mixkit.co/free-stock-video/young-female-model-posing-in-a-photo-studio-41665/ |
| Chorus c7 | Female model posing captivatingly in a photoshoot (41664) | https://mixkit.co/free-stock-video/female-model-posing-captivatingly-in-a-photoshoot-41664/ |
| Chorus c9 | Happy Mature Couple Posing (100743) | https://mixkit.co/free-stock-video/happy-mature-couple-posing-100743/ |
| Chorus c9, alternate | Happy couple posing in green screen (100766) | https://mixkit.co/free-stock-video/happy-couple-posing-in-green-screen-100766/ |

If the build swaps a chorus shot for one of the manifest's alternates, this table is updated to the clips actually
used. The full catalogue the clips were chosen from, with every licence check, is `footage/catalogue.md`.

## How the footage is changed

Each clip is trimmed, cropped to 9:16, scaled, converted to 30 fps and printed in the app's grey palette; one held
moment is slowed to half speed. The app's pose model (MediaPipe pose landmarker, the model file bundled with the app)
is run on the frames to draw the overlays. The captured photo near the end is the only frame shown in the clip's own
colour. Clip audio is not used.
