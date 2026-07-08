# AdGuard Rule Collection

**Multi-source AdGuard / AdGuardHome rule aggregation — with Surge, Loon, Clash output.**

Aggregates 14 curated upstream blocklists via [AdGuard HostlistCompiler](https://github.com/AdguardTeam/HostlistCompiler), then converts to multiple rule formats. Fully automated via GitHub Actions (daily 6AM/6PM CST).

> ⚠️ **Disclaimer**
>
> This project is created **for educational and learning purposes only**. It aggregates publicly available blocklists to study rule aggregation, format conversion, and CI/CD pipelines. The author makes no guarantee about the accuracy, completeness, or suitability of the resulting rule sets for any particular use. Users assume all responsibility for any consequences arising from the use of this project or its outputs. If you are seeking a production-grade ad-blocking solution, please refer directly to the [upstream sources](#-upstream-sources) maintained by their respective authors.

## 📦 Output Files

| File | Format | Description |
|---|---|---|
| `rule/adgh.txt` | AdGuard Home | AGH-validated `\|\|domain^` rules (hostlist-compiler) |
| `rule/surge.list` | Surge | `DOMAIN-SUFFIX,domain` |
| `rule/loon.list` | Loon | `DOMAIN-SUFFIX,domain` |
| `rule/clash.yaml` | Clash/Mihomo | YAML rule-set with `'+.domain'` |
| `rule/domains.txt` | Plain | One domain per line |

**Direct download:** `https://raw.githubusercontent.com/xiebaiyuan/adrule/main/rule/<filename>`

### Usage in clients

**Surge:**
```
RULE-SET,https://raw.githubusercontent.com/xiebaiyuan/adrule/main/rule/surge.list,REJECT
```

**Loon:**
```
https://raw.githubusercontent.com/xiebaiyuan/adrule/main/rule/loon.list
```

**Clash / Mihomo:**
```yaml
rule-providers:
  adrule:
    type: http
    behavior: domain
    url: https://raw.githubusercontent.com/xiebaiyuan/adrule/main/rule/clash.yaml
    interval: 86400
```

**AdGuard Home:**
Add as DNS allow/blocklist filter.

## 🗺️ Upstream Sources

14 curated sources, selected for low overlap and high relevance to Chinese users.

| # | Source | Maintainer | Why included |
|---|---|---|---|
| 1 | [Scam Blocklist](https://github.com/durablenapkin/scamblocklist) | @durablenapkin | Scam/fraud domains — anti-AD doesn't cover |
| 2 | [WindowsSpyBlocker](https://github.com/crazy-max/WindowsSpyBlocker) | @crazy-max | Telemetry hosts from Windows/Office |
| 3 | [AdGuard Base](https://github.com/AdguardTeam/FiltersRegistry) | AdGuard Team | Core AdGuard filter — extensive coverage |
| 4 | [AdGuard CNAME Tracker](https://github.com/AdguardTeam/cname-trackers) | AdGuard Team | CNAME-disguised trackers — unique, 45% of rule volume |
| 5 | [AdGuard DNS Filter](https://adguardteam.github.io/AdGuardSDNSFilter) | AdGuard Team | DNS-level blocking, overlaps minimally with anti-AD after dedup |
| 6 | [AdditionalFiltersCN](https://github.com/Crystal-RainSlide/AdditionalFiltersCN) | @Crystal-RainSlide | Chinese-specific supplements |
| 7 | [EasyList Anti-Adblock](https://easylist-downloads.adblockplus.org/antiadblockfilters.txt) | EasyList | Bypass anti-adblock walls |
| 8 | [AWAvenue-Ads-Rule](https://github.com/TG-Twilight/AWAvenue-Ads-Rule) | @TG-Twilight | Active Chinese ads list |
| 9 | [AdGuard Mobile Ads](https://filters.adtidy.org/extension/ublock/filters/11.txt) | AdGuard | Mobile web ads — complements phone usage |
| 10 | [AdGuard Mobile App ads](https://github.com/AdguardTeam/AdguardFilters) | AdGuard Team | In-app ad SDK domains — zero overlap with desktop-oriented sources |
| 11 | [anti-AD](https://github.com/privacy-protection-tools/anti-AD) | @privacy-protection-tools | Mainland China's highest-hit-rate ad list — primary source |
| 12 | [anti-AD PCDN](https://github.com/privacy-protection-tools/anti-AD) | @privacy-protection-tools | PCDN (P2P CDN) traffic — bandwidth waste prevention |
| 13 | [anti-AD HTTPDNS](https://github.com/privacy-protection-tools/anti-AD) | @privacy-protection-tools | HTTPDNS endpoints — prevents DNS hijack bypass |

### Sources pruned (with respect)

The following sources were previously used but removed after evaluation:

| Source | Reason |
|---|---|
| [blackmatrix7 Privacy](https://github.com/blackmatrix7/ios_rule_script) | 75% domain overlap with anti-AD; remaining 10k domains are 58% analytics (`smetrics.*`, `*.actonservice.com`) — high false-positive risk for analytics, not ads |
| [koolproxy adg rules](https://github.com/ilxp/koolproxy) | 156k lines but only 7 `\|\|domain^` rules extracted — 99.9% cosmetic (`##`) rules that hostlist-compiler discards |
| [uBlock Filters (main)](https://github.com/uBlockOrigin/uAssets) | Consistently triggers GitHub raw CDN 429 via `!#include` chains; 241 extracted rules fully covered by anti-AD |

## 🏗️ Architecture

The pipeline runs on the `ci` branch — `main` only contains generated rule files.

```
ci branch:
  config/hostlist-compiler.json      ← 14 curated upstream sources
  scripts/
    hostlist-compiler-retry.sh       ← auto-retries with CDN fallback on 429
    convert.py                       ← extract domains → multi-format
  .github/workflows/auto-update.yml  ← checks out both branches, writes to main

main branch:
  rule/
    adgh.txt      ← AGH format (||domain^)
    surge.list    ← DOMAIN-SUFFIX,domain
    loon.list     ← DOMAIN-SUFFIX,domain
    clash.yaml    ← YAML rule-set
    domains.txt   ← plain domains
```

### Update interval

Rules are automatically regenerated every 12 hours (6AM/6PM CST) via GitHub Actions.

## 🙏 Credits

This project would not exist without the incredible work of the open-source community. Sincere thanks to:

### Active upstream maintainers

- **[anti-AD](https://github.com/privacy-protection-tools/anti-AD)** — Chinese ad/tracking prevention, the primary source for this project
- **[AdGuard Team](https://github.com/AdguardTeam)** — HostlistCompiler, DNS filter, CNAME tracker, Base filter, Mobile filter — the backbone of this project
- **[uBlock Origin](https://github.com/uBlockOrigin/uAssets)** — Badware risks, Privacy, Quick fixes, Resource abuse, Unbreak
- **[EasyList](https://easylist.to)** — Foundational blocklists including anti-adblock
- **[Scam Blocklist](https://github.com/durablenapkin/scamblocklist)** — @durablenapkin
- **[WindowsSpyBlocker](https://github.com/crazy-max/WindowsSpyBlocker)** — @crazy-max
- **[AdditionalFiltersCN](https://github.com/Crystal-RainSlide/AdditionalFiltersCN)** — @Crystal-RainSlide
- **[AWAvenue-Ads-Rule](https://github.com/TG-Twilight/AWAvenue-Ads-Rule)** — @TG-Twilight
- **[MobileFilter Sections](https://github.com/AdguardTeam/AdguardFilters)** — AdGuard Team

### Previous sources (removed but appreciated)

- **[blackmatrix7](https://github.com/blackmatrix7/ios_rule_script)** — Excellent iOS rule collection; removed due to overlap but the work is respected
- **[koolproxy](https://github.com/ilxp/koolproxy)** — One of the earliest Chinese ad-filter projects

### Inspiration

- **[Cats-Team / AdRules](https://github.com/Cats-Team/AdRules)** — Reference for multi-source aggregation
- **[hululu1068 / AdGuard-Rule](https://github.com/hululu1068/AdGuard-Rule)** — Early inspiration and resources

### Tools

- [AdGuard HostlistCompiler](https://github.com/AdguardTeam/HostlistCompiler) — AGH-format validation and compilation
- jsDelivr — CDN mirror for upstream sources under rate-limit conditions

## 📄 License

MIT
