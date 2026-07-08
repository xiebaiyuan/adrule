# CLAUDE.md — AdGuard Rule Collection

## 分支策略

### `main` — 只含规则文件
- 仅追踪 `rule/*` 下的规则产物（adgh.txt、clash.yaml、surge.list 等）
- 无脚本、无 CI 配置、无工具
- 保持干净，方便他人 fork 直接取规则
- CI 通过 GitHub Actions 定时更新 main（每天 6:00 / 18:00 CST）

### `ci` — 工具脚本 + CI 配置
- 存所有支撑脚本、CI workflow、配置文件
- 包含：
  - `scripts/convert.py` — 文本格式转换 (Surge/Clash/Loon/Quantumult X/dnsmasq/smartdns/EasyList/domains + MD5)
  - `scripts/convert-binary.sh` — 二进制格式转换 (sing-box .srs, mihomo .mrs + MD5)
  - `scripts/audit-formats.py` — 格式完整性审计，对比 anti-AD + Cats-Team AdRules 标准
  - `scripts/hostlist-compiler-retry.sh` — hostlist-compiler 重试包装
  - `config/hostlist-compiler.json` — compiler 配置文件
  - `.github/workflows/auto-update.yml` — CI workflow
- 规则文件在 `ci` 上也有，但这是为了本地验证方便，不改动 `main` 上的规则

## 关键约定

- **永不从 ci 合并到 main**。ci 的工具在 ci 上开发、调试。
- main 只能被 CI workflow push 更新，或 cherry-pick 规则文件变更（极少发生）。
- ci 分支的改动通过 PR 到 ci 自身完成。

## 项目概览

聚合 14 个上游 AdGuard 订阅列表 → hostlist-compiler 去重/过滤 → 转成 12 种格式。

CI 流程：
1. hostlist-compiler 编译 → `rule/adgh.txt`
2. `convert.py` 转文本格式 + MD5
3. `convert-binary.sh` 转二进制格式 (.srs/.mrs) + MD5
4. `audit-formats.py --ci-warn` 审计格式完整性
5. push 到 main 分支

## 格式对齐

产出能力对齐 [anti-AD](https://github.com/privacy-protection-tools/anti-AD) + [Cats-Team AdRules](https://github.com/Cats-Team/AdRules)。

| 分类 | 文件 | 生成方式 |
|------|------|---------|
| Adblock+ | `easylist.list` + `.md5` | `convert.py` |
| AdGuard hostlist | `adgh.txt` + `.md5` | hostlist-compiler (原始输出) |
| Clash / Mihomo | `clash.yaml` | `convert.py` |
| dnsmasq | `dnsmasq.conf` | `convert.py` |
| 纯域名列表 | `domains.txt` + `.md5` | `convert.py` |
| Loon | `loon.list` | `convert.py` |
| Quantumult X | `quanx.list` | `convert.py` |
| SmartDNS | `smartdns.conf` | `convert.py` |
| Surge DOMAIN-SUFFIX | `surge.list` | `convert.py` |
| Surge DOMAIN-SET | `surge2.list` | `convert.py` |
| sing-box (.srs) | `adrules-singbox.srs` + `.md5` | `convert-binary.sh` |
| mihomo (.mrs) | `adrules-mihomo.mrs` + `.md5` | `convert-binary.sh` |

### 对齐保障

- CI 每次更新自动运行 `audit-formats.py --ci-warn`
- 缺格式报 warning + 提示修复命令
- 新增格式只需在 `audit-formats.py` 的 `EXPECTED` 列表加一行

## 工作目录

- `rule/` — 规则输出（main 追踪的核心内容）
- `scripts/` — Python + shell 工具
- `config/` — hostlist-compiler 的 JSON 配置
- `.github/workflows/` — GitHub Actions 工作流

## 注意

- `rule/mylist.txt` 是本地追加的自定义规则，仅 ci 分支有，不推到 main
- `.codegraph/`、`logs/` 均被 .gitignore 排除
