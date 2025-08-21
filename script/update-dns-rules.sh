#!/bin/bash
set -euo pipefail
echo "[debug] start update-dns-rules.sh"
LC_ALL='C'

# Integrate upstream downloaded lists (via update-upstream.sh) into dns.txt build.
ROOT_DIR="$(pwd)"

# In CI environment, tmp dirs are pre-populated by update-upstream.sh
# Only clean if running locally or explicitly requested
if [ "${CLEAN_TMP:-auto}" = "1" ] || ([ "${CLEAN_TMP:-auto}" = "auto" ] && [ "${SKIP_UPSTREAM:-0}" = "1" ]); then
    echo "[debug] cleaning temp dirs..."
    rm -rf ./tmp/dns ./tmp/content
fi

# Download upstream unless SKIP_UPSTREAM=1
if [ "${SKIP_UPSTREAM:-0}" != "1" ]; then
    echo "[info] Downloading upstream lists..."
    ( bash ./script/update-upstream.sh || echo "[warn] update-upstream.sh failed; proceeding with local rules only" )
else
    echo "[info] Skipping upstream download (SKIP_UPSTREAM=1)"
fi

cd "$ROOT_DIR"

update_time="$(TZ=UTC-8 date +'%Y-%m-%d %H:%M:%S')(GMT+8)"

