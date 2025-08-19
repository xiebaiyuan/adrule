package com.xiebaiyuan.adrule.stats;

import com.xiebaiyuan.adrule.model.RuleStats;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 规则统计收集器
 * 
 * @author xiebaiyuan
 */
@Slf4j
public class RuleStatsCollector {
    
    private final AtomicLong totalRules = new AtomicLong(0);
    private final AtomicLong domainRules = new AtomicLong(0);
    private final AtomicLong regexRules = new AtomicLong(0);
    private final AtomicLong hostsRules = new AtomicLong(0);
    private final AtomicLong modifyRules = new AtomicLong(0);
    private final AtomicLong invalidRules = new AtomicLong(0);
    private final AtomicLong originalRulesCount = new AtomicLong(0);
    
    private List<String> upstreamSources;
    private long startTime;
    private long endTime;
    
    public void start(List<String> sources) {
        this.upstreamSources = sources;
        this.startTime = System.currentTimeMillis();
        log.info("开始统计，上游源数量: {}", sources.size());
    }
    
    public void finish() {
        this.endTime = System.currentTimeMillis();
        log.info("统计完成，耗时: {}ms", getProcessingTime());
    }
    
    public void incrementDomainRules() {
        domainRules.incrementAndGet();
        totalRules.incrementAndGet();
    }
    
    public void incrementRegexRules() {
        regexRules.incrementAndGet();
        totalRules.incrementAndGet();
    }
    
    public void incrementHostsRules() {
        hostsRules.incrementAndGet();
        totalRules.incrementAndGet();
    }
    
    public void incrementModifyRules() {
        modifyRules.incrementAndGet();
        totalRules.incrementAndGet();
    }
    
    public void incrementInvalidRules() {
        invalidRules.incrementAndGet();
    }
    
    public void incrementOriginalRules() {
        originalRulesCount.incrementAndGet();
    }
    
    public long getProcessingTime() {
        return endTime > 0 ? endTime - startTime : System.currentTimeMillis() - startTime;
    }
    
    public double getDeduplicationRate() {
        long original = originalRulesCount.get();
        long total = totalRules.get();
        if (original == 0) return 0.0;
        return ((double) (original - total) / original) * 100.0;
    }
    
    public long getAdghRules() {
        // AdGuardHome 规则 = 域名规则 + 正则规则
        return domainRules.get() + regexRules.get();
    }
    
    public RuleStats build() {
        return RuleStats.builder()
                .upstreamSources(upstreamSources)
                .upstreamCount(upstreamSources != null ? upstreamSources.size() : 0)
                .totalRules(totalRules.get())
                .domainRules(domainRules.get())
                .regexRules(regexRules.get())
                .hostsRules(hostsRules.get())
                .modifyRules(modifyRules.get())
                .adghRules(getAdghRules())
                .processingTime(getProcessingTime())
                .deduplicationRate(getDeduplicationRate())
                .originalRulesCount(originalRulesCount.get())
                .invalidRules(invalidRules.get())
                .build();
    }
}
