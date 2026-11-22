#!/usr/bin/env python3
"""standing-leaning_credits.py: write picks/standing-leaning.json, one record per STANDING / LEANING / PROFESSIONAL pose:
the credit fields (source, page and file URLs, author, licence, attribution, changes) and both matcher scores for each
chosen photo, or photo: null and the reason when no candidate was good enough.

Pipeline for this group (work files in WORK/sl, outside git):
  standing-leaning_scan.py 4 <clip ...>     dense 4 fps rescan of shortlisted Mixkit previews with the full model, scored
                                            against the 27 poses (the harvest kept only up to 8 frames per clip)
  standing-leaning_extract.py <clip> <t ...> the clip's licensed 1080p download, sharpest frame within +-0.3 s of each
                                            hold (Laplacian variance on the person box), video deleted afterwards
  standing-leaning_finalize.py <pose> --hires <clip>_<ms> | --commons <pageid> [--zoom --cx --cy --quality]
                                            3:4 crop around the person, mirror when the match was with the flipped pose,
                                            675x900, WebP, landmarks of the shipped file, matcher score
  standing-leaning_credits.py               this file
"""
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from commons_harvest import standard_thumb  # noqa: E402

REPO = Path(__file__).resolve().parents[3]
WORK = Path("/home/user/poseit-photos-work")
SL = WORK / "sl"
SCORER = "tools/pose-photos/posematch.py (Python port of pose-core PoseMatcher)"

