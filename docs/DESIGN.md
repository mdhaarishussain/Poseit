# Poseit design system v2

Poseit is a free, offline pose guide for people who freeze in front of a camera: pick a pose, line up with a figure,
get the shot. v2 replaces "Studio" (grey paper, ink, Archivo).

**How it is built.** v2 follows the token and component model of [Astryx](https://github.com/meta/astryx), Meta's
open-source (MIT) design system, implemented natively in Jetpack Compose. Astryx itself is React + StyleX for the
web, and its native package is an experimental canary with only a SwiftUI binding, so none of its code runs in this
app. What we take is its model:

- **Semantic tokens.** Components read roles (`backgroundCard`, `textSecondary`, `accent`, `onAccent`, `border`,
  `track`), never raw colours. Role names follow Astryx's colour grammar (`background<Surface>`, `text<Emphasis>`,
  `on<Fill>`, the `Inverted` suffix for content on an inverted surface).
- **A theme is a set of values for those roles.** `PoseitPalette` holds the raw values; `PoseitColors` maps them
  onto roles. Poseit has one theme, and its values come from the mockups the redesign is based on.
- **Astryx's component inventory and anatomy**, mapped to Compose: Button variants (primary, secondary, ghost =
  `TertiaryButton`, destructive, plus Poseit's accent), IconButton, SegmentedControl, Card and ClickableCard, Badge,
  ProgressBar, Item and List (rows), Section, BottomSheet, AlertDialog, Toast (snackbar), Banner, EmptyState,
  MobileNav (the floating pill bar), Indicator (switch, radio).
- **Its rules:** one primary emphasis per area, radius follows role and nested curves stay concentric, status never
  by colour alone, 4.5:1 for text and 3:1 for essential non-text, Astryx's standard and exit easing curves, and its
  Android icon mapping to Material Symbols names.

Astryx's MIT notice ships in `app/src/main/assets/licenses/MIT-Astryx.txt` and in Settings, Open-source notices.

**The look, in one paragraph.** A warm off-white canvas. Deep green-black cards with large radii and cream text,
stacked 8dp apart and, where they belong together, **fused by small concave necks**: the canvas shows only as a thin
slot cut in from each side. One soft lavender accent, always with dark text on it, for the badge, filled progress,
the selected option of a pill switch, and the main call to action. Cream cards with a 1dp dark outline for list rows.
A friendly neo-grotesk whose big words and numbers are Light and tracked tight, and whose tiny controls are SemiBold
capitals tracked open. White lay figures stand on the dark cards. Two colours plus one accent; no gradients, no
shadows, few icons.

Code: `app/src/main/java/com/poseit/app/ui/theme` (tokens) and `ui/components` (components). The component gallery
renders everything into `app/build/screenshots/components.png` (`screens/ComponentGalleryScreenshotTest`).

## Colour

### Palette (the theme's values)

| Palette | Hex | Role it backs |
|---|---|---|
| Cream50 | `#FBFAF7` | `backgroundSurface`, `figureFill` |
| Cream100 | `#F4F3EF` | `backgroundBody` (canvas), `textPrimaryInverted` |
| Cream200 | `#E7E7E2` | `backgroundMuted` |
| Fog | `#D6D9D6` | `textNumeralPale` |
| Teal950 | `#0A2125` | `backgroundCard`, `backgroundStage`, `textPrimary`, `borderEmphasized`, `onAccent` |
| Teal900 | `#132D31` | `backgroundCardRaised` |
| Teal800 | `#1E383D` | `trackInverted` |
| Slate600 | `#586563` | `textSecondary` |
| Mist300 | `#AEB7B5` | `textSecondaryInverted`, `borderEmphasizedInverted` |
| Lavender200 | `#CFC9F8` | `accent` |
| Lavender100 | `#E6E2FC` | `accentMuted` |
| Red600 / Amber700 / Green700 | `#B8382A` / `#9A6408` / `#1F7A50` | `signalMiss` / `signalNear` / `signalMatch` |
| Red300 / Amber300 / Green300 | `#FF7B6B` / `#FFC857` / `#63DB9E` | the `Bright` signals (camera, dark cards) |
| FigureGrey1-3 | `#CFD6D4` `#A3AFAD` `#7D8B89` | second to fourth person in a multi-person pose |
| Black | `#000000` | `backgroundCamera` |

### Roles (`PoseitColors`)

