#!/bin/bash
LC_ALL='C'

update_time="$(TZ=UTC-8 date +'%Y-%m-%d %H:%M:%S')(GMT+8)"

cp ./mod/rules/*rule* ./tmp/dns/

cat ./tmp/dns/* | grep -Ev '[A-Z]' |grep -vE '@|:|\?|\$|\#|\!|/' | sort | uniq > dns.txt

# 初步处理黑名单(不定时更新)
if hostlist-compiler -c ./script/dns-rules-config.json -o dns-output.txt; then
    if [ -f dns-output.txt ] && [ -s dns-output.txt ]; then
        cat dns-output.txt |grep -P "^\|\|[a-z0-9\.\-\*]+\^$" > dns.txt
    else
        echo "Warning: dns-output.txt is empty or does not exist, using fallback"
        echo "||example.com^" > dns.txt
    fi
else
    echo "Error: hostlist-compiler failed, using fallback"
    echo "||example.com^" > dns.txt
fi

# 提取/合并,黑白名单
python ./script/remove.py

# 添加关键词过滤规则
cat ./mod/rules/first-dns-rules.txt >> dns.txt

python ./script/rule.py dns.txt
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
