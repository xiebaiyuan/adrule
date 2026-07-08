#!/usr/bin/env python3
"""
Convert hostlist-compiler output (adgh.txt) into multiple rule formats.

Reads ||domain^ rules from adgh.txt, extracts domain names, skips
exception rules (@@), and produces:
  - surge.list / loon.list    DOMAIN-SUFFIX,domain
  - clash.yaml               YAML payload
  - domains.txt              plain domain list

Usage: python3 scripts/convert.py <rule_dir>
"""

import re, sys, os, argparse
from datetime import datetime
from pathlib import Path


RE_ADBLOCK = re.compile(r'^\|\|([a-z0-9.\-]+)\^')
RE_HOSTS   = re.compile(r'^\d+\.\d+\.\d+\.\d+\s+([a-z0-9.\-]+)')
RE_DOMAIN  = re.compile(r'^([a-z0-9.\-]+\.[a-z]{2,})$')


def parse_adgh(path: str) -> list[str]:
    """Parse adgh.txt, return deduplicated sorted block domains."""
    domains: set[str] = set()
    with open(path, 'r', encoding='utf-8') as f:
        for line in f:
            raw = line.strip()
            if not raw or raw.startswith('!') or raw.startswith('#'):
                continue
            # Skip exception/allow rules
            if raw.startswith('@@'):
                continue

            # ||domain^  (standard adblock)
            m = RE_ADBLOCK.match(raw)
            if m:
                d = m.group(1).lower()
                # Skip invalid domains (must start with alphanumeric)
                if not d[0].isalnum():
                    continue
                domains.add(d)
                continue

            # 0.0.0.0 domain  (hosts format)
            m = RE_HOSTS.match(raw)
            if m:
                d = m.group(1).lower()
                if not d[0].isalnum():
                    continue
                domains.add(d)
                continue

            # bare domain (domains-only syntax)
            m = RE_DOMAIN.match(raw)
            if m:
                d = m.group(1).lower()
                if not d[0].isalnum():
                    continue
                domains.add(d)
                continue

    return sorted(domains)


def write_surge(domains: list[str], path: str):
    """DOMAIN-SUFFIX list — works in Surge, Loon, Shadowrocket."""
    with open(path, 'w', encoding='utf-8') as f:
        f.write('# Surge/Loon rule set — generated from adrule\n')
        f.write(f'# Total: {len(domains)} domains\n')
        for d in domains:
            f.write(f'DOMAIN-SUFFIX,{d}\n')


def write_surge2(domains: list[str], path: str, repo_url: str = "https://github.com/xiebaiyuan/adrule"):
    """Surge DOMAIN-SET format — one .domain per line, matching anti-AD format.

    Usage in Surge config: DOMAIN-SET,https://path/to/surge2.list,REJECT
    """
    raw_url = f"https://raw.githubusercontent.com/xiebaiyuan/adrule/refs/heads/main/rule/surge2.list"
    with open(path, 'w', encoding='utf-8') as f:
        f.write(f'#TITLE=adrule\n')
        f.write(f'#VER={datetime.now().strftime("%Y%m%d%H%M%S")}\n')
        f.write(f'#URL={repo_url}\n')
        f.write(f'#TOTAL_LINES={len(domains)}\n')
        f.write('\n')
        f.write(f'#DOMAIN-SET,{raw_url},REJECT\n')
        f.write('\n')
        for d in domains:
            f.write(f'.{d}\n')


def write_quanx(domains: list[str], path: str):
    """Quantumult X format: host-suffix,domain,reject"""
    with open(path, 'w', encoding='utf-8') as f:
        f.write('# Quantumult X — generated from adrule\n')
        f.write(f'# Total: {len(domains)} domains\n')
        for d in domains:
            f.write(f'host-suffix,{d},reject\n')


def write_dnsmasq(domains: list[str], path: str):
    """dnsmasq format: address=/domain/"""
    with open(path, 'w', encoding='utf-8') as f:
        f.write('# dnsmasq — generated from adrule\n')
        f.write(f'# Total: {len(domains)} domains\n')
        for d in domains:
            f.write(f'address=/{d}/\n')


def write_smartdns(domains: list[str], path: str):
    """SmartDNS format: address /domain/#"""
    with open(path, 'w', encoding='utf-8') as f:
        f.write('# SmartDNS — generated from adrule\n')
        f.write(f'# Total: {len(domains)} domains\n')
        for d in domains:
            f.write(f'address /{d}/#\n')


def write_clash(domains: list[str], path: str):
    """Clash/Mihomo domain rule-set YAML."""
    with open(path, 'w', encoding='utf-8') as f:
        f.write(f'# Clash/Mihomo rule-set — generated from adrule\n')
        f.write(f'# Total: {len(domains)} domains\n')
        f.write('payload:\n')
        for d in domains:
            f.write(f"  - '+.{d}'\n")  # '+.' = DOMAIN-SUFFIX in clash


def write_domains(domains: list[str], path: str):
    """Plain domain list, one per line."""
    with open(path, 'w', encoding='utf-8') as f:
        f.write(f'# Plain domain list — generated from adrule\n')
        f.write(f'# Total: {len(domains)} domains\n')
        for d in domains:
            f.write(f'{d}\n')


def main():
    parser = argparse.ArgumentParser(description='Convert adgh.txt to rule formats')
    parser.add_argument('rule_dir', nargs='?', default='rule',
                        help='rule output directory (default: rule/)')
    args = parser.parse_args()

    rule_dir = Path(args.rule_dir)
    adgh = rule_dir / 'adgh.txt'

    if not adgh.exists():
        print(f'Error: {adgh} not found. Run hostlist-compiler first.', file=sys.stderr)
        sys.exit(1)

    print(f'Parsing {adgh}...')
    domains = parse_adgh(str(adgh))
    print(f'  Extracted {len(domains)} unique block domains')

    write_surge(domains, str(rule_dir / 'surge.list'))
    print(f'  → surge.list ({len(domains)} rules)')

    write_surge2(domains, str(rule_dir / 'surge2.list'))
    print(f'  → surge2.list ({len(domains)} rules — DOMAIN-SET)')

    write_quanx(domains, str(rule_dir / 'quanx.list'))
    print(f'  → quanx.list ({len(domains)} rules — Quantumult X)')

    write_dnsmasq(domains, str(rule_dir / 'dnsmasq.conf'))
    print(f'  → dnsmasq.conf ({len(domains)} rules)')

    write_smartdns(domains, str(rule_dir / 'smartdns.conf'))
    print(f'  → smartdns.conf ({len(domains)} rules)')

    write_surge(domains, str(rule_dir / 'loon.list'))
    print(f'  → loon.list ({len(domains)} rules)')

    write_clash(domains, str(rule_dir / 'clash.yaml'))
    print(f'  → clash.yaml ({len(domains)} rules)')

    write_domains(domains, str(rule_dir / 'domains.txt'))
    print(f'  → domains.txt ({len(domains)} domains)')


if __name__ == '__main__':
    main()
