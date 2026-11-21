# Poseit promo: sound plan

Film: 664 frames at 30 fps (22.13 s), see `STORYBOARD.md`. All audio is synthesized in Node by our own scripts
(deterministic, seeded, 48 kHz 16-bit WAV), because there is no ElevenLabs key in this session. The voice-over is
written (`vo.txt`) but not voiced: no fake or placeholder voice goes into the mix. When a key arrives, the VO drops
into the windows below and the music is ducked under it; the picture does not move.

## As built

`src/events.ts` is the source of truth for every picture frame (print it with `npm run events`, which writes
`out/events.json`): the cuts, every chorus snap including the ripple, the three hits and the two silences. The act III
frames come from the app's own PoseMatcher, HintStabilizer and CaptureCoach (`public/match.json`), so they changed from
the plan: the cards land at f355, f400 and f455, the lock at f443, the digits every 30 f from f464, the shutter at f554.
The tables below are updated to the build. The wordmark lands at f620 (no sound, as planned).

The sound is built (`scripts/gen-audio.mjs`; measurements in `NOTE.md`, "Sound"). Where the build differs from this
plan, the plan below says "as built". In short:
- The coaching bed is 6 dB down, not 8: at -8 dB the kickless bed measured -25 LUFS, too close to silence on a phone.
- The countdown build is the shutter-click hats speeding up like a motor drive winding up (eighths, then 16ths) over
  a kick every 15 f and a filter-opening A pedal, not a snare roll.
- The countdown beep is the app's own tone at one pitch (tuned to D6), not a rising tick.
- "Perfect. Hold it" gets no tick: the lock 12 f before it already speaks for it.
- The final chord sounds at f600, with the pull-back, not on the f592 downbeat.
- An air crash joins the unison.

## Music

