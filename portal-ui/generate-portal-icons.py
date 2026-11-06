"""Generate inline Carbon SVG markup for the Wi-Fi portal shell."""
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parent
svg_dir = root / "node_modules/@carbon/icons/svg/32"
needed = {
    "dashboard": "dashboard",
    "building": "building",
    "document": "document",
    "ruler": "ruler",
    "users": "user--multiple",
    "wifi": "wifi",
    "backup": "data-backup",
    "enterprise": "enterprise",
    "task": "task",
    "notebook": "notebook",
    "arrow": "arrow--right",
    "logout": "logout",
    "home": "home",
    "download": "download",
    "upload": "upload",
    "receipt": "receipt",
    "folder": "folder--open",
    "partnership": "partnership",
    "construction": "construction",
    "chart": "chart--bar",
    "wallet": "wallet",
    "add": "add",
    "close": "close",
    "architecture": "ruler",
}

entries = []
for alias, file in needed.items():
    path = svg_dir / f"{file}.svg"
    if not path.exists():
        raise SystemExit(f"Missing icon SVG: {file}")
    svg = ET.parse(path).getroot()
    parts = []
    for node in svg.iter():
        tag = node.tag.rsplit("}", 1)[-1]
        if node.get("fill") == "none":
            continue
        if tag == "path":
            d = node.get("d")
            fr = node.get("fill-rule")
            attr = f' fill-rule="{fr}"' if fr else ""
            parts.append(f'<path d="{d}"{attr}/>')
        elif tag == "circle":
            parts.append(
                f'<circle cx="{node.get("cx")}" cy="{node.get("cy")}" r="{node.get("r")}"/>'
            )
    body = "".join(parts).replace("\\", "\\\\").replace('"', '\\"')
    entries.append(f'        "{alias}" to "{body}",')

out = root.parent / "app/src/main/java/com/holagundi/archiman/portal/CarbonPortalIcons.kt"
out.write_text(
    "\n".join(
        [
            "package com.holagundi.archiman.portal",
            "",
            "/** Inline Carbon Icons (Apache-2.0) for the Wi-Fi portal shell. Generated from @carbon/icons. */",
            "object CarbonPortalIcons {",
            '    fun svg(name: String, extraClass: String = ""): String {',
            '        val body = icons[name] ?: icons.getValue("document")',
            '        val cls = ("carbon-icon " + extraClass).trim()',
            '        return """<svg class="$cls" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32" width="20" height="20" aria-hidden="true" focusable="false">$body</svg>"""',
            "    }",
            "",
            "    private val icons = mapOf(",
            *entries,
            "    )",
            "}",
            "",
        ]
    ),
    encoding="utf-8",
)
print(f"Wrote {len(needed)} portal icons to {out}")
