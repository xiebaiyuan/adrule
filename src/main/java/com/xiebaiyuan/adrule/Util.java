package com.xiebaiyuan.adrule;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import com.xiebaiyuan.adrule.enums.RuleType;
import com.xiebaiyuan.adrule.model.RuleStats;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.hutool.core.thread.ThreadUtil.sleep;

/**
 * @author Chengfs on 2022/9/19
 */
@Slf4j
public class Util {

    /**
     * Write collection content to file with lock protection
     *
     * @param file    target file
     * @param content content collection
     */
    public static void write(File file, Collection<String> content) {
        if (CollUtil.isNotEmpty(content)) {
            try (RandomAccessFile accessFile = new RandomAccessFile(file, "rw");
                 FileChannel channel = accessFile.getChannel()) {
                // Lock file for writing, sleep if lock cannot be acquired
                while (true) {
                    try {
                        channel.tryLock();
                        break;
                    } catch (Exception e) {
                        sleep(1000);
                    }
                }
                // 将集合转换为列表并排序
                List<String> sortedContent = new ArrayList<>(content);
                // 使用自定义规则排序
                sortRules(sortedContent);
                
                accessFile.seek(accessFile.length());
                accessFile.write((CollUtil.join(sortedContent, StrUtil.CRLF)).getBytes(StandardCharsets.UTF_8));
                accessFile.write(StrUtil.CRLF.getBytes(StandardCharsets.UTF_8));
            } catch (IOException ioException) {
                log.error("Error writing to file, {} => {}", file.getPath(), ioException.getMessage());
            }
        }
    }

    /**
     * Create file by path, delete and recreate if exists
     *
     * @param path file path
     * @return {@link File}
     */
    public static File createFile(String path) {
        path = FileUtil.normalize(path);
        if (!FileUtil.isAbsolutePath(path)) {
            path = Constant.ROOT_PATH + File.separator + path;
        }
        File file = FileUtil.file(FileUtil.normalize(path));
        return createFile(file);
    }

    public static File createFile(File file) {
        if (FileUtil.exist(file)) {
            FileUtil.del(file);
        }
        FileUtil.touch(file);
        // 先写入简单的头部，统计信息会在后续更新
        FileUtil.appendUtf8String("# Generating rules, please wait...\r\n", file);
        return file;
    }

    /**
     * 更新文件头部信息，包含完整的统计数据
     */
    public static void updateFileHeader(File file, RuleStats stats) {
        if (!FileUtil.exist(file)) {
            return;
        }
        
        // 读取现有内容
        String content = FileUtil.readUtf8String(file);
        StringBuilder ruleContent = new StringBuilder();
        
        // 检查是否包含临时提示文本
        if (content.contains("# Generating rules")) {
            // 如果只有临时提示行，不包含其他内容
            if (content.trim().equals("# Generating rules, please wait...")) {
                // 内容为空，不需要保留任何内容
                ruleContent = new StringBuilder();
            } else {
                // 移除临时提示行，保留其他内容
                String[] lines = content.split("\\r?\\n");
                for (String line : lines) {
                    if (!line.startsWith("# Generating rules")) {
                        ruleContent.append(line).append("\r\n");
                    }
                }
            }
        } else if (content.contains("################################################################################")) {
            // 包含头部，需要提取出规则内容
            // 查找最后一个分隔线的位置
            int lastSeparatorIndex = content.lastIndexOf("################################################################################");
            // 找到分隔线之后的内容
            if (lastSeparatorIndex > 0) {
                int contentStartIndex = content.indexOf("\r\n", lastSeparatorIndex);
                if (contentStartIndex > 0) {
                    // 提取分隔线之后的内容
                    ruleContent.append(content.substring(contentStartIndex + 2));
                }
            }
        } else {
            // 不包含头部，全部是规则内容
            ruleContent.append(content);
        }
        
        // 排序规则内容
        if (ruleContent.length() > 0) {
            String[] rules = ruleContent.toString().split("\\r?\\n");
            List<String> ruleList = new ArrayList<>();
            for (String rule : rules) {
                if (!rule.trim().isEmpty() && !rule.trim().startsWith("#")) {
                    ruleList.add(rule);
                }
            }
            
            // 对规则进行排序
            sortRules(ruleList);
            
            // 重建规则内容
            ruleContent = new StringBuilder();
            for (String rule : ruleList) {
                ruleContent.append(rule).append("\r\n");
            }
        }
        
        // 生成上游源列表
        StringBuilder sourcesList = new StringBuilder();
        for (int i = 0; i < stats.getUpstreamSources().size(); i++) {
            String source = stats.getUpstreamSources().get(i);
            // 直接使用原始上游链接
            // 格式化为: "# nn. source_url                                        #"
            // 总长度78个字符，包括开头的"# "和结尾的" #"
            String formattedLine = String.format("# %2d. %s", i + 1, source);
            // 计算需要补充的空格数量，确保总长度为78
            int spacesNeeded = 76 - formattedLine.length(); // 76 = 78 - 2 (最后的" #")
            if (spacesNeeded < 0) {
                // 如果超长，截断源URL
                source = source.substring(0, source.length() + spacesNeeded - 3) + "...";
                formattedLine = String.format("# %2d. %s", i + 1, source);
                spacesNeeded = 76 - formattedLine.length();
            }
            sourcesList.append(formattedLine)
                       .append(" ".repeat(Math.max(0, spacesNeeded)))
                       .append(" #\r\n");
        }
        
        // 生成完整的文件头
        String header = StrUtil.format(Constant.HEADER_TEMPLATE,
            DateTime.now().toString(DatePattern.NORM_DATETIME_PATTERN),
            stats.getProcessingTime(),
            System.getProperty("java.version"),
            Runtime.getRuntime().availableProcessors(),
            Runtime.getRuntime().totalMemory() / 1024 / 1024,
            stats.getUpstreamCount(),
            stats.getTotalRules(),
            stats.getDomainRules(),
            stats.getRegexRules(),
            stats.getHostsRules(),
            stats.getModifyRules(),
            stats.getAdghRules(),
            String.format("%.2f", stats.getDeduplicationRate()),
            sourcesList.toString()
        );
        
        // 重写文件，保留规则内容
        FileUtil.writeUtf8String(header + ruleContent, file);
    }
    