mkdir -p ./tmp/dns
echo "[debug] copying rule sources..."
cp ./mod/rules/*rule* ./tmp/dns/
echo "[debug] rule sources copied: $(ls -1 ./tmp/dns | wc -l) files"

echo "[debug] building initial dns.txt from local rules only..."
cat ./tmp/dns/*rule* | grep -Ev '[A-Z]' |grep -vE '@|:|\?|\$|\#|\!|/' | sort | uniq > dns.txt || echo "[warn] initial build step produced no output"
echo "[debug] initial dns.txt lines: $(wc -l < dns.txt || echo 0)"

# Extract domains from UPSTREAM downloaded lists only (not local rules)
# Only process if we actually downloaded upstream content
if [ "${SKIP_UPSTREAM:-0}" != "1" ] && [ -d ./tmp/content ]; then
    echo "[debug] extracting upstream domains from downloaded content..."
    python3 ./script/extract_upstream_domains.py ./tmp/content ./tmp/dns > ./tmp/upstream-domains.txt 2>/dev/null || true
    if [ -f ./tmp/upstream-domains.txt ] && [ -s ./tmp/upstream-domains.txt ]; then
        upstream_count=$(wc -l < ./tmp/upstream-domains.txt 2>/dev/null || echo 0)
        echo "[debug] extracted $upstream_count upstream domains"
        echo "[debug] merging upstream domains..."
        cat ./tmp/upstream-domains.txt >> dns.txt
        combined_lines=$(wc -l < dns.txt || echo 0)
        echo "[debug] combined dns.txt lines: $combined_lines"
    else
        echo "[debug] no upstream domains extracted"
    fi
else
    echo "[debug] skipping upstream extraction (no download or SKIP_UPSTREAM=1)"
fi

# Download hostlist-compiler if not available
if ! command -v hostlist-compiler >/dev/null 2>&1; then
    echo "[info] hostlist-compiler not found, downloading..."
    HOSTLIST_VERSION="v1.13.9"
    case "$(uname -s)" in
        Darwin) HOSTLIST_ARCH="darwin-amd64" ;;
        Linux) HOSTLIST_ARCH="linux-amd64" ;;
        *) echo "[warn] unsupported OS for auto-download, using fallback"; HOSTLIST_ARCH="" ;;
    esac
    if [ -n "$HOSTLIST_ARCH" ]; then
        curl -sL "https://github.com/AdguardTeam/HostlistCompiler/releases/download/${HOSTLIST_VERSION}/hostlist-compiler-${HOSTLIST_ARCH}" -o ./hostlist-compiler
        chmod +x ./hostlist-compiler
        export PATH="$(pwd):$PATH"
        echo "[info] hostlist-compiler downloaded and added to PATH"
    fi
fi

# 初步处理黑名单(不定时更新)
if command -v hostlist-compiler >/dev/null 2>&1 && hostlist-compiler -c ./script/dns-rules-config.json -o dns-output.txt; then
    if [ -f dns-output.txt ] && [ -s dns-output.txt ]; then
        cat dns-output.txt |grep -P "^\|\|[a-z0-9\.\-\*]+\^$" > dns.txt
    else
        echo "Warning: dns-output.txt is empty or does not exist, using fallback"
        echo "||example.com^" > dns.txt
    fi
else
        echo "Error: hostlist-compiler failed, using fallback"
        if [ ! -s dns.txt ]; then
            echo "||example.com^" > dns.txt
        else
            echo "[warn] keeping pre-built dns.txt (no hostlist-compiler)";
        fi
fi

# 提取/合并,黑白名单
if command -v python3 >/dev/null 2>&1; then
    python3 ./script/remove.py || true
else
    echo "[warn] python3 not found; skip allowlist filtering"
fi

# 添加关键词过滤规则
cat ./mod/rules/first-dns-rules.txt >> dns.txt

if command -v python3 >/dev/null 2>&1; then
    python3 ./script/rule.py dns.txt || true
else
    echo "[warn] python3 not found; skip final sort"
fi
echo -e "! Total count: $(wc -l < dns.txt) \n! Update: $update_time" > total.txt
cat ./mod/title/dns-title.txt total.txt dns.txt | sed '/^$/d' > tmp.txt
mv tmp.txt dns.txt
cat dns.txt |grep -vE '(@|\*)' |grep -Po "(?<=\|\|).+(?=\^)" | grep -v "\*" > ./domain.txt

echo "# Title:AdRules Quantumult X List " > qx.conf
echo "# Title:AdRules SmartDNS List " > smart-dns.conf
echo "# Title:AdRules List " > adrules.list
echo "# Title:AdRules Domain List (Clash) " > adrules_domainset.txt
echo "# Update: $update_time" >> qx.conf
echo "# Update: $update_time" >> smart-dns.conf
echo "# Update: $update_time" >> adrules.list
echo "# Update: $update_time" >> adrules_domainset.txt

cat domain.txt |sed 's/^/host-suffix,/g'|sed 's/$/,reject/g' >> ./qx.conf
cat domain.txt |sed "s/^/address \//g"|sed "s/$/\/#/g" >> ./smart-dns.conf
cat domain.txt |sed "s/^/domain:/g" > ./mosdns_adrules.txt
cat domain.txt |sed "s/^/\+\./g" >> ./adrules_domainset.txt
cat domain.txt |sed "s/^/DOMAIN-SUFFIX,/g" >> ./adrules.list

# 添加规则统计到‘smart-dns.conf’
rule_count=$(grep -c 'address /' "smart-dns.conf")
sed -i "3i # $rule_count Rules" "smart-dns.conf"

# 添加规则统计到'adrules_domainset.txt','mihomo.mrs'
rule_count_clash=$(grep -vc '^#' "adrules_domainset.txt")
sed -i "3i # $rule_count_clash Rules" "adrules_domainset.txt"

python ./script/dns-script/singbox.py
python ./script/dns-script/surge.py

wget -O ssc https://github.com/PuerNya/sing-srs-converter/releases/download/v2.0.1/sing-srs-converter-v2.0.1-linux-x86_64_v3
chmod +x ssc
if [ -f adrules.list ] && [ -s adrules.list ]; then
    ./ssc adrules.list -m
    if [ -f adrules.list.srs ]; then
        mv adrules.list.srs adrules-singbox.srs
    else
        echo "Warning: Failed to generate adrules.list.srs"
    fi
else
    echo "Warning: adrules.list is empty or does not exist, skipping srs conversion"
fi

# 转换adrules_domainset.txt为clash.mrs(mihomo)
MIHOMO_VERSION=$(wget -qO- https://github.com/MetaCubeX/mihomo/releases/latest/download/version.txt)
if [ -n "$MIHOMO_VERSION" ]; then
    wget -O mihomo-linux-amd64-${MIHOMO_VERSION}.gz https://github.com/MetaCubeX/mihomo/releases/latest/download/mihomo-linux-amd64-${MIHOMO_VERSION}.gz
    gzip -d mihomo-linux-amd64-${MIHOMO_VERSION}.gz
    mv mihomo-linux-amd64-${MIHOMO_VERSION} mihomo
    chmod +x mihomo
    ./mihomo convert-ruleset domain text adrules_domainset.txt adrules-mihomo.mrs
else
    echo "Failed to get mihomo version, skipping mihomo conversion"
fi

rm -f ssc mihomo
rm -f dns-output.txt total.txt domain.txt

exit
