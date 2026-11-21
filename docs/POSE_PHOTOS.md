# Sample photos for the pose library (draft)

Every one of the 99 library poses is shown only as the white mannequin figure. The goal is one real photo per pose of
an adult stock model demonstrating it, shown next to the figure. There is no image-generation service here, so the
photos come from free sources whose terms allow use inside a free app. This file records the sources, what their terms
say (quoted, read on 2026-11-21), what we use, what we exclude and why. It is a first draft written by the harvest
stage; the selection stage fills in the chosen photos and credits.

Tools: `tools/pose-photos/` (see its README). Working files (downloads, frames, landmarks, candidates, contact
sheets) live outside git in `/home/user/poseit-photos-work/`.

## Sources at a glance

| Source | Used? | Why |
| --- | --- | --- |
| Mixkit stock videos (mixkit.co), **Free License clips only** | Yes, still frames | Licence allows modifying and distributing the items in commercial and non-commercial projects; no attribution required. See the limits and the open question below. |
| Wikimedia Commons | Yes, photos | Only CC0, Public Domain / PDM, CC BY (any version), CC BY-SA (any version), checked per file from its own metadata. |
| Picjumbo | No | Its terms allow commercial use and templates but say nothing about apps, forbid redistributing "the pictures as a file", and warn that some photos have no model release. Not clearly allowed inside an app, so left out. |
| Pexels, Pixabay, Unsplash (their own APIs) | No | Need API keys this session does not have. (CC0 photos that Pexels users uploaded before Pexels changed its licence, and that were copied to Commons, are used through Commons under CC0.) |
| Openverse | No | Blocked from this environment. |
| Coverr | No | Its licence bars using its content to evaluate AI models; matching photos runs a pose-detection model on them. |
| Mixkit Restricted License clips | No | Personal use only. |

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
- Mixkit is credited on the credits screen even though attribution is not required;
- frames for release come from the clip's licensed download (the 720p/1080p file linked from the clip page), not from
  the grid's hover preview; the harvester only uses previews to find candidates.

### Open question: scripted downloads

Clause 9 forbids "scripts or bots to mass download Items". The harvester does not download items through the download
buttons; it reads public grid and clip pages and fetches the small 360p hover previews that a browser plays on the
grid, one at a time with a gap, only for clips whose title and description suggest a person holding a pose (510 of the
3,146 clips listed on the pages read). That is far from "the entire library", but it is scripted. Before the app ships Mixkit frames, the
owner should decide whether that is acceptable, or ask Mixkit; the final frames themselves should be downloaded from the
clip pages by a person (a handful of clips).

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
- **Appropriate only.** No nudity, lingerie, swimwear, sexualised or "sensual" framing, beds and bathrooms, alcohol,
  smoking, drugs, injury, hospitals, police, protest, war, weapons, religion, crying or sadness, costumes and
  Halloween.
- **No public figures.** Skip files from events, red carpets, politics, sport, concerts, conferences, fashion shows
  (Commons), and files whose categories look like a person's name; Commons titles in scripts the rules cannot read are
  skipped too.
- **Photographs only** (Commons): no paintings, drawings, sculpture, prints, historical or archival photos, renders or
  AI-generated images, mannequins or dolls.
- **No watermarks, logos or overlaid text**; reviewer checks the frame.
- **Never in a bad light**: the model is shown as a capable person demonstrating a pose.

Allowed changes: **crop** and **horizontal mirror** (when the photo matches the flipped pose). Both are recorded in the
credit. Nothing else (no retouching, no compositing).

## Credit record (per chosen photo)

pose id; source; page URL; file URL (Mixkit: the clip's download link and the frame time); author / artist as stated
(Mixkit: "Mixkit", the page names no author); licence name and URL; attribution text to display; changes made (crop
box, mirrored or not); the app matcher score.

## How candidates are found and ranked

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

## Harvest run, 2026-11-21

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

What this means for the selection stage:

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
