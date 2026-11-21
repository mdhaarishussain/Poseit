/**
 * voiceover.ts: THE VOICE-OVER SLOT. One entry per line of ../../vo.txt (same order), with the frame on which the
 * line's first speech sample should be heard and the window it must finish inside. Nothing is voiced yet: there is
 * no ElevenLabs key in this session and no placeholder voice goes into the film.
 *
 * When the clips exist (public/audio/vo/lineNN.wav + lines.index.json from motionmaxxing's voice.py split, see
 * ../../README.md), `npm run audio` places line N so its speech starts on VO_LINES[N-1].at (clip start = at - lead),
 * ducks the music under each spoken region (duckDb, 80 ms in, 300 ms out) and re-normalises the mix to -14 LUFS.
 * The composition needs no edit: src/audio/Soundtrack.tsx plays the mix. If a real line overruns its window, the
 * mixer prints a warning; move the line's `at` (and only the line), never the picture.
 *
 * Windows from sound-plan.md (v2 deltas), planned against STORYBOARD.md v2. Lines 3 and 4 sit in the live act,
 * whose frames are the app's verdicts on the footage: re-check them against src/events.ts once match.json is
 * computed. Line 4 must be silent 8 f before the capture (the stillness before the shutter). The voice is the
 * narrator's, never the people's: no line is in the first person and none is said over a person as theirs.
 */
export type VoLine = {
  /** 1-based, = NN in lineNN.wav and the line number in vo.txt */
  i: number;
  text: string;
  /** frame on which the first speech sample lands */
  at: number;
  /** [from, to): the speech must end before `to` */
  window: [number, number];
  /** how far the music ducks under this line, in dB */
  duckDb: number;
  beat: string;
};

export const VO_LINES: VoLine[] = [
  { i: 1, text: "Poseit has ninety-nine poses to copy.", at: 150, window: [150, 222], duckDb: 6, beat: "B3 chorus (duck 6 dB, not 9: the chorus is the music's moment)" },
  { i: 2, text: "Pick one.", at: 292, window: [292, 310], duckDb: 9, beat: "B4 pick, on the frame marks after the unison, before the name is read" },
  { i: 3, text: "It tells you what to fix.", at: 344, window: [344, 400], duckDb: 9, beat: "B5 coaching, ends by the planned lock (re-check against MATCH_EVENTS.hold)" },
  { i: 4, text: "Hold it, and it takes the photo.", at: 426, window: [426, 505], duckDb: 9, beat: "B6 countdown, silent 8 f before the capture (re-check against MATCH_EVENTS.capture)" },
  { i: 5, text: "No account and no ads.", at: 590, window: [590, 650], duckDb: 9, beat: "B8, slower and warm, over the chord, after the relax" },
];
