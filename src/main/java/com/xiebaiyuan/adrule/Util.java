package com.xiebaiyuan.adrule;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import com.xiebaiyuan.adrule.enums.RuleType;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static cn.hutool.core.thread.ThreadUtil.sleep;

/**
 * @author Chengfs on 2022/9/19
 */
@Slf4j
public class Util {

    /**
     * Write collection content to file with lock protection
     *
     * @param file    target file
     * @param content content collection
     */
    public static void write(File file, Collection<String> content) {
        if (CollUtil.isNotEmpty(content)) {
            try (RandomAccessFile accessFile = new RandomAccessFile(file, "rw");
                 FileChannel channel = accessFile.getChannel()) {
                // Lock file for writing, sleep if lock cannot be acquired
                while (true) {
                    try {
                        channel.tryLock();
                        break;
                    } catch (Exception e) {
                        sleep(1000);
                    }
                }
                accessFile.seek(accessFile.length());
                accessFile.write((CollUtil.join(content, StrUtil.CRLF)).getBytes(StandardCharsets.UTF_8));
                accessFile.write(StrUtil.CRLF.getBytes(StandardCharsets.UTF_8));
            } catch (IOException ioException) {
                log.error("Error writing to file, {} => {}", file.getPath(), ioException.getMessage());
            }
        }
    }

    /**
     * Create file by path, delete and recreate if exists
     *
     * @param path file path
     * @return {@link File}
     */
    public static File createFile(String path) {
        path = FileUtil.normalize(path);
        if (!FileUtil.isAbsolutePath(path)) {
            path = Constant.ROOT_PATH + File.separator + path;
        }
        File file = FileUtil.file(FileUtil.normalize(path));
        return createFile(file);
    }

    public static File createFile(File file) {
        if (FileUtil.exist(file)) {
            FileUtil.del(file);
        }
        FileUtil.touch(file);
        FileUtil.appendUtf8String(StrUtil.format(Constant.UPDATE,
                DateTime.now().toString(DatePattern.NORM_DATETIME_PATTERN)), file);
        FileUtil.appendUtf8String(Constant.REPO, file);
        return file;
    }

    /**
     * Validate if content is a rule of specified type
     *
     * @param rule content
     * @param type rule type
     * @return validation result
     */
    public static boolean validRule(String rule, RuleType type) {

        // Match identifier, must match when identifier exists
        if (ArrayUtil.isNotEmpty(type.getIdentify())) {
            if (!StrUtil.containsAny(rule, type.getIdentify())) {
                return false;
            }
        }

        if (ArrayUtil.isNotEmpty(type.getMatch()) || ArrayUtil.isNotEmpty(type.getExclude())) {
            // Match positive rules, need to satisfy at least one
            if (ArrayUtil.isNotEmpty(type.getMatch())) {
                boolean math = false;
                for (String pattern : type.getMatch()) {
                    if (ReUtil.contains(pattern, rule)) {
                        return true;
                    }
                }
                if (!math) {
                    return false;
                }
            }

            // Match negative rules, need to satisfy none
            if (ArrayUtil.isNotEmpty(type.getExclude())) {
                for (String pattern : type.getExclude()) {
                    if (ReUtil.contains(pattern, rule)) {
                        return false;
                    }
                }
                return true;
            }

            return false;
        } else {
            return true;
        }

    }

    /**
     * Clean rule string, remove spaces and certain specific symbols
     *
     * @param content content
     * @return cleaned result
     */
    public static String clearRule(String content) {
        content = StrUtil.isNotBlank(content) ? StrUtil.trim(content) : StrUtil.EMPTY;

        // Validity check
        if (ReUtil.contains(Constant.EFFICIENT_REGEX, content)) {
            return StrUtil.EMPTY;
        }

        // Remove basic modifier symbols from start/end
        if (ReUtil.contains(Constant.BASIC_MODIFY_REGEX, content)) {
           content = ReUtil.replaceAll(content, Constant.BASIC_MODIFY_REGEX, StrUtil.EMPTY);
        }

        return StrUtil.trim(content);
    }

    public static <K, T> void safePut(Map<K, Set<T>> map, K key, T val) {
        if (map.containsKey(key)) {
            map.get(key).add(val);
        } else {
            map.put(key, CollUtil.newHashSet(val));
        }
    }

}
