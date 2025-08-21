package com.xiebaiyuan.adrule;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;

/**
 * 域名列表提取器：从domain.txt中提取deny和allow列表
 */
@Slf4j
public class DomainListExtractor {

    private static final String DOMAIN_FILENAME = "domain.txt";
    private static final String DOMAIN_DENY_FILENAME = "domain_deny.txt";
    private static final String DOMAIN_ALLOW_FILENAME = "domain_allow.txt";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        extractDomainLists();
    }

    /**
     * 从domain.txt中提取拦截列表和白名单列表
     */
    public static void extractDomainLists() {
        try {
            log.info("开始提取域名列表...");
            long startTime = System.currentTimeMillis();

            String domainFilePath = Constant.LOCAL_RULE_SUFFIX + File.separator + DOMAIN_FILENAME;
            String denyFilePath = Constant.LOCAL_RULE_SUFFIX + File.separator + DOMAIN_DENY_FILENAME;
            String allowFilePath = Constant.LOCAL_RULE_SUFFIX + File.separator + DOMAIN_ALLOW_FILENAME;

            // 确保源文件存在
            Path domainPath = Paths.get(domainFilePath);
            if (!Files.exists(domainPath)) {
                log.error("域名源文件不存在: {}", domainFilePath);
                return;
            }

            // 创建deny和allow文件的输出流
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                     Files.newInputStream(domainPath), StandardCharsets.UTF_8));
                 BufferedWriter denyWriter = new BufferedWriter(new OutputStreamWriter(
                     Files.newOutputStream(Paths.get(denyFilePath)), StandardCharsets.UTF_8));
                 BufferedWriter allowWriter = new BufferedWriter(new OutputStreamWriter(
                     Files.newOutputStream(Paths.get(allowFilePath)), StandardCharsets.UTF_8))) {

                // 写入文件头部信息
                writeHeader(denyWriter, "域名拦截规则", "Domain Deny Rules");
                writeHeader(allowWriter, "域名白名单规则", "Domain Allow Rules");

                String line;
                int denyCount = 0;
                int allowCount = 0;

                // 逐行读取domain.txt，分别写入deny和allow文件
                while ((line = reader.readLine()) != null) {
                    // 跳过注释行和空行
                    if (line.trim().isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    
                    if (line.startsWith("@@")) {
                        // 白名单规则：以@@开头
                        allowWriter.write(line);
                        allowWriter.newLine();
                        allowCount++;
                    } else if (line.startsWith("-") || line.startsWith("||") || 
                              (!line.startsWith("@@") && !line.startsWith("#") && !line.trim().isEmpty())) {
                        // 拦截规则：以-开头，或者以||开头，或者是普通域名
                        denyWriter.write(line);
                        denyWriter.newLine();
                        denyCount++;
                    }
                }

                // 写入文件底部信息
                writeFooter(denyWriter, denyCount);
                writeFooter(allowWriter, allowCount);

                long endTime = System.currentTimeMillis();
                log.info("域名列表提取完成，耗时{}ms，拦截规则数量: {}，白名单规则数量: {}", 
                    (endTime - startTime), denyCount, allowCount);
            }
        } catch (Exception e) {
            log.error("处理域名分离任务时发生错误", e);
        }
    }

    /**
     * 写入文件头部信息
     */
    private static void writeHeader(BufferedWriter writer, String titleCn, String titleEn) throws IOException {
        writer.write("################################################################################");
        writer.newLine();
        writer.write("#      ___    ____        ____          _          _____               _       #");
        writer.newLine();
        writer.write("#     / _ \\  |  _ \\      |  _ \\  _   _ | |   ___  | ____|  _ __    __ _ (_) _ __ #");
        writer.newLine();
        writer.write("#    | |_| | | | | |     | |_) || | | || |  / _ \\ |  _|  | '_ \\  / _` || || '_ \\#");
        writer.newLine();
        writer.write("#    |  _  | | |_| |     |  _ < | |_| || | |  __/ | |___ | | | || (_| || || | | |#");
        writer.newLine();
        writer.write("#    |_| |_| |____/      |_| \\_\\ \\__,_||_|  \\___| |_____||_| |_| \\__, ||_||_| |_|#");
        writer.newLine();
        writer.write("#                                                                |___/          #");
        writer.newLine();
        writer.write("################################################################################");
        writer.newLine();
        writer.write("#                                                                              #");
        writer.newLine();
        writer.write(String.format("# %-78s #", "🚀 " + titleCn + " 🚀"));
        writer.newLine();
        writer.write(String.format("# %-78s #", "🚀 " + titleEn + " 🚀"));
        writer.newLine();
        writer.write("#                                                                              #");
        writer.newLine();
        writer.write("################################################################################");
        writer.newLine();
        writer.write("#                                                                              #");
        writer.newLine();
        writer.write("# ⏰ 生成信息                                                                  #");
        writer.newLine();
        writer.write(String.format("# 生成时间: %-70s #", DATE_FORMAT.format(new Date())));
        writer.newLine();
        writer.write("#                                                                              #");
        writer.newLine();
        writer.write("# 📋 规则说明                                                                  #");
        writer.newLine();
        writer.write("# • @@ 开头：白名单规则，表示允许访问                                        #");
        writer.newLine();
        writer.write("# • - 开头：拦截规则，表示阻止访问                                            #");
        writer.newLine();
        writer.write("# • || 开头：拦截规则，表示阻止访问域名及其所有子域名                       #");
        writer.newLine();
        writer.write("# • 其他：拦截规则，表示阻止访问                                             #");
        writer.newLine();
        writer.write("#                                                                              #");
        writer.newLine();
        writer.write("################################################################################");
        writer.newLine();
        writer.newLine();
    }

    /**
     * 写入文件底部信息
     */
    private static void writeFooter(BufferedWriter writer, int count) throws IOException {
        writer.newLine();
        writer.write("################################################################################");
        writer.newLine();
        writer.write(String.format("# 总规则数: %-70s #", count + " 条"));
        writer.newLine();
        writer.write("# 生成工具: ADG-Rule Engine                                                    #");
        writer.newLine();
        writer.write("################################################################################");
    }
}
