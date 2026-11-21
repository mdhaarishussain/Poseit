# Poseit design system: "Studio"

Poseit is a pose guide for people who freeze in front of a camera. They pick a pose, line up with a figure and get the
shot. The design borrows from the place where posing happens: a **photo studio**. The app screens are the studio
floor, a cool grey seamless-paper backdrop, and the poses are white **lay figures** (artists' mannequins) standing on
that paper. The camera is the dark live view. Chrome is quiet ink on grey, so the figures and the photo carry the
screen.

**The one bold element** is the pairing of white mannequins on studio grey with pose names set in the wide cut of
Archivo, which recalls the lettering engraved on camera bodies. Everything else stays quiet and disciplined.

## Colour

| Token | Hex | Use |
|---|---|---|
| `Studio.Paper` | `#E4E5E1` | App background (the seamless backdrop) |
| `Studio.Set` | `#D2D3CF` | The patch of paper a figure stands on: pose tiles, previews, editor canvas |
| `Studio.Sheet` | `#F3F3F0` | Raised surfaces: bottom sheets, dialogs, menus, text fields, snackbars |
| `Studio.Ink` | `#17181A` | Text, icons, primary buttons, selected states |
| `Studio.Graphite` | `#5A5C5B` | Secondary text, inactive icons, helper text (4.5:1 or better on Paper) |
| `Studio.Hairline` | Ink at 12% alpha | Rules that encode grouping (settings groups, tab underline track, outlines) |
| `Studio.Mannequin` | `#FBFBF9` | Figure fill. Contour is Ink at 55%, the face dot is Ink |
| `Studio.LiveView` | `#000000` | Camera surround. True black, because it is a viewfinder |

**Signal colours** are functional, never decorative. They appear only for the live match score, the per-limb
feedback, and destructive actions: `Miss #C8372D`, `Near #D9930D`, `Match #24935A`. Brighter variants for use on
the dark camera are `#FF6B5E`, `#FFC24A` and `#4FD18B`.

Not allowed:
- purple or violet, or any brand "accent" colour;
- gradients, glows and radial washes;
- tinted near-black backgrounds;
- per-setting coloured icon tiles;
- colour as the only carrier of meaning: pair it with text or a shape.

Multi-person figures differ by tone: Mannequin, `#C9CBC6`, `#A6A8A3`, `#83857F`, each with the same Ink contour.

## Type

There is one family, **Archivo** (OFL, bundled in `res/font`), and its width axis does the expressive work.

| Role | Cut | Size / line height | Use |
|---|---|---|---|
| Display | Expanded SemiBold | 28/32, tracking -0.2sp | Pose name on detail, onboarding headline, wordmark (22/28) |
| Title | Expanded Medium | 20/26 | Section headings, sheet titles, screen titles in top bars (18/24) |
| Body | Regular | 15/22 | Paragraphs, descriptions, tips |
| Body strong | Medium | 15/22 | Row titles, the name under a pose tile |
| Label | Medium | 13/18 | Buttons, chips, tabs, captions, helper text |
| Numeral | Expanded Bold, `tnum` | 15 to 96 | Match score, countdown, counts. Tabular figures; not monospace |

Rules:
- Sentence case everywhere. No ALL-CAPS labels and no tracked-out eyebrows.
- Never accent one word in a headline with italic, bold or colour.
- Don't join metadata with middle dots ("A · B · C"). Show separate items with spacing, or write a short phrase
  ("Standing, easy").
- Don't append "→" to button text. A button's label says what happens: "Try this pose", "Save pose", "Delete".
- Keep lines under about 60 characters on phones: body text sits inside the 20dp gutters.

## Shape, space, depth

- **Radii (only these):**
  - `Small` 6dp: chips, text fields, small buttons, tags;
  - `Medium` 12dp: pose tiles, buttons, dialogs, cards;
  - `Large` 20dp: top corners of bottom sheets;
  - circles only where the camera vernacular calls for them: shutter, round camera controls, the score ring.
- **Spacing:** a 4dp grid. Use 4, 8, 12, 16, 20, 24, 32 and 48. The screen gutter is 20dp, and the gap between
  sections is 32dp.
- **Depth:** no shadows and no elevation tint. Separate things by tone (Paper vs Set vs Sheet) and by space. The
  only shadow is the soft oval contact shadow under a mannequin's feet, because it puts the figure on the floor.
- **Icons:** Material Icons **Outlined**, 22dp, Ink (inactive: Graphite). Use filled icons only for an active
  toggle (a filled heart for a favourite).
- **Motion:** respond to the user's action only, at 150 to 220ms with standard easing. No looping decoration and no
  staggered entrances. The one orchestrated moment is the capture flash and countdown.

## Components

- **Buttons:**
  - Primary: Ink fill with Sheet text, 48dp tall, Medium radius.
  - Secondary: 1dp Hairline outline with Ink text.
  - Tertiary: text only, Ink.
  - Destructive: Miss text, or a Miss fill when confirming.
  - One primary per screen.
- **Tabs:** text tabs. The selected one is Ink with a 2dp Ink underline; the others are Graphite. No pill backgrounds.
- **Chips (filters):** Small radius, 32dp tall, 1dp Hairline outline with Ink label. Selected chips are Ink-filled
  with a Sheet label. No emoji and no leading icons, except the people count, which may use a person outline icon.
- **Pose tile:**
  - Set-coloured rectangle with Medium radius, holding the figure plus its contact shadow.
  - Name below the tile on Paper in Body strong; the category in Label, Graphite.
  - No container around the caption, no shadow and no gradient.
  - The favourite toggle is an outlined heart in the tile's top-right corner.
- **Frame marks:** thin Ink corner brackets (1.5dp, 12dp long) at the corners of a pose's photo frame on the detail
  preview and the editor canvas. They show the framing of the shot, like viewfinder frame lines.
- **Rows (settings, lists):**
  - Title in Body strong, subtitle in Label Graphite, control at the end; at least 56dp tall.
  - Groups start with a Title-style heading in sentence case and are separated by 32dp.
  - Hairline dividers only between rows of the same group. No icon tiles.
- **Switches:** checked track Ink, thumb Sheet; unchecked track Paper with a Graphite outline and a Graphite thumb.
- **Sliders:** Ink active track, Hairline inactive track, Ink thumb.
- **Sheets and dialogs:** Sheet background, Large top radius for sheets and Medium for dialogs, Title-style heading.
- **Empty states:** left-aligned. A short sentence that says what to do and one button that does it. No emoji and no
  big illustrations.

## Camera screen

- The live view is surrounded by LiveView black.
- Controls are Mannequin-white outlined icons on black at 45% in circles. An active toggle inverts: Sheet fill,
  Ink icon.
- The hint banner is a Sheet "cue card" with Ink text and Medium radius, so it stays legible over any scene.
- The score is the Numeral style inside a ring. The ring takes the bright signal colour only while a person is
  detected; otherwise it is white at 60%.
- The pose strip uses the same pose tiles as the library, at a smaller size. Sheets opened from the camera use the
  light Studio colours.
- The guide figure is Mannequin at the chosen opacity. Per-limb match colours use the bright signal variants.

## Writing

- Sentence case and plain verbs. Name things by what people understand: "Pose camera", "Your photos", "Make a pose
  from a photo".
- Buttons keep the same name through a flow: "Save pose" leads to the message "Pose saved".
- Errors say what happened and what to do, without apologising. Empty screens invite one action.
