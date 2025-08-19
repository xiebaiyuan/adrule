package com.xiebaiyuan.adrule;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.thread.ExecutorBuilder;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.xiebaiyuan.adrule.config.OutputConfig;
import com.xiebaiyuan.adrule.config.RuleConfig;
import com.xiebaiyuan.adrule.enums.RuleType;
import com.xiebaiyuan.adrule.stats.RuleStatsCollector;
import com.xiebaiyuan.adrule.thread.LocalRuleThread;
import com.xiebaiyuan.adrule.thread.RemoteRuleThread;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Component
@AllArgsConstructor
@SpringBootApplication
public class AdgRuleApplication implements ApplicationRunner {

    private final static int N = Runtime.getRuntime().availableProcessors();

    private final RuleConfig ruleConfig;

    private final OutputConfig outputConfig;

    private final ThreadPoolExecutor executor = ExecutorBuilder.create()
            .setCorePoolSize(2 * N)
            .setMaxPoolSize(2 * N)
            .setHandler(new ThreadPoolExecutor.CallerRunsPolicy())
            .build();


    @Override
    public void run(ApplicationArguments args) throws Exception {
        TimeInterval interval = DateUtil.timer();

        // 初始化统计收集器
        RuleStatsCollector statsCollector = new RuleStatsCollector();
        
        // 收集所有规则源
        List<String> allSources = new ArrayList<>();
        allSources.addAll(ruleConfig.getRemote());
        allSources.addAll(ruleConfig.getLocal());
        
        // 开始统计
        statsCollector.start(allSources);

        // Initialize and create files based on configuration
        final Map<RuleType, Set<File>> typeFileMap = MapUtil.newHashMap();
        if (!outputConfig.getFiles().isEmpty()) {
            outputConfig.getFiles().forEach((fileName, types) -> {
                File file = Util.createFile(outputConfig.getPath() + File.separator + fileName);
                types.forEach(type -> Util.safePut(typeFileMap, type, Util.createFile(file)));
            });
        }

        // Use Bloom filter for deduplication
        BloomFilter<String> filter = BloomFilter
                .create(Funnels.stringFunnel(Charset.defaultCharset()),
                        1000000,    // Actual about 200k, reserve 2.5x space
                        0.01);

        // Remote rules
        ruleConfig.getRemote().stream()
                .filter(StrUtil::isNotBlank)
                .map(URLUtil::normalize)
                .forEach(e -> executor.execute(new RemoteRuleThread(e, typeFileMap, filter, statsCollector)));
        // Local rules
        ruleConfig.getLocal().stream()
                .filter(StrUtil::isNotBlank)
                .map(e -> {
                    e = FileUtil.normalize(e);
                    if (FileUtil.isAbsolutePath(e)) {
                        return e;
                    }
                    return FileUtil.normalize(Constant.LOCAL_RULE_SUFFIX + File.separator + e);
                })
                .forEach(e -> executor.execute(new LocalRuleThread(e, typeFileMap, filter, statsCollector)));

        while (true) {
            if (executor.getActiveCount() > 0) {
                ThreadUtil.safeSleep(1000);
            } else {
                // 结束统计
                statsCollector.finish();
                
                // 更新所有文件的头部信息
                typeFileMap.values().stream()
                    .flatMap(Set::stream)
                    .forEach(file -> Util.updateFileHeader(file, statsCollector.build()));
                
                log.info("Done! {} ms", interval.intervalMs());
                System.exit(0);
            }
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(AdgRuleApplication.class, args);
    }
}
