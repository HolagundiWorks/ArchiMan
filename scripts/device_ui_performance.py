"""Read-only on-device navigation/scroll frame sampling; not a Macrobenchmark substitute."""
import argparse
import csv
import io
import json
import math
import pathlib
import re
import statistics
import subprocess
import time
import xml.etree.ElementTree as ET

parser = argparse.ArgumentParser()
parser.add_argument("--adb", default="adb")
args = parser.parse_args()
package = "com.holagundi.archiman"
directory = pathlib.Path("app/build/audit")
directory.mkdir(parents=True, exist_ok=True)

def shell(*command):
    return subprocess.run([args.adb, "shell", *command], capture_output=True, text=True, timeout=30, check=True).stdout

def screen():
    for attempt in range(3):
        output = shell("uiautomator", "dump", "/sdcard/archiman-audit.xml")
        if "dumped to:" in output:
            root = ET.fromstring(shell("cat", "/sdcard/archiman-audit.xml"))
            assert any(node.get("package") == package for node in root.iter("node"))
            return root
        time.sleep(0.5)
    raise RuntimeError("UI inspector unavailable; cannot verify navigation")

shell("am", "start", "-W", "-n", package + "/.MainActivity")
time.sleep(1)
root = screen()
dimensions = [int(x) for x in re.findall(r"\d+", next(root.iter("node")).get("bounds"))]
width, height = dimensions[2:4]
assert height > width, "Run in portrait orientation"
shell("dumpsys", "gfxinfo", package, "reset")
for label in ["Dashboard", "Projects", "Practice", "Dashboard"]:
    root = screen()
    parents = {child: parent for parent in root.iter() for child in parent}
    candidates = []
    for node in root.iter("node"):
        if node.get("text") == label:
            # Compose can expose an unbounded text node inside the bounded clickable item.
            target = node
            while target is not None:
                bounds = [int(x) for x in re.findall(r"\d+", target.get("bounds", ""))]
                if len(bounds) == 4 and 0 < bounds[2] - bounds[0] < width * 0.6 and bounds[3] > bounds[1] and bounds[1] > height * 0.7:
                    candidates.append(bounds)
                    break
                target = parents.get(target)
    assert candidates, f"Navigation item unavailable: {label}"
    left, top, right, bottom = candidates[-1]
    shell("input", "tap", str((left + right) // 2), str((top + bottom) // 2))
    time.sleep(0.5)
    assert any(node.get("text") == label and int(re.findall(r"\d+", node.get("bounds"))[1]) < 200 for node in screen().iter("node"))
    if label != "Practice":
        for start, end in [(0.70, 0.35), (0.70, 0.35), (0.35, 0.70), (0.35, 0.70)]:
            shell("input", "swipe", str(width // 2), str(int(height * start)), str(width // 2), str(int(height * end)), "300")
            time.sleep(0.2)
raw = shell("dumpsys", "gfxinfo", package, "framestats")
(directory / "ui-framestats.txt").write_text(raw, encoding="utf-8")
frames = []
for section in raw.split("---PROFILEDATA---")[1::2]:
    reader = csv.DictReader(io.StringIO(section.strip()))
    for row in reader:
        if row.get("Flags") == "0" and row.get("IntendedVsync") and row.get("FrameCompleted"):
            duration = (int(row["FrameCompleted"]) - int(row["IntendedVsync"])) / 1_000_000
            if duration > 0:
                frames.append(duration)
assert frames, "No frame samples available"
ordered = sorted(frames)
def percentile(q):
    return round(ordered[min(len(ordered)-1, math.ceil(len(ordered)*q)-1)], 2)
rendered = re.search(r"Total frames rendered:\s*(\d+)", raw)
janky = re.search(r"Janky frames:\s*(\d+) \(([\d.]+)%\)", raw)
result = {"totalRenderedFrames": int(rendered[1]) if rendered else None, "jankyFrames": int(janky[1]) if janky else None, "jankyPercent": float(janky[2]) if janky else None, "recentFrameSamples": len(frames), "recentMedianMs": round(statistics.median(frames), 2), "recentP95Ms": percentile(.95) if len(frames) >= 30 else None, "recentP99Ms": percentile(.99) if len(frames) >= 30 else None, "recentOver700Ms": sum(x > 700 for x in frames), "systemSummary": [line.strip() for line in raw.splitlines() if re.search(r"Total frames rendered|Janky frames|percentile|Missed Vsync", line)]}
(directory / "ui-performance.json").write_text(json.dumps(result, indent=2), encoding="utf-8")
print(json.dumps(result, indent=2))
shell("rm", "/sdcard/archiman-audit.xml")