    /**
     * 从URL中提取源名称
     */
    private static String extractSourceName(String url) {
        try {
            if (url.contains("anti-ad.net")) return "Anti-AD (反广告联盟)";
            if (url.contains("adguardteam")) return "AdGuard Official";
            if (url.contains("easylist")) return "EasyList";
            if (url.contains("uBlockOrigin")) return "uBlock Origin";
            if (url.contains("someonewhocares")) return "Dan Pollock's List";
            if (url.contains("pgl.yoyo.org")) return "Peter Lowe's List";
            if (url.contains("oisd.nl")) return "OISD Blocklist";
            if (url.contains("WindowsSpyBlocker")) return "Windows Spy Blocker";
            if (url.contains("jdlingyu")) return "Ad Wars";
            if (url.contains("blackmatrix7")) return "BlackMatrix7";
            if (url.contains("adaway.org")) return "AdAway";
            if (url.contains("nocoin")) return "No Coin";
            if (url.contains("scamblocklist")) return "Scam Blocklist";
            if (url.contains("koolproxy")) return "KoolProxy";
            if (url.contains("zhihu")) return "知乎去广告";
            if (url.contains("youtube")) return "YouTube去广告";
            if (url.contains("Crystal-RainSlide")) return "国内补充规则";
            if (url.contains("banbendalao")) return "手机去广告";
            if (url.contains("xinggsf")) return "乘风广告过滤";
            if (url.contains("o0HalfLife0o")) return "HalfLife合并规则";
            if (url.contains("Cats-Team")) return "星辰去广告";
            if (url.contains("TG-Twilight")) return "秋风广告规则";
            if (url.contains("privacy-protection-tools")) return "隐私保护工具";
            if (url.contains("thhbdd")) return "反PCDN";
            
            // 提取域名作为fallback
            String domain = url.replaceAll("https?://", "")
                              .replaceAll("www\\.", "")
                              .split("/")[0];
            return domain;
        } catch (Exception e) {
            return url.length() > 30 ? url.substring(0, 30) + "..." : url;
        }
    }
    
