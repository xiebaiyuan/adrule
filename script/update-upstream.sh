#!/bin/sh
LC_ALL='C'

download_file() {
  url=$1
  directory=$2
  filename=$(basename $url)
  filepath="$directory/$filename"
  retries=3
  while [ $retries -gt 0 ]; do
    if curl -sS -o $filepath $url; then
      echo "Downloaded $url successfully"
      # 在文件的第一行插入 ! url: $url
      sed -i "1i\\! url: $url" $filepath
      return  
    else
      echo "Failed to download $url, retrying..."
      retries=$((retries-1))
    fi
  done
  echo "Failed to download $url after 3 retries, exiting script."
  exit 1  
}

wait
# Create temporary folder
mkdir -p ./tmp/
cd tmp

# Start Download Filter File
echo 'Start Downloading...'

content=(  
  #damengzhu
  "https://raw.githubusercontent.com/damengzhu/banad/main/jiekouAD.txt" 
  #Noyllopa NoAppDownload
  "https://raw.githubusercontent.com/Noyllopa/NoAppDownload/master/NoAppDownload.txt" 
  #china
  #"https://filters.adtidy.org/extension/ublock/filters/224.txt" 
  #cjx
  "https://raw.githubusercontent.com/cjx82630/cjxlist/master/cjx-annoyance.txt"
  #anti-anti-ad
  "https://raw.githubusercontent.com/reek/anti-adblock-killer/master/anti-adblock-killer-filters.txt"
  "https://easylist-downloads.adblockplus.org/antiadblockfilters.txt"
  #"https://easylist-downloads.adblockplus.org/abp-filters-anti-cv.txt"
  #--normal
  #Clean Url
  "https://raw.githubusercontent.com/DandelionSprout/adfilt/master/ClearURLs%20for%20uBo/clear_urls_uboified.txt" 
  #english opt
  "https://filters.adtidy.org/extension/ublock/filters/2_optimized.txt"
  #EasyListPrvacy
  "https://easylist-downloads.adblockplus.org/easyprivacy.txt"
  #--plus
  #ubo annoyance
  "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/annoyances.txt" 
  #ubo privacy - 更新到最新URL
  "https://raw.githubusercontent.com/uBlockOrigin/uAssetsCDN/refs/heads/main/filters/privacy.min.txt"
  #adg base - 更新到最新URL
  "https://raw.githubusercontent.com/AdguardTeam/FiltersRegistry/master/filters/filter_2_Base/filter.txt"
  #adg privacy
  "https://filters.adtidy.org/windows/filters/3.txt" 
  #adg cn
  "https://filters.adtidy.org/windows/filters/224.txt" 
  #adg annoyance
  "https://filters.adtidy.org/windows/filters/14.txt"
  #adg mobile ads
  "https://filters.adtidy.org/extension/ublock/filters/11.txt"
  #uBlock filters
  "https://cdn.jsdelivr.net/gh/uBlockOrigin/uAssetsCDN@main/filters/filters.txt"
  #uBlock filters – Badware risks
  "https://ublockorigin.pages.dev/filters/badware.txt"
  #uBlock filters – Quick fixes
  "https://ublockorigin.github.io/uAssets/filters/quick-fixes.txt"
  #uBlock filters – Resource abuse
  "https://cdn.statically.io/gh/uBlockOrigin/uAssetsCDN/main/filters/resource-abuse.txt"
  #uBlock filters – Unbreak
  "https://gitcdn.link/cdn/uBlockOrigin/uAssetsCDN/main/filters/unbreak.txt"
  #AdGuard CNAME disguised tracker list
  "https://raw.githubusercontent.com/AdguardTeam/cname-trackers/master/data/combined_disguised_trackers.txt"
  #AdditionalFiltersCN
  "https://raw.githubusercontent.com/Crystal-RainSlide/AdditionalFiltersCN/master/CN.txt"
  #ADgk mobile ad rules
  "https://raw.githubusercontent.com/banbendalao/ADgk/master/ADgk.txt"
  #ChengFeng ad filter rules
  "https://raw.githubusercontent.com/xinggsf/Adblock-Plus-Rule/master/rule.txt"
  #ChengFeng video filter rules
  "https://raw.githubusercontent.com/xinggsf/Adblock-Plus-Rule/master/mv.txt"
  #HalfLife merged rules
  "https://raw.githubusercontent.com/o0HalfLife0o/list/master/ad.txt"
  #blackmatrix7 merged
  "https://cdn.jsdelivr.net/gh/blackmatrix7/ios_rule_script@master/rule/AdGuard/Advertising/Advertising.txt"
  #Zhihu standard version
  "https://raw.githubusercontent.com/zsakvo/AdGuard-Custom-Rule/master/rule/zhihu.txt"
  #Youtube-Adfilter-Web
  "https://raw.githubusercontent.com/timlu85/AdGuard-Home_Youtube-Adfilter/master/Youtube-Adfilter-Web.txt"
  #Autumn Wind ad rules
  "https://raw.githubusercontent.com/TG-Twilight/AWAvenue-Ads-Rule/main/AWAvenue-Ads-Rule.txt"
)

