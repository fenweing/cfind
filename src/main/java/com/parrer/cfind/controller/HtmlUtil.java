package com.parrer.cfind.controller;

import java.util.regex.Pattern;

public class HtmlUtil {
    private static Pattern SCRIPT_PATTERN=Pattern.compile("<script.*?></scripts>");
    private static Pattern LINK_PATTERN=Pattern.compile("<link.*?href=\"(.+?)\">");
}
