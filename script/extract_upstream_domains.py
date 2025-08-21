#!/usr/bin/env python3
"""Extract plain domain rules from upstream downloaded lists.

Usage:
  python extract_upstream_domains.py <dns_dir> <content_dir> > upstream-domains.txt

Input directories come from update-upstream.sh (dns / content). We parse:
  1. ABP style rules: lines beginning with ||domain^ (capture domain)
  2. Hosts style: lines like "0.0.0.0 domain" or "127.0.0.1 domain" or ":: domain"
  3. Raw domain lines (single token containing a dot) if they appear in certain lists

We output unique lines in the form: ||domain^   (suitable for hostlist-compiler)
We skip domains containing wildcard '*' or '/' or starting with '#'.
"""
import sys
import re
from pathlib import Path

ABP_RULE = re.compile(r"^\|\|([a-z0-9][a-z0-9\-\.]*\.[a-z]{2,})\^$")
HOSTS_RULE = re.compile(r"^(?:0\.0\.0\.0|127\.0\.0\.1|::1?|::)\s+([a-z0-9][a-z0-9\-\.]*\.[a-z]{2,})\b")
PLAIN_DOMAIN = re.compile(r"^([a-z0-9][a-z0-9\-\.]*\.[a-z]{2,})$")


def collect_from_dir(dir_path: Path, bucket: set):
    if not dir_path.exists():
        return
    for fp in dir_path.glob('**/*'):
        if not fp.is_file():
            continue
        try:
            with fp.open('r', encoding='utf-8', errors='ignore') as f:
                for line in f:
                    line = line.strip()
                    if not line or line.startswith('!') or line.startswith('#'):
                        continue
                    if '*' in line or '/' in line:
                        # skip wildcard / path rules (keep it conservative)
                        pass
                    m = ABP_RULE.match(line)
                    if m:
                        bucket.add(m.group(1))
                        continue
                    m = HOSTS_RULE.match(line)
                    if m:
                        bucket.add(m.group(1))
                        continue
                    # Some lists provide plain domain only, guard length
                    if line.startswith('||') and line.endswith('^'):
                        # weird variant that failed regex due to unusual chars, skip
                        continue
                    if ' ' not in line and '\t' not in line:
                        m = PLAIN_DOMAIN.match(line)
                        if m:
                            bucket.add(m.group(1))
        except Exception:
            # Ignore unreadable file
            continue


def main():
    if len(sys.argv) < 3:
        print("Usage: extract_upstream_domains.py <dns_dir> <content_dir>", file=sys.stderr)
        sys.exit(1)
    dns_dir = Path(sys.argv[1])
    content_dir = Path(sys.argv[2])
    domains = set()
    collect_from_dir(dns_dir, domains)
    collect_from_dir(content_dir, domains)
    # Output sorted for reproducibility
    for d in sorted(domains):
        print(f"||{d}^")


if __name__ == '__main__':
    main()
