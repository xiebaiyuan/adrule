package com.xiebaiyuan.adrule.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "application.rule")
public class RuleConfig {

    /**
     * Remote rules, http or https
     */
    private List<String> remote;

    /**
     * Local rules
     */
    private List<String> local;
}