REASONS = {
    "stand_hand_on_hip": "Full-body model in a printed mini dress, one hand on the hip and the other arm hanging, weight on one leg: the figure's shape; sharp 1080p hold in a clothing store.",
    "stand_crossed_ankles": "Full-body studio shot, boots crossed at the ankles with the weight on one leg; arms hang loose where the figure has one hand on the hip, so the legs carry the pose. Mirrored to read the same way as the figure.",
    "stand_collar_touch": "Full-body model in a white dress with one hand at the neck and collarbone and the other arm down, as drawn; plain dark studio wall. The photographer's camera in the clip is cropped out.",
    "stand_hand_in_hair": "Full-body woman on an empty road with one hand in her hair and the other arm down: clearly the pose. The long dress hides her legs from the pose model, so the matcher score is low. Mirrored to match the figure.",
    "lean_foot_on_wall": "Man leaning back on a street wall with one foot planted on it behind him, the figure's leg shape exactly; he holds a phone at chest height where the figure's arms hang. Mirrored to match the figure. The phone in his hands keeps the matcher score near 0.54.",
    "lean_railing_back": "Back against a railing, both arms spread along the rail, ankles crossed: the 'elbows on the rail' shape; sharp 1080p hold in front of a fairground ride at night.",
    "lean_crossed_legs": "Woman leaning back against the side of a parked car, legs crossed at the ankles, one hand in her jacket pocket: a clear cross-legged lean. Dark clothes against a dark car defeat parts of the pose model, so the matcher score is low.",
    "pro_headshot": "Head-and-shoulders portrait of a man in a dark suit and tie, smiling at the camera, hands loosely steepled: a classic LinkedIn headshot (chalkboard of equations behind him). The source frame scores 1.0. The shipped 3:4 crop scores 0.0 because the scorer cannot place the guide without visible hips, not because of the pose.",
    "pro_blazer_pocket": "Full-body model in a white blazer dress, one forearm across the waist and the other arm hanging, as drawn. Her hand rests on her forearm, not in a pocket.",
}
GAPS = {
    "stand_hands_in_pockets": "The only clear candidate (Commons 'Young man standing on a rusty bridge', CC BY-SA 4.0) has a person's name as its whole description, so the rules exclude it. The other candidates have their hands on the hips or clasped in front, not in pockets.",
    "stand_look_away": "No candidate shows a relaxed full-body stance with the head turned away: the top-ranked frames look at the camera, are backlit silhouettes (Mixkit 407), or show a shirtless man under an open jacket (Mixkit 100148).",
    "stand_arms_crossed": "No full-body arms-crossed photo passes. The Commons photo of a woman with crossed arms names her in its categories (excluded). The red-suit Mixkit clips (50643, 50634, 50649) cut the legs at the shins or knees, or show a lace bralette, or have the photographer in front. The pink-filter clip (1236) has no clean crossed-arms frame at 1080p.",
    "stand_over_shoulder": "Nobody turns away from the camera and looks back over the shoulder. Commons 'Kubanino sur stratpesilo' and 'Priscilla Modelling 1' name the person, so the rules exclude them.",
    "stand_power_pose": "No clean both-hands-on-hips stance. The Mixkit 41665 hold has the photographer and his camera rig in front of the model. Mixkit 43650 is a flesh-tone bodysuit. Commons 88226302 is a man looking up at an eclipse through viewing glasses, not a power pose.",
    "stand_sunglasses": "No one touches sunglasses in a full-body shot. Every sunglasses clip is a close-up, a phone screen or a selfie. The hand-at-temple studio frame (Mixkit 52257) shows a sequined veil, not glasses.",
    "lean_shoulder_wall": "No shoulder-to-the-wall lean: the wall leans found are back-against-the-wall (used for 'Foot up on the wall') or waist-up portraits (Commons 79971536).",
    "lean_hand_on_wall": "No one has a raised hand braced on a wall; the arm-up frames are selfie arms (Mixkit 51661) or dance moves.",
    "lean_railing_forward": "No one bent forward over a railing on the forearms; Commons railing photos are waist-up or tiny and side-on.",
    "lean_door_frame": "No doorway lean among the candidates.",
    "lean_pole_swing": "No one swinging around a pole or lamppost; no candidate above 0.5.",
    "lean_table_edge": "No one perched on a table edge; the top-ranked frames are people standing.",
    "pro_arms_crossed": "Same as stand_arms_crossed. The red-suit clip (Mixkit 50643) is the closest, but its legs are cut below the knee and the shipped crop scores 0.34. The full-body framing the pose needs is not available.",
    "pro_desk_edge": "No desk or table perch among the candidates (best 0.60, a standing person).",
    "pro_open_palm": "The open-palm gesture exists only waist-up (Mixkit 4623, a teacher at a chalkboard at 0.6 s), and the pose is full-body (tcov 0.30). The full-body candidates are standing with the arms down.",
    "pro_table_lean": "No one leaning on a table with both hands. The high matcher scores come from people simply standing with their arms down.",
    "pro_cuff_adjust": "Closest is the red-suit model touching her extended forearm (Mixkit 50643 at 7.2 s), but her legs are cut below the knee and the shipped crop scores 0.39. Other candidates clasp the hands or cross the arms.",
    "pro_folder_hold": "No one holds a folder, tablet or notebook against the chest in a full-body shot.",
}