Surfaces: `backgroundBody` (canvas), `backgroundSurface` (sheets, dialogs, menus, outlined rows, fields),
`backgroundCard` (dark cards, the top bar, connected stacks, pose tiles), `backgroundCardRaised` (a tile or pressed
area inside a dark card; the segmented pill's track on a dark card), `backgroundMuted` (placeholders, pale fills),
`backgroundStage` (where figures stand: the dark card colour), `backgroundCamera`, `overlay` (scrim, card ink at 40%).

Content on cream: `textPrimary`, `textSecondary`, `textDisabled` (ink 38%), `textNumeralPale`. Content on a dark card:
`textPrimaryInverted`, `textSecondaryInverted`, `textDisabledInverted`. Accent: `accent`, `onAccent`, `accentMuted`.
Edges: `borderEmphasized` (1dp outlines on cream), `border` (hairlines, ink 12%), `borderEmphasizedInverted`,
`borderInverted`, `track`, `trackInverted`. Signals: `signalMiss`, `signalNear`, `signalMatch`, their `Bright`
variants, `onSignal`, and the Astryx intent aliases `error` / `errorInverted`. Figures: `figureFill`,
`figureContour` (ink 55%), `figureFace`, `figureFloorOnDark` (cream 10%), `figureFloorOnLight` (ink 8%),
`figureTones`. Camera: `cameraControlScrim` (black 45%), `cameraControlActive`, `cameraContent`.

**Surface tone.** Every dark container (dark cards, `ConnectedCardStack`, `TopBarCard`, `PoseTile`, the snackbar)
provides `LocalSurfaceTone = Dark` and cream as `LocalContentColor`; cream containers provide `Light`. Shared
components read it, so a `SecondaryButton` inside a dark card gets a mist outline and cream capitals by itself.
`toneColors()` returns the matching set (`PoseitColors.onLight` / `onDark`) for custom drawing, and
`ProvideSurfaceTone(SurfaceTone.Dark)` marks a custom dark area (the camera HUD, a full-bleed dark screen).

### Contrast (WCAG 2.x)

| Foreground | On | Ratio | Use |
|---|---|---|---|
| textPrimary `#0A2125` | canvas / surface / muted | 15.0 / 16.0 / 13.5 | all text on cream |
| textPrimary | accent / accentMuted | 10.6 / 13.2 | content on lavender |
| textSecondary `#586563` | canvas / surface / muted | 5.5 / 5.8 / 4.9 | secondary text on cream |
| textPrimaryInverted `#F4F3EF` | card / cardRaised / camera | 15.0 / 13.1 / 18.9 | text on dark |
| textSecondaryInverted `#AEB7B5` | card / cardRaised | 8.1 / 7.1 | secondary text on dark |
| accent `#CFC9F8` | card / cardRaised | 10.6 / 9.2 | lavender text or glyph on dark (snackbar action) |
| accent | canvas | 1.4 | **never** as text or a lone edge on cream |
| signalMiss / Near / Match | canvas | 5.2 / 4.5 / 4.8 | signal text and marks on cream |
| onSignal `#FBFAF7` | signalMiss | 5.5 | filled destructive button |
| Miss / Near / Match Bright | card | 6.6 / 10.9 / 9.7 | signals on dark cards |
| Miss / Near / Match Bright | black | 8.3 / 13.7 / 12.2 | signals on the camera |
| borderEmphasized | canvas | 15.0 | outlined rows, outlined buttons, filter pills |
| borderEmphasizedInverted | card | 8.1 | outlined buttons on dark |
| accent filled segment | trackInverted | 7.9 | segmented progress on dark |
| figureFill | stage | 16.0 | the figure on a dark tile |
| figureContour (ink 55%) | canvas | 3.7 | the edge that defines a figure on cream |
| darkest figure tone `#7D8B89` | stage | 4.7 | fourth person on a dark tile |
| cameraContent | black 45% over a white scene | 3.2 | camera glyphs, worst case |
| textNumeralPale `#D6D9D6` | canvas | 1.3 | **display only**: huge decorative numerals whose value is also readable elsewhere |

Disabled controls are exempt (38% content). The accent never carries meaning alone: a selected segment is also
announced as selected, and its label changes colour.

Not allowed: more hues than these (no blue links, no per-category colours); gradients, glows and shadows; lavender
text on cream; signal colours as decoration; colour as the only carrier of meaning.

## Type

One family, **Hanken Grotesk** (SIL OFL 1.1), bundled as static Latin subsets in `res/font`: Light 300, Regular
400, Medium 500, SemiBold 600. Licence: `app/src/main/assets/licenses/OFL-HankenGrotesk.txt`. Its figures are
tabular by default (numerals also set `tnum`).

| Role (`PoseitType`) | Weight, size/line, tracking | Use |
|---|---|---|
| `display` | Light 34/38, -0.8sp | Onboarding and explainer headlines ("Pick a pose to copy."), the pose name on detail |
| `headline` | Regular 26/32, -0.5sp | A card's headline (Today's pose name), large page titles |
| `title` | Regular 20/26, -0.3sp | Screen titles in the top bar card, sheet and dialog titles, card titles |
| `wordmark` | Regular 22/28, -0.6sp | "Poseit" in the home top bar card only |
| `heading` | Medium 16/22, -0.1sp | Section and group headings on the canvas or in sheets, empty-state titles in lists |
| `bodyLarge` | Regular 17/24 | A card's label ("Today's pose", "Best match"), outlined row titles |
| `body` | Regular 15/22 | Paragraphs, descriptions, tips |
| `bodyStrong` | Medium 15/22 | Setting row titles, the pose name on a tile |
| `supporting` | Regular 13/18 | Secondary lines ("Standing, easy"), helper text, row subtitles, captions |
| `label` | Medium 14/20 | Sentence-case controls: text buttons, menu items, filter pills |
| `caps` | SemiBold 12/16, +0.8sp, CAPITALS | Pill buttons, badges, segmented options and tabs, and nothing else |
| `numeralDisplay` | Light 72/72, -2% | The capture countdown, a timer |
| `numeralLarge` | Light 44/48, -2% | A headline number ("87%") |
| `numeralMedium` | Regular 26/30, -2% | A stat value |
| `numeral(size)` | Light from 28sp, Regular below, -2% | Any other number |

