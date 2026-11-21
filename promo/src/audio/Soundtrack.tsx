/**
 * Soundtrack.tsx: the film's sound, one pre-mixed file.
 *
 *   public/audio/mix.wav   written by scripts/gen-audio.mjs (`npm run audio`; draft, render and studio run it first)
 *                          from out/events.json, so every sound sits on the frame of the picture event it belongs to.
 *                          Music + SFX (+ the voice-over once it exists), -14 LUFS integrated, true peak <= -1.5 dBTP.
 *
 * Why one file and not an <Html5Audio> per cue: the mix is sample-exact (a cue lands on its frame's first sample, not
 * on a frame-stepped volume curve), the music ducks under the voice with 80 ms / 300 ms ramps, a look-ahead true-peak
 * limiter keeps it under the ceiling, and the loudness is measured (BS.1770) and set to the target in the same pass.
 * Remotion only has to play it from frame 0.
 *
 * VOICE-OVER SLOT. The voice is part of the same mix, so this component does not change when it arrives:
 *   1. src/audio/voiceover.ts   VO_LINES: the frame each line of vo.txt starts on, the window it must end inside and
 *                               how far the music ducks under it (this is the slot; edit `at` to move a line)
 *   2. public/audio/vo/         lineNN.wav + lines.index.json from motionmaxxing's voice.py (README.md, "Voice-over")
 *   3. npm run audio            places each line on its frame (clip start = at - lead), ducks the music, re-normalises
 * Until step 2 exists the slot is empty and the film runs on music and SFX alone; nothing stands in for the voice.
 */
import React from "react";
import { Html5Audio, staticFile } from "remotion";

export const MIX = "audio/mix.wav";

export const Soundtrack: React.FC = () => <Html5Audio src={staticFile(MIX)} />;
