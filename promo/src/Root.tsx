/**
 * Root.tsx: compositions.
 *   Promo        the film: 1080x1920, 30 fps, 664 f (STORYBOARD.md)
 *   Dev-*        foundation checks rendered as stills (figure sheet, blends, worlds); not part of the film
 */
import React from "react";
import { Composition } from "remotion";
import { theme } from "./theme";
import { ensureFonts } from "./fonts";
import { Promo } from "./Promo";
import { FigureSheet } from "./dev/FigureSheet";
import { BlendSheet } from "./dev/BlendSheet";
import { StudioDemo } from "./dev/StudioDemo";
import { ViewfinderDemo } from "./dev/ViewfinderDemo";
import { EndDemo } from "./dev/EndDemo";

ensureFonts();

const { W, H, fps, duration } = theme.size;

export const RemotionRoot: React.FC = () => (
  <>
    <Composition id="Promo" component={Promo} width={W} height={H} fps={fps} durationInFrames={duration} />
    <Composition id="Dev-FigureSheet" component={FigureSheet} width={W} height={H} fps={fps} durationInFrames={1} />
    <Composition id="Dev-BlendSheet" component={BlendSheet} width={W} height={H} fps={fps} durationInFrames={1} />
    <Composition id="Dev-StudioDemo" component={StudioDemo} width={W} height={H} fps={fps} durationInFrames={96} />
    <Composition id="Dev-Viewfinder" component={ViewfinderDemo} width={W} height={H} fps={fps} durationInFrames={2} />
    <Composition id="Dev-End" component={EndDemo} width={W} height={H} fps={fps} durationInFrames={1} />
  </>
);