**Character.** A dry, playful studio-disco groove: four-on-the-floor kick (soft and round, not club-heavy), offbeat
hats made from camera shutter-click transients, a handclap on 2 and 4, a muted plucked bass, one bright chord stab.
Small room, no big reverb, no swells over the result. It should sound like a photo studio with the radio on, and it
nods at the hero pose, Disco point. Key: A minor with a dorian sixth (F#) so it stays cheeky rather than dark.

**Tempo and grid.** 112.5 BPM: one beat is exactly 16 frames, an eighth 8 f, a bar 64 f. Bar lines sit at f16 + 64n,
so the downbeats are f80, 144, 208, 272, 336, 400, 464, 528, 592 and 656. The picture uses the same grid: every cut
and pose snap is on a multiple of 8 f. Half notes (32 f, 1.07 s) carry the countdown at almost the app's real speed.

**Sections.**

| Frames | Time (s) | Picture | Music |
|---|---|---|---|
| 0-80 | 0.00-2.67 | The freeze | No music. Room tone only (soft pink noise around -42 dBFS). The awkward silence is the joke. |
| 80-248 | 2.67-8.27 | Studio reveal, chorus | Groove A enters on the cut: kick, bass, shutter-click hats. Clap joins at f144. On the eighth-note snaps (f176-208) a 16th-note tick layer joins and the bass climbs. |
| 248-272 | 8.27-9.07 | The drop | Everything cuts out except a reverse noise swell rising into f272 (music 15 dB down). |
| 272-336 | 9.07-11.20 | Unison, pick, push-in | Groove B, full: the unison hit, then the groove with the stab on the downbeat; a whoosh into the cut. |
| 336-464 | 11.20-15.47 | Coaching | Groove thinned to bass, soft hats and two warm chords, no kick, 6 dB down (as built; 8 dB measured too quiet), so the cue cards can be read. |
| 464-546 | 15.47-18.20 | Countdown | Build: the kick returns as a pulse every 15 f, the shutter-click hats speed up like a motor drive (eighths, then 16ths; as built, instead of a snare), a rising filter on the bass pedal, a riser. The digits come every 30 f (the app's real 1 s), not every 32 f, so let the build follow the ticks (464, 494, 524) rather than the 112.5 BPM grid: an accelerando into the shutter. |
| 546-554 | 18.20-18.47 | Half a beat of stillness | Silence: every music layer off, room tone only. |
| 554-600 | 18.47-20.00 | Shutter, flash, relax | The shutter alone, then its tail. |
| 600-664 | 20.00-22.13 | Pull-back, name, hold | One final chord (Am add9 over a low A) at f600, when the pull-back starts (as built; the plan said the f592 downbeat), left to ring and decay naturally to the last frame, with room tone under it. No fade, no silent tail. |

## The sound events

Three hand-placed hits (unison, lock, shutter), each with something cleared before it. Everything else is the
groove or a quiet tick.

| # | Frame | Time (s) | Picture event | Sound | Level |
|---|---|---|---|---|---|
| 1 | 74-80, then 80 | 2.47-2.67 | Hard cut from the frozen skeleton to the studio of 99 frozen figures | A short air whoosh (6 f) peaking on the cut, then the groove's first kick with a shutter click on top: the first big sound, tied to the cut | kick -8 dB, whoosh -14 dB |
| 2 | 112, 128, 144, 160, 176, 184, 192, 200, 208, 224, 240 | 3.73-8.00 | Each pose change of the chorus (11 snaps) | One shutter click per snap, one sound family. On-beat snaps sit on the kick; the eighth-note snaps rise a semitone each; Couple and Group get a double click | -14 to -10 dB |
| 3 | 248-272, then 272 | 8.27-9.07 | Silence, then all 99 figures snap into Disco point | **Hit 1.** Music drops for 0.8 s with a reverse swell; on f272 kick, clap, chord stab and a shutter click together | -6 dB |
| 4 | 288 | 9.60 | Frame marks close around one figure | A tiny dry pop | -16 dB |
| 5 | 324-336 | 10.80-11.20 | Push-in and hard cut into the viewfinder | Airy whoosh starting 3-4 f before peak speed so its peak meets the cut | -12 dB |
| 6 | 355, 400 | 11.83, 13.33 | The first two cue cards arrive (the app's HintStabilizer timing) | A soft UI tick (one sound for both). As built, "Perfect. Hold it" (f455) gets none: it lands 12 f after the lock, which already says it | -17 dB |
| 7 | 443 | 14.77 | The lock: the score reaches the match threshold (every limb green, the ring turns green, its hold arc starts running round); "Perfect. Hold it" follows at 455 | **Hit 2, soft.** A two-note rising chime (two sine partials, a fifth apart), short and clean; music stays down | -12 dB |
| 8 | 464, 494, 524 | 15.47, 16.47, 17.47 | Countdown "3", "2", "1" | The app's own countdown beep per digit (VoiceCoach: ToneGenerator TONE_PROP_BEEP, 1200 Hz, 120 ms), tuned to D6 and at one pitch as in the app (as built), over the build | -14 dB |
| 9 | 546-554, then 554 | 18.20-18.47 | Half a beat of stillness, then the capture flash | **Hit 3, the loudest sound.** Silence for 8 f, then a two-part mechanical shutter (open click, close clack about 40 ms apart) with a low thump and a short room tail | -4 dB (loudest SFX) |
| 10 | 600 | 20.00 | The camera pulls back from the relaxed figure | The final chord (part of the music), resolving | -10 dB |

The wordmark (f608) and the end line get no sound: the chord is already ringing and the ending is quieter than the
climax.

## SFX kit (our own synthesis, as built)

Four families reused as a system, plus two one-offs. Each file in `public/audio/sfx/` is peak-normalised to -3 dBFS.

| File | Recipe |
|---|---|
| `snap.wav`, `snap_up1-4.wav` | The chorus snap: a shutter-click transient (two micro-strikes 7 ms apart through four body modes, 0.9-5.6 kHz). Pitched up a semitone each for the eighth-note snaps and their ripple. |
| `pop.wav` | The frame marks: the same click family, pitched up a fifth. |
| `wood.wav` | The relax on twos: the click family pitched down, with more body, as a lay figure's friction joint giving. |
| `shutter.wav` | Hit 3: the mirror and first curtain (a hard double strike through five modes plus a 180-470 Hz thunk), then the second curtain and the mirror slap 68 ms later, a 95 -> 48 Hz thump, a faint spring ting, a small room (RT60 0.42 s). |
| `lock.wav` | Hit 2: a latch seating (two ticks 16 ms apart), then E5 and B5 bell tones 55 ms apart with a 200 ms decay. |
| `tick.wav` | A card arrives: a 1.6 kHz blip on a 520 Hz "tok", 50 ms. |
| `beep.wav` | The countdown: the app's TONE_PROP_BEEP (1200 Hz, 120 ms), tuned to D6 (1174.7 Hz), with softened edges. |
| `whoosh_cut.wav`, `whoosh_push.wav` | Pink noise through a band-pass sweeping up to a peak (0.2 s / 0.42 s in), then dying. Placed so the peak meets the f80 cut / the f336 match cut. |
| `swell.wav` | The unison chord's own room tail (RT60 1.4 s) played backwards, plus rising filtered noise, 0.8 s, cut dead on f272. |
| `hit.wav` | The hero landing: a 72 -> 40 Hz sub, driven for harmonics a phone can play, an air transient and a room. |
| `fabric_flick.wav`, `fabric_flop.wav` | The hook's fidget: band-passed noise with a grainy, uneven envelope. |

The score (kick, clap, shutter-click hats, plucked bass, FM keys, an air crash, a riser, room tone) is synthesized by
the same script straight to the film's length, 22.133 s, so it covers the whole cut.

## Voice-over windows (for later)

The slot is `src/audio/voiceover.ts` (`VO_LINES`, read by `npm run audio`); `README.md` has the steps to voice it.

| Line (`vo.txt`) | Beat | Window (frames) | Words | Notes |
|---|---|---|---|---|
| Poseit has ninety-nine poses to copy. | B3 chorus | 116-188 | 6 | The only time the name is spoken. Duck music 6 dB, not 9: the chorus is the music's moment. |
| Pick one. | B4 pick | 292-310 | 2 | After the unison hit, on the frame marks. |
| Line up with it. | B5 step back | 340-392 | 4 | Over the cut into the viewfinder's first second. |
| It tells you what to change. | B6 raise arm | 402-460 | 6 | Ends before "Perfect. Hold it" settles. |
| Hold the pose, and it takes the photo. | B7 countdown | 466-546 | 8 | Must end before the f546 stillness (as built: the silence moved with the app's capture frame). Tight: 3 words a second. |
| No account and no ads. | B8 name | 596-656 | 5 | Slower, warm, over the chord. |

Delivery: about 2.5 words a second, plain, friendly, not salesy, British English. B1 and B2 have no voice: the
question is read on screen in silence. On-screen words land 0-2 f before they are heard; the voice never reads the
screen. With real word timings, re-check that each line fits its window and move only the line, not the picture.

## Mix

- About -14 LUFS integrated, true peak at or under -1 dBTP, small loudness range.
- Music 6 dB down for reading in the coaching act (f336-464) and 4 dB down while "Disco point" is read (f288-310);
  out in the drop (f248-272) except the reverse swell; off from f546 until just before the final chord (as built).
- The shutter (event 9) is the loudest SFX and the only one allowed above the music's peak.
- With VO: duck the music 9 dB under speech (6 dB in the chorus), 80 ms in and 300 ms out.
- No silent tail: the final chord and room tone run to f664. Check with `look.py --expect 22.13 --expect-audio`
  (integrated LUFS, true peak, silent tail, hit count against picture), and scrub each hit frame once.

## Licences

The synthesis scripts are written from scratch for this repo. The remotion skill's example scripts (MIT) and
motionmaxxing's (Apache-2.0) were read for approach only; nothing is copied.