def main():
    poses = json.loads((REPO / "promo/public/poses.json").read_text())
    poses = poses if isinstance(poses, list) else poses.get("poses", poses)
    files = json.loads((WORK / "commons/files.json").read_text())
    scan = json.loads((SL / "scan.json").read_text())
    out = []
    for p in poses:
        if p["category"] not in ("STANDING", "LEANING", "PROFESSIONAL"):
            continue
        pid = p["id"]
        base = {"pose_id": pid, "pose_name": p["name"], "category": p["category"]}
        if pid not in REASONS:
            out.append({**base, "photo": None, "reason": GAPS[pid]})
            continue
        f = json.loads((SL / "final" / f"{pid}.json").read_text())
        photo = REPO / "app/src/main/assets/pose_photos" / f"{pid}.webp"
        ps, ss = f["photo_score"] or {}, f["source_frame_score"] or {}
        x0, y0, x1, y1 = f["crop_px"]
        rec = {**base, "photo": f"app/src/main/assets/pose_photos/{pid}.webp", "bytes": photo.stat().st_size}
        if f["source"] == "Mixkit":
            clip_id = f["clip"]
            clip = json.loads((WORK / "mixkit/clips" / f"{clip_id}.json").read_text()) \
                if (WORK / "mixkit/clips" / f"{clip_id}.json").exists() else scan[str(clip_id)]
            if not clip.get("licence", {}).get("ok"):
                clip = scan[str(clip_id)]
            lic = clip["licence"]
            vw, vh = f["video_size"]
            rec.update({
                "source": "Mixkit", "title": clip["title"], "page_url": clip["page"],
                "file_url": f["download_page"], "video_file": f["file_url"],
                "video_quality": "1080p" if f["download_label"] == "Full HD" else "720p",
                "frame_time_s": f["time_s"], "frame_index": f["frame_index"], "hold_time_s_in_preview": f["requested_t"],
                "author": lic.get("author") or "Mixkit (no author named on the clip page)",
                "licence": {"name": lic["panel_licence"], "url": "https://mixkit.co/license/#videoFree",
                            "stated_on_page": lic["stated_on_page"], "checked": clip.get("checked", "2026-11-21")},
                "attribution": f"Video still: Mixkit, Mixkit Stock Video Free License ({clip['page']})",
                "changes": [f"still frame at {f['time_s']} s of the {('1080p' if f['download_label'] == 'Full HD' else '720p')} download "
                            f"(sharpest of {f['frames_compared']} frames within 0.3 s of the hold)",
                            f"cropped to [{x0}, {y0}, {x1}, {y1}] px of the {vw}x{vh} source (3:4)"]
                           + (["mirrored horizontally"] if f["mirrored"] else [])
                           + ["resized to 675x900, WebP quality 78, metadata stripped"],
                "source_size": [vw, vh],
            })
        else:
            r = files[f["commons"]]
            lic = r["licence"]
            sw, sh = f["image_size"]
            sa = "BY-SA" in (lic.get("short_name") or "")
            rec.update({
                "source": "Wikimedia Commons", "title": r["title"], "page_url": r["page"],
                "file_url": r["file_url"].split("?")[0], "rendition_used": standard_thumb(r),
                "author": lic.get("artist"), "credit_line": lic.get("credit"),
                "licence": {"name": lic.get("short_name"), "url": lic.get("url"),
                            "attribution_required": lic.get("attribution_required"),
                            "restrictions": lic.get("restrictions"), "checked": "2026-11-21"},
                "attribution": f"Photo: {lic.get('artist')}, {lic.get('short_name')} ({lic.get('url')}), via Wikimedia Commons "
                               f"({r['page']})" + ("; cropped" + (", mirrored" if f["mirrored"] else "") + ", resized"),
                "changes": [f"cropped to [{x0}, {y0}, {x1}, {y1}] px of the {sw}x{sh} copy of that rendition stored by commons_harvest.py (3:4)"]
                           + (["mirrored horizontally"] if f["mirrored"] else [])
                           + [f"resized to 675x900, WebP quality {72 if pid == 'stand_hand_in_hair' else 78}, metadata stripped"],
                "source_size": [sw, sh],
            })
            if sa:
                rec["share_alike"] = f"The adapted file is offered under {lic.get('short_name')}, as the licence requires."
        rec.update({
            "mirrored": f["mirrored"], "crop_px": f["crop_px"],
            "app_matcher_score": ps.get("score"),
            "score_detail": {"score_source_frame": ss.get("score"), "score_shipped_file": ps.get("score"),
                             "shipped_matches_flipped_pose": ps.get("mirror_photo"), "fit": ps.get("fit"),
                             "tcov": ps.get("tcov"), "people": ps.get("people"), "framing": ps.get("framing"),
                             "scorer": SCORER},
            "landmarks": f"tools/pose-photos/landmarks/{pid}.json",
            "reason": REASONS[pid],
        })
        out.append(rec)
    dest = REPO / "tools/pose-photos/picks/standing-leaning.json"
    dest.write_text(json.dumps(out, indent=1, ensure_ascii=False) + "\n")
    print(dest, len(out), "poses,", sum(1 for r in out if r.get("photo")), "with a photo")


if __name__ == "__main__":
    main()
