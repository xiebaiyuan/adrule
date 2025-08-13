package com.xiebaiyuan.adrule.config;

import lombok.Data;
import com.xiebaiyuan.adrule.enums.RuleType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Output configuration
 *
 * @author Chengfs on 2022/9/19
 */
@Data
@Component
@ConfigurationProperties(prefix = "application.output")
public class OutputConfig {

    /**
     * Output file path
     */
    private String path;

    /**
     * Output file list
     */
    private Map<String, List<RuleType>> files;
}
