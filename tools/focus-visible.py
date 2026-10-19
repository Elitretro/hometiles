#!/usr/bin/env python3
"""Walks the focus with the direction keys and checks on the drawn pixels that it shows.

The counterpart to `tools/unreachable.py`. That one asks whether the focus gets somewhere,
this one whether anybody can *see* where it is. Both can fail on their own: on 19.10.2026
every row of the settings was reachable and unreachable.py had nothing to report, while on a
Qin F21 the focus wandered through the same rows without a mark. To its owner that looked
like a list that only scrolls.

    tools/focus-visible.py [device] [--steps N]

**What counts as visible.** The focus border is a ring of 8 dp in one colour. For every step
the tool takes the bounds of the focused node and of its ancestors and samples two lines
along each edge: 3 dp inside, where the focus border lies, and 6 dp further in, where it does
not. Visible means the outer line is one colour almost everywhere and clearly differs from
the inner one. A plain row has the same fill on both lines; the 3 dp border of an empty tile
lies further out than the first line.

Ancestors count because a text field is focused in its inner node while the border belongs
to the whole search row around it.

**It never selects.** Down until the focus stops moving, then up again, nothing else. So it
starts nothing and dials nothing, and it may run on a phone with a SIM in it, like
unreachable.py. Leading the way into a screen is the caller's job.

Needs Pillow.
"""
import io
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

from PIL import Image

STEPS = 25
# share of samples on the outer line that have to be one colour.
UNIFORM = 0.85
# how far apart outer and inner line have to be, summed over r, g and b.
APART = 150


def adb(device, *args, binary=False):
    out = subprocess.run(["adb", "-s", device, *args], capture_output=True)
    return out.stdout if binary else out.stdout.decode(errors="replace")


def density(device):
    hit = re.search(r"(\d+)", adb(device, "shell", "wm", "density").split(":")[-1])
    return int(hit.group(1)) / 160 if hit else 1.375


def focused(device):
    """the focused node as (label, [bounds of it and its ancestors]), or None."""
    adb(device, "shell", "uiautomator", "dump", "/data/local/tmp/fv.xml")
    xml = adb(device, "shell", "cat", "/data/local/tmp/fv.xml")
    try:
        root = ET.fromstring(xml[xml.index("<"):])
    except (ValueError, ET.ParseError):
        return None

    def walk(node, chain):
        if node.get("focused") == "true":
            return node, chain
        for child in node:
            hit = walk(child, chain + [node])
            if hit:
                return hit
        return None

    hit = walk(root, [])
    if not hit:
        return None
    node, chain = hit
    texts = [n.get("text") or n.get("content-desc") for n in node.iter()]
    label = " / ".join(t for t in texts if t)[:60] or node.get("class", "?").split(".")[-1]
    boxes = []
    for n in [node] + chain[::-1]:
        b = re.findall(r"\d+", n.get("bounds", ""))
        if len(b) == 4:
            boxes.append(tuple(int(v) for v in b))
    return label, boxes, node.get("class", "").endswith("EditText")


def line(image, box, inset):
    """pixels along the middle 60 % of each edge, `inset` pixels inside."""
    left, top, right, bottom = box
    width, height = image.size
    points = []
    for i in range(12):
        f = 0.2 + 0.6 * i / 11
        x = int(left + (right - left) * f)
        y = int(top + (bottom - top) * f)
        points += [(x, top + inset), (x, bottom - 1 - inset), (left + inset, y), (right - 1 - inset, y)]
    return [image.getpixel(p)[:3] for p in points if 0 <= p[0] < width and 0 <= p[1] < height]


def ring(image, box, dp):
    outer = line(image, box, int(3 * dp))
    inner = line(image, box, int(9 * dp))
    if len(outer) < 16 or len(inner) < 16:
        return False
    mode = max(set(outer), key=outer.count)
    near = [p for p in outer if sum(abs(a - b) for a, b in zip(p, mode)) < 60]
    if len(near) / len(outer) < UNIFORM:
        return False
    inside = [p for p in inner if sum(abs(a - b) for a, b in zip(p, mode)) < 60]
    return len(inside) / len(inner) < 0.5 and any(
        sum(abs(a - b) for a, b in zip(p, mode)) >= APART for p in inner
    )


def frame_around(image, box, dp):
    """for a text field: a band of border colour on all four sides, up to 60 dp outside.

    the search row draws the border, and the row is no node of its own in the dump, so its
    bounds are not known. from the middle of each edge of the field the tool walks outwards
    and looks for a run of at least 5 dp in one colour far from the field's own fill.
    """
    left, top, right, bottom = box
    width, height = image.size
    # the most common colour along the field's top, not its middle: there stands the text.
    row = [image.getpixel((x, min(top + 2, height - 1)))[:3] for x in range(max(left, 0), min(right, width))]
    fill = max(set(row), key=row.count) if row else (0, 0, 0)
    cx, cy = (left + right) // 2, (top + bottom) // 2
    reach = int(60 * dp)
    need = int(5 * dp)

    def band(points):
        run, best, colour = 0, 0, None
        for p in points:
            if not (0 <= p[0] < width and 0 <= p[1] < height):
                break
            px = image.getpixel(p)[:3]
            far = sum(abs(a - b) for a, b in zip(px, fill)) >= APART
            same = colour is not None and sum(abs(a - b) for a, b in zip(px, colour)) < 60
            run = run + 1 if far and (same or run == 0) else (1 if far else 0)
            colour = px if far else None
            best = max(best, run)
        return best >= need

    return all([
        band([(x, cy) for x in range(left, left - reach, -1)]),
        band([(x, cy) for x in range(right, right + reach)]),
        band([(cx, y) for y in range(top, top - reach, -1)]),
        band([(cx, y) for y in range(bottom, bottom + reach)]),
    ])


def shows(image, boxes, field, dp):
    """the focus border around the node, an ancestor or (for a text field) its row."""
    return any(ring(image, box, dp) for box in boxes[:4]) or (field and frame_around(image, boxes[0], dp))


def main():
    args = sys.argv[1:]
    steps = STEPS
    if "--steps" in args:
        i = args.index("--steps")
        steps = int(args[i + 1])
        del args[i:i + 2]
    device = args[0] if args else "emulator-5554"
    dp = density(device)

    findings = []
    seen = []
    for key in ["DPAD_DOWN", "DPAD_UP"]:
        still = 0
        for _ in range(steps):
            adb(device, "shell", "input", "keyevent", "KEYCODE_" + key)
            time.sleep(0.5)
            hit = focused(device)
            if not hit:
                findings.append(f"{key}: nothing has the focus")
                print(f"{key:10} --   nothing focused")
                continue
            label, boxes, field = hit
            image = Image.open(io.BytesIO(adb(device, "exec-out", "screencap", "-p", binary=True))).convert("RGB")
            visible = shows(image, boxes, field, dp)
            print(f"{key:10} {'ok  ' if visible else 'GONE'} {label} {boxes[0]}")
            if not visible:
                findings.append(f"{key}: focus on '{label}' {boxes[0]} draws no border")
            # at the end of the list the focus stands still; three presses settle it.
            still = still + 1 if seen and seen[-1] == (label, boxes[0]) else 0
            seen.append((label, boxes[0]))
            if still >= 2:
                break

    print()
    if findings:
        print(f"{len(findings)} finding(s):")
        for f in dict.fromkeys(findings):
            print("  " + f)
        sys.exit(1)
    print(f"{len(seen)} steps, the focus showed every time")


if __name__ == "__main__":
    main()
