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
  - `scripts/convert.py` — 规则格式转换器
  - `scripts/hostlist-compiler-retry.sh` — hostlist-compiler 重试包装
  - `config/hostlist-compiler.json` — compiler 配置文件
  - `.github/workflows/auto-update.yml` — CI workflow
- 规则文件在 `ci` 上也有，但这是为了本地验证方便，不改动 `main` 上的规则

## 关键约定

- **永不从 ci 合并到 main**。ci 的工具在 ci 上开发、调试。
- main 只能被 CI workflow push 更新，或 cherry-pick 规则文件变更（极少发生）。
- ci 分支的改动通过 PR 到 ci 自身完成。

## 项目概览

聚合 14 个上游 AdGuard 订阅列表 → hostlist-compiler 去重/过滤 → 转成多种格式（Surge、Clash、Loon、Quantumult X、dnsmasq、smartdns、AdGuard Home、纯域名列表）。

CI 流程：
1. hostlist-compiler 编译 → `rule/adgh.txt`
2. `convert.py` 转其他格式
3. push 到 main 分支

## 工作目录

- `rule/` — 规则输出（main 追踪的核心内容）
- `scripts/` — Python + shell 工具
- `config/` — hostlist-compiler 的 JSON 配置
- `.github/workflows/` — GitHub Actions 工作流

## 注意

- `rule/mylist.txt` 是本地追加的自定义规则，仅 ci 分支有，不推到 main
- `.codegraph/`、`logs/` 均被 .gitignore 排除
