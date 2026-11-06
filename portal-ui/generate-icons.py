"""Generate native vectors from pinned official Carbon SVGs; retain upstream provenance."""
from pathlib import Path
import json
import shutil
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parent
repo = root.parent
mapping = json.loads((root / "icon-map.json").read_text())
package = root / "node_modules/@carbon/icons"
version = json.loads((package / "package.json").read_text())["version"]
lines = [f"// Generated from @carbon/icons {version}. Copyright IBM Corp. Apache-2.0.",
    "package com.holagundi.archiman.ui.theme", "", "import androidx.compose.ui.graphics.Color",
    "import androidx.compose.ui.graphics.SolidColor", "import androidx.compose.ui.graphics.PathFillType",
    "import androidx.compose.ui.graphics.vector.ImageVector", "import androidx.compose.ui.graphics.vector.PathParser",
    "import androidx.compose.ui.unit.dp", "", "object CarbonIcons {"]
for alias, name in mapping.items():
    svg = ET.parse(package / "svg/32" / (name + ".svg")).getroot()
    assert svg.get("viewBox") == "0 0 32 32"
    mirrored = str(alias in ("ArrowBack", "ArrowForward", "ChevronRight")).lower()
    lines.extend([f"    val {alias}: ImageVector by lazy {{",
        f'        ImageVector.Builder(name="Carbon.{name}", defaultWidth=24.dp, defaultHeight=24.dp, viewportWidth=32f, viewportHeight=32f, autoMirror={mirrored}).apply {{'])
    for node in svg.iter():
        tag = node.tag.rsplit("}", 1)[-1]
        if node.get("fill") == "none":
            continue
        assert not node.get("transform"), "Add explicit transform support before using this SVG"
        if tag == "path":
            path = node.get("d")
        elif tag == "circle":
            cx, cy, radius = (float(node.get(key)) for key in ("cx", "cy", "r"))
            path = f"M {cx-radius},{cy} a {radius},{radius} 0 1,0 {2*radius},0 a {radius},{radius} 0 1,0 {-2*radius},0 Z"
        else:
            assert tag in ("svg", "title"), f"Unsupported element: {tag}"
            continue
        fill = "EvenOdd" if node.get("fill-rule") == "evenodd" else "NonZero"
        alpha = float(node.get("opacity", "1"))
        lines.append(f'            addPath(pathData=PathParser().parsePathString({json.dumps(path)}).toNodes(), fill=SolidColor(Color.Black), pathFillType=PathFillType.{fill}, fillAlpha={alpha}f)')
    lines.extend(["        }.build()", "    }"])
lines.append("}")
(repo / "app/src/main/java/com/holagundi/archiman/ui/theme/CarbonIcons.kt").write_text("\n".join(lines) + "\n", encoding="utf-8")
license_dir = repo / "third_party/carbon-icons"
license_dir.mkdir(parents=True, exist_ok=True)
shutil.copyfile(package / "LICENSE", license_dir / "LICENSE")
(license_dir / "SOURCE.json").write_text(json.dumps({"package": "@carbon/icons", "version": version, "mapping": mapping}, indent=2), encoding="utf-8")
print(f"Generated {len(mapping)} native Carbon icon mappings")