Rules:
- The voice is in weight and tracking: big text Light or Regular and tight, tiny controls SemiBold capitals and
  open. Never bold a headline; the heaviest weight in the app is SemiBold at 12sp.
- Sentence case everywhere in code and copy. The capitals are display-only: `CapsLabel` uppercases for drawing and
  gives screen readers and tests the sentence-case words. Numbers and a unit after a number keep their case
  ("3 s"). Never type capitals into a string.
- Display, headline, title and heading balance their lines (`LineBreak.Heading`).
- Never accent one word of a headline with weight, italics or colour. A card may set a two-line headline in one
  role only.
- Don't join metadata with middle dots. Write a short phrase ("Standing, easy").
- Keep lines under about 60 characters: body text sits inside cards (24dp padding) or on the 32dp text inset.

## Shape, space, depth

**Radii by role** (`PoseitRadius`, `PoseitShapes`); nested curves stay concentric (an element inside a card is
about the card padding less round):

| Role | Radius | Use |
|---|---|---|
| `inner` | 8dp | Badges, icon tiles and thumbnails inside a card |
| `control` | 12dp | Block buttons inside cards, text fields, the camera cue card, compact pose tiles |
| `row` | 16dp | Outlined rows, row groups, menus, snackbars, banners |
| `tile` | 20dp | Pose tiles and photo thumbnails in a grid |
| `card` | 24dp | Dark cards, connected stacks, the top bar card |
| `sheet` | 28dp | Bottom sheet top corners, dialogs, a full-screen onboarding card |
| `Pill` / `Circle` | full | Pill buttons, filter pills, segmented pills, circle icon buttons, the shutter, the score ring |

**Spacing** (`Space`, a 4dp grid; Astryx spacing steps 1 to 12):
- `edge` 8dp: cards to the screen sides, and the gap between separate cards (joined cards sit 6dp apart). The cream frame of 8dp
  around nearly full-width cards is part of the look.
- `cardPadding` 24dp inside dark cards and the top bar card.
- `textInset` 32dp (= edge + cardPadding): text placed directly on the canvas (section headings, "99 poses",
  empty states) starts here, in line with text inside cards.
- `section` 24dp between groups on the canvas. Other steps: `xs` 4, `s` 8, `m` 12, `l` 16, `xl` 24, `xxl` 48.
- `Space.gutter` (20dp) is the deprecated v1 value; don't use it in new layouts.

