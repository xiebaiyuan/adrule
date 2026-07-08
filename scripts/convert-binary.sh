#!/usr/bin/env bash
# Generate sing-box (.srs) and mihomo (.mrs) binary rule-set files.
#
# Usage: bash scripts/convert-binary.sh <rule_dir>
#
# Depends: sing-box, mihomo (installed via curl on CI, or local brew)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RULE_DIR="${1:-"${SCRIPT_DIR}/../rule"}"
TOOLS_DIR="${RULE_DIR}/.tools"
mkdir -p "$RULE_DIR" "$TOOLS_DIR"

# ----- download helpers for CI -----------------------------------------------
ensure_sing_box() {
    if command -v sing-box &>/dev/null; then
        echo "Using system sing-box: $(command -v sing-box)"
        return 0
    fi
    local ver="${SING_BOX_VERSION:-1.11.11}"
    local url="https://github.com/SagerNet/sing-box/releases/download/v${ver}/sing-box-${ver}-linux-amd64.tar.gz"
    local bin="${TOOLS_DIR}/sing-box"
    if [[ ! -x "$bin" ]]; then
        echo "Downloading sing-box ${ver}..."
        curl -sL "$url" | tar -xz -C "$TOOLS_DIR" --strip-components=1 "sing-box-${ver}-linux-amd64/sing-box"
        chmod +x "$bin"
    fi
    export PATH="${TOOLS_DIR}:${PATH}"
}

ensure_mihomo() {
    if command -v mihomo &>/dev/null; then
        echo "Using system mihomo"
        return 0
    fi
    local ver="${MIHOMO_VERSION:-v1.19.20}"
    local url="https://github.com/MetaCubeX/mihomo/releases/download/${ver}/mihomo-linux-amd64-${ver}.gz"
    local bin="${TOOLS_DIR}/mihomo"
    if [[ ! -x "$bin" ]]; then
        echo "Downloading mihomo ${ver}..."
        curl -sL "$url" | gzip -d > "$bin"
        chmod +x "$bin"
    fi
    export PATH="${TOOLS_DIR}:${PATH}"
}

# ----- conversion ------------------------------------------------------------
ADGH="${RULE_DIR}/adgh.txt"
DOMAINS="${RULE_DIR}/domains.txt"
EASYLIST="${RULE_DIR}/easylist.list"
SRS_OUT="${RULE_DIR}/adrules-singbox.srs"
MRS_OUT="${RULE_DIR}/adrules-mihomo.mrs"

if [[ ! -f "$ADGH" ]]; then
    echo "Error: ${ADGH} not found. Run hostlist-compiler first." >&2
    exit 1
fi

# Use easylist if available (cleaner for sing-box), else adgh.txt
INPUT_SRS="$EASYLIST"
if [[ ! -f "$EASYLIST" ]]; then
    echo "Warning: easylist.list not found, falling back to adgh.txt" >&2
    INPUT_SRS="$ADGH"
fi

ensure_sing_box
echo "Converting to sing-box rule-set (${SRS_OUT})..."
sing-box rule-set convert "$INPUT_SRS" -t adguard --output "$SRS_OUT"
echo "  → $(ls -lh "$SRS_OUT" | awk '{print $5}')"

ensure_mihomo
echo "Converting to mihomo rule-set (${MRS_OUT})..."
mihomo convert-ruleset domain text "$DOMAINS" "$MRS_OUT"
echo "  → $(ls -lh "$MRS_OUT" | awk '{print $5}')"

# ── MD5 checksums for binary outputs ────────────────────────────────────────
for f in "$SRS_OUT" "$MRS_OUT"; do
    if command -v md5sum &>/dev/null; then
        digest=$(md5sum "$f" | cut -d' ' -f1)
    else
        digest=$(md5 -r "$f" | cut -d' ' -f1)
    fi
    echo "${digest}  $(basename "$f")" > "${f}.md5"
    echo "  → $(basename "${f}.md5") (${digest})"
done
