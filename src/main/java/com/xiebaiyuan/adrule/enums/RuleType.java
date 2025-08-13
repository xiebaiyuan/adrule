package com.xiebaiyuan.adrule.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Chengfs on 2022/9/19
 */
@Getter
@AllArgsConstructor
public enum RuleType {

    /**
     * Domain rules, like xxx.com, xx.oo.com
     */
    DOMAIN("Domain rules", true, null, new String[]{"^([\\w,\\d,-]+\\.)+[\\w,\\d,-]+(\\^$)?$"}, null),

    /**
     * Hosts rules
     */
    HOSTS("Hosts rules", true, null, new String[]{"^\\d+\\.\\d+\\.\\d+\\.\\d+\\s+.*$"}, null),

    /**
     * Regex rules, including modifier rules
     */
    REGEX("Regex rules", true, null, new String[]{},
            new String[]{"[/,#,&,=,:]", "^[\\*,@,\\-,_,\\.,&,\\?]","[\\$][^\\s]", "[\\^][^\\s]"}),


    /**
     * Modifier rules, not supported by AdGuardHome
     */
    MODIFY("Modifier rules", false, null, null, null)
    ;


    /**
     * Description
     */
    private final String desc;

    /**
     * Support status, true means AdGuardHome supports it
     */
    private final boolean usually;

    /**
     * Identification markers, pass if contains any
     */
    private final String[] identify;

    /**
     * Positive regex patterns, pass if matches any
     */
    private final String[] match;

    /**
     * Exclude regex patterns, pass if matches none
     */
    private final String[] exclude;
}