**Depth:** none. No shadows, no elevation, no tonal tint. Things separate by colour (dark card vs cream canvas vs
cream surface) and by the 8dp gaps. The only "floor" is the flat oval floor spot under a figure's feet.

## Connected cards (the motif)

Dark cards stacked a 6dp slot apart and fused into one body, so the canvas shows only as a thin slot cut in from
each side of every join (`ConnectedCardStack`, `ConnectedCardRow`, `ConnectedCardShape`,
`connectedCardsPath`):

```
 ╭──────────────────────────╮
 │  top card                │
 ╰──╮                    ╭──╯   join corners: 20dp radius, flaring into the slot
════○                    ○════  slot: 6dp tall, runs 22dp past the join corners, round tip of radius 3dp
 ╭──╯                    ╰──╮
 │  next card               │
 ╰──────────────────────────╯   outer corners: 24dp
```

Geometry (`ConnectedCardDefaults`): outer `Radius` 24dp, `JoinRadius` 20dp, `Gap` 6dp (the slot's thickness, a
little thinner than the 8dp gap between separate cards, so a join reads as a seam), the slot runs `NotchLength`
22dp straight past the join corners and ends in a half-circle of radius Gap/2. So a notch reaches 45dp in from each
side; between the two tips the cards are one piece. Measured from the mockups: slot about 5 to 7dp, flare about
20dp, straight run about 22dp. A row (`ConnectedCardRow`) is the
same turned 90 degrees: slots come in from the top and bottom.

When cards connect:
- The top bar card and the card it introduces (home: Today's pose; detail: the preview; onboarding: the headline
  card). Use `TopBarCard(joined = { ... })`.
- Parts of one subject: a pose's preview and its facts; a stat pair; the stages of one flow.

When they stand apart (a plain 8dp gap): cards about different things; anything scrolling in a list or grid (pose
tiles never connect); outlined cream cards and rows (never connect); anything that moves independently.

At most one connected stack per screen region; two or three cards per stack, four at most.

## Icons

Material Symbols Outlined names, as Astryx maps its icons on Android, drawn with the Outlined Material Icons from
androidx (the same outlined family, already bundled). Use the `PoseitIcons` vocabulary (`close`, `chevronRight`,
`chevronDown`, `check`, `success`, `error`, `warning`, `info`, `moreHorizontal`, `search`, `back`, `camera`,
`photos`, `settings`, `add`, `favouriteOff`, `favouriteOn`, `edit`, `delete`, `share`, `person`).

Sizes (`IconSize`): 24dp standalone in a 48dp target, 18dp inline with a label. Outlined only; the one filled glyph
is the favourite heart when on. Few icons: one per control, none as decoration, no icon tiles in rows.

## Motion