    /**
     * 对规则列表进行智能排序，按以下顺序:
     * 1. 首先是排除规则 (-开头)
     * 2. 然后是正则表达式规则 (/开头/)
     * 3. 接着是例外规则 (@@开头)
     * 4. 最后是普通域名规则
     * 每种类型内部按字母顺序排序
     *
     * @param rules 规则列表
     */
    private static void sortRules(List<String> rules) {
        if (rules == null || rules.isEmpty()) {
            return;
        }
        
        List<String> excludeRules = new ArrayList<>(); // -开头的规则
        List<String> regexRules = new ArrayList<>();   // /开头/结尾的规则
        List<String> exceptionRules = new ArrayList<>(); // @@开头的规则
        List<String> normalRules = new ArrayList<>();  // 普通域名规则
        
        // 分类规则
        for (String rule : rules) {
            if (rule.startsWith("-")) {
                excludeRules.add(rule);
            } else if (rule.startsWith("/") && (rule.endsWith("/") || rule.contains("/$"))) {
                regexRules.add(rule);
            } else if (rule.startsWith("@@")) {
                exceptionRules.add(rule);
            } else {
                normalRules.add(rule);
            }
        }
        
        // 对各类规则内部排序
        Collections.sort(excludeRules);
        Collections.sort(regexRules);
        Collections.sort(exceptionRules);
        Collections.sort(normalRules);
        
        // 清空原始列表
        rules.clear();
        
        // 按顺序添加各类规则
        rules.addAll(excludeRules);
        rules.addAll(regexRules);
        rules.addAll(exceptionRules);
        rules.addAll(normalRules);
    }
    
    /**
     * Validate if content is a rule of specified type
     *
     * @param rule content
     * @param type rule type
     * @return validation result
     */
    public static boolean validRule(String rule, RuleType type) {
        // 预过滤：检查是否包含可疑内容
        if (rule.contains("<") && rule.contains(">") ||
            rule.contains("function") || rule.contains("return") ||
            rule.contains("console.") || rule.contains("Copyright") ||
            rule.contains("pageOptions") || rule.contains("xhr.send") ||
            (rule.contains("{") && rule.contains("}")) ||
            rule.contains("Rights Reserved") || rule.contains("Privacy Policy") ||
            rule.contains("购买该域名") || rule.contains("More domains") ||
            rule.contains("Seo.Domains") || rule.equals("];") || rule.startsWith("];") ||
            (rule.startsWith("#") && rule.replace("#", "").trim().isEmpty())) {
            return false;
        }

        // 特殊处理：AdGuard Home格式的正则表达式规则
        // 格式如: /^example\.com$/ 或 /example\.(net|org)/
        if (type == RuleType.REGEX) {
            // 检查是否包含不支持的修饰符
            if (containsUnsupportedModifiers(rule)) {
                return false;
            }
            
            // 检查基本正则表达式格式
            if (ReUtil.isMatch(Constant.ADG_REGEX_PATTERN, rule)) {
                return true;
            }
            
            // 检查带修饰符的正则表达式格式，如: /^(\S+\.)?advert/$denyallow=...
            if (ReUtil.isMatch(Constant.ADG_REGEX_WITH_MODIFIER_PATTERN, rule)) {
                // 提取修饰符部分
                String modifierPart = null;
                int dollarPos = rule.lastIndexOf('$');
                if (dollarPos > rule.lastIndexOf('/') && dollarPos > 0) {
                    modifierPart = rule.substring(dollarPos);
                    
                    // 检查修饰符是否包含不支持的修饰符
                    if (modifierPart != null && containsUnsupportedModifiers(modifierPart)) {
                        return false;
                    }
                    
                    // 检查修饰符是否有效
                    if (modifierPart != null) {
                        for (String modifier : Constant.ADG_MODIFIERS) {
                            if (modifierPart.contains(modifier)) {
                                return true;
                            }
                        }
                    }
                }
                
                // 没有修饰符或没有识别到有效修饰符，但格式仍然是正则表达式
                // 再次检查是否为纯正则表达式（无修饰符）
                if (dollarPos < 0 || dollarPos <= rule.lastIndexOf('/')) {
                    return true;
                }
                return false;
            }
        }

        // Match identifier, must match when identifier exists
        if (ArrayUtil.isNotEmpty(type.getIdentify())) {
            if (!StrUtil.containsAny(rule, type.getIdentify())) {
                return false;
            }
        }

        if (ArrayUtil.isNotEmpty(type.getMatch()) || ArrayUtil.isNotEmpty(type.getExclude())) {
            // Match positive rules, need to satisfy at least one
            if (ArrayUtil.isNotEmpty(type.getMatch())) {
                boolean math = false;
                for (String pattern : type.getMatch()) {
                    if (ReUtil.contains(pattern, rule)) {
                        math = true;
                        break;
                    }
                }
                if (!math) {
                    return false;
                }
            }

            // Match negative rules, need to satisfy none
            if (ArrayUtil.isNotEmpty(type.getExclude())) {
                for (String pattern : type.getExclude()) {
                    if (ReUtil.contains(pattern, rule)) {
                        return false;
                    }
                }
                return true;
            }

            return true; // 如果有match模式且已通过，无exclude模式，则通过
        } else {
            return true;
        }
    }

