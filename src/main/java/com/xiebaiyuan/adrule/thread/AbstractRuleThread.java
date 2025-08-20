package com.xiebaiyuan.adrule.thread;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.LineHandler;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.google.common.hash.BloomFilter;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import com.xiebaiyuan.adrule.Util;
import com.xiebaiyuan.adrule.enums.RuleType;
import com.xiebaiyuan.adrule.stats.RuleStatsCollector;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Abstract rule processing thread
 *
 * @author ChengFengsheng on 2022/7/7
 */
@Slf4j
@Data
public abstract class AbstractRuleThread implements Runnable {

    private final String ruleUrl;

    private final Map<RuleType, Set<File>> typeFileMap;

    private final BloomFilter<String> filter;
    
    private final RuleStatsCollector statsCollector;

    public AbstractRuleThread(String ruleUrl, Map<RuleType, Set<File>> typeFileMap, BloomFilter<String> filter, RuleStatsCollector statsCollector) {
        this.ruleUrl = ruleUrl;
        this.typeFileMap = typeFileMap;
        this.filter = filter;
        this.statsCollector = statsCollector;
    }

    private Charset charset = Charset.defaultCharset();

    abstract InputStream getContentStream();

    @Override
    public void run() {
        TimeInterval interval = DateUtil.timer();
        AtomicReference<Integer> invalid = new AtomicReference<>(0);
        Map<File, Set<String>> fileDataMap = MapUtil.newHashMap();
        try {
            // Read and process line by line
            IoUtil.readLines(getContentStream(), charset, (LineHandler) line -> {
                if (StrUtil.isNotBlank(line)) {
                    statsCollector.incrementOriginalRules(); // 统计原始规则数
                    
                    String content = Util.clearRule(line);
                    if (StrUtil.isNotBlank(content)) {
                        if (!filter.mightContain(line)) {
                            filter.put(line);

                            if (Util.validRule(content, RuleType.DOMAIN)) {
                                typeFileMap.getOrDefault(RuleType.DOMAIN, Collections.emptySet())
                                        .forEach(item -> Util.safePut(fileDataMap, item, line));
                                statsCollector.incrementDomainRules(); // 统计域名规则
                                log.debug("Domain rule: {}", line);

                            } else if (Util.validRule(content, RuleType.HOSTS)) {
                                typeFileMap.getOrDefault(RuleType.HOSTS, Collections.emptySet())
                                        .forEach(item -> Util.safePut(fileDataMap, item, line));
                                statsCollector.incrementHostsRules(); // 统计Hosts规则
                                log.debug("Hosts rule: {}", line);

                            } else if (Util.validRule(content, RuleType.REGEX)) {
                                // 直接检查是否为正则表达式规则
                                typeFileMap.getOrDefault(RuleType.REGEX, Collections.emptySet())
                                        .forEach(item -> Util.safePut(fileDataMap, item, line));
                                statsCollector.incrementRegexRules(); // 统计正则规则
                                log.debug("Regex rule: {}", line);

                            } else if (Util.validRule(content, RuleType.MODIFY)) {
                                // 如果不是正则表达式规则但是修饰符规则
                                typeFileMap.getOrDefault(RuleType.MODIFY, Collections.emptySet())
                                        .forEach(item -> Util.safePut(fileDataMap, item, line));
                                statsCollector.incrementModifyRules(); // 统计修饰符规则
                                log.debug("Modifier rule: {}", line);
                            } else {
                                invalid.getAndSet(invalid.get() + 1);
                                statsCollector.incrementInvalidRules(); // 统计无效规则
                                log.debug("Invalid rule: {}", line);
                            }
                        }
                    }else {
                        invalid.getAndSet(invalid.get() + 1);
                        statsCollector.incrementInvalidRules(); // 统计无效规则
                        log.debug("Not a rule: {}", line);
                    }
                }
            });
        } catch (Exception e) {
            log.error(ExceptionUtil.stacktraceToString(e));
        }finally {
            fileDataMap.forEach(Util::write);
            log.info("Rule<{}> time consumed => {} ms invalid count => {}",
                    ruleUrl, interval.intervalMs(), invalid.get());
        }
    }
}
