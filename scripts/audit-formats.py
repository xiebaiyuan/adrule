#!/usr/bin/env python3
"""
Audit: compare our output formats against anti-AD & Cats-Team AdRules.

Generates a capability matrix and warns about gaps.

Usage:
  python3 scripts/audit-formats.py <rule_dir> [--ci-warn]

Expected files (per anti-AD + AdRules alignment):
  [adblock+]   easylist.list          — EasyList-compatible
  [adguard]    adgh.txt               — AdGuard hostlist (source)
  [clash]      clash.yaml             — Clash / Mihomo YAML rule-set
  [dnsmasq]    dnsmasq.conf           — dnsmasq format
  [domain]     domains.txt            — plain domain list
  [loon]       loon.list              — Loon DOMAIN-SUFFIX
  [quanx]      quanx.list             — Quantumult X host-suffix
  [smartdns]   smartdns.conf          — SmartDNS
  [surge]      surge.list             — Surge DOMAIN-SUFFIX
  [surge2]     surge2.list            — Surge DOMAIN-SET
  [sing-box]   adrules-singbox.srs    — sing-box binary rule-set
  [mihomo]     adrules-mihomo.mrs     — mihomo binary rule-set
  [md5]        *.md5                  — MD5 checksums (key files)
"""

import os, sys, hashlib
from pathlib import Path

# ── capability matrix ────────────────────────────────────────────────────────
EXPECTED = [
    ("easylist.list",          "adblock+",  "EasyList-compatible (uBO, AdBlock, AdGuardHome)"),
    ("adgh.txt",               "adguard",   "AdGuard hostlist (compiler output)"),
    ("clash.yaml",             "clash",     "Clash / Mihomo YAML rule-set"),
    ("dnsmasq.conf",           "dnsmasq",   "dnsmasq format"),
    ("domains.txt",            "domain",    "Plain domain list"),
    ("loon.list",              "loon",      "Loon DOMAIN-SUFFIX"),
    ("quanx.list",             "quanx",     "Quantumult X host-suffix"),
    ("smartdns.conf",          "smartdns",  "SmartDNS"),
    ("surge.list",             "surge",     "Surge DOMAIN-SUFFIX"),
    ("surge2.list",            "surge2",    "Surge DOMAIN-SET"),
    ("adrules-singbox.srs",    "sing-box",  "sing-box binary rule-set"),
    ("adrules-mihomo.mrs",     "mihomo",    "Mihomo binary rule-set"),
]

MD5_FILES = [
    "adgh.txt",
    "easylist.list",
    "adrules-singbox.srs",
    "adrules-mihomo.mrs",
]


def fmt_size(path: Path) -> str:
    """Human-readable file size."""
    b = path.stat().st_size
    if b < 1024:
        return f"{b}B"
    elif b < 1024 ** 2:
        return f"{b / 1024:.1f}K"
    return f"{b / 1024 ** 2:.1f}M"


def audit(rule_dir: Path, ci_warn: bool = False) -> int:
    """Run audit. Returns count of missing files."""
    missing = []
    md5_missing = []

    print("=" * 62)
    print(f"  Format Capability Audit  —  {rule_dir}")
    print(f"  Targets: anti-AD + Cats-Team AdRules alignment")
    print("=" * 62)
    print()

    # ── file presence ────────────────────────────────────────────────────
    installed = 0
    print(f"  {'Format':<14} {'File':<28} {'Status':<10} Size")
    print(f"  {'-'*14} {'-'*28} {'-'*10} {'-'*8}")
    for filename, fmt_tag, desc in EXPECTED:
        fp = rule_dir / filename
        present = fp.is_file()
        status = "✅" if present else "❌"
        size = fmt_size(fp) if present else "-"
        print(f"  {fmt_tag:<14} {filename:<28} {status:<10} {size}")
        if present:
            installed += 1
        else:
            missing.append(filename)

    # ── MD5 checksums ────────────────────────────────────────────────────
    print()
    print(f"  {'File':<30} {'MD5':<6} {'Status'}")
    print(f"  {'-'*30} {'-'*6} {'-'*10}")
    for filename in MD5_FILES:
        fp = rule_dir / filename
        md5p = Path(str(fp) + ".md5")
        if not fp.is_file():
            print(f"  {filename:<30} {'-':<6} {'⏭️  (src missing)'}")
            continue
        if not md5p.is_file():
            print(f"  {filename:<30} {'❌':<6} {'missing .md5'}")
            md5_missing.append(str(md5p))
            continue
        # verify
        with open(fp, "rb") as f:
            actual = hashlib.md5(f.read()).hexdigest()
        with open(md5p) as f:
            declared = f.read().split()[0]
        ok = actual == declared
        mark = "✅" if ok else "❌"
        print(f"  {filename:<30} {mark:<6} {'match' if ok else 'MISMATCH'}")

    # ── rule count sanity ────────────────────────────────────────────────
    print()
    print(f"  Rule Counts:")
    print(f"  {'-'*48}")
    for filename, fmt_tag, _ in EXPECTED:
        fp = rule_dir / filename
        if not fp.is_file() or fmt_tag in ("sing-box", "mihomo"):
            continue
        with open(fp, "rb") as f:
            # count non-header lines (skip lines starting with ! / #)
            count = sum(
                1 for line in f
                if line.strip()
                and not line.startswith(b"!")
                and not line.startswith(b"#")
                and not line.startswith(b"[")
            )
        print(f"  {fmt_tag:<14} {filename:<28} {count:>8,} rules")

    # ── summary ──────────────────────────────────────────────────────────
    total = len(EXPECTED)
    print()
    print(f"  Summary: {installed}/{total} files present")
    if missing:
        print(f"  Missing: {', '.join(missing)}")
    if md5_missing:
        print(f"  MD5 missing: {', '.join(md5_missing)}")

    if missing or md5_missing:
        if ci_warn:
            print(f"\n  ⚠️  CI WARNING: format gaps detected")
        print()
        print(f"  To fix missing formats:")
        print(f"    - text formats:  python3 scripts/convert.py {rule_dir}")
        print(f"    - binary formats: bash scripts/convert-binary.sh {rule_dir}")
        print()

    return len(missing)


if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser(description="Audit rule format coverage")
    parser.add_argument("rule_dir", nargs="?", default="rule",
                        help="rule output directory")
    parser.add_argument("--ci-warn", action="store_true",
                        help="exit with error code if gaps found (for CI)")
    args = parser.parse_args()

    missing = audit(Path(args.rule_dir), args.ci_warn)
    if args.ci_warn and missing:
        sys.exit(1)