`PoseitMotion`: 150ms (colour and selection), 200ms (the segmented pill's selection sliding, small moves), 250ms
(a card expanding, progress filling). Easing: Astryx's standard curve `cubic-bezier(0.24, 1, 0.4, 1)` for arrivals
and state changes, its exit curve `cubic-bezier(0.3, 0, 0.6, 0.6)` for things leaving. Motion answers the user's
action only: no looping decoration, no staggered entrances. The capture countdown and flash stay the one
orchestrated moment, with their own timing.

## Components (`ui/components`)

All take a `modifier`, keep their accessibility semantics, and pick colours from the surface tone.

- **Cards** (`Surfaces.kt`): `PoseitCard(variant = Dark | Outlined | Accent | Muted, onClick?, onClickLabel?)`.
  Dark is the default and the one the mockups are built from; Outlined is cream with a 1dp dark outline and 16dp
  corners; Accent (lavender, dark content) at most once per screen; Muted for placeholders. `onClick` makes the
  whole card a button (Astryx ClickableCard). `ProvideSurfaceTone`, `CapsLabel`.
- **Connected cards** (`ConnectedCards.kt`): `ConnectedCardStack { ... }` and `ConnectedCardRow { ... }`: every
  direct child is one card; use `CardSegment(onClick?, contentPadding)` for padded, optionally clickable cards.
- **Buttons** (`Buttons.kt`): pills at least 48dp tall with `caps` labels; `form = ButtonForm.Block` (12dp corners)
  for compact buttons inside cards, like the mockups' START.
  - `AccentButton`: lavender, dark capitals. The one thing to do next ("Try this pose", "Get started"). One per
    screen.
  - `PrimaryButton`: dark pill on cream, cream pill on a dark card. The main action when there is no accent.
  - `SecondaryButton`: 1dp outlined pill. The alternative, beside the main action (the mockups' RESTART / PAUSE).
  - `TertiaryButton`: text only, sentence case (Astryx ghost): "Cancel", "Skip", "Show all".
  - `DestructiveButton`: red sentence-case text, or `filled = true` for a red pill confirming a delete.
- **Icon buttons** (`IconButtons.kt`): `CircleIconButton(variant = Dark | Light | Accent | Outlined | Camera, size)`
  (48dp; 52dp in the pill bar), `CircleIconToggle` (camera toggles: scrim disc, cream disc when on), `PlainIconButton`
  (bare glyph, top bar actions, the tile heart), `IconToggle` (view options such as Mirror: a 40dp disc while on).
- **Segmented pill** (`SegmentedPill.kt`): `SegmentedPill(options, selectedIndex, onSelect, size = Medium | Large,
  role = RadioButton | Tab)`: dark track, lavender selected option with dark capitals, cream capitals for the rest,
  equal widths, the selection slides. For two or three options: a value choice (countdown "Off / 3 s / 10 s") or
  views. `PillTabs(labels, selectedIndex, onSelect, edgePadding)`: the same look with options hugging their labels
  and horizontal scrolling, for three to five tabs (All / Favourites / Recent / My poses). Tabs and segmented
  options are the same thing in v2; there are no underline tabs.
- **Badge** (`Badge.kt`): `Badge(text, tone = Accent | Dark | Light, icon?)`: 24dp tag, 8dp corners, capitals. The
  top bar's count ("99 poses"), "New", "2 people". Not interactive.
- **Filter pills** (`Chips.kt`): `FilterPill(label, selected, onClick, leadingIcon?)`: 36dp pill, sentence-case
  label; outlined when off, filled dark when on (cream on a dark card). Not lavender.
- **Top bar card** (`TopBar.kt`): `TopBarCard(title, onBack?, wordmark, badge?, actions?, joined?)`: a 64dp dark
  card 8dp below the status bar; title in `title` (or the wordmark), cream glyph actions, optional lavender badge,
  and `joined` cards fused below it. `TopBarContent` is its inside, for your own stack.
- **Floating pill bar** (`PillBar.kt`): `FloatingPillBar { CircleIconButton(size = PillBarItemSize);
  PillBarSegments(options, ...); CircleIconButton(...) }`: separate pills on a canvas band above the navigation
  bar; content scrolls under the band, never through it.
- **Stats** (`Stats.kt`): `Stat(label, value, caption?)` (label in bodyLarge, caption supporting, value in
  numerals; one accessibility node), `StatRow { CompactStat(label, value) ... }` for two or three in one card.
- **Progress** (`Progress.kt`): `SegmentedProgress(progress, segments, contentDescription?)`: narrow rounded bars,
  lavender when done, the surface's track colour otherwise; reports a progress range. `PoseitSpinner`.
- **Rows** (`Rows.kt`): `OutlinedRow(title, onClick, subtitle?)`: a single cream row card with a chevron (a place
  to go). `RowGroup { ... }`: one outlined card of rows with inset hairlines between them, for settings. Rows:
  `SettingRow`, `SwitchRow`, `SliderRow`, `RadioRow`, `RowChevron`. At least 56dp; title `bodyStrong`, subtitle
  `supporting` secondary. `poseitSwitchColors()` (on: dark track, lavender thumb), `poseitSliderColors()`.
- **Slider** (`Slider.kt`): `ValueSlider`: 6dp track (dark filled, track colour after), 22dp lavender thumb ringed
  in ink; cream filled track on dark surfaces.
- **Feedback** (`Feedback.kt`): `PoseitSnackbarHost` (a small dark card, cream text, lavender sentence-case
  action, hairline edge for the camera), `PoseitAlertDialog` / `DialogCard` (cream, 28dp corners, `title` heading,
  body secondary, pill confirm + text dismiss), `PoseitBottomSheet` (cream, 28dp top corners, `SheetHandle`),
  `Banner(message, title?, status = Info | Error, action?)`.
- **Menu** (`Menu.kt`): `PoseitMenu`, `PoseitMenuItem`, `PoseitMenuDivider`: cream, 16dp corners, hairline edge,
  `label` items, a check for the chosen option.
- **Sections** (`Sections.kt`): `SectionHeading` (heading role, on `textInset`), `HairlineDivider`,
  `VerticalHairline`.
- **Empty state** (`EmptyState.kt`): left-aligned on `textInset`: `title` role title, one sentence, one button.
- **Pose tile** (`PoseTile.kt`): Regular: a dark card (20dp) with the figure in a 3:4 area and the caption inside
  (name `bodyStrong` cream, category `supporting` mist), the cream heart top-right. Compact (camera strip): the
  3:4 tile alone with 12dp corners, a lavender 2dp outline when selected, one optional line below.
- **Figures** (`PoseFigure.kt`): `PoseThumbnail` defaults to the dark stage with flat floor spots; pass
  `background = null, floorColor = PoseitColors.figureFloorOnLight` on cream. `FigurePaint` defaults (cream fill,
  ink 55% contour, ink face dot) read on both. Multi-person poses take `figureTones` in order.
- **Frame marks** (`FrameMarks.kt`): 1.5dp, 12dp brackets in cream by default (they sit on the dark stage); pass
  `textPrimary` on cream.
- **Icons** (`Icons.kt`): `PoseitIcons`, `IconSize`, `IconGlyphInset`.
- **System bars:** dark icons on cream screens (the app default); call `LightSystemBarIconsEffect()` on a screen
  whose top and bottom edges are dark (photo viewer, a full-bleed dark onboarding). The camera keeps its own
  effect, and `LightSystemBarIcons { }` for sheets opened over it.

**v1 names.** `Studio`, `StudioType`, `StudioShapes`, `StudioIconButton`, `StudioIconToggle`, `StudioChip`,
`StudioTabs`, `StudioTopBar`, `StudioLargeTopBar`, `StudioSlider`, `StudioSnackbarHost`, `StudioAlertDialog`,
`StudioMenu*`, `studioSwitchColors`, `studioSliderColors`, `StudioIconSize`, `StudioInlineIconSize`,
`StudioButtonHeight` and `Space.gutter` are deprecated aliases onto v2 so the app builds during the restyle.
`Studio.Set` maps to the light `backgroundMuted` (not the dark stage) so nothing drawn on it in ink vanishes.
Replace them all; none should remain when the restyle is done.

## Screen map

How the mockups translate to Poseit's screens. Behaviour, navigation, callbacks and accessibility labels stay as
they are; where a label changes, its tests change with it.

**Home / library** (`library/`)
- `TopBarCard("Poseit", wordmark = true, badge = { Badge("<n> poses") }, actions = { PlainIconButton(settings,
  "Settings") })`, **joined** to the Today's pose card: label "Today's pose" (`supporting`, mist), the pose name in
  `headline`, the detail line ("Walking, easy") in `supporting`, an `AccentButton("Try this pose")` at the foot,
  and the white figure on the stage filling the right side of the card. The whole card can open the pose
  (`CardSegment(onClick, onClickLabel)`); the button opens the camera.
- Below, on the canvas: the search field (cream surface, 16dp corners, 1dp `border`), then `PillTabs` (All /
  Favourites / Recent / My poses) and one scrolling row of `FilterPill`s (people, category, place, mood groups
  split by short `VerticalHairline`s), both starting on `Space.edge`.
- While a tab or a filter narrows the list, the count ("12 poses") in `supporting` on `textInset` with
  `TertiaryButton("Clear filters")` while filters are on; on the unfiltered list the badge already gives the count.
  Then the grid: `PoseTile`s two columns (more on wide screens), 8dp apart, 8dp from the sides. Each row reserves
  the name lines its longest name needs (measured), so the dark cards of a row end on one line.
- "My poses" starts with a "Create a pose" tile: an outlined cream card with the footprint of a pose tile.
- `FloatingPillBar`: left `CircleIconButton(camera, "Pose camera")`, middle `PillBarSegments(["Poses", "Photos"])`
  with Poses selected (Photos calls `onOpenGallery`; returning shows Poses again), right
  `CircleIconButton(add, "Create a pose")` opening the create sheet (`PoseitBottomSheet`, two `OutlinedRow`s:
  "Pose from a photo", "Draw a pose"). The extended "Pose camera" FAB goes away: tests that wait for the text
  "Pose camera" move to its content description.
- Empty tabs and filters: `EmptyState` inside a dark `PoseitCard`, its one button an `AccentButton` (the Today's
  pose card is never shown with it).
- The status bar sits on a canvas band, so the list scrolls under it, as it does under the pill bar's band.

**Pose detail** (`detail/`)
- `TopBarCard(pose name or "", onBack)` **joined** to the preview card: the figure on the stage inside cream
  `frameMarks`, the Mirror `IconToggle` in the card's corner (lavender disc when on, since it sits on dark).
- The pose name in `display` on `textInset` if it is not in the bar; facts as `Badge(tone = Dark)` or a
  `StatRow` in a dark card ("People 2", "Level Easy", "Shot Full body").
- "How to do it": `SectionHeading` + `NumberedSteps` on the canvas; "Camera setup" in a `RowGroup`.
- Bottom action band on the canvas: `AccentButton("Try this pose")` with `SecondaryButton("Customise")` beside it.
  Delete in the overflow (`PoseitMenu`, destructive item) with `PoseitAlertDialog`.

**Camera** (`camera/`)
- Stays a dark viewfinder on true black. Wrap the HUD in `ProvideSurfaceTone(SurfaceTone.Dark)`.
- Three cards on the black, 8dp from the sides and from each other: a 56dp top bar card (back, then flash, grid,
  the aspect ratio as a small outlined pill, timer, quick settings as cream glyphs; a set timer turns its glyph into
  a cream disc), the viewfinder itself clipped to the `card` radius, and the dock: the pose strip fused to the
  shutter row by the connected-card necks (`CameraDock`). On black the camera's cards take `backgroundCardRaised`
  (1.4:1 against black, so they read as cards); the pose tiles, "All" / "None" tiles and the round side controls
  inside take the stage colour and sit in them as inset tiles.
- Where a short screen pushes the dock over the bottom of the preview it turns translucent (62%), so feet stay
  visible, and below 600dp of height it goes compact (48dp tiles, 64dp shutter). The HUD then lays out only in the
  part of the preview the dock leaves clear, and the tool rail drops below the score badge when centring it would
  collide.
- Controls over the preview: `CircleIconButton(variant = Camera)` and `CircleIconToggle` (cream disc when on; the
  padlock also closes). The shutter stays a circle: cream ring and fill.
- Score: Light numerals with a smaller percent sign inside the ring, on a solid `backgroundCard` disc so the thin
  figures read over any scene; the ring takes the `Bright` signal colour only while a person is detected, else
  cream 60%, and the hold arc runs round it in the same colour. Per-limb colours use the `Bright` signals. The words
  of the hint always say what is wrong, so colour is never alone.
- Cue cards (hint, pose card, notices): cream `backgroundSurface` with the mockups' 1dp ink outline, 12dp corners;
  the hint in `bodyLarge` so it reads at arm's length, with a small signal dot. The photoshoot counter is a cream
  pill.
- Countdown: 120sp Light numerals in cream with an ink contour; the one orchestrated moment.
- Pose strip: `PoseTile(size = Compact, onDark = true)`; selected = lavender outline.
- Sheets (quick settings, pose tips, all poses): `PoseitBottomSheet` in cream with headings on `textInset` and
  `RowGroup`s on `edge`, like Settings. The countdown is a segmented pill (a 48dp private variant whose options carry
  a spoken form, "2 seconds" for "2s"); the guide style is three small stage tiles drawing the pose in each style;
  zoom presets and the photoshoot choices are `FilterPill`s. The tips sheet opens with the pose as a dark card
  (name, facts, camera height and framing as label/value pairs, the figure). Dialogs as `PoseitAlertDialog`.
- Permission screen: the top bar card joined to a headline card ("Let Poseit use the camera") and, when there is
  room, a picture card with a figure in viewfinder marks; `AccentButton("Allow camera")` and
  `TertiaryButton("Not now")` at the foot.

**Settings** (`settings/`)
- The top bar card (`TopBarContent("Settings", onBack)` in a `ConnectedCardStack`) **joined** to the two dark cards
  that decide when the photo is taken: "Match strictness" (the value in `numeralLarge` Light beside a `ValueSlider`,
  like the mockups' "79%" and its bar; the slider announces "80%, balanced") and "Countdown" (a five-option segmented
  pill, "Off / 2 s / 3 s / 5 s / 10 s", spoken as "No countdown", "3 seconds"). The stack scrolls with the list; a
  canvas band covers the status bar.
- Groups (Coaching, Guide, Camera): `SectionHeading` on `textInset`, then a `RowGroup` of `SwitchRow`s and
  `SliderRow`s. The guide style and grid choices are small dark stage tiles (12dp corners) in a radio group; the
  selected one takes a lavender ring 4dp inside its edge and a medium-weight label.
- Your poses: Export and Import as two `OutlinedRow`s; a spinner beside the heading while one runs.
- About: a connected stack of an info card (wordmark, version, "Works offline, with no ads and no account." in
  `headline`) and two dark link rows, "Open-source notices" (a `PoseitAlertDialog` listing every notice, Astryx's
  MIT notice included) and "Show intro again".

**Gallery and photo viewer** (`gallery/`)
- The top bar card ("Your photos", back, `Badge("<n> photos")`) **joined** to a dark card, "Keep the good ones", with
  a Block `AccentButton("Review")` that opens the viewer at the newest photo (the button moves under the words when
  the title would no longer fit on one line beside it). Then a `SectionHeading` per day and the photos as 3:4 tiles
  (most photos are portraits) with `tile` corners, 8dp apart on `Space.edge`. Hidden older photos: a `Banner` with
  "Allow access".
- Empty: the top bar joined to a dark card holding the `EmptyState` ("No photos yet") and
  `AccentButton("Open the pose camera")`.
- Photo viewer (Astryx Lightbox anatomy: media, close button, counter): full-bleed black (`backgroundCamera`) with
  `LightSystemBarIconsEffect()`; no bars across the photo: `CircleIconButton(variant = Camera)` controls and a dark
  position pill ("2 / 12", date) at the top, and at the foot the mockups' outlined-and-filled pill pair, Delete
  (outlined, bright red label) and Keep (cream pill, "Keep and finish" on the last photo). The delete confirmation is
  `PoseitAlertDialog` with a filled `DestructiveButton`.

**Pose from a photo** (`create/`)
- `TopBarCard("Pose from a photo", onBack)` joined to an explainer card (`display`-sized line "Make a pose from a
  photo." is too long for the bar: use `headline` in the card) with `NumberedSteps` in cream; the picked photo in a
  dark card; working state with `SegmentedProgress` or `PoseitSpinner`; the review on the stage; actions
  `AccentButton("Save pose")` + `SecondaryButton`. Errors as `Banner(status = Error)`.

**Editor** (`editor/`)
- `TopBarCard(title, onBack, actions = { save })` (save can be a compact `AccentButton(form = Block)` in the bar's
  actions slot). The canvas is the dark stage in a card with `frameMarks`; joints and handles in cream and
  lavender (selected joint lavender). Tools as a `FloatingPillBar` of `CircleIconToggle`s / a `SegmentedPill`.
  The details sheet is a `PoseitBottomSheet`.

**Onboarding** (`onboarding/`)
- Like the mockups' "Train your coordination." screen: the cream canvas with a connected stack filling the screen:
  a top bar card (wordmark "Poseit" and a step `Badge("1 / 4")`, read as "Page 1 of 4"), joined to the headline
  card (`display` Light, e.g. "Pick a pose to copy", and the body in mist), joined to a tall picture card that takes
  the rest of the height (`onboarding/FillingCardStack`), where the figure demo stands on the stage; the last
  page's camera note sits at its foot in `supporting` mist. Each page is its own stack, so the whole stack slides
  with the pager.
- Below the stack on the canvas: `TertiaryButton("Skip")` and `AccentButton("Next")` taking the rest of the row;
  on the last page `AccentButton("Get started")` full width. Page text stays the same words ("Next", "Get
  started" are looked up by tests).

**Dialogs, snackbars, empty states, menus** everywhere: `PoseitAlertDialog`, `PoseitSnackbarHost` (messages
confirm with the button's own words: "Save pose" leads to "Pose saved"), `EmptyState`, `PoseitMenu`.

## Writing

- Sentence case and plain verbs in all strings; the capitals of pill buttons, badges and segmented options are
  drawn by `CapsLabel`, never typed.
- Name things by what people understand: "Pose camera", "Your photos", "Make a pose from a photo".
- A button says what happens and keeps its name through the flow: "Save pose" leads to "Pose saved". One action has
  one name everywhere: "Try this pose", "Customise", "Draw a pose", "Pose from a photo".
- British spelling: favourite, colour, centre, customise, licence.
- Hints are short sentences: the reason, a full stop, then what to do ("Their head is cut off. Tilt the phone up").
  No exclamation marks, no "X – fragment" labels, no arrows after button text.
- Errors say what happened and what to do, without apologising. Empty screens invite one action.
