# Sample photos for the pose library

Every one of the 99 library poses is drawn as a white mannequin figure. This adds a real photo of an adult demonstrating
the pose, to show next to the figure. There is no image-generation service in the environment that prepared them, so
the photos are stills and photos from free sources whose terms allow use inside a free app. This file records the
sources, what their terms say (quoted, read on 2026-11-21), the people rules, how the photos were found and checked
with the app's own matcher, which poses have a photo, and what the remaining gaps need.

**Status (2026-11-22, after the review and a second look): 35 of 99 poses have a photo** (27 Mixkit video stills
from 27 clips, 8 Wikimedia Commons photos: 6 CC0, 2 CC BY 2.0). 64 poses have none yet, and poses people make
themselves never have one. The app reads the credits (`PosePhotos`), has the photo and credit components
(`PosePhoto`, `PhotoCredit`, `PhotoCreditSheet`) and lists every photo with its full credit in Settings, About, Photo
credits; showing the photos on the pose screens is the next UI step. A review of the shipped set on 2026-11-22 removed
10 photos and replaced 3 (see [Review, 2026-11-22](#review-2026-11-22)); a second look before the photos went into the
app removed one of the replacements, `selfie_classic` (see [Second look](#second-look-2026-11-22)). Before the review,
46 poses had a photo.

| Where | What |
| --- | --- |
| `app/src/main/assets/pose_photos/<poseId>.webp` | the photos: 675x900 (676x900 for two, 900x675 for the landscape group pose), WebP quality 68-78, no metadata; 35 files, 1,486,296 bytes (1.42 MiB, about 42 KB each) |
| `app/src/main/assets/pose_photos/credits.json` | one credit per photo, keyed by pose id (38 KB) |
| `app/src/main/assets/licenses/` | full licence texts (CC0 1.0, CC BY 2.0 / 3.0 / 4.0, CC BY-SA 3.0, the Mixkit licence and the User Terms that limit it; also the app's font licence and the Astryx notice) and `index.json`, which lists every file in the folder |
| `tools/pose-photos/` | the harvest, ranking, selection and credit tools (see its README) |
| `tools/pose-photos/landmarks/<poseId>.json` | the app's pose model on each shipped photo |
| `tools/pose-photos/app-match.json` | the app's PoseMatcher on each shipped photo (pose-core `PosePhotoMatch`) |
| `tools/pose-photos/coverage.json` | all 99 poses: photo or not, source, licence, app score, nearest pose, the selection's reason, the confirm verdict |
| `/home/user/poseit-photos-work/` (outside git) | downloads, frames, candidates, contact sheets, `all-picks*.jpg` (every photo beside its figure) |

## Method

1. **Harvest** (`mixkit_harvest.py`, `commons_harvest.py`): Mixkit clips whose own page says "Mixkit Stock Video Free
   License" and Wikimedia Commons files under CC0, PD, CC BY or CC BY-SA, filtered by the people rules on their text.
   About 3,250 images (Mixkit preview frames and Commons photos) in the first run, more in two gap-filling rounds.
2. **Detect and rank** (`detect_images.py`, `score.py`): the app's full pose model (MediaPipe, IMAGE mode, up to 4
   people) on every image, then every image against every pose with a Python port of the app's PoseMatcher, the guide
   dragged and pinched onto the person as a user would, for the pose as drawn and flipped. Contact sheets per pose.
3. **Select** (one pass per category, then two gap rounds, `picks/*_finalize.py`): a person looks at each pose's
   sheet, rejects anything that breaks the people rules, prefers a clear hold at a good size, then the final frame or
   photo is taken from the licensed download (Mixkit 1080p/720p file, Commons rendition up to 3840 px), cropped to the
   pose's frame (3:4, or 4:3 for the landscape group pose), mirrored when the photo matches the flipped figure, saved
   as WebP without metadata, detected again, scored, and given a credit record. 48 photos came out of this step:
   sitting/floor/walking 8 of 29, standing/leaning/professional 9 of 27, portrait/selfie/fun 16 of 26, couples 7 of
   10, groups 0 of 7, gap round 1 5 of 59, gap round 2 3 of 54.
4. **Confirm** (this stage, `PosePhotoMatch`, `credits.py`, `allpicks.py`): the app's own Kotlin PoseMatcher on every
   shipped photo; every photo with a low score or nearer to another pose looked at again; 2 dropped, 46 kept;
   credits merged into `credits.json`; licence texts added; `all-picks.jpg` drawn for review.
5. **Review** (2026-11-22, `picks/review_rescore.py`, `picks/review_finalize.py`): every shipped photo looked at again
   against the people rules, its figure and its source record; 10 removed, 3 replaced from the existing candidates,
   the matcher and credits re-run. Details in [Review, 2026-11-22](#review-2026-11-22).
6. **Second look** (2026-11-22, before the photos went into the app's screens): `selfie_classic` removed, the matcher
   and credits re-run (35 photos). See [Second look](#second-look-2026-11-22).

## Sources at a glance

| Source | Used? | Why |
| --- | --- | --- |
| Mixkit stock videos (mixkit.co), **Free License clips only** | Yes, still frames (27 photos from 27 clips) | Licence allows modifying and distributing the items in commercial and non-commercial projects; no attribution required. See the limits and the open questions below. |
| Wikimedia Commons | Yes, photos (8) | Only CC0, Public Domain / PDM, CC BY (any version), CC BY-SA (any version), checked per file from its own metadata. Used: CC0 (6, mostly Pexels / Unsplash / Pixabay uploads copied to Commons), CC BY 2.0 (2). |
| Picjumbo | No | Its terms allow commercial use and templates but say nothing about apps, forbid redistributing "the pictures as a file", and warn that some photos have no model release. Not clearly allowed inside an app, so left out. |
| Pexels, Pixabay, Unsplash (their own APIs) | No | Need API keys this environment does not have. (CC0 photos that their users uploaded before those sites changed their licences, and that were copied to Commons, are used through Commons under CC0.) |
| Openverse | No | Blocked from this environment. |
| Coverr | No | Its licence bars using its content to evaluate AI models; matching photos runs a pose-detection model on them. |
| Mixkit Restricted License clips | No | Not the Free License. Many missing poses have only Restricted clips (see the coverage table). |

## Mixkit

### What the licence says

Each clip page's download panel names its licence. We keep a clip only if the panel reads exactly "Download this free
stock video clip for commercial or personal use, under the Mixkit Stock Video Free License" (link
`data-license="videoFree"`) and the page's schema.org `license` is `https://mixkit.co/license/#videoFree`. The
harvester stores that evidence per clip.

Mixkit Stock Video Free License (https://mixkit.co/license/#videoFree, text of
https://mixkit.co/license/modal/videoFree/):

> Videos under the Free license can be used in YouTube videos, Social Media video posts, Online marketing ads,
> Educational purposes, Music videos, Commercial projects, Filmmaking
>
> Items under the Mixkit Stock Video Free License can be used in your commercial and non-commercial projects, for free.
> You're permitted to download, copy, modify, distribute, publicly perform and broadcast the Items. Your rights are
> non-exclusive, worldwide, sub-licensable and ongoing. Attribution is not required, however, we would appreciate it if
> you credit Mixkit where reasonably possible.
> There are some important limits to these rights, described in our User Terms.

Mixkit User Terms (https://mixkit.co/terms/), the relevant parts of clause 9 "Restrictions" ("You must not ..."):

> use an Item in any way that breaches our Acceptable Use Policy;
>
> sell physical or digital copies of Items without first altering them by applying human skill and effort, and
> incorporating other elements (merely printing an Item on an object such as a mug or shirt is not sufficient
> alteration);
>
> rent, license, sublicense, sell, resell or otherwise commercially exploit or make Mixkit or any Item available to any
> third party (except as expressly contemplated by these User Terms) including aggregate or collate an Item(s) and make
> available on a stock or inventory basis;
>
> use Mixkit or its related systems and networks, or any Item, to build a similar or competitive product or service;
>
> remove any watermarking, copyright notices or other protective measures from any Item, including preview files;
>
> use scripts or bots to mass download Items (this includes using any means whatsoever to scrape/download the entire
> library and/or database of Items)

Clause 13 "Third party rights": "For some Items, a component of the Item (such as a trade mark or brand) may be sourced
from a third party and different license terms may apply to the component ... it is your responsibility to evaluate
whether your use of these components require the permission of those third parties". Clause 7: users must be 18+.

The Envato Acceptable Use Policy and Fair Use Policy that the terms incorporate
(help.elements.envato.com/hc/en-us/articles/31035788503321 and .../360000621743) could not be read from here (HTTP
403, a browser check). A search summary of the Acceptable Use Policy says assets that depict or reference sensitive
topics may only be used in an educational, informational, journalistic or advocacy context, and items must not be used
to create a false identity or imply that a depicted person endorses a product. **Someone should read both policies in a
browser before release.**

### Are still frames inside an app allowed?

Our reading: **yes, with conditions.** The licence lets us "copy, modify, distribute" the items in "commercial and
non-commercial projects"; the list of uses is examples, and a free app is a project. A still frame cut from a clip and
cropped is a modification. Distributing it inside the app, next to the app's own pose figure and coaching, is neither
selling copies nor making items available "on a stock or inventory basis". Conditions we keep:

- frames are shown only inside the pose guide, cropped and placed with the figure; the app never offers them as
  downloadable or exportable files;
- the people are stock models: no names, no captions that put words in their mouths, nothing that suggests they use
  or endorse Poseit, nothing that shows them in a bad light, no sensitive contexts;
- Mixkit is credited under each still ("Photo: Mixkit") and on the credits screen, although attribution is not
  required;
- frames for release come from the clip's licensed download (the 720p/1080p file linked from the clip page), not from
  the grid's hover preview; the harvester only uses previews to find candidates.

### Open question: scripted downloads

Clause 9 forbids "scripts or bots to mass download Items". The harvester read public grid and clip pages and fetched
the small 360p hover previews that a browser plays on the grid, one at a time with a gap, only for clips whose title
and description suggest a person holding a pose (about 700 previews over the first run and the gap rounds, from about
970 clip pages checked for the licence; several thousand clips were listed on the grid and search pages read). The
release frames were then cut from each chosen clip's licensed 1080p (else 720p) file, fetched from the clip's own
download page by the selection scripts, one clip at a time, and the video was deleted once its frames were saved: 31
clips for the 33 stills shipped before the review. The review fetched 4 more licensed files the same way (40038,
41181, 43219, 43806; 41181 is not used) and removed 8 Mixkit stills, and the second look removed the 40038 still, so
27 clips give the 27 stills shipped now. That
is far from "the entire library", but it is scripted. Before the app ships the Mixkit stills, the owner should decide
whether that is acceptable, or download those 27 clips by hand from the pages listed in `credits.json` (`pageUrl`,
frame time in `frameTimeS`) and re-cut the frames, or ask Mixkit.

Licence re-checks on 2026-11-22: the reviewer read five clip pages again (52257, 51119, 32800, 51645, 101458) and the
review read the four new ones before fetching them; every page still says "Mixkit Stock Video Free License"
(`data-license="videoFree"`), with no "Restricted" or "Premium" wording. The Envato Acceptable Use and Fair Use
Policies are still unread (HTTP 403).

The Envato Acceptable Use Policy also needs a human read (see above): the shipped stills are ordinary lifestyle and
studio shots (no sensitive topics), and the app never names the people or suggests they endorse it.

## Wikimedia Commons

Files are found with the MediaWiki API (`commons.wikimedia.org/w/api.php`, File: namespace) and each file's licence is
read from its own `extmetadata`: LicenseShortName, LicenseUrl, Artist, Credit, AttributionRequired, Restrictions,
UsageTerms, ImageDescription, Categories.

Allowed: **CC0, Public Domain / Public Domain Mark, CC BY (any version), CC BY-SA (any version)**. Never NC or ND,
never GFDL-only, never Commons' bare "Attribution" template, never fair use, never a file whose licence is missing or
unclear.

What the licences require (Creative Commons deeds, creativecommons.org, read 2026-11-21):

- CC BY 4.0: "Attribution — You must give appropriate credit, provide a link to the license, and indicate if changes
  were made. You may do so in any reasonable manner, but not in any way that suggests the licensor endorses you or your
  use." Older versions (2.0, 2.5, 3.0) have the same core requirement.
- CC BY-SA 4.0: the same attribution, plus "ShareAlike — If you remix, transform, or build upon the material, you must
  distribute your contributions under the same license as the original." A cropped or mirrored photo is an adaptation:
  the cropped photo file itself must be offered under the same CC BY-SA licence (the app around it is a separate work
  and is not affected). Both deeds also warn: "other rights such as publicity, privacy, or moral rights may limit how
  you use the material."
- CC0 1.0: "You can copy, modify, distribute and perform the work, even for commercial purposes, all without asking
  permission." and "In no way are the patent or trademark rights of any person affected by CC0, nor are the rights that
  other persons may have in the work or in how the work is used, such as publicity or privacy rights. ... When using or
  citing the work, you should not imply endorsement by the author or the affirmer."

So every Commons photo gets a credit (author as stated in Artist, licence name and link, "cropped" / "mirrored" when
done), CC0 and PD included as a courtesy. Commons' own reuse guideline could not be fetched (rate limit), so it is not
quoted here.

`Restrictions` field: files tagged `personality` (an identifiable person; use must not imply endorsement) are kept but
flagged for the reviewer; `trademarked`, `insignia` and similar are rejected.

### Rate limits

Wikimedia allows about 10 API requests a minute to clients whose User-Agent carries no contact details (200 with
contact details), and this environment's address is shared, so most API requests first got HTTP 429 with a Retry-After
of 30-60 s: about one search a minute in practice. Images must be fetched at a standard thumbnail width (1280, 960,
500 ...; mediawiki.org/wiki/Common_thumbnail_sizes): originals and other widths are refused with 429. The harvester is
serial, honours Retry-After and uses standard widths. Adding a contact address to the User-Agent (the owner's choice;
the tools do not send one) would raise the API limit.

## People rules (all sources)

Applied to titles, descriptions and categories by `tools/pose-photos/rules.py`, then again by a person looking at the
contact sheets before anything is chosen:

- **Adults only.** Anything that could show a minor is skipped (child, kid, boy, teen, school, student, family,
  daughter, little, ...). "girl" and "young" are flagged for an age check, because stock captions use them for adults.
  The reviewer also skips a photo whose person could look under 18, whatever the caption says (the review removed
  `lean_railing_back` for this).
- **Appropriate only.** No nudity, lingerie or clothing that reads as underwear, swimwear, cleavage as the centre
  of the frame, sexualised, "sensual" or "seductive" framing (Commons categories such as 'Seduction (facial
  expression)' included), beds and bathrooms, alcohol, smoking, drugs, injury, hospitals, police, protest, war,
  weapons, religion, crying or sadness, costumes and Halloween.
- **No public figures.** Skip files from events, red carpets, politics, sport, concerts, conferences, fashion shows
  (Commons), and files whose title, description or categories name a person, models included ('Alisa (photo model)',
  'Isabela Salvador'; since the review the text rules also catch a name with a role in brackets and hyphenated or
  accented names such as 'Marie-Béatrice Levaux'); Commons titles in scripts the rules cannot read are skipped too.
- **Photographs only** (Commons): no paintings, drawings, sculpture, prints, historical or archival photos, renders or
  AI-generated images, mannequins or dolls.
- **No watermarks, logos or overlaid text**; reviewer checks the frame.
- **Never in a bad light**: the model is shown as a capable person demonstrating a pose (not, for example, a file
  titled as showing someone "looking strangely").
- **One photo per model where possible.** The review found one model in three photos and removed them. Still shared,
  for want of alternatives: one couple in 3 of the 6 couple photos (`couple_forehead_touch`,
  `couple_hold_hands_facing`, `couple_sit_lean`: Mixkit 100759/100733/100799, one production) and one set, probably
  one model, in `port_chin_rest` and `floor_side_lie` (Mixkit 101458/101457).

Allowed changes: **crop** and **horizontal mirror** (when the photo matches the flipped pose). Both are recorded in the
credit. Nothing else (no retouching, no compositing).

Incidental brand marks on clothing, shoes and devices (a shoe stripe or swoosh, a small chest logo) and signs that are
part of the scene (an exit sign in a hotel corridor) are not treated as overlaid logos or text. In the shipped set:
Nike marks on the shoes in `sit_elbows_on_knees`, faint lettering on a headphone earcup in `port_dreamy_gaze`, a chest
mark on a T-shirt in `walk_twirl`, a bandana printed with lettering in `port_collar_adjust`, jacket lettering and an
EXIT sign in `selfie_mirror`. A prominent product logo held toward the camera (the Apple logo on the phone in the
removed `selfie_mirror_two_hands`; the gold phone in Mixkit 41181) counts against a candidate.

## Credits and licence files

`app/src/main/assets/pose_photos/credits.json`, keyed by pose id, one entry per shipped photo (written by
`tools/pose-photos/credits.py` from the selection records and `app-match.json`):

| Field | What |
| --- | --- |
| `photo` | file name in `pose_photos/` |
| `source` | `Mixkit` or `Wikimedia Commons` |
| `title` | the clip's or file's title as published |
| `pageUrl` | the clip page or the Commons file page |
| `fileUrl` | Mixkit: the clip's 1080p download link; Commons: the original file |
| `author` | as stated (Commons `Artist`; Mixkit names no author: "Mixkit (no author named on the clip page)") |
| `licence`, `licenceUrl`, `licenceText` | licence name, its deed URL, and the full text shipped in `assets/licenses/` |
| `attribution` | **the short line to show under the photo**: `Photo: Mixkit`; for every Commons photo `Photo: <author>, <licence>, Wikimedia Commons (cropped[ and mirrored])` (CC BY / BY-SA ask for changes to be indicated; CC0 does not, but since the review every Commons credit says it the same way); the Rydale photo adds the link its author asks for (`www.rydale.com`) |
| `changes` | exactly what was done: frame time and source size, crop box in source pixels, mirrored or not, resize, WebP quality, metadata stripped |
| `mirrored` | true when the file was flipped horizontally so the person matches the figure as drawn |
| `appScore` | the app's PoseMatcher score of the shipped photo against its own pose as drawn (see below); null when the detector finds nobody |
| optional | `frameTimeS`, `videoQuality` (Mixkit); `sourceCredit` (Commons `Credit`), `attributionRequest`, `shareAlike` (BY-SA: the adapted file is shared under the same licence), `restrictions` (Commons `personality`: an identifiable person, use must not imply endorsement), `note` (why a flagged photo was kept) |

What the app should show: the `attribution` line under (or on) the photo, and a credits screen listing every entry
with its title, author, licence name linking to `licenceUrl` or opening `licenceText`, the source page, and the
changes. CC BY 2.0 asks for the licence's URI or a copy to go with the work: `licenceUrl` plus the shipped text covers
that. No shipped photo is under CC BY-SA since the review removed `pro_arms_crossed`; a CC BY-SA photo added later is
an adaptation once cropped and is offered under the same licence (the app around it is not affected). Never present a
photo as Poseit's own, never caption the people, never suggest they use or endorse Poseit.

`app/src/main/assets/licenses/index.json` lists the licence texts: `CC0-1.0.txt`, `CC-BY-2.0.txt` (converted from
the HTML legal code; CC publishes no plain-text file for 2.0), `CC-BY-3.0.txt`, `CC-BY-4.0.txt`, `CC-BY-SA-3.0.txt`
(the CC legal code files, fetched 2026-11-21), `Mixkit-Stock-Video-Free-License.txt` (the licence and the clauses of the
User Terms that limit it), and, for the app itself, `OFL-HankenGrotesk.txt` (the typeface) and `MIT-Astryx.txt` (the
design system's notice). `index.json` lists exactly the files in the folder; the app unit test `PhotoAssetsTest` fails
when a file is not indexed, an indexed file is missing, a credit has no photo or no licence text, a photo has no
credit, or a credit's `attribution` is empty. CC BY 3.0, CC BY 4.0 and CC BY-SA 3.0 are not used by
any shipped photo (CC BY-SA 3.0 was, until the review); they stay for replacements. A photo under any other licence (e.g. CC BY-SA 4.0) needs its text added first;
`credits.py` refuses a credit whose licence text is missing.

## Confirm stage: the app's matcher on every shipped photo

pose-core `src/test/kotlin/com/poseit/app/tools/PosePhotoMatch.kt` (modelled on `PromoChorusMatch`) reads
`tools/pose-photos/landmarks/<poseId>.json` for every shipped photo and scores it with the app's real `PoseMatcher`:

```sh
export ANDROID_HOME=/opt/android-sdk
./gradlew --console=plain -q :pose-core:test --tests 'com.poseit.app.tools.PosePhotoMatch' --rerun
```

(`--rerun` because the landmark files are not declared task inputs.) It writes `tools/pose-photos/app-match.json`.

- Scene = the whole photo (`LandmarkMapping.toScene`, scene aspect = photo aspect). The guide is placed as a user would
  drag and pinch it (mid-hips onto mid-hips, torso length onto torso length, mid-shoulders when a hip is hidden), then
  `PoseMatcher.match` (back camera, not mirrored, photographer audience).
- **appScore** = the shipped photo against its own pose **as drawn**. A mirrored photo was flipped before it was saved
  and its landmarks come from the saved file, so it is not flipped a second time; the score with the flipped figure is
  reported too (`flipped`). For 3 of the 35 photos the flipped figure scores higher, by 0.01 or less except
  `selfie_peace` (0.92 / 0.98); looked at, it reads the same way round as the figure.
- **Nearest pose**: every library pose whose people count fits the photo, as drawn and flipped, ranked by score minus
  penalties for poor framing fit, people-count mismatch, the pose's frame leaving the photo, the pose's own segments
  not seen (`tcov`) and the person reaching outside the pose's frame (`inFrame`; without it a headshot template pinched
  down onto a full-body photo counts as a whole standing figure). Also: own pose's rank, part scores per body part,
  fit, tcov.
- The Kotlin scores agree with the Python port used for ranking (`posematch.py`) to 0.0001 on all 42 photos where both
  scored the same orientation.

Results for the 46 photos shipped at this stage (2026-11-21): app score >= 0.8 for 17, 0.5-0.8 for 11, below 0.5 for
17, none for 1 (the detector found nobody in the old `selfie_classic` close-up). The own pose was the nearest for 14.
After the review and the second look (35 photos): >= 0.8 for 11, 0.5-0.8 for 9, below 0.5 for 15, none for 0; the own
pose is the nearest for 8; 29 are flagged. The notes below describe the confirm stage; several of the photos they name were removed or
replaced by the review.

**Flag rule**: app score below 0.5 (or none), or another pose nearer by rank, or fewer people detected than the pose
has. 35 of the 48 selected photos were flagged and every one was looked at again beside its figure (sheets in
`/home/user/poseit-photos-work/all-picks*.jpg`):

- **Dropped (2)**: `couple_piggyback` (a waist-up hug from behind; nothing shows a piggyback ride, and it duplicated
  the back hug) and `fun_superhero` (a member of the public at an eclipse viewing, an event, not a stock model, head
  tipped back in eclipse glasses). No acceptable replacement exists among the candidates (checked: the remaining
  superhero candidates are a performer in a leotard, a studio shot with the photographer in front of the model, and a
  near-silhouette under a pink filter).
- **Kept (33)**: each shows the pose's defining position. Why their scores are low, in short (full notes in
  `credits.json` and `coverage.json`): the detector separates only one of two hugging or kissing people
  (`couple_back_hug`, `couple_cheek_kiss`, `couple_sit_lean`) or loses limbs in dark clothes or a long dress
  (`lean_crossed_legs`, `stand_hand_in_hair`); the photo is tighter than the drawn frame (`pro_headshot`: chest-up
  where the figure reaches the hips, score 0) or seen from another side than the figure (`sit_elbows_on_knees` from the
  front, `selfie_high_angle` and `selfie_classic` from beside the phone); a horizontal body (`floor_side_lie`); the
  hands do something else while the body matches (`sit_crossed_legs`, `sit_one_knee_up`, `lean_foot_on_wall`,
  `floor_one_knee` holding a ring box, `selfie_mirror`, `fun_twirl`); and for portraits and selfies the "nearest" is
  another few-segment template (usually the headshot) scoring within a few points of the own pose, which ranks 2nd
  to 4th.

Low app scores therefore mostly measure the matcher's limits (2D limb directions, one detected person per embrace,
required segments outside a tight photo), not wrong photos; but they also mean these photos are poor references for
the app's live score. If the UI ever uses a photo as a target for the live matcher, use only those with appScore
>= 0.8.

## Review, 2026-11-22

A reviewer went through every shipped photo against the people and licence rules and its figure and raised 24
findings. Each was checked by looking at the shipped file, the full-resolution source frame where there was one, and
the cached licence record. To look for replacements, `picks/review_rescore.py` ranked all ~8,100 frames and photos
already detected by the earlier rounds against the affected poses (contact sheets in
`/home/user/poseit-photos-work/review/`), and every cached Mixkit and Commons record was searched by title (selfie,
mirror, kneel, crossed arms, couple walking, ...). Replacements went through the same steps as before
(`picks/review_finalize.py`: licensed 1080p frame, 3:4 crop, mirror, WebP, landmarks, `PosePhotoMatch`).

| Pose | Finding | Checked | Action |
| --- | --- | --- | --- |
| `selfie_mirror_two_hands` | shorts that read as boxer briefs, unflattering title, Apple logo | confirmed | removed; no other full-length two-hand mirror selfie |
| `couple_walk_toward` | Shixart1985 "own work" CC BY 2.0: licence cannot be verified | Commons answered HTTP 429 again (uploader talk page) | removed; the Mixkit couples walking toward the camera are 16:9 thigh-up shots too wide for 3:4, 'Coney Island couple' has brand signs and children behind |
| `selfie_high_angle` | same uploader; soft face; score 0.08 | confirmed | removed; no other usable raised-phone selfie |
| `pro_blazer_pocket` | no hand in a pocket; second frame of the `stand_crossed_ankles` clip; veil like blood spatter | confirmed | removed; no hand-in-pocket candidate |
| `stand_crossed_ankles` | ankles not crossed; one model in 3 photos; veil | confirmed | removed; no crossed-ankle candidate |
| `selfie_classic` | close-up from beside the phone, no extended arm; same model as `selfie_peace` | confirmed | **replaced**: Mixkit 40038 at 0.367 s, mirrored: phone at arm's length, face and phone in frame (a two-person selfie; app score 0.11, side view); removed at the second look |
| `floor_one_knee` | a marriage proposal, not a pose demonstration | confirmed | removed; the other kneelers are a photographer in front of a model and a gardener behind plants |
| `port_arms_crossed_smile` | veil hides the smile; soft; third photo of the same model | confirmed | **replaced**: Mixkit 43806 at 1.242 s, mirrored: arms folded, closed-mouth smile, plain mint backdrop (app score 0.97) |
| `pro_arms_crossed` | categories name the model ('Alisa (photo model)') | confirmed | removed under the named-person rule, no exception made; it is in 'Full consent', so the owner could decide to allow consenting named models and restore it (record in `picks/gaps-r2.json`) |
| `floor_street_squat` | credit title names a woman, the frame shows the man (coach) in clip 32800 | confirmed | note added to the credit |
| `fun_twirl` | not a spin: a flannel shirt swung overhead while walking | confirmed | **replaced**: Mixkit 43219 at 12.425 s, mirrored: a dancer mid-turn, one arm high and one out (app score 0.40, its own pose the nearest of 82) |
| `fun_victory` | motion blur; low-cut top at the centre | confirmed | removed |
| `lean_railing_back` | could be under 18 | looked at the 1080p face: cannot rule it out | removed |
| `port_shoulder_touch` | category 'Seduction (facial expression)' | confirmed | removed; `rules.py` now rejects 'seduction' |
| `stand_hand_on_hip` | deep neckline; fits the power pose better? | fashion jumpsuit, nothing exposed; only one hand on the hip | kept, note added |
| `couple_sit_lean`, `couple_forehead_touch` | one couple in 3 couple photos; chroma-green background | confirmed | kept (no alternatives), noted |
| `sit_elbows_on_knees` | Pexels CC0 not verified; Nike marks | Commons copy dated 2017-04-04, in Pexels' CC0 period; earliest archived Pexels page (2019) HTTP 403 | kept, note added |
| `sit_crossed_legs`, `selfie_mirror` | hands differ from the figure; jacket text, EXIT sign, uploader self-portrait | confirmed | kept, notes updated |
| `port_dreamy_gaze` and others | small third-party marks | confirmed | kept; listed under the people rules |
| `floor_side_lie` | same set as `port_chin_rest` | confirmed | kept, noted |
| CC0 attributions | "(cropped)" / "(mirrored)" missing for CC0 | confirmed | `credits.py` now adds them for every Commons photo |
| all Mixkit stills | scripted downloads vs. User Terms clause 9 | still open | numbers updated above; five clip pages re-read by the reviewer, four new ones by the review |

Net effect: 46 -> 36 photos (10 removed, 3 replaced). The removed records stay in `credits.py`'s `DROPPED` with the
reason; their selection records stay in the earlier fragments, so a photo could be rebuilt if a decision changes.

### Second look, 2026-11-22

Before the photos went into the app's screens, the replacement for `selfie_classic` was looked at again: the frame
shows three people taking a selfie in a car (a third person's arm and knees at the edge), while the pose is a solo
selfie with the arm extended, so it misleads as a guide. It was removed (`DROPPED` in `credits.py`, its photo and
landmarks file deleted, `PosePhotoMatch` and `credits.py` re-run): 36 -> 35 photos, 27 of them Mixkit stills. No other
solo arm's-length selfie is among the candidates, so the pose has no photo for now.

## Replacing a photo or adding one

1. **Find a candidate** whose licence allows use in an app and that passes the people rules: the harvest tools
   (`tools/pose-photos/README.md`), the contact sheets in the work folder, or a file found by hand. Record the licence
   as the source states it (Commons: the file's `extmetadata`; Mixkit: the clip page's licence panel).
2. **Make the photo** with a finalize script, which crops to the pose's frame (3:4, or 4:3 for a landscape pose),
   mirrors when the photo matches the flipped figure, resizes to 675x900, saves WebP without metadata as
   `app/src/main/assets/pose_photos/<poseId>.webp`, runs the app's pose model on the saved file and writes
   `tools/pose-photos/landmarks/<poseId>.json`. For one photo the simplest is
   `picks/standing-leaning_finalize.py POSE --commons PAGEID` (a Commons rendition in the work folder) or
   `--hires CLIP_MS` (a frame saved by `picks/standing-leaning_extract.py CLIP T` from the clip's licensed download),
   with `--mirror auto|yes|no` and `--crop person|template|x0,y0,x1,y1`. Python: the promo venv (mediapipe, OpenCV,
   Pillow).
3. **Write the selection record** in a fragment under `tools/pose-photos/picks/` (a new file must be added to `ORDER`
   in `credits.py`; later fragments win): `pose_id`, `photo`, `source`, `title`, `page_url`, `file_url`, `author`,
   `licence` {`name`, `url`, ...}, `changes`, `mirrored`, `crop_px`, `reason`, and for Commons `credit_line` and any
   `attribution_request`.
4. **Check it with the app's matcher**: run `PosePhotoMatch` (command above), then
   `python3 tools/pose-photos/credits.py` (rewrites `credits.json` and `coverage.json`) and
   `promo/.venv/bin/python tools/pose-photos/allpicks.py` (the review sheets). Look at the photo beside its figure. If
   it is flagged, keep it only when it plainly shows the pose, and add a `NOTES` entry saying why the score is low.
5. A new licence needs its full text in `app/src/main/assets/licenses/`, an entry in `licenses/index.json`, and in
   `LICENCE_URLS` / `LICENCE_FILES` in `credits.py`.
6. **To remove a photo**: delete its `.webp` and landmarks file, add the pose to `DROPPED` in `credits.py` with the
   reason, and rerun steps 4. `credits.py` refuses photo files without a credit and credits without a file.

`python3 tools/pose-photos/credits.py --table` prints the coverage table below from `coverage.json`.

## Coverage, all 99 poses

35 with a photo, 64 without (after the review and the second look). App score = the app's PoseMatcher on the shipped photo against its own pose as drawn
(1 = perfect; "-" = the detector finds nobody). "Flagged" = score below 0.5, fewer people detected than the pose has,
or another pose nearer by the matcher's rank; every flagged photo was looked at again (the note says what it shows).
For poses without a photo the note is the selection's last reason, or why the confirm stage or the review removed the photo (in full in `coverage.json`).

| Category | Poses | With a photo |
| --- | --- | --- |
| Standing | 10 | 3 |
| Sitting | 10 | 3 |
| Leaning | 9 | 2 |
| Walking | 10 | 3 |
| On the ground | 9 | 4 |
| Portrait | 9 | 8 |
| Selfie | 8 | 2 |
| Professional | 8 | 1 |
| Fun & jumping | 9 | 2 |
| Couple | 10 | 6 |
| Group | 7 | 1 |
| **All** | **99** | **35** |

| Pose | Category | Photo | Source | Licence | App score | Note |
| --- | --- | --- | --- | --- | --- | --- |
| Hand on hip (`stand_hand_on_hip`) | Standing | yes | Mixkit | Mixkit Free | 0.86 |  |
| Hands in pockets (`stand_hands_in_pockets`) | Standing | **no** | | | | Round 2's best full-body hands-in-pockets photo ('Italian style 2013', CC0: both hands in her front jeans ... |
| Crossed ankles (`stand_crossed_ankles`) | Standing | **no** | | | | removed at the review: the ankles are not crossed (the boots stand side by side, one half a step ahead), the model also filled ... |
| Collar touch (`stand_collar_touch`) | Standing | yes | Mixkit | Mixkit Free | 0.92 | flagged (nearest: walk_coffee_run), kept: full length, one hand at the neck and collarbone, the other arm down, as ... |
| Candid look away (`stand_look_away`) | Standing | **no** | | | | No full-body relaxed stance with the head turned away |
| Relaxed arms crossed (`stand_arms_crossed`) | Standing | **no** | | | | The full-body crossed-arms photos found name the person ('Wena Opena, 2021', 'Danielagalliano', ... |
| Hand in hair (`stand_hand_in_hair`) | Standing | yes | Commons | CC0 | 0.14 | flagged (low score, nearest: selfie_high_angle), kept: standing tall with one hand in the hair and the other arm down, as drawn |
| Over the shoulder (`stand_over_shoulder`) | Standing | **no** | | | | Nobody looks back over the shoulder |
| Power pose (`stand_power_pose`) | Standing | **no** | | | | No clean wide-stance hands-on-hips |
| Sunglasses touch (`stand_sunglasses`) | Standing | **no** | | | | No full-body hand-on-sunglasses |
| Crossed legs (`sit_crossed_legs`) | Sitting | yes | Mixkit | Mixkit Free | 0.40 | flagged (low score, nearest: fun_tuck_jump), kept: seated upright with the legs crossed at the knee as drawn (legs 1.0 / 0.81) |
| Elbows on knees (`sit_elbows_on_knees`) | Sitting | yes | Commons | CC0 | 0.33 | flagged (low score, nearest: floor_street_squat), kept: sitting on steps with the forearms on the knees and the hands clasped |
| One knee up (`sit_one_knee_up`) | Sitting | yes | Commons | CC BY 2.0 | 0.48 | flagged (low score, nearest: pro_desk_edge), kept: on stairs with one knee drawn up high and the other leg reaching down (legs ... |
| Side-saddle (`sit_side_saddle`) | Sitting | **no** | | | | No knees-together side sit |
| Sideways on steps (`sit_steps_sideways`) | Sitting | **no** | | | | Nearest is 'Man using smartphone, Passeio das Tágides' (CC BY-SA 4.0) |
| Hugging knees (`sit_hug_knees`) | Sitting | **no** | | | | Mixkit 323 ('Woman sitting on skateboard', Free) hugs one knee in a cross-legged sit, with the feet cut ... |
| Armchair lounge (`sit_armchair_lounge`) | Sitting | **no** | | | | 'People sitting on armchairs' (103 files) gave upright sitters and named people |
| Backwards chair (`sit_chair_backwards`) | Sitting | **no** | | | | Nobody astride a backwards chair in the phrase search (38 files) or 'People sitting on chairs' (472). |
| Ankle on knee (`sit_ankle_on_knee`) | Sitting | **no** | | | | 'Crossed legs' (deep category) gives knee-over-knee sits |
| High perch (`sit_high_perch`) | Sitting | **no** | | | | 'People sitting on stools' (35 files) and the stool phrases gave no clean high perch. |
| Shoulder lean (`lean_shoulder_wall`) | Leaning | **no** | | | | Closest is 'Cuba (6959893471)' (CC BY 2.0: shoulder on a door frame, arms and ankles crossed), but the two ... |
| Foot up on the wall (`lean_foot_on_wall`) | Leaning | yes | Mixkit | Mixkit Free | 0.54 | flagged (nearest: pro_arms_crossed), kept: leaning on a wall with one foot planted on it behind him (legs 0.77 / 1.0) |
| Hand on the wall (`lean_hand_on_wall`) | Leaning | **no** | | | | 'Italian girl 2013' (hand on the fence above a wall) is described as a teenager |
| Railing lean (`lean_railing_forward`) | Leaning | **no** | | | | The railing phrases gave 10 files and the leaning categories nobody bent forward on the forearms over a rail. |
| Cross-legged lean (`lean_crossed_legs`) | Leaning | yes | Mixkit | Mixkit Free | 0.36 | flagged (low score, nearest: fun_disco_point), kept: leaning back on a car with the ankles crossed |
| Doorway lean (`lean_door_frame`) | Leaning | **no** | | | | The doorway phrases (213 files) and the leaning categories gave nobody with an arm up on a door frame. |
| Pole swing (`lean_pole_swing`) | Leaning | **no** | | | | Nobody swinging round a pole or lamp post (24 lamp-post files, the jumping and leaning categories). |
| Table-edge perch (`lean_table_edge`) | Leaning | **no** | | | | 'People sitting on desks' and 'People sitting on tables' (24 files) and the desk-perch phrases (27) gave ... |
| Elbows on the rail (`lean_railing_back`) | Leaning | **no** | | | | removed at the review: the clip calls her a woman, but in the full-resolution frame she could be under 18, and the rules skip ... |
| Walk toward the camera (`walk_toward_camera`) | Walking | yes | Mixkit | Mixkit Free | 0.67 | flagged (nearest: walk_kerb_step), kept: walking straight at the camera |
| Walk across (`walk_across`) | Walking | yes | Mixkit | Mixkit Free | 0.80 |  |
| Walk away, look back (`walk_look_back`) | Walking | **no** | | | | Nobody walking away and turning back |
| Off the kerb (`walk_kerb_step`) | Walking | **no** | | | | No step down from a kerb in any new candidate. |
| Happy skip (`walk_skip`) | Walking | **no** | | | | No skip: the Commons 'Skipping' category is rope skipping and children |
| Hat in the wind (`walk_hat_hold`) | Walking | **no** | | | | 'People holding hats' (21 files) has nobody walking |
| Pocket stroll (`walk_pockets`) | Walking | **no** | | | | Nobody walking with the hands in the pockets |
| Arms-out spin (`walk_twirl`) | Walking | yes | Mixkit | Mixkit Free | 0.60 |  |
| Coffee run (`walk_coffee_run`) | Walking | **no** | | | | 'People with cups' (42 files) has nobody walking |
| Jacket over shoulder (`walk_jacket_shoulder`) | Walking | **no** | | | | 'Black woman with women's suit 8' holds her jacket over her shoulder but stands still and is cut at the thighs |
| Street squat (`floor_street_squat`) | Floor | yes | Mixkit | Mixkit Free | 0.49 | flagged (low score), kept: a deep flat-footed squat with the forearms on the knees |
| Low crouch (`floor_low_crouch`) | Floor | **no** | | | | Women/Men/People crouching (592 files) give named people ('TamaraAnnaPawlak', 'WikiTamara'), sport and ... |
| One knee down (`floor_one_knee`) | Floor | **no** | | | | removed at the review: a marriage proposal (ring box held out, partner cropped away, mouth open mid-speech), not a pose ... |
| Cross-legged (`floor_cross_legged`) | Floor | yes | Mixkit | Mixkit Free | 0.53 | flagged (nearest: selfie_lean_in), kept: cross-legged on grass with the hands on the knees (three-quarter view) |
| Side lie on elbow (`floor_side_lie`) | Floor | yes | Mixkit | Mixkit Free | 0.27 | flagged (low score, nearest: floor_one_knee), kept: lying on her side propped on the elbow, head in hand, knees bent, head to ... |
| Feet in the air (`floor_feet_up`) | Floor | **no** | | | | Nobody lying on the stomach with the feet up |
| Lean back & relax (`floor_lean_back`) | Floor | yes | Commons | CC0 | 0.83 |  |
| Overhead dreamer (`floor_overhead_lie`) | Floor | **no** | | | | No overhead shot of someone lying down in the lying categories or the stock imports. |
| Sit on your heels (`floor_sit_on_heels`) | Floor | **no** | | | | 'Seiza' and Women/Men/People kneeling (608 files) are religious, sport, martial-arts and named photos |
| Thoughtful chin rest (`port_chin_rest`) | Portrait | yes | Mixkit | Mixkit Free | 0.87 | flagged (nearest: pro_headshot), kept: portrait and selfie templates need few segments (no legs), so several of ... |
| Face frame (`port_face_frame`) | Portrait | yes | Mixkit | Mixkit Free | 0.86 | flagged (nearest: pro_headshot), kept: portrait and selfie templates need few segments (no legs), so several of ... |
| Fingers through hair (`port_hand_in_hair`) | Portrait | yes | Commons | CC BY 2.0 | 0.63 | flagged (nearest: pro_headshot), kept: portrait and selfie templates need few segments (no legs), so several of ... |
| Shoulder glance (`port_over_shoulder`) | Portrait | yes | Mixkit | Mixkit Free | 0.55 |  |
| Crossed-arm smile (`port_arms_crossed_smile`) | Portrait | yes | Mixkit | Mixkit Free | 0.97 | flagged (nearest: pro_headshot), kept: portrait and selfie templates need few segments (no legs), so several of ... |
| Café chin rest (`port_chin_on_hand`) | Portrait | yes | Commons | CC0 | 0.96 | flagged (nearest: pro_headshot), kept: portrait and selfie templates need few segments (no legs), so several of ... |
| Dreamy gaze (`port_dreamy_gaze`) | Portrait | yes | Mixkit | Mixkit Free | 0.98 |  |
| Collar adjust (`port_collar_adjust`) | Portrait | yes | Mixkit | Mixkit Free | 0.86 | flagged (nearest: pro_headshot), kept: portrait and selfie templates need few segments (no legs), so several of ... |
| Shoulder touch (`port_shoulder_touch`) | Portrait | **no** | | | | removed at the review: the Commons file is in the category 'Seduction (facial expression)' |
| Classic arm selfie (`selfie_classic`) | Selfie | **no** | | | | removed at the second look: three people taking a selfie in a car (the review's replacement, Mixkit 40038 at 0.367 s), while the pose ... |
| Peace sign (`selfie_peace`) | Selfie | yes | Mixkit | Mixkit Free | 0.92 | flagged (nearest: pro_headshot), kept: portrait and selfie templates need few segments (no legs), so several of ... |
| High-angle selfie (`selfie_high_angle`) | Selfie | **no** | | | | removed at the review: same uploader and licence doubt as couple_walk_toward (Shixart1985, 'Own work', CC BY 2.0, ... |
| Cheek rest selfie (`selfie_hand_on_cheek`) | Selfie | **no** | | | | 'Woman in Eyeglasses (5)' (PxHere, CC0) rests her cheek on a hand, but its categories name the model ... |
| Mirror selfie (`selfie_mirror`) | Selfie | yes | Commons | CC0 | 0.52 | flagged (nearest: pro_folder_hold), kept: a full-length mirror selfie |
| Two-hand mirror (`selfie_mirror_two_hands`) | Selfie | **no** | | | | removed at the review: the man wears tight black shorts that read as boxer briefs, barefoot in a kitchen (the rules exclude ... |
| Sky gaze selfie (`selfie_look_up`) | Selfie | **no** | | | | No selfie looking up past the phone in the selfie categories or the stock imports. |
| Lean-in selfie (`selfie_lean_in`) | Selfie | **no** | | | | No lean toward a phone with a hand on the hip |
| LinkedIn headshot (`pro_headshot`) | Professional | yes | Mixkit | Mixkit Free | 0.00 | flagged (low score, nearest: selfie_look_up), kept: a textbook headshot (suit, smile, eyes on the lens) |
| Confident crossed arms (`pro_arms_crossed`) | Professional | **no** | | | | removed at the review: the file's categories name the model ('Alisa (photo model)'), which the named-person rule excludes (the ... |
| Blazer pocket (`pro_blazer_pocket`) | Professional | **no** | | | | removed at the review: no hand is in a pocket (one hand holds the other forearm), it was a second frame of the clip used for ... |
| Desk-edge perch (`pro_desk_edge`) | Professional | **no** | | | | As lean_table_edge: no desk perch in the desk/table categories or phrases |
| Open-palm speaker (`pro_open_palm`) | Professional | **no** | | | | The Mixkit speaker clips found in round 2 (36791, 36817, 36820, 37043, 45957) are all Restricted |
| Boardroom lean (`pro_table_lean`) | Professional | **no** | | | | Nobody leaning on a table with both hands |
| Cuff adjust (`pro_cuff_adjust`) | Professional | **no** | | | | No cuff adjust in any new candidate (the 'Black woman with women's suit' studio series has crossed arms ... |
| Folder in hand (`pro_folder_hold`) | Professional | **no** | | | | 'People holding clipboards' (69 files) gave workers and events |
| Star jump (`fun_star_jump`) | Fun | **no** | | | | 'Young ladies jump for joy Canberra-1' (CC BY-SA 2.0) has a clean star jump, but in front of Parliament ... |
| Knee-tuck jump (`fun_tuck_jump`) | Fun | **no** | | | | No knee-tuck jump among Women/Men jumping (216 files) or the jump phrases. |
| Heel-flick twirl (`fun_twirl`) | Fun | yes | Mixkit | Mixkit Free | 0.40 | flagged (low score), kept: mid-turn with one arm thrown high and the other out to the side, as drawn |
| Superhero stance (`fun_superhero`) | Fun | **no** | | | | dropped: the only candidate with the stance is a Commons photo of a member of the public at an event ('Man watching ... |
| Disco point (`fun_disco_point`) | Fun | yes | Mixkit | Mixkit Free | 0.95 | flagged (nearest: selfie_high_angle), kept: one arm pointing high, the other hand on the hip, weight on one leg, as ... |
| Laughing bend (`fun_laugh_bend`) | Fun | **no** | | | | Nobody doubled over laughing in Women/Men laughing (828 files) |
| High kick (`fun_high_kick`) | Fun | **no** | | | | No front kick with open arms (martial-arts competition photos only). |
| Double peace lean (`fun_peace_lean`) | Fun | **no** | | | | The peace-sign phrase (91 files) gave named people and events. |
| Victory arms (`fun_victory`) | Fun | **no** | | | | removed at the review: motion blur on the face and hands, and a low-cut top with visible cleavage at the centre of the frame (the ... |
| Side hug (`couple_side_hug`) | Couple, 2 people | **no** | | | | No side-by-side hug: the couple matches are events or families, and the Mixkit couple clips are Restricted ... |
| Forehead touch (`couple_forehead_touch`) | Couple, 2 people | yes | Mixkit | Mixkit Free | 0.31 | flagged (low score, nearest: selfie_classic), kept: foreheads together, eyes closed, hands joined between them, as drawn |
| Back hug (`couple_back_hug`) | Couple, 2 people | yes | Mixkit | Mixkit Free | 0.48 | flagged (low score, 1 of 2 people detected), kept: he hugs her from behind, both smiling at the camera |
| Hand-in-hand walk (`couple_walk_hands`) | Couple, 2 people | yes | Mixkit | Mixkit Free | 0.39 | flagged (low score, nearest: port_over_shoulder), kept: walking hand in hand, free arms swinging |
| Piggyback (`couple_piggyback`) | Couple, 2 people | **no** | | | | dropped: the Mixkit frame ('Portrait of a happy couple on a pink background', waist-up) shows her arms round his ... |
| Romantic dip (`couple_dip`) | Couple, 2 people | **no** | | | | 'Dip (dance move)' (10 files) and 'Tango dancers' are stage performances |
| Face to face (`couple_hold_hands_facing`) | Couple, 2 people | yes | Mixkit | Mixkit Free | 0.73 | flagged (nearest: couple_cheek_kiss), kept: facing each other an arm's length apart holding both hands, as drawn |
| Sitting close (`couple_sit_lean`) | Couple, 2 people | yes | Mixkit | Mixkit Free | 0.46 | flagged (low score, 1 of 2 people detected), kept: on a bench she rests her head on his shoulder, the gesture the pose describes |
| Walk and chat (`couple_walk_toward`) | Couple, 2 people | **no** | | | | removed at the review: the licence cannot be verified |
| Cheek kiss (`couple_cheek_kiss`) | Couple, 2 people | yes | Mixkit | Mixkit Free | 0.01 | flagged (low score, 1 of 2 people detected), kept: she kisses his cheek, both smiling (he looks down, not at the camera) |
| Shoulder line-up (`group_arms_around`) | Group, 3 people | yes | Commons | CC0 | 0.44 | flagged (low score, nearest: group_walk_line), kept: seven women in a line with their arms round each other's shoulders and ... |
| V-formation (`group_v_formation`) | Group, 3 people | **no** | | | | No V-formation: the group matches are events, political groups or archive photos |
| Jump together (`group_jump`) | Group, 3 people | **no** | | | | No group jump among the jump categories and phrases |
| Walking line (`group_walk_line`) | Group, 4 people | **no** | | | | No line of friends walking |
| Sitting row (`group_sitting_row`) | Group, 3 people | **no** | | | | Closest is 'HN007sRamon casaNroute' (three men sitting on steps), but one of them may be a teenager |
| Stacked levels (`group_levels`) | Group, 3 people | **no** | | | | No three head heights in any group candidate. |
| Back to back (`group_back_to_back`) | Group, 3 people | **no** | | | | No three-person back-to-back |

## The 64 gaps and what they need

The free sources reachable here are exhausted for these poses: across the first run and two gap rounds every pose
was searched by name and category, and what is left is either missing (nobody does the pose: pole swing, kerb step,
star jump, group levels), Mixkit Restricted (most walking, office, couple and group clips), or ruled out by the people
rules (named people, events, possible minors, glamour series). The 10 poses emptied by the review and the one emptied
by the second look are in the same state: their remaining candidates were searched again (see the review table). Options, best first:

1. **A Pexels or Unsplash API key** (Pixabay too). These sites hold far more stock-model photos than the old CC0
   copies on Commons, searchable by pose words ("hands in pockets", "leaning on wall", "jumping friends"), and their
   current licences allow free use including in apps, with limits to read before use (no selling unaltered copies, no
   competing stock service, no implied endorsement; neither promises model releases). The same tools would rank the
   results with the app's matcher; only a key and a small fetcher are missing. Expect most of the standing, leaning,
   walking, professional and fun gaps to fill; couples and groups less so.
2. **Pose-conditioned image generation with an API key**: an image-generation service that accepts a pose skeleton
   (each pose's own joints, as exported in `promo/public/poses.json`) as a condition can render a person in exactly
   the drawn pose, in one consistent style, for all 99 poses including the multi-person ones. The images must be
   reviewed like the photos (hands, faces, anatomy, no likeness of real people), labelled as generated in the app, and
   produced under the service's terms for commercial use; the matcher check here applies unchanged.
3. **Photos shot by friends with the app's "Pose from a photo"**: adult friends who agree in writing to their photos
   being shown in the app pose for each missing pose (a phone and an afternoon; couples and groups are easiest this
   way), the photos go through the same check (`PosePhotoMatch`), and with a written release from the people and
   permission from the photographer no third-party credit or licence text is needed. This also gives a consistent
   look and lets each photo match the drawn framing exactly.

Smaller steps: ask Mixkit whether the Restricted clips that match (named in the notes) may be used this way; read the
Envato policies; check the Shixart1985 uploads by hand (uploader talk page, deletion requests, the original files'
EXIF, a reverse image search): if they prove genuine own work, `couple_walk_toward` could return (its records are in
`picks/couple-group.json`); decide whether consenting named models ('Full consent' category) may be shown, which would
bring back `pro_arms_crossed`; replace the weakest kept photos (app score below 0.3: `pro_headshot`,
`couple_cheek_kiss`, `stand_hand_in_hair`, `floor_side_lie`) when one of the options above produces better ones.

## Appendix: how candidates were found and ranked (first run)

1. **Mixkit**: `mixkit_harvest.py crawl` reads up to 4 grid pages of each of 56 people tags (and Mixkit search pages
   for 25 more phrases); `scan` ranks the clips by their text, checks each clip page's licence, downloads the 360p
   preview, runs the app's lite pose model on 3 frames a second and keeps up to 8 frames per clip where a person shows
   at least head, shoulders and hips, holds preferred; a second `--focus` run fills weak categories (couples, groups,
   sitting, floor).
2. **Commons**: `commons_harvest.py search` runs one or two phrases per pose (its name and category words) and broad
   "pexels" phrases per category, filters by licence and the people rules, `download` fetches ~1200 px renditions.
3. `detect_images.py` runs the app's full pose model (up to 4 people) on every kept frame and photo.
4. `score.py` ranks every image against every pose with a Python port of the app's PoseMatcher (identical to the
   Kotlin to 4 decimals on the promo film's nine chorus shots), trying the flipped pose too, with the guide dragged and
   pinched onto the person as a user would; it records the people count and how well the photo's framing fits the
   pose.
5. `sheets.py` draws a contact sheet per pose: the figure next to its top 8 candidates with score, source and licence.

The selection stage looks at every sheet, rejects anything that breaks the people rules, prefers holds at a good size,
and writes the credits.

### Harvest run, 2026-11-21 (first run)

| | |
| --- | --- |
| Mixkit clips listed (grid and search pages read) | 3,146 |
| Clip pages checked for the licence | 576: 510 Free License, 66 Restricted (skipped) |
| Clips scanned (360p preview, lite model, 3 fps) | 510 (420 best by text, then 90 for weak categories); previews deleted after the scan |
| Frames kept | 3,043 from 484 clips (622 holds, the rest "moving") |
| Commons searches | 134: one or two phrases for **every one of the 99 poses** (117), 15 broad "pexels" phrases, 2 others |
| Commons files read (licence metadata) | 1,927; 212 pass the licence and people rules (CC0 57, PD 16, CC BY 2.0-4.0 64, CC BY-SA 2.0-4.0 75; 10 carry the "personality" restriction flag) |
| Commons photos downloaded and scored | 196 |
| Images scored against every pose | 3,239 (3,043 Mixkit frames + 196 Commons photos) |

Best **eligible** candidate per pose (right people count, framing fits, tcov >= 0.9), app matcher score:

| Category | Poses | best > 0.7 | best > 0.6 | mean best |
| --- | --- | --- | --- | --- |
| Standing | 10 | 9 | 10 | 0.88 |
| Sitting | 10 | 1 | 2 | 0.55 |
| Leaning | 9 | 7 | 7 | 0.78 |
| Walking | 10 | 7 | 9 | 0.83 |
| On the ground | 9 | 1 | 2 | 0.54 |
| Portrait | 9 | 8 | 9 | 0.92 |
| Selfie | 8 | 8 | 8 | 0.91 (inflated, see below) |
| Professional | 8 | 7 | 8 | 0.91 |
| Fun & jumping | 9 | 5 | 5 | 0.65 |
| Couple | 10 | 1 | 3 | 0.52 |
| Group | 7 | 0 | 0 | 0 (no eligible photo) |
| **All** | **99** | **54** | **63** | |

What this meant for the selection stage:

- **Groups have nothing yet**: only 18 frames show three or more people, and none fits a group pose with full bodies.
  Couples, sitting, on-the-ground and jumping poses are thin too. Real photos of these need more sources (or staged
  photos of the owner's friends, or a different illustration).
- **Selfie and portrait scores are inflated.** Those templates require few segments (the phone arm leaves the frame),
  so any waist-up frame with the arms down scores 0.9-1.0 while real selfies score 0.5-0.7. The contact sheets keep
  three places for candidates whose title or description names the pose ("T").
- Walking poses are often matched by people standing still in a similar stance; check the text and the frame.
- Mixkit candidates are 360p preview frames (crops of 200-600 px); the release frame must be re-cut from the clip's
  licensed 720p/1080p download at the recorded time.
- The text rules miss things (e.g. a Commons swimwear photo whose title said only "nearby the pool" got through until
  categories were checked with plurals); every chosen photo needs a look for age, clothing, context, watermarks and
  public figures. Mixkit flags "check age" on titles with "girl" or "young".
