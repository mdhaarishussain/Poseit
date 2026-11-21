# Poseit promo film

A 22-second vertical film (1080 x 1920, 30 fps, 664 frames) that promotes Poseit, built in
[Remotion](https://www.remotion.dev/): real people from free stock footage (Mixkit, credited in `CREDITS.md`) with the
app's own graphics drawn on them (the live skeleton, the see-through lay figure, the per-limb colours, the score ring,
the cue cards, the countdown), computed by the app's own pose model and matcher on that footage, plus the app's 99
poses as lay figures, and a soundtrack synthesized in Node. The plan is in `STORYBOARD.md`, the sound design in
`sound-plan.md`, the voice-over script in `vo.txt`, and the build notes (what is real, what was measured, what
changed) in `NOTE.md`.

## Render it

```bash
cd promo
npm install
npm run footage     # download the Mixkit clips and build the film-time plates (public/footage/, never committed)
npm run draft       # half-size draft with sound -> out/draft.mp4   (about a minute)
npm run render      # the full-size master        -> out/promo.mp4   (h264, crf 16, AAC 48 kHz)
npm run studio      # Remotion Studio, to scrub frames
```

`draft`, `render` and `studio` regenerate the soundtrack first (`npm run audio`, about 3 seconds), so the sound always
matches the picture as it is now. Renders go to `out/` and are not committed. The landmark and verdict files the
overlays read (`public/footage/*.json`) are committed; rebuilding them needs the Python venv (`NOTE.md`, "Run it").

Review a render with motionmaxxing's `look.py`, which gives a contact sheet, the cuts, the end hold and the AUDIO block
(loudness, true peak, silences, onsets against the cuts):

```bash
node -e "const e=require('./out/events.json');require('fs').writeFileSync('out/cuts.events.json',JSON.stringify(e.cuts.map(f=>({t:f/30,frame:f,type:'cut'}))))"
python3 <motionmaxxing>/scripts/look.py out/draft.mp4 --out out/look --expect 22.13 --expect-audio --events out/cuts.events.json
```

The current numbers are in `NOTE.md` under "Sound".

## The sound

`npm run audio` runs `npm run events`, which writes `out/events.json` (every picture event by frame, from
`src/events.ts`), and then `node scripts/gen-audio.mjs`, which reads that file and writes:

| File | What it is |
|---|---|
| `public/audio/mix.wav` | The soundtrack the film plays (`src/audio/Soundtrack.tsx`): music, SFX and, once it exists, the voice. -14 LUFS integrated, true peak at or under -1.5 dBTP, stereo 48 kHz 16-bit. |
| `public/audio/sfx/*.wav` | The SFX kit, one file per sound, peak-normalised to -3 dBFS: the shutter, the lock, the chorus snaps, the countdown beep, the card tick, two whooshes, the swell, the low hit, the hook's fabric, the wooden joint. |
| `public/audio/stems/music.wav`, `sfx.wav` | The score and the placed SFX at their mix levels (before the voice duck and the limiter). |
| `public/audio/cues.json` | Where each SFX sits: `[{f, t, sfx, gain, label}]`. The same format motionmaxxing's `mix.py --events` takes. |

Everything is synthesized from scratch, with no samples, no network and no clock. The noise comes from seeded
generators, so the same picture always gives byte-identical files. These files are rebuilt on demand and are
gitignored. To change a level or a sound, edit the cue sheet or the score in `scripts/gen-audio.mjs`. To move a sound
with the picture, change the picture: the frames come from `src/events.ts`.

## Voice-over (when an ElevenLabs key is available)

The film is built so a voice drops in without touching the picture or the composition. `vo.txt` has five lines, and
`src/audio/voiceover.ts` (the slot) gives the frame each line starts on, the window it must end inside, and how far the
music ducks under it. Until real clips exist the slot stays empty, and no placeholder or fake voice goes into the
film.

The voice is made with motionmaxxing's `scripts/voice.py`, which uses only the Python standard library. It reads the
key from `ELEVENLABS_API_KEY` (or `./.env`, which is gitignored here). If the key is in `ELEVENLABS_KEY`, copy it
across first:

```bash
cd promo
export ELEVENLABS_API_KEY="$ELEVENLABS_KEY"
MM=<path to motionmaxxing>                           # e.g. /home/user/tejashmakwana/motionmaxxing

python3 $MM/scripts/voice.py voices                  # list voices; pick a plain, friendly British English one
python3 $MM/scripts/voice.py vo --script vo.txt --out public/audio/vo.mp3 --voice "<name>"
#   -> public/audio/vo.mp3, vo.words.json, vo.lines.json (one entry per line of vo.txt, timings refined to the speech)
python3 $MM/scripts/voice.py split --vo public/audio/vo.mp3 --lines public/audio/vo.lines.json --out public/audio/vo
#   -> public/audio/vo/line01.wav ... line05.wav + lines.index.json (each clip's lead = silence before the first word)

npm run audio      # places line N so its first word lands on VO_LINES[N-1].at, ducks the music, re-normalises
npm run draft && python3 $MM/scripts/look.py out/draft.mp4 --out out/look --expect 22.13 --expect-audio --events out/cuts.events.json
```

`npm run audio` prints one line per placed clip, and a warning when a line is still speaking past the end of its
window. In particular, line 4 must be silent by f504, where the half beat of stillness before the app's capture (f512)
begins. If a
line overruns, move its `at` in `src/audio/voiceover.ts`, or re-voice it a little faster (`voice.py vo ... --speed
1.08 --force`). Never move the picture to fit the voice. The music ducks 9 dB under speech (6 dB under line 1, because
the chorus is the music's moment), with 80 ms in and 300 ms out. Commit the clips in `public/audio/vo/`, because they
cost credits to regenerate. Never commit the key.

To mix outside the film instead (for example with motionmaxxing's `mix.py`), use `public/audio/stems/music.wav`, the
SFX list `public/audio/cues.json` and the clips at `VO_LINES[i].at / 30 - lead` seconds.

## Licences

- Remotion is free for individuals and for companies of up to three people; larger companies need a company licence
  (remotion.dev/license). This project uses the free licence.
- Archivo (the app's typeface, in `public/fonts/`) is under the SIL OFL 1.1.
- The sound is our own synthesis, in `scripts/gen-audio.mjs`. The skills that guided it, motionmaxxing (Apache-2.0)
  and remotion-motion-graphics (MIT), were read for technique only, and none of their files are copied here.
