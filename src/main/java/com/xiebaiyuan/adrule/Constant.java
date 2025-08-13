package com.xiebaiyuan.adrule;

import java.io.File;

public class Constant {

    public static final String ROOT_PATH = System.getProperty("user.dir");

    public static final String UPDATE = "# Update time: {}\r\n";

    public static final String REPO = "# Repo URL: AdGuard/AdGuardHome ad filter rules merge/dedup\r\n\r\n###################################   Merged/Deduped from the following rules   ####################################\r\n\r\n# Updates every 12 hours, if there's false positive, please manually resolve\r\n\r\n";

    public static final String LOCAL_RULE_SUFFIX = ROOT_PATH + File.separator + "rule";

    /**
     * Basic validity check regex: lines starting with !, lines wrapped with [], lines starting with special # markers are considered invalid rules
     */
    public static final String EFFICIENT_REGEX = "^!|^#[^#,^@,^%,^\\$]|^\\[.*\\]$";

    /**
     * Regex to remove basic modifier symbols from start/end for rule classification
     * Includes: @@, ||, @@||, / at start, $important, / at end
     */
    public static final String BASIC_MODIFY_REGEX = "^@@\\|\\||^\\|\\||^@@|\\$important$|\\s#[^#]*$";

}