dns=(
  #Ultimate Ad Filter (横幅、弹窗)(适合浏览器扩展)
  #"https://filters.adavoid.org/ultimate-ad-filter.txt"
  #Ultimate Privacy Filter （移动端隐私过滤）(终极隐私过滤器)
  #"https://filters.adavoid.org/ultimate-privacy-filter.txt"
  #Social 社交媒体过滤器 （适合国外网站）
  #"https://filters.adtidy.org/windows/filters/4.txt"
  #Annoying (!适合桌面端)
  #"https://filters.adtidy.org/windows/filters/14.txt"
  #"https://easylist-downloads.adblockplus.org/fanboy-annoyance.txt"
  #Mobile Ads (国内适配差)
  #"https://filters.adtidy.org/windows/filters/11.txt"
  #EasyList + AdGuard English filter # 英语网站
  #"https://filters.adtidy.org/windows/filters/2.txt"
  #"https://easylist-downloads.adblockplus.org/easylistchina+easylist.txt"
  #"https://filters.adtidy.org/windows/filters/224.txt" 
  #Fuck Tracking （主打隐私、跟踪）
  #"https://easylist-downloads.adblockplus.org/easyprivacy.txt"
  #"https://filters.adtidy.org/windows/filters/3.txt"
  #anti-coin
  "https://cdn.jsdelivr.net/gh/hoshsadiq/adblock-nocoin-list/hosts.txt"
  #scam blocklist
  "https://cdn.jsdelivr.net/gh/durablenapkin/scamblocklist/adguard.txt"
  #damengzhu (主要去除色情悬浮广告)
  #"https://raw.githubusercontent.com/damengzhu/banad/main/jiekouAD.txt"
  #xinggsf (乘风 视频过滤规则)(前面已合并)
  #"https://raw.githubusercontent.com/xinggsf/Adblock-Plus-Rule/master/mv.txt" 
  #uBO （!提取误差大）
  #"https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/annoyances.txt" 
  #"https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/badware.txt" 
  #"https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/filters.txt"
  #"https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/privacy.txt"
  #cjx #提取误差大、太多误杀了
  #"https://raw.githubusercontent.com/cjx82630/cjxlist/master/cjx-annoyance.txt"
  #anti-anti-ad
  "https://raw.githubusercontent.com/reek/anti-adblock-killer/master/anti-adblock-killer-filters.txt"
  #"https://easylist-downloads.adblockplus.org/antiadblockfilters.txt"
  #"https://easylist-downloads.adblockplus.org/abp-filters-anti-cv.txt"
  #HostsVN
  "https://raw.githubusercontent.com/bigdargon/hostsVN/master/filters/adservers-all.txt"
  #hosts
  #anti-windows-spy - 更新CDN URL
  "https://cdn.jsdelivr.net/gh/crazy-max/WindowsSpyBlocker/data/hosts/spy.txt"
  #Notarck-Malware
  "https://gitlab.com/quidsup/notrack-blocklists/-/raw/master/malware.hosts"
  #StevenBlack
  "https://raw.githubusercontent.com/StevenBlack/hosts/master/data/StevenBlack/hosts"
  #Dan Pollock's List
  "https://someonewhocares.org/hosts/zero/hosts"
  #Peter Lowe's List
  "https://pgl.yoyo.org/adservers/serverlist.php?hostformat=adblockplus&showintro=1&mimetype=plaintext"
  #OISD Blocklist Basic
  "https://abp.oisd.nl/basic/"
  #AdAway official ad-blocking Host rules
  "https://adaway.org/hosts.txt"
  #DaSheng Ad Clean
  "https://raw.githubusercontent.com/jdlingyu/ad-wars/master/hosts"
  #Brave
  "https://raw.githubusercontent.com/brave/adblock-lists/master/brave-lists/brave-firstparty.txt"
  #Me (Cats-Team)
  "https://raw.githubusercontent.com/Cats-Team/dns-filter/main/abp.txt"
  #Smart-TV
  "https://raw.githubusercontent.com/Perflyst/PiHoleBlocklist/master/SmartTV-AGH.txt"
  ### 自用添加 ↓ ###
  #KoolProxy规则
  "https://github.com/ilxp/koolproxy/raw/main/rules/koolproxy.txt"
  "https://raw.githubusercontent.com/ilxp/koolproxy/main/rules/daily.txt"
  "https://github.com/ilxp/koolproxy/raw/main/rules/steven.txt"
  "https://github.com/ilxp/koolproxy/blob/main/rules/adg.txt"
  #"https://github.com/ilxp/koolproxy/blob/main/rules/adgk.txt"
  #"https://github.com/ilxp/koolproxy/blob/main/rules/yhosts.txt"
  #乘风 广告过滤规则 (下方已合并)
  #"https://raw.githubusercontent.com/xinggsf/Adblock-Plus-Rule/master/rule.txt"
  #"https://raw.githubusercontent.com/xinggsf/Adblock-Plus-Rule/master/mv.txt"
  #HalfLife吧 (合并自乘风视频广告过滤规则、EasylistChina、EasylistLite、CJX'sAnnoyance)
  "https://raw.githubusercontent.com/o0HalfLife0o/list/master/ad.txt"
  #AdditionalFiltersCN (适合浏览器扩展)
  #"https://raw.githubusercontent.com/Crystal-RainSlide/AdditionalFiltersCN/master/CN.txt"
  #AdGuard DNS (AdGuard Base filter, Social media filter, Tracking Protection filter, Mobile ads filter, EasyList, EasyPrivacy, etc)
  "https://adguardteam.github.io/AdGuardSDNSFilter/Filters/filter.txt"
  #GoodbyeAds-YouTube(可能误杀)
  #"https://raw.githubusercontent.com/jerryn70/GoodbyeAds/master/Formats/GoodbyeAds-YouTube-AdBlock-Filter.txt"
  #anti-AD easylist
  "https://anti-ad.net/easylist.txt"
  "https://raw.githubusercontent.com/privacy-protection-tools/anti-AD/master/anti-ad-adguard.txt"
  #XingShao AdRules DNS List
  "https://raw.githubusercontent.com/Cats-Team/AdRules/main/dns.txt"
  #catteam (自己的规则)
  #"https://raw.githubusercontent.com/xiebaiyuan/AdGuard-Rule/adrules/dns.txt"
  ### PCDN rules ###
  #fuck pcdn
  "https://thhbdd.github.io/Block-pcdn-domains/ban.txt"
  #anti-AD PCDN rules
  "https://raw.githubusercontent.com/privacy-protection-tools/anti-AD/refs/heads/master/discretion/pcdn.txt"
  #anti-AD httpdns
  "https://raw.githubusercontent.com/privacy-protection-tools/anti-AD/refs/heads/master/discretion/dns.txt"
)

mkdir -p content
mkdir -p dns

for content in "${content[@]}"
do
  download_file $content "content"
done

for dns in "${dns[@]}" 
do
  download_file $dns "dns"
done

#修复换行符问题
sed -i 's/\r//' ./content/*.txt

echo 'Finish'

exit
