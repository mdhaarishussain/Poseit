/**
 * UiFragment.tsx: a real app screenshot magnified by the camera (G1: readability comes from the camera, never from
 * inflating the UI). The screenshot is mapped so that `focus` (a rectangle in screenshot pixels) fills `into` (a
 * rectangle on screen, default the whole frame), cover-fit; animate `focus` with motion.keys() to push, pan or snap.
 *
 *   <UiFragment src="screens/detail.png" imageSize={[1200, 2580]} focus={{ x, y, w, h }} into? device? />
 *     src        a file in public/ (run `npm run assets` to copy the app's screenshots into public/screens/)
 *     imageSize  the screenshot's pixel size (app/build/screenshots are 1200 x 2580)
 *     device     draw the phone's own edge: the screenshot's rounded corners (radius in screenshot px) and a 1 dp
 *                Hairline outline, so a fragment that does not fill the frame reads as a lit object, not a card
 *   uiTextHeight(spPx, imageHeightPx, focusH, intoH)   on-screen text height for a given text size in the
 *              screenshot: use it to check G1 (>= 0.04 H for text that must be read)
 * Screenshot density: app/build/screenshots/*.png are 1200 px wide for a 411 dp screen (2.92 px per dp), so 15 sp
 * body text is about 44 px tall in the image.
 */
import React from "react";
import { Img, staticFile } from "remotion";
import { theme } from "../theme";

type Rect = { x: number; y: number; w: number; h: number };

export const uiTextHeight = (textPx: number, focusH: number, intoH: number) => (textPx * intoH) / focusH;

export const UiFragment: React.FC<{
  src: string;
  imageSize: [number, number];
  focus: Rect;
  into?: Rect;
  device?: { radius: number } | null;
  style?: React.CSSProperties;
}> = ({ src, imageSize, focus, into, device = null, style }) => {
  const box = into ?? { x: 0, y: 0, w: theme.size.W, h: theme.size.H };
  const s = Math.max(box.w / focus.w, box.h / focus.h);
  const ox = box.x + box.w / 2 - (focus.x + focus.w / 2) * s;
  const oy = box.y + box.h / 2 - (focus.y + focus.h / 2) * s;
  return (
    <div style={{ position: "absolute", left: box.x, top: box.y, width: box.w, height: box.h, overflow: "hidden", ...style }}>
      <div
        style={{
          position: "absolute",
          left: ox - box.x,
          top: oy - box.y,
          width: imageSize[0] * s,
          height: imageSize[1] * s,
          borderRadius: device ? device.radius * s : 0,
          overflow: "hidden",
          boxShadow: device ? `0 0 0 ${theme.size.dp}px ${theme.color.hairline}` : undefined,
        }}
      >
        <Img src={staticFile(src)} style={{ width: "100%", height: "100%", display: "block" }} />
      </div>
    </div>
  );
};
