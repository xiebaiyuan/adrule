package com.xiebaiyuan.adrule;

import java.io.File;

public class Constant {

    public static final String ROOT_PATH = System.getProperty("user.dir");

    public static final String HEADER_TEMPLATE = 
            "################################################################################\r\n" +
            "#      ___    ____        ____          _          _____               _       #\r\n" +
            "#     / _ \\  |  _ \\      |  _ \\  _   _ | |   ___  | ____|  _ __    __ _ (_) _ __ #\r\n" +
            "#    | |_| | | | | |     | |_) || | | || |  / _ \\ |  _|  | '_ \\  / _` || || '_ \\#\r\n" +
            "#    |  _  | | |_| |     |  _ < | |_| || | |  __/ | |___ | | | || (_| || || | | |#\r\n" +
            "#    |_| |_| |____/      |_| \\_\\ \\__,_||_|  \\___| |_____||_| |_| \\__, ||_||_| |_|#\r\n" +
            "#                                                                |___/          #\r\n" +
            "################################################################################\r\n" +
            "#                                                                              #\r\n" +
            "#                    🚀 AdGuard/AdGuardHome Rule Engine 🚀                     #\r\n" +
            "#                   High-Performance Ad Filtering Rule Aggregator             #\r\n" +
            "#                                                                              #\r\n" +
            "################################################################################\r\n" +
            "#                                                                              #\r\n" +
            "# ⚡ Project Information                                                      #\r\n" +
            "# Project Name: ADG-Rule Engine                                                #\r\n" +
            "# Description: Multi-source ad filtering rule aggregation & deduplication     #\r\n" +
            "# License: MIT License                                                         #\r\n" +
            "#                                                                              #\r\n" +
            "# ⏰ Generation Information                                                   #\r\n" +
            "# Generated Time: {}                                       #\r\n" +
            "# Processing Time: {}ms                                         #\r\n" +
            "# System Info: Java {} | CPU Cores: {} | Memory: {}MB            #\r\n" +
            "#                                                                              #\r\n" +
            "# 📊 Statistics Information                                                   #\r\n" +
            "# Upstream Sources: {} sources                                      #\r\n" +
            "# Total Rules: {} rules                                          #\r\n" +
            "# Domain Rules: {} rules                                         #\r\n" +
            "# Regex Rules: {} rules                                          #\r\n" +
            "# Hosts Rules: {} rules                                          #\r\n" +
            "# Modifier Rules: {} rules                                       #\r\n" +
            "# AdGuardHome Rules: {} rules                                     #\r\n" +
            "# Deduplication Rate: {}%                                        #\r\n" +
            "#                                                                              #\r\n" +
            "# 🔧 Technical Features                                                       #\r\n" +
            "# ✓ Bloom Filter Deduplication                                                #\r\n" +
            "# ✓ Multi-threaded Processing                                                 #\r\n" +
            "# ✓ Intelligent Rule Classification                                           #\r\n" +
            "# ✓ Memory Optimized Algorithm                                                #\r\n" +
            "# ✓ Automatic Error Recovery                                                  #\r\n" +
            "#                                                                              #\r\n" +
            "# 🌐 Supported Rule Formats                                                   #\r\n" +
            "# • AdGuard Format                                                            #\r\n" +
            "# • AdBlock Plus Format                                                       #\r\n" +
            "# • Hosts File Format                                                         #\r\n" +
            "# • Regular Expression Format                                                 #\r\n" +
            "# • Domain List Format                                                        #\r\n" +
            "#                                                                              #\r\n" +
            "# 📋 Upstream Sources List                                                    #\r\n" +
            "{}#                                                                              #\r\n" +
            "# ⚠️  IMPORTANT DISCLAIMER                                                    #\r\n" +
            "# ALL RULES ARE SOURCED FROM UPSTREAM PROVIDERS AND NOT CREATED BY US         #\r\n" +
            "# THIS COLLECTION IS FOR EDUCATIONAL AND LEARNING PURPOSES ONLY              #\r\n" +
            "# WE DO NOT TAKE RESPONSIBILITY FOR ANY ISSUES CAUSED BY USING THESE RULES    #\r\n" +
            "# RULES ARE AUTO-UPDATED EVERY 12 HOURS FROM PUBLIC FILTER LISTS             #\r\n" +
            "# IF YOU ENCOUNTER FALSE POSITIVES, PLEASE HANDLE THEM MANUALLY               #\r\n" +
            "#                                                                              #\r\n" +
            "# 🎯 Usage Instructions                                                       #\r\n" +
            "# AdGuard Home: Add this file's URL to your filter lists                     #\r\n" +
            "# AdGuard Client: Import this file into user rules                           #\r\n" +
            "# Pi-hole: Import domain rules into blacklist                                #\r\n" +
            "# Other Software: Choose appropriate file format based on support            #\r\n" +
            "#                                                                              #\r\n" +
            "################################################################################\r\n" +
            "\r\n";

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
