package com.xiebaiyuan.adrule;

import com.xiebaiyuan.adrule.enums.RuleType;

/**
 * test method
 * mvn -DskipTests compile exec:java -Dexec.mainClass=com.xiebaiyuan.adrule.TestRegexChecker && sed -n '1,200p' rule/filtered-http-regex.txt
 */
public class TestRegexChecker {
    public static void main(String[] args) {
        String[] base = new String[]{
            "/^https:\\/\\/((a|c)\\.)?[0-9a-f]{56}\\.com$/",
            "/^https:\\/\\/ik\\.imagekit\\.io\\/[a-z0-9]+\\/[a-zA-Z0-9_]+\\.(jpg|png)\\?updatedAt=\\/$/",
            "/^https?:\\/\\/\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\/.*(__cpo=aHR0c)/$"
        };

        String[] ipExamples = new String[]{
            "/139\\.45\\.197\\.2(4[0-9]|5[0-4])/",
            "/94\\.242\\.247\\.(2[0-9]|3[0-2]):/"
        };

        // use simplified safe examples for domain+path patterns
        String[] domainPathExamples = new String[]{
            "/doseofporn.com/path/to/script123.js/",
            "/idnes.cz/aaaaaaaaaaaaaaaaaaaa/bbbbbbbbbbbbbbbbbbbb/cccccccccccccccccccc/dddddddddddddddddd/$subdocument,image,important",
            "/m.realgfporn.com/abc123def456ghi.js/",
            "/torrenttrackerlist.com/wp-content/uploads/abcdefghij/klmnopqrst.js/"
        };

    // merge base + ipExamples into all
    String[] all = new String[base.length + ipExamples.length];
    System.arraycopy(base, 0, all, 0, base.length);
    System.arraycopy(ipExamples, 0, all, base.length, ipExamples.length);

    // merge all + domainPathExamples into inputs
    String[] inputs = new String[all.length + domainPathExamples.length];
    System.arraycopy(all, 0, inputs, 0, all.length);
    System.arraycopy(domainPathExamples, 0, inputs, all.length, domainPathExamples.length);

    for (String r : inputs) {
            if (r == null || r.isEmpty()) continue;
            String cleared = Util.clearRule(r);
            boolean isDomain = Util.validRule(cleared, RuleType.DOMAIN);
            boolean isHosts = Util.validRule(cleared, RuleType.HOSTS);
            boolean isRegex = Util.validRule(cleared, RuleType.REGEX);
            boolean isModify = Util.validRule(cleared, RuleType.MODIFY);
            System.out.println("Rule: " + r);
            System.out.println("  Cleared: " + cleared);
            System.out.println("  DOMAIN=" + isDomain + " HOSTS=" + isHosts + " REGEX=" + isRegex + " MODIFY=" + isModify);
            System.out.println();
        }
    }
}
