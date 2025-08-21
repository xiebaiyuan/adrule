package com.xiebaiyuan.adrule;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 域名列表提取器：从domain.txt中提取deny和allow列表
 * 独立运行版本，不依赖外部库
 * 
 * 规则说明：
 * 1. "@@" 开头的规则是白名单规则 -> domain_allow.txt
 * 2. "-" 开头的规则是拦截规则 -> domain_deny.txt
 * 3. "||" 开头的规则是拦截规则 -> domain_deny.txt
 * 4. 其他规则，如普通域名，也是拦截规则 -> domain_deny.txt
 */
public class SimpleDomainExtractor {

    private static final String DOMAIN_FILENAME = "domain.txt";
    private static final String DOMAIN_DENY_FILENAME = "domain_deny.txt";
    private static final String DOMAIN_ALLOW_FILENAME = "domain_allow.txt";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final String ROOT_PATH = System.getProperty("user.dir");
    private static final String RULE_PATH = ROOT_PATH + File.separator + "rule";

    public static void main(String[] args) {
        System.out.println("开始提取域名黑白名单...");
        long startTime = System.currentTimeMillis();
        
        try {
            extractDomainLists();
            long endTime = System.currentTimeMillis();
            System.out.println("域名列表提取完成，耗时" + (endTime - startTime) + "ms");
            System.out.println("文件已保存到：");
            System.out.println("- " + RULE_PATH + File.separator + DOMAIN_DENY_FILENAME);
            System.out.println("- " + RULE_PATH + File.separator + DOMAIN_ALLOW_FILENAME);
        } catch (Exception e) {
            System.err.println("处理域名分离任务时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 从domain.txt中提取拦截列表和白名单列表
     */
    public static void extractDomainLists() throws IOException {
        String domainFilePath = RULE_PATH + File.separator + DOMAIN_FILENAME;
        String denyFilePath = RULE_PATH + File.separator + DOMAIN_DENY_FILENAME;
        String allowFilePath = RULE_PATH + File.separator + DOMAIN_ALLOW_FILENAME;

        // 确保源文件存在
        Path domainPath = Paths.get(domainFilePath);
        if (!Files.exists(domainPath)) {
            throw new FileNotFoundException("域名源文件不存在: " + domainFilePath);
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
            int headerCount = 0;

            // 逐行读取domain.txt，分别写入deny和allow文件
            while ((line = reader.readLine()) != null) {
                // 跳过注释行和空行
                if (line.trim().isEmpty() || line.startsWith("#")) {
                    // 头部信息中的注释行不计入规则数
                    headerCount++;
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

            System.out.println("拦截规则数量: " + denyCount);
            System.out.println("白名单规则数量: " + allowCount);
            System.out.println("总规则数量: " + (denyCount + allowCount));
            System.out.println("头部信息行数: " + headerCount);
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
        writer.write(String.format("# %s #", centerText("🚀 " + titleCn + " 🚀", 78)));
        writer.newLine();
        writer.write(String.format("# %s #", centerText("🚀 " + titleEn + " 🚀", 78)));
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
    
    /**
     * 文本居中显示
     */
    private static String centerText(String text, int width) {
        if (text == null || width <= 0) {
            return "";
        }
        int textLength = text.length();
        int spaces = width - textLength;
        if (spaces <= 0) {
            return text;
        }
        int leftSpaces = spaces / 2;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < leftSpaces; i++) {
            sb.append(" ");
        }
        sb.append(text);
        while (sb.length() < width) {
            sb.append(" ");
        }
        return sb.toString();
    }
}
