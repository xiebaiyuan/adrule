#!/usr/bin/env bash
# Retry hostlist-compiler with dynamic CDN fallback for 429 rate limits.
#
# On 429 from raw.githubusercontent.com, swap the offending owner/repo's
# source URLs to jsDelivr CDN mirror & retry on a temp config copy.
# Original config file never modified.
set -euo pipefail

CONFIG="${1:-config/hostlist-compiler.json}"
OUTPUT="${2:-rule/adgh-hostlistcompiler.txt}"
MAX_ATTEMPTS="${HC_MAX_ATTEMPTS:-12}"

log() { echo "[$i/$MAX_ATTEMPTS] $*"; }

_hc_attempt=0

rm -f /tmp/hc_config.$$ /tmp/hc_err.$$

for i in $(seq 1 "$MAX_ATTEMPTS"); do
    log "Running hostlist-compiler..."

    # Use temp config so original never modified
    cfg="$CONFIG"
    [ -f /tmp/hc_config.$$ ] && cfg="/tmp/hc_config.$$"

    set +e
    hostlist-compiler -c "$cfg" -o "$OUTPUT" 2>/tmp/hc_err.$$
    rc=$?
    set -e

    if [ $rc -eq 0 ]; then
        log "Success."
        rm -f /tmp/hc_config.$$ /tmp/hc_err.$$
        exit 0
    fi

    err=$(cat /tmp/hc_err.$$)

    # Check for 429 on raw.githubusercontent.com
    failed=$(echo "$err" \
        | grep -oE 'https?://raw\.githubusercontent\.com/[^" '"'"'<>]+' \
        | head -1 || true)

    if [ -z "$failed" ]; then
        log "No raw.githubusercontent.com 429. Sleeping 30s before retry..."
        sleep 30
        continue
    fi

    # Extract owner/repo from failed URL
    # URL pattern: https://raw.githubusercontent.com/$owner/$repo/refs/heads/$branch/...
    owner_repo=$(echo "$failed" | sed -nE \
        's|https?://raw\.githubusercontent\.com/([^/]+/[^/]+)/.*|\1|p')

    log "429 on: $failed"
    log "Owner/repo: $owner_repo"

    # First attempt: copy original config to temp (if not already)
    if [ ! -f /tmp/hc_config.$$ ]; then
        cp "$CONFIG" /tmp/hc_config.$$
    fi

    # Swap all source URLs from this owner/repo to jsDelivr CDN
    python3 - /tmp/hc_config.$$ "$owner_repo" <<'PYEOF'
import json, sys, re

config_path = sys.argv[1]
owner_repo = sys.argv[2]

with open(config_path) as f:
    config = json.load(f)

RAW_PREFIX = "https://raw.githubusercontent.com/"
CDN_PREFIX = "https://cdn.jsdelivr.net/gh/"
# Handle both long form (/refs/heads/branch/path) and short form (/branch/path)
RAW_RE = re.compile(
    r"^" + re.escape(RAW_PREFIX)
    + r"([^/]+/[^/]+)/(?:refs/heads/)?([^/]+)/(.+)$"
)

swapped = 0
for src in config["sources"]:
    url = src.get("source", "")
    if RAW_PREFIX not in url:
        continue
    m = RAW_RE.match(url)
    if not m:
        continue
    # m.group(1) = owner/repo, e.g. "uBlockOrigin/uAssets"
    # m.group(2) = branch, e.g. "master"
    # m.group(3) = path, e.g. "filters/filters.txt"
    if m.group(1) == owner_repo:
        new_url = f"{CDN_PREFIX}{m.group(1)}@{m.group(2)}/{m.group(3)}"
        print(f"  Swap: {src.get('name', url)}")
        print(f"    ->  {new_url}")
        src["source"] = new_url
        swapped += 1

if swapped:
    with open(config_path, "w") as f:
        json.dump(config, f, indent=2)
    print(f"Swapped {swapped} source(s) to CDN.")
else:
    print(f"No matching source for {owner_repo}, retrying raw.")
PYEOF

    log "Retrying with CDN mirrors..."
done

log "FAILED after $MAX_ATTEMPTS attempts."
rm -f /tmp/hc_config.$$ /tmp/hc_err.$$
exit 1
