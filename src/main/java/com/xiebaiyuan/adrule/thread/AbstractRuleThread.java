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

    public AbstractRuleThread(String ruleUrl, Map<RuleType, Set<File>> typeFileMap, BloomFilter<String> filter) {
        this.ruleUrl = ruleUrl;
        this.typeFileMap = typeFileMap;
        this.filter = filter;
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
                    String content = Util.clearRule(line);
                    if (StrUtil.isNotBlank(content)) {
                        if (!filter.mightContain(line)) {
                            filter.put(line);

                            if (Util.validRule(content, RuleType.DOMAIN)) {
                                typeFileMap.getOrDefault(RuleType.DOMAIN, Collections.emptySet())
                                        .forEach(item -> Util.safePut(fileDataMap, item, line));
                                log.debug("Domain rule: {}", line);

                            } else if (Util.validRule(content, RuleType.HOSTS)) {
                                typeFileMap.getOrDefault(RuleType.HOSTS, Collections.emptySet())
                                        .forEach(item -> Util.safePut(fileDataMap, item, line));
                                log.debug("Hosts rule: {}", line);

                            } else if (Util.validRule(content, RuleType.MODIFY)) {

                                if (Util.validRule(content, RuleType.REGEX)) {
                                    typeFileMap.getOrDefault(RuleType.REGEX, Collections.emptySet())
                                            .forEach(item -> Util.safePut(fileDataMap, item, line));
                                    log.debug("Regex rule: {}", line);

                                } else {

                                    typeFileMap.getOrDefault(RuleType.MODIFY, Collections.emptySet())
                                            .forEach(item -> Util.safePut(fileDataMap, item, line));
                                    log.debug("Modifier rule: {}", line);
                                }
                            } else {
                                invalid.getAndSet(invalid.get() + 1);
                                log.debug("Invalid rule: {}", line);
                            }
                        }
                    }else {
                        invalid.getAndSet(invalid.get() + 1);
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