    /**
     * Clean rule string, remove spaces and certain specific symbols
     *
     * @param content content
     * @return cleaned result
     */
    public static String clearRule(String content) {
        content = StrUtil.isNotBlank(content) ? StrUtil.trim(content) : StrUtil.EMPTY;

        // Basic validity check
        if (ReUtil.contains(Constant.EFFICIENT_REGEX, content)) {
            return StrUtil.EMPTY;
        }
        
        // Additional check for code fragments
        if (ReUtil.contains(Constant.CODE_FRAGMENT_REGEX, content)) {
            return StrUtil.EMPTY;
        }
        
        // Check for long separator lines (e.g., ########################)
        if (content.startsWith("#") && content.replace("#", "").trim().isEmpty()) {
            return StrUtil.EMPTY;
        }
        
        // Check for domain sales messages and common website footer patterns
        if (content.contains("购买该域名") || content.contains("More domains") || 
            content.contains("Seo.Domains") || content.contains("Copyright") ||
            content.startsWith("];") || content.equals("];") ||
            content.matches(".*\\d{4}.*Copyright.*") || content.matches(".*©.*\\d{4}.*")) {
            return StrUtil.EMPTY;
        }
        
        // Check for common non-rule patterns
        if (content.contains("{") && content.contains("}") ||
            content.contains("(") && content.contains(")") && content.length() > 30 ||
            content.startsWith(".") || content.startsWith("#") && !content.startsWith("##") ||
            content.contains("JSON.parse") || content.contains("arguments.push") ||
            content.length() > 300) { // 一般规则不会太长
            return StrUtil.EMPTY;
        }

        // 保留正则表达式规则中的修饰符
        if (content.startsWith("/") && content.contains("/$")) {
            // 这是带修饰符的正则表达式规则，直接返回，不做进一步处理
            return content;
        }

        // Remove basic modifier symbols from start/end
        if (ReUtil.contains(Constant.BASIC_MODIFY_REGEX, content)) {
           content = ReUtil.replaceAll(content, Constant.BASIC_MODIFY_REGEX, StrUtil.EMPTY);
        }

        return StrUtil.trim(content);
    }

    public static <K, T> void safePut(Map<K, Set<T>> map, K key, T val) {
        if (map.containsKey(key)) {
            map.get(key).add(val);
        } else {
            map.put(key, CollUtil.newHashSet(val));
        }
    }
    
    /**
     * 检查规则是否包含AdGuard Home不支持的修饰符
     * @param rule 规则字符串
     * @return true if contains unsupported modifiers
     */
    private static boolean containsUnsupportedModifiers(String rule) {
        if (rule == null || rule.isEmpty()) {
            return false;
        }
        
        // 提取修饰符部分（$之后的内容）
        int dollarPos = rule.lastIndexOf('$');
        if (dollarPos < 0) {
            return false; // 没有修饰符
        }
        
        String modifierPart = rule.substring(dollarPos + 1);
        
        // 首先检查是否包含AdGuard Home支持的修饰符
        boolean hasAdGuardModifier = false;
        for (String supportedModifier : Constant.ADG_MODIFIERS) {
            if (modifierPart.contains(supportedModifier)) {
                hasAdGuardModifier = true;
                break;
            }
        }
        
        // 如果包含AdGuard Home支持的修饰符，则保留该规则
        if (hasAdGuardModifier) {
            return false;
        }
        
        // 检查是否只包含不支持的修饰符
        for (String unsupportedModifier : Constant.UNSUPPORTED_MODIFIERS) {
            if (modifierPart.contains(unsupportedModifier)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 注意：域名规则不应该去重！
     * aa.com 和 aa.com^ 有不同的含义：
     * - aa.com: 只阻止确切的域名
     * - aa.com^: 阻止域名及其所有子域
     * 这个方法已废弃，保留是为了向后兼容
     * @param rule 原始域名规则
     * @return 原始规则（不做任何修改）
     */
    @Deprecated
    public static String cleanDomainRule(String rule) {
        // 不再进行任何清理，保持原始规则
        return rule;
    }

}
