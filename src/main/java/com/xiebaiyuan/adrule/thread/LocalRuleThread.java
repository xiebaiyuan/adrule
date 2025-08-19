package com.xiebaiyuan.adrule.thread;

import cn.hutool.core.io.FileUtil;
import com.google.common.hash.BloomFilter;
import com.xiebaiyuan.adrule.enums.RuleType;
import com.xiebaiyuan.adrule.stats.RuleStatsCollector;

import java.io.File;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;

/**
 * Local rule processing
 *
 * @author ChengFengsheng on 2022/7/7
 */
public class LocalRuleThread extends AbstractRuleThread {


    public LocalRuleThread(String ruleUrl, Map<RuleType, Set<File>> typeFileMap, BloomFilter<String> filter, RuleStatsCollector statsCollector) {
        super(ruleUrl, typeFileMap, filter, statsCollector);
    }

    @Override
    InputStream getContentStream() {
        return FileUtil.getInputStream(getRuleUrl());
    }
}
