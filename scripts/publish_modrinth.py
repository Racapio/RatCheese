"""Publish Rat Cheese Helper to Modrinth.

Updates the project description (docs/modrinth-description.md) and issue link,
then uploads every jar in build/libs that is not on Modrinth yet — one version
per Minecraft target (ratcheese-<mod>+<mc>.jar).

The token is read from the MODRINTH_TOKEN environment variable and is never
written anywhere. It needs the PROJECT_WRITE and VERSION_CREATE scopes.

Usage:
    python scripts/publish_modrinth.py --dry-run   # show what would happen
    python scripts/publish_modrinth.py             # actually publish
"""
import argparse
import json
import os
import re
import sys
import urllib.error
import urllib.request
import uuid
from pathlib import Path

API = "https://api.modrinth.com/v2"
PROJECT = "rat-cheese-helper"
USER_AGENT = "Racapio/RatCheese-publish (github.com/Racapio/RatCheese)"
DEPENDENCIES = [
    {"project_id": "P7dR8mSH", "dependency_type": "required"},  # Fabric API
    {"project_id": "mOgUt4GM", "dependency_type": "optional"},  # Mod Menu
]
ROOT = Path(__file__).resolve().parent.parent
JAR_NAME = re.compile(r"^ratcheese-(\d+\.\d+\.\d+)\+(.+)\.jar$")


def request(method, path, token=None, body=None, content_type="application/json"):
    headers = {"User-Agent": USER_AGENT}
    if token:
        headers["Authorization"] = token
    if body is not None:
        headers["Content-Type"] = content_type
        if isinstance(body, (dict, list)):
            body = json.dumps(body).encode("utf-8")
    req = urllib.request.Request(API + path, data=body, method=method, headers=headers)
    try:
        with urllib.request.urlopen(req) as resp:
            raw = resp.read()
            return json.loads(raw) if raw else None
    except urllib.error.HTTPError as e:
        detail = e.read().decode("utf-8", "replace")
        raise SystemExit(f"{method} {path} failed: HTTP {e.code} {detail}")


def version_key(v):
    return tuple(int(p) for p in v.split("."))


def changelog_since(published_base):
    """CHANGELOG.md sections newer than the newest version already on Modrinth."""
    text = (ROOT / "CHANGELOG.md").read_text(encoding="utf-8")
    sections = re.split(r"(?m)^## ", text)[1:]
    keep = []
    for section in sections:
        m = re.match(r"\[(\d+\.\d+\.\d+)\]", section)
        if m and (published_base is None or version_key(m.group(1)) > version_key(published_base)):
            keep.append("## " + section.strip())
    return "\n\n".join(keep)


def multipart(data, jar):
    boundary = uuid.uuid4().hex
    parts = [
        f"--{boundary}\r\nContent-Disposition: form-data; name=\"data\"\r\n"
        f"Content-Type: application/json\r\n\r\n{json.dumps(data)}\r\n".encode("utf-8"),
        f"--{boundary}\r\nContent-Disposition: form-data; name=\"file\"; filename=\"{jar.name}\"\r\n"
        f"Content-Type: application/java-archive\r\n\r\n".encode("utf-8") + jar.read_bytes() + b"\r\n",
        f"--{boundary}--\r\n".encode("utf-8"),
    ]
    return b"".join(parts), f"multipart/form-data; boundary={boundary}"


def main():
    # Windows consoles may not encode em dashes from the changelog; never crash on output.
    sys.stdout.reconfigure(errors="replace")
    parser = argparse.ArgumentParser()
    parser.add_argument("--dry-run", action="store_true", help="print the plan, send nothing")
    args = parser.parse_args()

    token = os.environ.get("MODRINTH_TOKEN", "").strip()
    if not token and not args.dry_run:
        raise SystemExit("Set MODRINTH_TOKEN first (Modrinth > Settings > PATs).")

    project = request("GET", f"/project/{PROJECT}")
    published = request("GET", f"/project/{PROJECT}/version")
    published_numbers = {v["version_number"] for v in published}
    bases = [v["version_number"].split("+")[0] for v in published]
    newest_base = max(bases, key=version_key) if bases else None
    print(f"Project {project['title']} ({project['id']}), on Modrinth: {sorted(published_numbers) or 'nothing'}")

    jars = sorted((ROOT / "build" / "libs").glob("ratcheese-*+*.jar"))
    uploads = []
    for jar in jars:
        m = JAR_NAME.match(jar.name)
        if not m:
            continue
        mod_version, mc = m.groups()
        number = f"{mod_version}+{mc}"
        if number in published_numbers:
            print(f"  skip {jar.name}: already published")
            continue
        uploads.append((jar, mod_version, mc, number))
    if not uploads:
        print("No new jars to upload. Run buildAll.ps1 first if you expected some.")

    changelog = changelog_since(newest_base)
    description = (ROOT / "docs" / "modrinth-description.md").read_text(encoding="utf-8")
    patch = {"body": description, "issues_url": "https://github.com/Racapio/RatCheese/issues"}

    print(f"\nWill update project description ({len(description)} chars) and issue link.")
    for jar, _, mc, number in uploads:
        print(f"Will upload {jar.name} as {number} for Minecraft {mc}")
    print(f"\nChangelog for new versions:\n{changelog or '(empty)'}\n")
    if args.dry_run:
        print("Dry run — nothing sent.")
        return

    request("PATCH", f"/project/{project['id']}", token, patch)
    print("Description and issue link updated.")

    for jar, mod_version, mc, number in uploads:
        data = {
            "project_id": project["id"],
            "name": f"Rat Cheese Helper {mod_version} for {mc}",
            "version_number": number,
            "changelog": changelog,
            "dependencies": DEPENDENCIES,
            "game_versions": [mc],
            "version_type": "release",
            "loaders": ["fabric"],
            "featured": False,
            "status": "listed",
            "file_parts": ["file"],
            "primary_file": "file",
        }
        body, content_type = multipart(data, jar)
        created = request("POST", "/version", token, body, content_type)
        print(f"Uploaded {number}: https://modrinth.com/mod/{PROJECT}/version/{created['id']}")


if __name__ == "__main__":
    sys.exit(main())
