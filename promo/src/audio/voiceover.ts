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
 * Windows from sound-plan.md, re-checked against the built picture (src/events.ts):
 *   line 5 must be silent by f546, where the half beat of stillness before the shutter begins.
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
  { i: 1, text: "Poseit has ninety-nine poses to copy.", at: 116, window: [116, 188], duckDb: 6, beat: "B3 chorus (duck 6 dB, not 9: the chorus is the music's moment)" },
  { i: 2, text: "Pick one.", at: 292, window: [292, 310], duckDb: 9, beat: "B4 pick, on the frame marks after the unison" },
  { i: 3, text: "Line up with it.", at: 340, window: [340, 392], duckDb: 9, beat: "B5, over the match cut's first second" },
  { i: 4, text: "It tells you what to change.", at: 402, window: [402, 460], duckDb: 9, beat: "B6, ends before \"Perfect. Hold it\" settles" },
  { i: 5, text: "Hold the pose, and it takes the photo.", at: 466, window: [466, 546], duckDb: 9, beat: "B7 countdown, silent by the f546 stillness" },
  { i: 6, text: "No account and no ads.", at: 596, window: [596, 656], duckDb: 9, beat: "B8, slower and warm, over the final chord" },
];
