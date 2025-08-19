package com.xiebaiyuan.adrule.model;

import lombok.Data;
import lombok.Builder;

import java.util.List;

/**
 * 规则统计信息
 * 
 * @author xiebaiyuan
 */
@Data
@Builder
public class RuleStats {
    
    /** 上游规则源列表 */
    private List<String> upstreamSources;
    
    /** 上游源数量 */
    private int upstreamCount;
    
    /** 总规则数 */
    private long totalRules;
    
    /** 域名规则数 */
    private long domainRules;
    
    /** 正则规则数 */
    private long regexRules;
    
    /** Hosts规则数 */
    private long hostsRules;
    
    /** 修饰符规则数 */
    private long modifyRules;
    
    /** AdGuardHome规则数 (DOMAIN+REGEX) */
    private long adghRules;
    
    /** 处理耗时（毫秒） */
    private long processingTime;
    
    /** 去重比例 */
    private double deduplicationRate;
    
    /** 原始规则总数（去重前） */
    private long originalRulesCount;
    
    /** 无效规则数 */
    private long invalidRules;
}
