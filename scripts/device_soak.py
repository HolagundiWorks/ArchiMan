"""Run isolated device regressions repeatedly; never reset the installed practice."""
import argparse
import json
import pathlib
import subprocess
import time

parser = argparse.ArgumentParser()
parser.add_argument("--adb", default="adb")
parser.add_argument("--minutes", type=int, default=120)
args = parser.parse_args()
assert 1 <= args.minutes <= 180
directory = pathlib.Path("app/build/audit/soak")
directory.mkdir(parents=True, exist_ok=True)
deadline = time.monotonic() + args.minutes * 60
results = []
while time.monotonic() < deadline:
    started = time.monotonic()
    result = subprocess.run([args.adb, "shell", "am", "instrument", "-w", "com.holagundi.archiman.test/com.holagundi.archiman.BackupDeviceChecks"], capture_output=True, text=True, timeout=600)
    passed = result.returncode == 0 and "10,000-row audit checks passed using isolated data" in result.stdout and "failed:" not in result.stdout.lower()
    (directory / f"cycle-{len(results) + 1}.txt").write_text(result.stdout + result.stderr)
    results.append({"cycle": len(results) + 1, "seconds": round(time.monotonic() - started, 2), "passed": passed})
    (directory / "results.json").write_text(json.dumps(results, indent=2))
    print(json.dumps(results[-1]), flush=True)
    if not passed:
        raise SystemExit("Soak regression failed; inspect the cycle evidence")
print(f"Completed {len(results)} isolated regression cycles.")
