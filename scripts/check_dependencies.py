"""Fail closed on incomplete OSV responses or published advisory matches."""
import json
import pathlib
import urllib.request

directory = pathlib.Path("app/build/audit")
request = json.loads((directory / "osv-request.json").read_text())
findings = []
checked = 0
for offset in range(0, len(request["queries"]), 100):
    queries = request["queries"][offset:offset + 100]
    body = json.dumps({"queries": queries}).encode()
    call = urllib.request.Request("https://api.osv.dev/v1/querybatch", data=body, headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(call, timeout=60) as response:
        result = json.load(response)
    if len(result.get("results", [])) != len(queries):
        raise RuntimeError("Incomplete vulnerability lookup; cannot report a clean scan")
    for query, item in zip(queries, result["results"]):
        if item.get("error") or item.get("next_page_token"):
            raise RuntimeError("Unresolved or truncated vulnerability lookup")
        checked += 1
        if item.get("vulns"):
            findings.append({"package": query["package"], "advisories": item["vulns"]})
(directory / "dependency-findings.json").write_text(json.dumps({"checked": checked, "findings": findings}, indent=2))
print(f"Checked {checked} exact Maven versions; {len(findings)} packages with advisory matches.")
raise SystemExit(1 if findings or checked == 0 else 0)
